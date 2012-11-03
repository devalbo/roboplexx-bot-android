/**
 * 
 */
package com.roboplexx.android.appengine;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.json.JSONObject;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.roboplexx.android.R;
import com.roboplexx.android.RoboplexxCommand;
import com.roboplexx.android.R.string;
import com.roboplexx.android.appengine.channel.ChannelAPI;
import com.roboplexx.android.appengine.channel.ChannelService;
import com.roboplexx.android.appengine.channel.XHR;
import com.romotive.library.RomoCommandInterface;

/**
 * @author ajb
 *
 */
public class AppEngineServiceThread extends Thread {

  public enum ServiceState {
    INITIALIZING,
    INITIALIZING_ROMO,
    INITIALIZING_ACCOUNT,
    AUTHORIZING,
    CONNECTING,
    CONNECTED,
    SHUTTING_DOWN,
    DISCONNECTED,
    DISCONNECTED_ERR,
    FAILED
  };

  //  public final static String ROBOPLEXX_ROOT_URL = "http://192.168.0.151:8080";
  //  public final static String ROBOPLEXX_ROOT_URL = "http://10.10.10.104:8080";
  public final static String ROBOPLEXX_ROOT_URL = "https://roboplexx.appspot.com";

  //  public final static String ROBOPLEX_ROBOT_INFO_PREFS_ID = "roboplexx_robot_info";
  //  public final static String ROBOPLEXX_ROBOT_NAME = "robot_name";
  //  public final static String ROBOPLEXX_ROBOT_ID = "robot_id";
  public final static String ROBOPLEXX_ROBOT_SUBSCRIBE_URL = "robot_subscribe_url";
  public final static String REGISTER_ROBOT_ROBOPLEXX_URL = ROBOPLEXX_ROOT_URL + "/api/v1/register-robot";
  public final static String UPDATE_ROBOT_ROBOPLEXX_URL = ROBOPLEXX_ROOT_URL + "/api/v1/update-robot";

  private HttpClient mHttpClient;
  private Handler mServiceThreadHandler = new Handler();
  private RomoCommandInterface mCommandInterface;
  private ServiceState mStatus;
  private String mStatusMessage;
  private String mAuthToken;
  private ChannelAPI mChannelApi;
  private String mRoboplexxId;
  private String mRoboplexxRobotSubscribeUrl;
  private long mLastAppliedCommandMs;

  private AppEngineService mRoboplexxService;

  public AppEngineServiceThread(AppEngineService service) {
    mRoboplexxService = service;
    setStatus(ServiceState.INITIALIZING);
  }

