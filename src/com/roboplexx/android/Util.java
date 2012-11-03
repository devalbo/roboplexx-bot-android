/**
 * 
 */
package com.roboplexx.android;

import java.util.List;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;

/**
 * @author ajb
 *
 */
public class Util {
  private static String LOG_TAG = Util.class.getName();

  public static boolean isServiceRunning(Activity activity, Class serviceClass) {
      final ActivityManager activityManager = (ActivityManager)activity.getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
      final List<RunningServiceInfo> services = activityManager.getRunningServices(Integer.MAX_VALUE);

      for (RunningServiceInfo runningServiceInfo : services) {
          if (runningServiceInfo.service.getClassName().equals(serviceClass.getCanonicalName())){
              return true;
          }
      }
      return false;
   }

  public static int convertSpeedPercentToRomoCommand(double speed) {
    double workingSpeed = Math.min(speed, 100.0);
    workingSpeed = Math.max(workingSpeed, -100.0);

    // at this point, workingSpeed is at most 100.0 and at least -100.0
    int command = 0x80 + (int)((workingSpeed / 100.0) * 0x7F);
    
    return command;
  }


}