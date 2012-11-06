/**
 * 
 */
package com.roboplexx.android.service.http;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import android.app.Service;
import android.content.res.AssetManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.roboplexx.android.service.RoboplexxService;

/**
 * @author ajb
 *
 */
public class HttpIoServer extends NanoHTTPD {

	private String _activeConfiguration = "Stopped";
	private String _activeEmotion = "Unknown";
	private RoboplexxService _roboplexxService;
	private AssetManager _assetManager = null;
	
  private int _portNumber;

	public HttpIoServer(int port, RoboplexxService roboplexxService) throws IOException 
			{
		super(port, null);
    _roboplexxService = roboplexxService;
    _portNumber = port;

    setStatus("HTTP IO server running");
    updateConnectionInfo();
		
		_assetManager = _roboplexxService.getAssets();
			}
	
	@Override
	public Response serve(String uri, String method, Properties header, 
			Properties parms, Properties files) 
	{

		if (uri.equalsIgnoreCase("/motors")) {
			if (method.equalsIgnoreCase("post")) {
			  double leftMotorSpeedPercent = _roboplexxService.getRobotLeftMotorSpeed();
			  double rightMotorSpeedPercent = _roboplexxService.getRobotRightMotorSpeed();
			  
				try {
				  leftMotorSpeedPercent = Double.parseDouble((String) parms.get("left_speed"));
				} catch (NumberFormatException e) {
					setStatus("Invalid left motor speed: " + (String) parms.get("left_speed"));
				}
				try {
					rightMotorSpeedPercent = Double.parseDouble((String) parms.get("right_speed"));
				} catch (NumberFormatException e) {
					setStatus("Invalid right motor speed: " + (String) parms.get("right_speed"));
				}

				setRobotMotorSpeeds(leftMotorSpeedPercent, rightMotorSpeedPercent);
			}
				
			return new Response(HTTP_OK, "text/plain", _roboplexxService.getMotorReport());

			
		} else if (uri.equalsIgnoreCase("/devices/camera/image")) {

		} else if (uri.equalsIgnoreCase("/devices/left-motor")) {

		} else if (uri.equalsIgnoreCase("/devices/emotionator")) {
			if (method.equalsIgnoreCase("post")) {
				handleEmotion(parms);
			}			
			return new Response(HTTP_OK, "text/plain", _activeEmotion);

		} else if (uri.equalsIgnoreCase("/configurations/direction")) {

			if (method.equalsIgnoreCase("post")) {
				handleDirectionConfiguration(parms);
			}
			return new Response(HTTP_OK, "text/plain", _activeConfiguration);

		} else if (uri.startsWith("/static/css")) {
			return getStaticAssetResponse(uri, "text/css");

		} else if (uri.startsWith("/static/img")) {
			return getStaticAssetResponse(uri, "image/png");

		} else if (uri.startsWith("/static/js")) {
			return getStaticAssetResponse(uri, "text/javascript");

		} else if (uri.endsWith(".html")) {
			return getHtmlAssetResponse(uri);

		}

		return getHtmlAssetResponse("/index.html");
	}

	private Response getStaticAssetResponse(String uri, String mimeType) {
		InputStream stream = null;
		try {
			stream = _assetManager.open("web" + uri);
			Response response = new Response(HTTP_OK, mimeType, stream);
			return response;

		} catch (IOException e) {
			Log.e("getStaticAssetResponse", e.getLocalizedMessage());
		}

		return new Response(HTTP_NOTFOUND, "text/plain", "Content not found for '" + uri + "'");
	}
	
	private Response getHtmlAssetResponse(String uri) {
		InputStream stream = null;
		try {
			stream = _assetManager.open("web/html" + uri);
			Response response = new Response(HTTP_OK, "text/html", stream);
			return response;

		} catch (IOException e) {
			Log.e("getHtmlAssetResponse", e.getLocalizedMessage());
		}

		return new Response(HTTP_NOTFOUND, "text/plain", "Content not found for '" + uri + "'");
	}

	private void handleEmotion(Properties parms) {
		String newEmotion = (String) parms.get("value");

		if (newEmotion.equalsIgnoreCase("happy")) {
			setEmotion("Happy");

		} else if (newEmotion.equalsIgnoreCase("sad")) {
			setEmotion("Sad");

		} else if (newEmotion.equalsIgnoreCase("meh")) {
			setEmotion("Meh");

		} else if (newEmotion.equalsIgnoreCase("love")) {
			setEmotion("In love");

		} else if (newEmotion.equalsIgnoreCase("angry")) {
			setEmotion("Angry");

		} else if (newEmotion.equalsIgnoreCase("silly")) {
			setEmotion("Silly");

		}
	}
	private void handleDirectionConfiguration(Properties parms) {
		String newConfig = (String) parms.get("value");

		if (newConfig.equalsIgnoreCase("veer-left")) {
			setStatus("Veering left");
			setRobotMotorSpeeds(75.0, 100.0);

		} else if (newConfig.equalsIgnoreCase("forward")) {
			setStatus("Going forward");
			setRobotMotorSpeeds(100.0, 100.0);

		} else if (newConfig.equalsIgnoreCase("veer-right")) {
			setStatus("Veering right");
			setRobotMotorSpeeds(100.0, 75.0);

		} else if (newConfig.equalsIgnoreCase("spin-left")) {
			setStatus("Spinning left");
			setRobotMotorSpeeds(0.0, 100.0);

		} else if (newConfig.equalsIgnoreCase("stop")) {
			setStatus("Stopped");
			setRobotMotorSpeeds(0.0, 0.0);

		} else if (newConfig.equalsIgnoreCase("spin-right")) {
			setStatus("Spinning right");
			setRobotMotorSpeeds(100.0, 0.0);

		} else if (newConfig.equalsIgnoreCase("back-left")) {
			setStatus("Backing left");
			setRobotMotorSpeeds(-75.0, 0.0);

		} else if (newConfig.equalsIgnoreCase("reverse")) {
			setStatus("In reverse");
			setRobotMotorSpeeds(-100.0, -100.0);

		} else if (newConfig.equalsIgnoreCase("back-right")) {
			setStatus("Backing right");
			setRobotMotorSpeeds(0.0, -75.0);

		}
	}
	
  public String updateConnectionInfo() {
    WifiManager wifiManager = (WifiManager) _roboplexxService.getSystemService(Service.WIFI_SERVICE);
    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
    int ip = wifiInfo.getIpAddress();

    String ipString = String.format(
      "Connection: %d.%d.%d.%d:%d",
      (ip & 0xff),
      (ip >> 8 & 0xff),
      (ip >> 16 & 0xff),
      (ip >> 24 & 0xff),
      _portNumber );

    _roboplexxService.setConnectionInfo(ipString);
    return ipString;
  }

	private void setStatus(String statusText) {
		_activeConfiguration = statusText;
		_roboplexxService.setStatusText(statusText);
	}
	
	private void setRobotMotorSpeeds(double leftMotorSpeed, double rightMotorSpeed) {
    _roboplexxService.setRobotMotorSpeeds(leftMotorSpeed, rightMotorSpeed);
	}

	private void setEmotion(String emotion) {
		_roboplexxService.setEmotion(emotion);
	}
	
}
