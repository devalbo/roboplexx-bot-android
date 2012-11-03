package com.roboplexx.android;

import java.io.ByteArrayOutputStream;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.net.http.AndroidHttpClient;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.roboplexx.android.appengine.AppEngineService;
import com.roboplexx.android.appengine.AppEngineService.RoboplexxServiceCommand;
import com.roboplexx.android.appengine.AppEngineServiceThread;
import com.roboplexx.android.http.HttpIoService;
import com.roboplexx.android.http.HttpIoService.HttpIoServiceCommand;

public class RoboplexxMain extends Activity {

  private IRoboplexxService mRoboplexxService;
  private RoboplexxServiceConnection mRoboplexxServiceConnection;
  private RoboplexxServiceConnection mHttpIoServiceConnection;
  private int mControlMode = -1;
  private Handler mHandler = new Handler();


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.roboplexx_main);
    mRoboplexxServiceConnection = new RoboplexxServiceConnection();
    mHttpIoServiceConnection = new RoboplexxServiceConnection();
  }

  @Override
  protected void onResume() {
    super.onResume();

    Intent http_io_service_intent = new Intent(RoboplexxMain.this, HttpIoService.class);
    bindService(http_io_service_intent, mHttpIoServiceConnection, Context.BIND_AUTO_CREATE);

    Intent roboplexx_service_intent = new Intent(RoboplexxMain.this, AppEngineService.class);
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

        //        new PreferenceManager().getSharedPreferencesName();
        SharedPreferences robotInfo = PreferenceManager.getDefaultSharedPreferences(RoboplexxMain.this);
        //        SharedPreferences robotInfo = mRoboplexxService.getSharedPreferences(
        //          RoboplexxServiceThread.ROBOPLEX_ROBOT_INFO_PREFS_ID, Context.MODE_PRIVATE);
        Editor editor = robotInfo.edit();
        editor.putString(getResources().getString(R.string.pref_key_robot_name), 
          getResources().getString(R.string.pref_default_robot_name));
        editor.putString(getString(R.string.pref_key_roboplexx_robot_id), "");
        editor.putString(getString(R.string.pref_key_roboplexx_accountname), "");
        editor.putString(AppEngineServiceThread.ROBOPLEXX_ROBOT_SUBSCRIBE_URL, "");
        editor.commit();
      }
    });

//    boolean roboplexxServiceRunning = ServiceTools.isServiceRunning(this, RoboplexxService.class);
//    boolean roboplexxServiceRunning = false;
//    if (mRoboplexxServiceBound) {
//      roboplexxServiceRunning = mRoboplexxService.pingBinder();
//    }
//    final CheckBox checkBoxConnectToRoboplexx = (CheckBox)findViewById(R.id.checkbox_connect_to_roboplexx);
////        checkBoxConnectToRoboplexx.setChecked(roboplexxServiceRunning);
//    checkBoxConnectToRoboplexx.setOnClickListener(new OnClickListener() {
//
//      public void onClick(View v) {
//        if (checkBoxConnectToRoboplexx.isChecked()) {
//          Intent intent = new Intent(RoboplexxIntro.this, RoboplexxService.class);
//          intent.putExtra("command", RoboplexxServiceCommand.CONNECT_TO_ROBOPLEXX.toString());
//          Toast.makeText(RoboplexxIntro.this, "starting Roboplexx", Toast.LENGTH_SHORT).show();
//          startService(intent);
//
//        } else {
//          Intent intent = new Intent(RoboplexxIntro.this, RoboplexxService.class);
//          intent.putExtra("command", RoboplexxServiceCommand.DISCONNECT_FROM_ROBOPLEXX.toString());
//          Toast.makeText(RoboplexxIntro.this, "stopping Roboplexx", Toast.LENGTH_SHORT).show();
//          startService(intent);
//
//        }
//
//        //        mProgressDialog = ProgressDialog.show(RoboplexxIntro.this, "Connecting", 
//        //          "Loading. Please wait...", true);
//      }
//    });
    
    Spinner spinner = (Spinner) findViewById(R.id.spinner_control_mode);
    // Create an ArrayAdapter using the string array and a default spinner layout
    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
      R.array.control_mode_options, android.R.layout.simple_spinner_item);
    // Specify the layout to use when the list of choices appears
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    // Apply the adapter to the spinner
    spinner.setAdapter(adapter);
    
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
          stopHttpIoServer();
          stopRoboplexxServer();
          break;
          
        case 1:
          stopRoboplexxServer();
          startHttpIoServer();
          break;
          
        case 2:
          stopHttpIoServer();
          startRoboplexxServer();
          break;
          
        default:
          break;
        
        }
      }

      public void onNothingSelected(AdapterView<?> arg0) {
        // TODO Auto-generated method stub
        
      }
    });

