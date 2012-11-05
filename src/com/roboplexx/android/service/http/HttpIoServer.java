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

import com.roboplexx.android.romo.RomoUtil;
import com.roboplexx.android.service.RoboplexxService;
import com.romotive.library.RomoCommandInterface;

/**
 * @author ajb
 *
 */
public class HttpIoServer extends NanoHTTPD {

	private String _activeConfiguration = "Stopped";
	private String _activeEmotion = "Unknown";
	private RomoCommandInterface _commandInterface;
	private RoboplexxService _roboplexxService;
	private AssetManager _assetManager = null;
	
	private double _leftMotorSpeedPercent = 0.0;
	private double _rightMotorSpeedPercent = 0.0;
	private int _leftMotorSpeed = 0x80;
	private int _rightMotorSpeed = 0x80;
  private int _portNumber;

	public HttpIoServer(int port, RoboplexxService roboplexxService) throws IOException 
			{
		super(port, null);
    _roboplexxService = roboplexxService;
    _portNumber = port;

    setStatus("HTTP IO server running");
    setConnectionInfo();
		
    _commandInterface = new RomoCommandInterface();
		_assetManager = _roboplexxService.getAssets();
			}
	
	@Override
	public Response serve(String uri, String method, Properties header, 
			Properties parms, Properties files) 
	{

		if (uri.equalsIgnoreCase("/motors")) {
			if (method.equalsIgnoreCase("post")) {
				try {
					_leftMotorSpeedPercent = Double.parseDouble((String) parms.get("left_speed"));
				} catch (NumberFormatException e) {
					setStatus("Invalid left motor speed: " + (String) parms.get("left_speed"));
				}
				try {
					_rightMotorSpeedPercent = Double.parseDouble((String) parms.get("right_speed"));
				} catch (NumberFormatException e) {
					setStatus("Invalid right motor speed: " + (String) parms.get("right_speed"));
				}
				_leftMotorSpeed = RomoUtil.convertSpeedPercentToCommand(_leftMotorSpeedPercent);
				_rightMotorSpeed = RomoUtil.convertSpeedPercentToCommand(_rightMotorSpeedPercent);
//				setStatus("Motor commands: " + getMotorCommands());
				updateMotorSpeeds(_leftMotorSpeedPercent, _rightMotorSpeedPercent);
				
				_commandInterface.playMotorCommand(_leftMotorSpeed, _rightMotorSpeed);
			}
				
			return new Response(HTTP_OK, "text/plain", getMotorSpeeds());

			
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
			_commandInterface.playMotorCommand(0xC0, 0xFF);

		} else if (newConfig.equalsIgnoreCase("forward")) {
			setStatus("Going forward");
			_commandInterface.playMotorCommand(0xFF, 0xFF);

		} else if (newConfig.equalsIgnoreCase("veer-right")) {
			setStatus("Veering right");
			_commandInterface.playMotorCommand(0xFF, 0xC0);

		} else if (newConfig.equalsIgnoreCase("spin-left")) {
			setStatus("Spinning left");
			_commandInterface.playMotorCommand(0x00, 0xFF);

		} else if (newConfig.equalsIgnoreCase("stop")) {
			setStatus("Stopped");
			_commandInterface.playMotorCommand(0x80, 0x80);

		} else if (newConfig.equalsIgnoreCase("spin-right")) {
			setStatus("Spinning right");
			_commandInterface.playMotorCommand(0xFF, 0x00);

		} else if (newConfig.equalsIgnoreCase("back-left")) {
			setStatus("Backing left");
			_commandInterface.playMotorCommand(0x40, 0x00);

		} else if (newConfig.equalsIgnoreCase("reverse")) {
			setStatus("In reverse");
			_commandInterface.playMotorCommand(0x00, 0x00);

		} else if (newConfig.equalsIgnoreCase("back-right")) {
			setStatus("Backing right");
			_commandInterface.playMotorCommand(0x00, 0x40);

		}
	}
	
  public String setConnectionInfo() {
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
	
	private void updateMotorSpeeds(double leftMotorSpeed, double rightMotorSpeed) {
    _roboplexxService.updateRobotSpeeds(leftMotorSpeed, rightMotorSpeed);
	}

	private void setEmotion(String emotion) {
		_roboplexxService.setEmotion(emotion);
	}
	
	private String getMotorSpeeds() {
		return "L: " + _leftMotorSpeedPercent + "%  <<->>  R: " + _rightMotorSpeedPercent + " %";
	}

}
