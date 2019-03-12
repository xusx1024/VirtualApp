package com.hairui.autologin.repo;

import android.content.Context;
import com.hairui.autologin.models.AppData;
import com.hairui.autologin.models.AppInfo;
import com.hairui.autologin.models.AppInfoLite;
import com.lody.virtual.remote.InstallResult;
import java.io.File;
import java.util.List;
import org.jdeferred.Promise;

/**
 * @author Lody
 * @version 1.0
 */
public interface AppDataSource {

  /**
   * @return All the Applications we Virtual.
   */
  Promise<List<AppData>, Throwable, Void> getVirtualApps();

  /**
   * @param context Context
   * @return All the Applications we Installed.
   */
  Promise<List<AppInfo>, Throwable, Void> getInstalledApps(Context context);

  Promise<List<AppInfo>, Throwable, Void> getStorageApps(Context context, File rootDir);

  InstallResult addVirtualApp(AppInfoLite info);

  boolean removeVirtualApp(String packageName, int userId);
}
