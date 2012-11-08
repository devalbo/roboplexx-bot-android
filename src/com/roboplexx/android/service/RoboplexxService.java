package com.roboplexx.android.service;

import java.io.IOException;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.IBinder;
import android.widget.RemoteViews;

import com.roboplexx.android.IRoboplexxMonitor;
import com.roboplexx.android.IRoboplexxService;
import com.roboplexx.android.R;
import com.roboplexx.android.RoboplexxBinder;
import com.roboplexx.android.RoboplexxMain;
import com.roboplexx.android.romo.RomoUtil;
import com.roboplexx.android.service.appengine.AppEngineServer;
import com.roboplexx.android.service.http.HttpIoServer;
import com.romotive.library.RomoCommandInterface;

public class RoboplexxService extends Service implements IRoboplexxService {

  public enum RoboplexxServiceCommand {
    NONE,
    CONNECT_TO_ROBOPLEXX,
    START_HTTP_SERVER,
    STOP_SERVING,
  };

  private int _portNumber = 8080;

  private AppEngineServer mRoboplexxThread = null;
  private HttpIoServer mHttpIoServer = null;
  private IRoboplexxMonitor mRoboplexxMonitor = null;
  private RomoCommandInterface mCommandInterface = null;
  private Notification mNotification;

  private String mConnectionString = "";
  private String mLastStatus = "Disconnected";
  private String mLastEmotion = "Normal";
  private double _leftMotorSpeedPercent = 0.0;
  private double _rightMotorSpeedPercent = 0.0;

  // Binder given to clients
  private final IBinder mBinder = new RoboplexxServiceBinder();

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    RoboplexxServiceCommand command = RoboplexxServiceCommand.NONE;
    if (intent != null && 
        intent.getExtras() != null &&
        intent.getExtras().containsKey("command")) 
    {
      command = RoboplexxServiceCommand.valueOf(intent.getExtras().getString("command"));
    }

    switch (command) {
    case CONNECT_TO_ROBOPLEXX:
      clearConnectionInfo();
      initRoboplexxNotification();
      initRomoInterface();
      mRoboplexxThread = new AppEngineServer(this);
      mRoboplexxThread.start();

      break;

    case START_HTTP_SERVER:
      try {
        clearConnectionInfo();
        initRoboplexxNotification();
        initRomoInterface();
        setStatus("Starting HTTP IO server");
        mHttpIoServer = new HttpIoServer(_portNumber, this);

      } catch (IOException e) {
        e.printStackTrace();
        setStatus("HTTP IO server error: " + e.getLocalizedMessage());
      }

      break;

    case STOP_SERVING:
      try {
        clearConnectionInfo();
        cancelRoboplexxNotification();
        if (mRoboplexxThread != null) {
          mRoboplexxThread.terminate();
        }
        if (mHttpIoServer != null) {
          mHttpIoServer.stop();
        }
        shutdownRomoInterface();
        setStatus("Disconnected");
        
      } catch (Exception e) {
        e.printStackTrace();
        setStatus("Error shutting down: " + e.getLocalizedMessage());

      } finally {
        mRoboplexxThread = null;
        mHttpIoServer = null;
        stopSelf();
      }

      break;

    case NONE:
    default:
      break;
    }