  @Override
  public void run() {
    try {

      setStatus(ServiceState.INITIALIZING_ROMO);
      // Initialize the RomoCommandInterface
      mCommandInterface = new RomoCommandInterface();
      AudioManager manager = (AudioManager) mRoboplexxService.getSystemService(AppEngineService.AUDIO_SERVICE);
      manager.setStreamVolume(AudioManager.STREAM_MUSIC, manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);

      setStatus(ServiceState.INITIALIZING_ACCOUNT);
      Account roboplexxAccount = getActiveAccount();
      mAuthToken = getAuthTokenForAccount(roboplexxAccount);

      setStatus(ServiceState.AUTHORIZING);
      if (usingDevServer()) {
        mHttpClient = devLogin(roboplexxAccount.name);
      } else {
        mHttpClient = login();
      }

      setStatus(ServiceState.CONNECTING);

      SharedPreferences robotInfo = PreferenceManager.getDefaultSharedPreferences(mRoboplexxService);
      mRoboplexxId = robotInfo.getString(mRoboplexxService.getString(R.string.pref_key_roboplexx_robot_id), "");
      mRoboplexxRobotSubscribeUrl = robotInfo.getString(ROBOPLEXX_ROBOT_SUBSCRIBE_URL, "");

      // check to see if we've registered; if we don't have an ID/URL, assume we haven't; if we do,
      // update the profile to make sure all settings are registered
      if (mRoboplexxId.length() < 1 || mRoboplexxRobotSubscribeUrl.length() < 1) {
        HttpPost registerPost = new HttpPost(REGISTER_ROBOT_ROBOPLEXX_URL);

        registerPost.setEntity(new UrlEncodedFormEntity(getRegisterParams(), HTTP.UTF_8));
        XHR xhr = new XHR(mHttpClient.execute(registerPost));
        if (xhr.getStatus() != 200) {
          throw new LifecycleException(ServiceState.FAILED, "Unable to register robot - status code: " + xhr.getStatus());
        }
        JSONObject json = new JSONObject(xhr.getResponseText());
        mRoboplexxId = json.getString(mRoboplexxService.getString(R.string.pref_key_roboplexx_robot_id));
        mRoboplexxRobotSubscribeUrl = json.getString(ROBOPLEXX_ROBOT_SUBSCRIBE_URL);

        Editor editor = robotInfo.edit();
        editor.putString(mRoboplexxService.getString(R.string.pref_key_roboplexx_robot_id), mRoboplexxId);
        editor.putString(ROBOPLEXX_ROBOT_SUBSCRIBE_URL, mRoboplexxRobotSubscribeUrl);
        editor.commit();
      } else {

        HttpPost registerPost = new HttpPost(UPDATE_ROBOT_ROBOPLEXX_URL);
        registerPost.setEntity(new UrlEncodedFormEntity(getRegisterParams(), HTTP.UTF_8));
        XHR xhr = new XHR(mHttpClient.execute(registerPost));
        if (xhr.getStatus() != 200) {
          throw new LifecycleException(ServiceState.FAILED, "Unable to update robot profile - status code: " + xhr.getStatus());
        }
      }

      mChannelApi = new ChannelAPI(mRoboplexxRobotSubscribeUrl, mRoboplexxId, mHttpClient, new ChannelService() {

        public void onOpen() {
          setStatus(ServiceState.CONNECTED);
        }

        public void onMessage(final String message) {
          System.out.println("New Channel API message! " + message);
          try {
            RoboplexxCommand roboplexxCommand = new RoboplexxCommand(message);

            if (roboplexxCommand.lastSentMs > mLastAppliedCommandMs) {
              System.out.println("Motor speeds: " + roboplexxCommand.speedLeft + ", " + roboplexxCommand.speedRight);
              mRoboplexxService.updateRobotSpeeds(roboplexxCommand.speedLeft, roboplexxCommand.speedRight);
              mCommandInterface.playMotorCommand(roboplexxCommand.speedLeftCommand, roboplexxCommand.speedRightCommand);
              mLastAppliedCommandMs = roboplexxCommand.lastSentMs;
            }

          } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }

        }

        public void onError(Integer errorCode, String description) {
          setStatus(ServiceState.DISCONNECTED_ERR);
          mHttpClient.getConnectionManager().shutdown();
        }

        public void onClose() {
          setStatus(ServiceState.DISCONNECTED);
          mHttpClient.getConnectionManager().shutdown();
        }
      });

      mChannelApi.open();

    } catch (Exception e) {
      final String message = e.getClass().getName() + ": " + e.getLocalizedMessage();
      setStatus(ServiceState.FAILED, message);
      mServiceThreadHandler.post(new Runnable() {

        public void run() {
          Toast.makeText(mRoboplexxService.getApplicationContext(), message, Toast.LENGTH_LONG).show();           
        }
      });

    }
  }

  public String getStatus() {
    if (mStatusMessage != null) {
      return mStatusMessage;
    }
    return mStatus.toString();
  }

  private String getKey(int resourceId) {
    return mRoboplexxService.getResources().getString(resourceId);
  }

  public static boolean usingDevServer() {
    if (ROBOPLEXX_ROOT_URL.startsWith("https://roboplexx.appspot.com")) {
      return false;
    }
    return true;
  }

  private Account getActiveAccount() throws LifecycleException {
    String accountName = PreferenceManager.getDefaultSharedPreferences(mRoboplexxService).getString(
      getKey(R.string.pref_key_roboplexx_accountname), "");
    AccountManager accountManager = AccountManager.get(mRoboplexxService.getApplicationContext());
    Account roboplexxAccount = null;
    for (Account account : accountManager.getAccounts()) {
      if (accountName.equals(account.name)) {
        roboplexxAccount = account;
        break;
      }
    }
    if (roboplexxAccount == null) {
      throw new LifecycleException(ServiceState.FAILED, "No account available for Roboplexx - see Preferences");
    }

    return roboplexxAccount;
  }

  private String getAuthTokenForAccount(Account roboplexxAccount) throws LifecycleException, OperationCanceledException, AuthenticatorException, IOException {

    AccountManager accountManager = AccountManager.get(mRoboplexxService.getApplicationContext());
    AccountManagerFuture<Bundle> authTokenFuture = accountManager.getAuthToken(
      roboplexxAccount, "ah", false, new AccountManagerCallback<Bundle>() {

        public void run(AccountManagerFuture<Bundle> result) {
          try {
            Bundle bundle = result.getResult();
            Intent intent = (Intent)bundle.get(AccountManager.KEY_INTENT);
            if (intent != null) {
              // User input required
              mRoboplexxService.startActivity(intent);
            }
          } catch (Exception e) {
            setStatus(ServiceState.FAILED, e.getClass().getName() + ": " + e.getLocalizedMessage());
          }

        }
      }, null);
    String authToken = authTokenFuture.getResult().getString(AccountManager.KEY_AUTHTOKEN);
    accountManager.invalidateAuthToken("com.google", mAuthToken);
    authTokenFuture = accountManager.getAuthToken(
      roboplexxAccount, "ah", false, new AccountManagerCallback<Bundle>() {

        public void run(AccountManagerFuture<Bundle> result) {
          try {
            Bundle bundle = result.getResult();
            Intent intent = (Intent)bundle.get(AccountManager.KEY_INTENT);
            if (intent != null) {
              // User input required
              mRoboplexxService.startActivity(intent);
            }
          } catch (Exception e) {
            setStatus(ServiceState.FAILED, e.getClass().getName() + ": " + e.getLocalizedMessage());
          }

        }
      }, null);
    authToken = authTokenFuture.getResult().getString(AccountManager.KEY_AUTHTOKEN);

    return authToken;
  }

  private HttpClient login() throws ClientProtocolException, IOException, LifecycleException {
    DefaultHttpClient httpClient = new DefaultHttpClient();
    // Don't follow redirects
    httpClient.getParams().setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, false);
    HttpGet http_get = new HttpGet(ROBOPLEXX_ROOT_URL + "/_ah/login?auth=" + mAuthToken);

    HttpResponse loginReponse = httpClient.execute(http_get);

    //    byte[] response = new byte[100000];
    //    loginReponse.getEntity().getContent().read(response);
    //    String c = new String(response);
    //    System.out.println(c);
    if (loginReponse.getStatusLine().getStatusCode() != 302) {
      // Response should be a redirect
      throw new LifecycleException(ServiceState.FAILED, "No redirect when authorizing with Roboplexx");
    }

    boolean foundSacsidCookie = false;
    for (Cookie cookie : httpClient.getCookieStore().getCookies()) {
      if (cookie.getName().equals("SACSID")) {
        foundSacsidCookie = true;
        break;
      }
    }
    if (!foundSacsidCookie) {
      http_get.abort();
      throw new LifecycleException(ServiceState.FAILED, "No authentication cookie found");
    }

    httpClient.getParams().setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, true);
    http_get.abort();

    return httpClient;
  }

  private HttpClient devLogin(String accountName) throws ClientProtocolException, IOException, LifecycleException {
    HttpClient httpClient = new DefaultHttpClient();
    String login_url = ROBOPLEXX_ROOT_URL + "/_ah/login?email=" + URLEncoder.encode(accountName, "UTF-8") + "&action=Login";

    HttpGet http_get = new HttpGet(login_url);
    HttpResponse loginReponse = httpClient.execute(http_get);

    if (loginReponse.getStatusLine().getStatusCode() != 200) {
      http_get.abort();
      throw new LifecycleException(ServiceState.FAILED, "Dev login failed");

    } else {
      http_get.abort();
    }

    return httpClient;
  }

  private List<NameValuePair> getRegisterParams() {
    String robot_name = PreferenceManager.getDefaultSharedPreferences(mRoboplexxService).getString(
      mRoboplexxService.getResources().getString(R.string.pref_key_robot_name), "My Robot");
    boolean robot_public = PreferenceManager.getDefaultSharedPreferences(mRoboplexxService).getBoolean(
      mRoboplexxService.getResources().getString(R.string.pref_key_roboplexx_publicrobot), false);

    List <NameValuePair> nvps = new ArrayList <NameValuePair>();
    nvps.add(new BasicNameValuePair(getKey(R.string.pref_key_robot_name), robot_name));
    nvps.add(new BasicNameValuePair(getKey(R.string.pref_key_roboplexx_publicrobot), Boolean.toString(robot_public)));

    if (mRoboplexxId.length() > 0) {
      nvps.add(new BasicNameValuePair(getKey(R.string.pref_key_roboplexx_robot_id), mRoboplexxId));
    }

    return nvps;
  }

  private void setStatus(ServiceState status) {
    mStatus = status;
    mRoboplexxService.setStatus(status.toString());
  }

  private void setStatus(ServiceState status, String statusMessage) {
    setStatus(status);
    mStatusMessage = statusMessage;
  }

  public void terminate() {
    try {

      stopMovement();

      if (mChannelApi != null) {
        mChannelApi.close();
      }
      
      if (mCommandInterface != null) {
//        mCommandInterface.shutdown();
      }

    } catch (Exception e) {
      e.printStackTrace();
      setStatus(ServiceState.FAILED, e.getLocalizedMessage());

    } finally {
      mChannelApi = null;
      mCommandInterface = null;
    }
  }

  private void stopMovement() {
    if (mCommandInterface != null) {
      mCommandInterface.playMotorCommand(128, 128);
    }
  }

  @SuppressWarnings("serial")
  class LifecycleException extends Exception {

    public ServiceState mTerminationState;
    public String mTerminationMessage;

    public LifecycleException(ServiceState terminationState, String terminationMessage) {
      mTerminationState = terminationState;
      mTerminationMessage = terminationMessage;
    }

  }

}
