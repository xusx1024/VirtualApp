package com.hairui.autologin.repo;

import com.hairui.autologin.AutoLoginApp;
import com.hairui.autologin.abs.Callback;
import com.hairui.autologin.kit.VUiKit;
import com.hairui.autologin.models.PackageAppData;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.remote.InstalledAppInfo;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Lody
 * <p>
 * Cache the loaded PackageAppData.缓存加载过的应用数据
 */
public class PackageAppDataStorage {

  private static final PackageAppDataStorage STORAGE = new PackageAppDataStorage();
  private final Map<String, PackageAppData> packageDataMap = new HashMap<>();

  public static PackageAppDataStorage get() {
    return STORAGE;
  }

  public PackageAppData acquire(String packageName) {
    PackageAppData data;
    synchronized (packageDataMap) {
      data = packageDataMap.get(packageName);
      if (data == null) {
        data = loadAppData(packageName);
      }
    }
    return data;
  }

  public void acquire(String packageName, Callback<PackageAppData> callback) {
    VUiKit.defer().when(() -> acquire(packageName)).done(callback::callback);
  }

  private PackageAppData loadAppData(String packageName) {
    InstalledAppInfo setting = VirtualCore.get().getInstalledAppInfo(packageName, 0);
    if (setting != null) {
      PackageAppData data = new PackageAppData(AutoLoginApp.getApp(), setting);
      synchronized (packageDataMap) {
        packageDataMap.put(packageName, data);
      }
      return data;
    }
    return null;
  }
}
