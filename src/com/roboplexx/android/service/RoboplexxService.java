package com.roboplexx.android.service;

import java.io.IOException;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.os.IBinder;

import com.roboplexx.android.IRoboplexxMonitor;
import com.roboplexx.android.IRoboplexxService;
import com.roboplexx.android.RoboplexxBinder;
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
      initRomoInterface();
      mRoboplexxThread = new AppEngineServer(this);
      mRoboplexxThread.start();

      break;

    case START_HTTP_SERVER:
      try {
        clearConnectionInfo();
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
        shutdownRomoInterface();
        if (mRoboplexxThread != null) {
          mRoboplexxThread.terminate();
        }
        if (mHttpIoServer != null) {
          mHttpIoServer.stop();
        }
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
      setStatusText("Initializing Romo audio");
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
  
  public void stopMovement() {
    if (mCommandInterface != null) {
      mCommandInterface.playMotorCommand(128, 128);
    }
  }

  public String getStatusMessage() {
    if (mRoboplexxThread != null) {
      return mRoboplexxThread.getStatus();
    }
    return "Not started";
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

  public void setStatus(String string) {
    if (mRoboplexxMonitor != null) {
      mRoboplexxMonitor.updateRoboplexxStatus(string);
    }
  }

  public void setStatusText(String statusText) {
    if (mRoboplexxMonitor != null) {
      mRoboplexxMonitor.updateRoboplexxStatus(statusText);
    }
  }

  public void setEmotion(String emotion) {
    if (mRoboplexxMonitor != null) {
      mRoboplexxMonitor.updateEmotion(emotion);
    }
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

  public void setConnectionInfo(String ipString) {
    if (mRoboplexxMonitor != null) {
      mRoboplexxMonitor.setConnectionInfo(ipString);
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