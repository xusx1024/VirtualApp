package com.hairui.autologin;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.multidex.MultiDexApplication;
import com.hairui.autologin.delegate.MyAppRequestListener;
import com.hairui.autologin.delegate.MyComponentDelegate;
import com.hairui.autologin.delegate.MyPhoneInfoDelegate;
import com.hairui.autologin.delegate.MyTaskDescriptionDelegate;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.stub.VASettings;

/**
 * Fun:
 *
 * @author sxx.xu@shunwang.com on 3/12/2019.
 */
public class AutoLoginApp extends MultiDexApplication {

  private static AutoLoginApp gApp;
  private SharedPreferences mPreferences;

  public static SharedPreferences getPreferences() {
    return getApp().mPreferences;
  }

  public static AutoLoginApp getApp() {
    return gApp;
  }

  @Override protected void attachBaseContext(Context base) {
    super.attachBaseContext(base);
    /** 跨进程实现数据共享的方式，推荐使用ContentProvider。 */
    mPreferences = base.getSharedPreferences("va", Context.MODE_MULTI_PROCESS);
    // 文件IO重定向
    VASettings.ENABLE_IO_REDIRECT = true;
    // 快捷方式
    VASettings.ENABLE_INNER_SHORTCUT = true;
    try {
      VirtualCore.get().startup(base);
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  @Override public void onCreate() {
    gApp = this;
    super.onCreate();
    final VirtualCore virtualCore = VirtualCore.get();
    virtualCore.initialize(new VirtualCore.VirtualInitializer() {

      @Override public void onMainProcess() {

      }

      @Override public void onVirtualProcess() {
        //listener components
        virtualCore.setComponentDelegate(new MyComponentDelegate());
        //fake phone imei,macAddress,BluetoothAddress
        virtualCore.setPhoneInfoDelegate(new MyPhoneInfoDelegate());
        //fake task description's icon and title
        virtualCore.setTaskDescriptionDelegate(new MyTaskDescriptionDelegate());
      }

      @Override public void onServerProcess() {
        virtualCore.setAppRequestListener(new MyAppRequestListener(gApp));
        virtualCore.addVisibleOutsidePackage("com.tencent.mobileqq");
        virtualCore.addVisibleOutsidePackage("com.tencent.mobileqqi");
        virtualCore.addVisibleOutsidePackage("com.tencent.minihd.qq");
        virtualCore.addVisibleOutsidePackage("com.tencent.qqlite");
        virtualCore.addVisibleOutsidePackage("com.facebook.katana");
        virtualCore.addVisibleOutsidePackage("com.whatsapp");
        virtualCore.addVisibleOutsidePackage("com.tencent.mm");
        virtualCore.addVisibleOutsidePackage("com.immomo.momo");
      }
    });
  }
}