    // If we get killed, after returning from here, restart
    return START_REDELIVER_INTENT;
  }

  private void clearConnectionInfo() {
    if (mRoboplexxMonitor != null) {
      mRoboplexxMonitor.setConnectionInfo("Connection: None");
    }
  }

  private void initRomoInterface() {
    if (mCommandInterface == null) {
      setStatus("Initializing Romo audio");
      mCommandInterface = new RomoCommandInterface();
      AudioManager manager = (AudioManager) getSystemService(Service.AUDIO_SERVICE);
      manager.setStreamVolume(AudioManager.STREAM_MUSIC, manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
    }
  }

  private void shutdownRomoInterface() {
    if (mCommandInterface != null) {
//      mCommandInterface.shutdown();
      mCommandInterface = null;
    }
  }
  
  private void initRoboplexxNotification() {
    final NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

    int notificationIcon = R.drawable.notification;
    if (mNotification == null) {
      mNotification = new Notification();
      startForeground(R.layout.status_bar_notification, mNotification);
   }
    Notification notif = mNotification;

    // This is who should be launched if the user selects our notification.
    notif.contentIntent = PendingIntent.getActivity(this, 0,
        new Intent(this, RoboplexxMain.class), 0);

    // the icon for the status bar
    notif.icon = notificationIcon;

    // our custom view
    RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.status_bar_notification);
    contentView.setImageViewResource(R.id.icon, notificationIcon);
    notif.contentView = contentView;
    notif.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
    
    String notificationText = mConnectionString;
    notif.setLatestEventInfo(getApplicationContext(), "Roboplexx Running", notificationText, notif.contentIntent);

    // we use a string id because is a unique number.  we use it later to cancel the
    // notification
    notificationManager.notify(R.layout.status_bar_notification, notif);
  }
  
  private void cancelRoboplexxNotification() {
    final NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
    notificationManager.cancel(R.layout.status_bar_notification);
    mNotification = null;
    stopForeground(true);
  }
  
  public void stopMovement() {
    if (mCommandInterface != null) {
      mCommandInterface.playMotorCommand(128, 128);
    }
  }

  public String getStatusMessage() {
    return mLastStatus;
  }
  
  public int getControlModeIndex() {
    if (mRoboplexxThread != null) {
      return 2;
    }
    if (mHttpIoServer != null) {
      return 1;
    }
    return 0;
  }
  
  public void setRoboplexxMonitor(IRoboplexxMonitor iRoboplexxListener) {
    mRoboplexxMonitor = iRoboplexxListener;
  }

  public void setStatus(String statusText) {
    if (mRoboplexxMonitor != null) {
      mRoboplexxMonitor.updateRoboplexxStatus(statusText);
    }
    mLastStatus = statusText;
  }

  public void setEmotion(String emotion) {
    if (mRoboplexxMonitor != null) {
      mRoboplexxMonitor.updateEmotion(emotion);
    }
    mLastEmotion = emotion;
  }

  public void setRobotMotorSpeeds(double left_speed, double right_speed) {
    if (mRoboplexxMonitor != null) {
      _leftMotorSpeedPercent = left_speed;
      _rightMotorSpeedPercent = right_speed;
      int leftMotorSpeed = RomoUtil.convertSpeedPercentToCommand(left_speed);
      int rightMotorSpeed = RomoUtil.convertSpeedPercentToCommand(right_speed);
      mCommandInterface.playMotorCommand(leftMotorSpeed, rightMotorSpeed);
      mRoboplexxMonitor.updateRobotSpeeds(left_speed, right_speed);
    }
  }
  
  public double getRobotLeftMotorSpeed() {
    return _leftMotorSpeedPercent;
  }
  
  public double getRobotRightMotorSpeed() {
    return _rightMotorSpeedPercent;
  }
  
  public String getEmotion() {
    return mLastEmotion;
  }

  public String getConnectionInfo() {
    return mConnectionString;
  }


  public void setConnectionInfo(String ipString) {
    if (mRoboplexxMonitor != null) {
      mConnectionString = ipString;
      mRoboplexxMonitor.setConnectionInfo(mConnectionString);
      initRoboplexxNotification();
    }
  }
  
  public String getMotorReport() {
    return "L: " + _leftMotorSpeedPercent + "%  <<->>  R: " + _rightMotorSpeedPercent + " %";
  }

  @Override
  public IBinder onBind(Intent intent) {
    return mBinder;
  }

  class RoboplexxServiceBinder extends RoboplexxBinder {

    public IRoboplexxService getService() {
      // Return this instance of RoboplexxBinder so clients can call public methods
      return RoboplexxService.this;
    }

  }

}