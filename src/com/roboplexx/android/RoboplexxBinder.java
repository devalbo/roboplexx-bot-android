/**
 * 
 */
package com.roboplexx.android;

import android.os.Binder;

/**
 * @author ajb
 *
 */
public abstract class RoboplexxBinder extends Binder {

    abstract public IRoboplexxService getService();

}
