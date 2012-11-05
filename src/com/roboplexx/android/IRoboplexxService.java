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

}
