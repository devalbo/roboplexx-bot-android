package com.roboplexx.android.appengine;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.roboplexx.android.IRoboplexxMonitor;
import com.roboplexx.android.IRoboplexxService;
import com.roboplexx.android.RoboplexxBinder;

public class AppEngineService extends Service implements IRoboplexxService {

  public enum RoboplexxServiceCommand {
    NONE,
    CONNECT_TO_ROBOPLEXX,
    DISCONNECT_FROM_ROBOPLEXX
  };

  private AppEngineServiceThread mRoboplexxThread = null;
  private IRoboplexxMonitor mRoboplexxMonitor = null;

  // Binder given to clients
  private final IBinder mBinder = new AppEngineBinder();

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
//      Toast.makeText(this, "connecting to Roboplexx", Toast.LENGTH_SHORT).show();
      mRoboplexxThread = new AppEngineServiceThread(this);
      mRoboplexxThread.start();

      //      Notification notification = new Notification();
      //      startForeground(R.layout.status_bar_notification, notification);

      break;
      
    case DISCONNECT_FROM_ROBOPLEXX:
//      Toast.makeText(this, "disconnecting from Roboplexx", Toast.LENGTH_SHORT).show();
      if (mRoboplexxThread != null) {
        mRoboplexxThread.terminate();
      }
      stopSelf();
      break;
      
    case NONE:
    default:
      break;
    }

    // If we get killed, after returning from here, restart
    return START_REDELIVER_INTENT;
  }
  
  public String getStatusMessage() {
    if (mRoboplexxThread != null) {
      return mRoboplexxThread.getStatus();
    }
    return "Not started";
  }
  
  public void setRoboplexxMonitor(IRoboplexxMonitor iRoboplexxListener) {
    mRoboplexxMonitor = iRoboplexxListener;
  }
  
  public void setStatus(String string) {
    if (mRoboplexxMonitor != null) {
      mRoboplexxMonitor.updateRoboplexxStatus(string);
    }
  }
  
  public void updateRobotSpeeds(double left_speed, double right_speed) {
    mRoboplexxMonitor.updateRobotSpeeds(left_speed, right_speed);
  }

  @Override
  public IBinder onBind(Intent intent) {
    return mBinder;
  }


  class AppEngineBinder extends RoboplexxBinder {

    public IRoboplexxService getService() {
      // Return this instance of RoboplexxBinder so clients can call public methods
      return AppEngineService.this;
    }

  }
  
//  public interface IRoboplexxListener {
//    
//    public void updateRoboplexxStatus(String status);
//    
//    public void updateRobotSpeeds(double left_speed, double right_speed);
//    
//  }

}