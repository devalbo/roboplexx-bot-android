/**
 * 
 */
package com.roboplexx.android;

import com.roboplexx.android.R;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.view.Gravity;
import android.widget.Toast;

/**
 * @author ajb
 *
 */
public class RoboplexxPreferences extends PreferenceActivity {

  private OnPreferenceChangeListener mRobotNameChangeListener;
  private OnPreferenceChangeListener mServerPortChangeListener;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    addPreferencesFromResource(R.xml.preferences);
    refreshActivityTitle();
  }

  @Override
  protected void onResume() {
    super.onResume();
    mRobotNameChangeListener = new OnPreferenceChangeListener() {

      public boolean onPreferenceChange(Preference sharedPreferences, Object newValue) {
        String strValue = (String)newValue;
        if (strValue.trim().length() < 1) {
          Toast toast = Toast.makeText(getApplicationContext(), 
            "Robot name must not be blank!", 
            Toast.LENGTH_LONG);
          toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
          toast.show();
          return false;
        }
        new Handler().post(new Runnable() {
          
          public void run() {
            refreshActivityTitle();            
          }
        });
        return true;
      }
    };
    findPreference(getResources().getString(R.string.pref_key_robot_name)).
      setOnPreferenceChangeListener(mRobotNameChangeListener);

    mServerPortChangeListener = new OnPreferenceChangeListener() {

      public boolean onPreferenceChange(Preference sharedPreferences, Object newValue) {
        String strValue = (String)newValue;
        try {
          int intValue = Integer.parseInt(strValue);
          if (intValue < 1 || intValue > 65535) {
            return false;

          }
        } catch (Exception e) {
          return false;
        }
        return true;
      }
    };
    findPreference(getResources().getString(R.string.pref_key_local_network_port)).
      setOnPreferenceChangeListener(mServerPortChangeListener);

    AccountManager accountManager = AccountManager.get(getApplicationContext());
    Account[] accounts = accountManager.getAccountsByType("com.google");

    ListPreference listPreferenceCategory = (ListPreference) findPreference(
      getResources().getString(R.string.pref_key_roboplexx_accountname));
    if (listPreferenceCategory != null) {
      CharSequence entries[] = new String[accounts.length];
      CharSequence entryValues[] = new String[accounts.length];
      int i = 0;
      for (Account account : accounts) {
        entries[i] = account.name;
        entryValues[i] = account.name;
        i++;
      }
      listPreferenceCategory.setEntries(entries);
      listPreferenceCategory.setEntryValues(entryValues);
    }
  }
  
  private void refreshActivityTitle() {
    String robotName = ((EditTextPreference)findPreference(getResources().getString(R.string.pref_key_robot_name))).getText();
    setTitle(getResources().getString(R.string.title_activity_preferences) + ": " + robotName);
  }

}
