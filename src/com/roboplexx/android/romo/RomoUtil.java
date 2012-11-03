/**
 * 
 */
package com.roboplexx.android.romo;

/**
 * @author ajb
 *
 */
public class RomoUtil {

  public static int convertSpeedPercentToCommand(double speed) {
    double workingSpeed = Math.min(speed, 100.0);
    workingSpeed = Math.max(workingSpeed, -100.0);

    // at this point, workingSpeed is at most 100.0 and at least -100.0
    int command = 0x80 + (int)((workingSpeed / 100.0) * 0x7F);
    
    return command;
  }


}
