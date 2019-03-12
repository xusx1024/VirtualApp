package com.hairui.autologin.models;

import android.graphics.drawable.Drawable;

/**
 * @author Lody
 */

public class AppInfo {
  public String packageName;
  public String path;
  public boolean fastOpen;
  public Drawable icon;
  public CharSequence name;
  public int cloneCount;

  @Override public String toString() {
    return "AppInfo{"
        + "packageName='"
        + packageName
        + '\''
        + ", path='"
        + path
        + '\''
        + ", fastOpen="
        + fastOpen
        + ", icon="
        + icon
        + ", name="
        + name
        + ", cloneCount="
        + cloneCount
        + '}';
  }
}
