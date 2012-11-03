/**
 * 
 */
package com.roboplexx.android;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author ajb
 *
 */
public class RoboplexxCommand {
  
  public double speedLeft;
  public double speedRight;
  public int speedLeftCommand;
  public int speedRightCommand;
  public long lastSentMs; 
  
  public RoboplexxCommand(String message) throws JSONException {

    JSONObject json = new JSONObject(message);
    speedLeft = json.getDouble("speed_left");
    speedRight = json.getDouble("speed_right");
    lastSentMs = json.getLong("sent_time_ms");
    speedLeftCommand = Util.convertSpeedPercentToRomoCommand(speedLeft);
    speedRightCommand = Util.convertSpeedPercentToRomoCommand(speedRight);
  }



}
