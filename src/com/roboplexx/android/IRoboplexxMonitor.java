/**
 * 
 */
package com.roboplexx.android;

/**
 * @author ajb
 *
 */
public interface IRoboplexxMonitor {

  public void updateRoboplexxStatus(String status);
  
  public void updateRobotSpeeds(double left_speed, double right_speed);

  public void updateEmotion(String emotion);

  public void setConnectionInfo(String ipString);

}
