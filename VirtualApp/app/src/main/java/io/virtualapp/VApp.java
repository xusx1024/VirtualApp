package io.virtualapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.multidex.MultiDexApplication;
import com.flurry.android.FlurryAgent;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.stub.VASettings;
import io.virtualapp.delegate.MyAppRequestListener;
import io.virtualapp.delegate.MyComponentDelegate;
import io.virtualapp.delegate.MyPhoneInfoDelegate;
import io.virtualapp.delegate.MyTaskDescriptionDelegate;
import jonathanfinerty.once.Once;

/**
 * @author Lody
 */
public class VApp extends MultiDexApplication {

  private static VApp gApp;
  private SharedPreferences mPreferences;

  public static VApp getApp() {
    return gApp;
  }

  public static SharedPreferences getPreferences() {
    return getApp().mPreferences;
  }

  /**
   * 执行顺序：constructor() -> attachBaseContext() -> onCreate()
   * 把初始化时间点提前到极致。
   */
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

  /**
   * 在这里初始化各种全局变量数据是推荐做法。
   */
  @Override public void onCreate() {
    gApp = this;
    super.onCreate();
    VirtualCore virtualCore = VirtualCore.get();
    virtualCore.initialize(new VirtualCore.VirtualInitializer() {

      @Override public void onMainProcess() {
        Once.initialise(VApp.this);
        new FlurryAgent.Builder().withLogEnabled(true).withListener(() -> {
          // nothing
        }).build(VApp.this, "48RJJP7ZCZZBB6KMMWW5_");
        // 这里我注释掉了数据采集
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
        virtualCore.setAppRequestListener(new MyAppRequestListener(VApp.this));
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
