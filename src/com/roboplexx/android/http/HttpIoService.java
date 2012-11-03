package com.roboplexx.android.http;

import java.io.IOException;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.roboplexx.android.IRoboplexxMonitor;
import com.roboplexx.android.IRoboplexxService;
import com.roboplexx.android.RoboplexxBinder;

public class HttpIoService extends Service implements IRoboplexxService {

  public enum HttpIoServiceCommand {
    NONE,
    START_SERVER,
    STOP_SERVER
  };

  private IRoboplexxMonitor mRoboplexxMonitor = null;
  private HttpIoServer mHttpIoServer = null;
  private int _portNumber = 8080;
//  private RomoCommandInterface mCommandInterface = null;

  // Binder given to clients
  private final HttpIoBinder mBinder = new HttpIoBinder();

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    HttpIoServiceCommand command = HttpIoServiceCommand.NONE;
    if (intent != null && 
        intent.getExtras() != null &&
        intent.getExtras().containsKey("command")) 
    {
      command = HttpIoServiceCommand.valueOf(intent.getExtras().getString("command"));
    }

    switch (command) {
    case START_SERVER:
      try {
        setStatus("Starting HTTP IO server");

        mHttpIoServer = new HttpIoServer(_portNumber, this);
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
        setStatus("HTTP IO server error: " + e.getLocalizedMessage());
      }

      break;
      
    case STOP_SERVER:
//      Toast.makeText(this, "disconnecting from Roboplexx", Toast.LENGTH_SHORT).show();
      if (mHttpIoServer != null) {
        mHttpIoServer.stop();
        mHttpIoServer = null;
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
    return "Not started";
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


  public class HttpIoBinder extends RoboplexxBinder {

    public IRoboplexxService getService() {
      // Return this instance of HttpIoBinder so clients can call public methods
      return HttpIoService.this;
    }

  }
  
  public void setRoboplexxMonitor(IRoboplexxMonitor roboplexxMonitor) {
    mRoboplexxMonitor = roboplexxMonitor;    
  }

  public void setStatusText(String statusText) {
    mRoboplexxMonitor.updateRoboplexxStatus(statusText);
  }

  public void setEmotion(String emotion) {
    mRoboplexxMonitor.updateEmotion(emotion);
  }

}