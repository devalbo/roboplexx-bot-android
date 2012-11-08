package com.roboplexx.android;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.roboplexx.android.service.RoboplexxService;
import com.roboplexx.android.service.RoboplexxService.RoboplexxServiceCommand;
import com.roboplexx.android.service.appengine.AppEngineServer;

public class RoboplexxMain extends Activity {

  private IRoboplexxService mRoboplexxService;
  private RoboplexxServiceConnection mRoboplexxServiceConnection;
  private int mControlMode = -1;
  private Handler mHandler = new Handler();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.roboplexx_main);
    mRoboplexxServiceConnection = new RoboplexxServiceConnection();
  }

  @Override
  protected void onResume() {
    super.onResume();

    Intent roboplexx_service_intent = new Intent(RoboplexxMain.this, RoboplexxService.class);
    bindService(roboplexx_service_intent, mRoboplexxServiceConnection, Context.BIND_AUTO_CREATE);

    Button robotSettingsButton = (Button)findViewById(R.id.button_robot_settings);
    robotSettingsButton.setOnClickListener(new OnClickListener() {

      public void onClick(View v) {
        Intent intent = new Intent(RoboplexxMain.this, RoboplexxPreferences.class);
        startActivity(intent);
      }
    });

    Button resetRobotSettingsButton = (Button)findViewById(R.id.button_robot_settings_reset);
    resetRobotSettingsButton.setOnClickListener(new OnClickListener() {

      public void onClick(View v) {

        SharedPreferences robotInfo = PreferenceManager.getDefaultSharedPreferences(RoboplexxMain.this);
        Editor editor = robotInfo.edit();
        editor.putString(getResources().getString(R.string.pref_key_robot_name), 
          getResources().getString(R.string.pref_default_robot_name));
        editor.putString(getString(R.string.pref_key_roboplexx_robot_id), "");
        editor.putString(getString(R.string.pref_key_roboplexx_accountname), "");
        editor.putString(AppEngineServer.ROBOPLEXX_ROBOT_SUBSCRIBE_URL, "");
        editor.commit();
      }
    });

    final Spinner spinner = (Spinner) findViewById(R.id.spinner_control_mode);
    // Create an ArrayAdapter using the string array and a default spinner layout
    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
      R.array.control_mode_options, android.R.layout.simple_spinner_item);
    // Specify the layout to use when the list of choices appears
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    // Apply the adapter to the spinner
    spinner.setAdapter(adapter);

  }

  @Override
  protected void onPause() {
    super.onPause();
    unbindService(mRoboplexxServiceConnection);
  }

  public void startRoboplexxServer() {
    Toast.makeText(RoboplexxMain.this, "Controlling via roboplexx.com", Toast.LENGTH_SHORT).show();
    Intent intent = new Intent(RoboplexxMain.this, RoboplexxService.class);
    intent.putExtra("command", RoboplexxServiceCommand.CONNECT_TO_ROBOPLEXX.toString());
    startService(intent);
  }

  public void startHttpIoServer() {
    Toast.makeText(RoboplexxMain.this, "Controlling via LAN", Toast.LENGTH_SHORT).show();
    Intent intent = new Intent(RoboplexxMain.this, RoboplexxService.class);
    intent.putExtra("command", RoboplexxServiceCommand.START_HTTP_SERVER.toString());
    startService(intent);
  }

  public void stopServers() {
    Toast.makeText(RoboplexxMain.this, "Stopping servers", Toast.LENGTH_SHORT).show();
    Intent intent = new Intent(RoboplexxMain.this, RoboplexxService.class);
    intent.putExtra("command", RoboplexxServiceCommand.STOP_SERVING.toString());
    startService(intent);
  }
  
  public void refreshFieldsFromService() {
    final TextView statusText = (TextView)findViewById(R.id.text_roboplexx_status);
    final TextView leftSpeedText = (TextView)findViewById(R.id.text_robot_speed_left);
    final TextView rightSpeedText = (TextView)findViewById(R.id.text_robot_speed_right);
    final TextView emotionText = (TextView)findViewById(R.id.emotion_info);
    final TextView connectionText = (TextView)findViewById(R.id.conn_info);

    statusText.setText(mRoboplexxService.getStatusMessage());
    leftSpeedText.setText(String.format("%.2f", mRoboplexxService.getRobotLeftMotorSpeed()));
    rightSpeedText.setText(String.format("%.2f", mRoboplexxService.getRobotRightMotorSpeed()));
    emotionText.setText("Emotion: " + mRoboplexxService.getEmotion());
    connectionText.setText(mRoboplexxService.getConnectionInfo());
  }

  /** Defines callbacks for service binding, passed to bindService() */
  class RoboplexxServiceConnection implements ServiceConnection {

    public void onServiceConnected(ComponentName className, IBinder service) {
      // We've bound to RoboplexxService, cast the IBinder and get RoboplexxService instance
      RoboplexxBinder binder = (RoboplexxBinder) service;
      mRoboplexxService = binder.getService();

      final TextView statusText = (TextView)findViewById(R.id.text_roboplexx_status);
      final TextView leftSpeedText = (TextView)findViewById(R.id.text_robot_speed_left);
      final TextView rightSpeedText = (TextView)findViewById(R.id.text_robot_speed_right);
      final TextView emotionText = (TextView)findViewById(R.id.emotion_info);
      final TextView connectionText = (TextView)findViewById(R.id.conn_info);
      statusText.setText(mRoboplexxService.getStatusMessage());

      mRoboplexxService.setRoboplexxMonitor(new IRoboplexxMonitor() {

        public void updateRoboplexxStatus(final String status) {
          mHandler.post(new Runnable() {

            public void run() {
              statusText.setText(status);
            }

          });
        }

        public void updateRobotSpeeds(final double left_speed, final double right_speed) {
          boolean postStatus = mHandler.post(new Runnable() {

            public void run() {
              leftSpeedText.setText(String.format("%.2f", left_speed));
              rightSpeedText.setText(String.format("%.2f", right_speed));
            }

          });
          if (!postStatus) {
            System.out.println("Can't show updated robot speed :(");
          }
        }

        public void updateEmotion(final String emotion) {
          mHandler.post(new Runnable() {

            public void run() {
              emotionText.setText("Emotion: " + emotion);
            }

          });
        }

        @Override
        public void setConnectionInfo(final String ipString) {
          mHandler.post(new Runnable() {

            public void run() {
              connectionText.setText(ipString);
            }

          });
        }
      });

      mControlMode = mRoboplexxService.getControlModeIndex();
      final Spinner spinner = (Spinner) findViewById(R.id.spinner_control_mode);
      spinner.setSelection(mRoboplexxService.getControlModeIndex());

      mHandler.post(new Runnable() {

        public void run() {

          spinner.setOnItemSelectedListener(new OnItemSelectedListener() {

            public void onItemSelected(AdapterView<?> parent, View arg1, int arg2,
                long arg3) 
            {
              int selectedControlMode = (int)parent.getSelectedItemId();
              if (selectedControlMode == mControlMode) {
                return;
              }

              mControlMode = selectedControlMode;
              switch (mControlMode) {
              case 0:
                stopServers();
                break;

              case 1:
                startHttpIoServer();
                break;

              case 2:
                startRoboplexxServer();
                break;

              default:
                break;

              }
            }

            public void onNothingSelected(AdapterView<?> arg0) {

            }
          });
        }
      });
      
      mHandler.post(new Runnable() {

        @Override
        public void run() {
          refreshFieldsFromService();          
        }
      });
    }

    public void onServiceDisconnected(ComponentName arg0) {
      mRoboplexxService.setRoboplexxMonitor(null);
      mRoboplexxService = null;
    }
  };

}