//    final Button uploadImageButton = (Button)findViewById(R.id.upload_camera_image);
//    uploadImageButton.setOnClickListener(new OnClickListener() {
//
//      public void onClick(View v) {
//        Thread uploadThread = new Thread() {
//
//          public void run() {
//            try {
//              AndroidHttpClient uploadClient = AndroidHttpClient.newInstance("roboplexx-upload");
//              //        HttpClient httpclient = new DefaultHttpClient();
//              Bitmap digit2 = BitmapFactory.decodeResource(getResources(), R.drawable.digit_2);
//              ByteArrayOutputStream bos = new ByteArrayOutputStream();
//              digit2.compress(CompressFormat.JPEG, 75, bos);
//              byte[] data = bos.toByteArray();
//              ByteArrayBody bab = new ByteArrayBody(data, "digit_2.jpg");
//              MultipartEntity reqEntity = new MultipartEntity(
//                HttpMultipartMode.BROWSER_COMPATIBLE);
//              reqEntity.addPart("robot_latest_image", bab);
//              reqEntity.addPart("robot_id", new StringBody("12345"));
//              HttpPost httppost = new HttpPost(AppEngineServiceThread.ROBOPLEXX_ROOT_URL + "/upload_test");
//              //        entity.addPart(new FilePart("picture", imageFile, null, "image/jpeg"));
//              httppost.setEntity(reqEntity);
//              HttpResponse httpResponse = uploadClient.execute(httppost);
//
//              uploadClient.close();
//              
//              Log.d("Image uploader", "Upload complete: " + httpResponse.getStatusLine());
//
//            } catch (Exception e) {
//              // TODO Auto-generated catch block
//              e.printStackTrace();
//            }
//
//          };
//        };
//        uploadThread.start();
//
//      }
//    });
  }

  @Override
  protected void onPause() {
    super.onPause();
    //    if (mRoboplexxServiceBound) {
    unbindService(mRoboplexxServiceConnection);
    //    }
  }

  public void setStatusText(final String statusText) {
    mHandler.post(new Runnable() {
      public void run() {
        TextView statusInfo = (TextView)findViewById(R.id.status_info);
        statusInfo.setText("Status: " + statusText);
      }
    });
  }

  public void setEmotion(final String emotion) {
    mHandler.post(new Runnable() {
      public void run() {
        TextView statusInfo = (TextView)findViewById(R.id.emotion_info);
        statusInfo.setText("Emotion: " + emotion);
      }
    });   
  }
  
  public void startRoboplexxServer() {
    Toast.makeText(RoboplexxMain.this, "Controlling via roboplexx.com", Toast.LENGTH_SHORT).show();
    Intent intent = new Intent(RoboplexxMain.this, AppEngineService.class);
    intent.putExtra("command", RoboplexxServiceCommand.CONNECT_TO_ROBOPLEXX.toString());
    startService(intent);
  }
  
  public void stopRoboplexxServer() {
    Toast.makeText(RoboplexxMain.this, "Stopping control via roboplexx.com", Toast.LENGTH_SHORT).show();
    Intent intent = new Intent(RoboplexxMain.this, AppEngineService.class);
    intent.putExtra("command", RoboplexxServiceCommand.DISCONNECT_FROM_ROBOPLEXX.toString());
    startService(intent);
  }
  
  public void startHttpIoServer() {
    Toast.makeText(RoboplexxMain.this, "Controlling via LAN", Toast.LENGTH_SHORT).show();
    Intent intent = new Intent(RoboplexxMain.this, HttpIoService.class);
    intent.putExtra("command", HttpIoServiceCommand.START_SERVER.toString());
    startService(intent);
  }
  
  public void stopHttpIoServer() {
    Toast.makeText(RoboplexxMain.this, "Stopping control via LAN", Toast.LENGTH_SHORT).show();
    Intent intent = new Intent(RoboplexxMain.this, HttpIoService.class);
    intent.putExtra("command", HttpIoServiceCommand.STOP_SERVER.toString());
    startService(intent);
  }
  
  /** Defines callbacks for service binding, passed to bindService() */
  class RoboplexxServiceConnection implements ServiceConnection {

    public void onServiceConnected(ComponentName className, IBinder service) {
      // We've bound to RoboplexxService, cast the IBinder and get RoboplexxService instance
      RoboplexxBinder binder = (RoboplexxBinder) service;
      mRoboplexxService = binder.getService();
      //      mRoboplexxServiceBound = true;

      final TextView statusText = (TextView)findViewById(R.id.text_roboplexx_status);
      final TextView leftSpeedText = (TextView)findViewById(R.id.text_robot_speed_left);
      final TextView rightSpeedText = (TextView)findViewById(R.id.text_robot_speed_right);
      final TextView emotionText = (TextView)findViewById(R.id.emotion_info);
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

        public void updateEmotion(String emotion) {
          emotionText.setText(emotion);
        }
      });
    }

    public void onServiceDisconnected(ComponentName arg0) {
      //      mRoboplexxServiceBound = false;
      mRoboplexxService.setRoboplexxMonitor(null);
    }
  };

}
