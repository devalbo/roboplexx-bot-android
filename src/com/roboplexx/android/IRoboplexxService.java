/**
 * 
 */
package com.roboplexx.android;

/**
 * @author ajb
 *
 */
public interface IRoboplexxService {

  void setRoboplexxMonitor(IRoboplexxMonitor iRoboplexxMonitor);

  String getStatusMessage();
  
  int getControlModeIndex();
  
  double getRobotLeftMotorSpeed();
  
  double getRobotRightMotorSpeed();
  
  String getEmotion();
  
  String getConnectionInfo();

}
