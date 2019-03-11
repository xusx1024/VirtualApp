package com.lody.virtual.client.core;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.ConditionVariable;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import android.os.RemoteException;
import com.lody.virtual.R;
import com.lody.virtual.client.VClientImpl;
import com.lody.virtual.client.env.Constants;
import com.lody.virtual.client.env.VirtualRuntime;
import com.lody.virtual.client.fixer.ContextFixer;
import com.lody.virtual.client.hook.delegate.ComponentDelegate;
import com.lody.virtual.client.hook.delegate.PhoneInfoDelegate;
import com.lody.virtual.client.hook.delegate.TaskDescriptionDelegate;
import com.lody.virtual.client.ipc.ServiceManagerNative;
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.client.ipc.VPackageManager;
import com.lody.virtual.client.stub.VASettings;
import com.lody.virtual.helper.compat.BundleCompat;
import com.lody.virtual.helper.ipcbus.IPCBus;
import com.lody.virtual.helper.ipcbus.IPCSingleton;
import com.lody.virtual.helper.ipcbus.IServerCache;
import com.lody.virtual.helper.utils.BitmapUtils;
import com.lody.virtual.os.VUserHandle;
import com.lody.virtual.remote.InstallResult;
import com.lody.virtual.remote.InstalledAppInfo;
import com.lody.virtual.server.ServiceCache;
import com.lody.virtual.server.interfaces.IAppManager;
import com.lody.virtual.server.interfaces.IAppRequestListener;
import com.lody.virtual.server.interfaces.IPackageObserver;
import com.lody.virtual.server.interfaces.IUiCallback;
import dalvik.system.DexFile;
import java.io.IOException;
import java.util.List;
import mirror.android.app.ActivityThread;

/**
 * @author Lody
 * @version 3.5
 */
public final class VirtualCore {

  public static final int GET_HIDDEN_APP = 0x00000001;

  @SuppressLint("StaticFieldLeak") private static VirtualCore gCore = new VirtualCore();
  /**
   * Os.getuid();
   * 进程UID的唯一标识。标识app运行在它特定的沙盒环境中。
   */
  private final int myUid = Process.myUid();
  /**
   * Client Package Manager
   * Android系统的 PackageManager
   */
  private PackageManager unHookPackageManager;
  /**
   * Host package name
   */
  private String hostPkgName;
  /**
   * ActivityThread instance
   */
  private Object mainThread;
  /**
   * 从接入端的Application传来的上下文对象
   */
  private Context context;
  /**
   * Main ProcessName
   */
  private String mainProcessName;
  /**
   * Real Process Name
   */
  private String processName;
  /**
   * 自定义进程类型
   */
  private ProcessType processType;
  private IPCSingleton<IAppManager> singleton = new IPCSingleton<>(IAppManager.class);
  /** true，Application第一次冷启动 */
  private boolean isStartUp;
  /** 宿主app的信息。 */
  private PackageInfo hostPkgInfo;
  private int systemPid;
  /** 多线程协同，条件变量。 */
  private ConditionVariable initLock = new ConditionVariable();
  /** 设备ID，蓝牙地址，Mac地址。 */
  private PhoneInfoDelegate phoneInfoDelegate;
  private ComponentDelegate componentDelegate;
  private TaskDescriptionDelegate taskDescriptionDelegate;

  private VirtualCore() {
  }

  public static VirtualCore get() {
    return gCore;
  }

  /** 获取系统PM */
  public static PackageManager getPM() {
    return get().getPackageManager();
  }

  /** 获取程序入口：ActivityThread。 */
  public static Object mainThread() {
    return get().mainThread;
  }

  /** 全局条件变量。 */
  public ConditionVariable getInitLock() {
    return initLock;
  }

  /** UID */
  public int myUid() {
    return myUid;
  }

  /** 获取userID */
  public int myUserId() {
    return VUserHandle.getUserId(myUid);
  }

  public ComponentDelegate getComponentDelegate() {
    return componentDelegate == null ? ComponentDelegate.EMPTY : componentDelegate;
  }

  public void setComponentDelegate(ComponentDelegate delegate) {
    this.componentDelegate = delegate;
  }

  public PhoneInfoDelegate getPhoneInfoDelegate() {
    return phoneInfoDelegate;
  }

  public void setPhoneInfoDelegate(PhoneInfoDelegate phoneInfoDelegate) {
    this.phoneInfoDelegate = phoneInfoDelegate;
  }

  public void setCrashHandler(CrashHandler handler) {
    VClientImpl.get().setCrashHandler(handler);
  }

  public TaskDescriptionDelegate getTaskDescriptionDelegate() {
    return taskDescriptionDelegate;
  }

  public void setTaskDescriptionDelegate(TaskDescriptionDelegate taskDescriptionDelegate) {
    this.taskDescriptionDelegate = taskDescriptionDelegate;
  }

  public int[] getGids() {
    return hostPkgInfo.gids;
  }

  public Context getContext() {
    return context;
  }

  public PackageManager getPackageManager() {
    return context.getPackageManager();
  }

  public String getHostPkg() {
    return hostPkgName;
  }

  public PackageManager getUnHookPackageManager() {
    return unHookPackageManager;
  }

  /**
   * 启动
   * 1. 管理服务缓存
   * 2. 判断在哪个进程
   * 3. 
   *
   * @param context 上下文
   * @throws Throwable 抛出错误
   */
  public void startup(Context context) throws Throwable {
    if (!isStartUp) {
      if (Looper.myLooper() != Looper.getMainLooper()) {
        throw new IllegalStateException("VirtualCore.startup() must called in main thread.");
      }
      VASettings.STUB_CP_AUTHORITY = context.getPackageName() + "." + VASettings.STUB_DEF_AUTHORITY;
      ServiceManagerNative.SERVICE_CP_AUTH =
          context.getPackageName() + "." + ServiceManagerNative.SERVICE_DEF_AUTH;
      this.context = context;
      mainThread = ActivityThread.currentActivityThread.call();
      unHookPackageManager = context.getPackageManager();
      hostPkgInfo = unHookPackageManager.getPackageInfo(context.getPackageName(),
          PackageManager.GET_PROVIDERS);
      IPCBus.initialize(new IServerCache() {
        @Override public void join(String serverName, IBinder binder) {
          ServiceCache.addService(serverName, binder);
        }

        @Override public IBinder query(String serverName) {
          return ServiceManagerNative.getService(serverName);
        }
      });
      detectProcessType();
      InvocationStubManager invocationStubManager = InvocationStubManager.getInstance();
      invocationStubManager.init();
      invocationStubManager.injectAll();
      ContextFixer.fixContext(context);
      isStartUp = true;
      if (initLock != null) {
        initLock.open();
        initLock = null;
      }
    }
  }

  public void waitForEngine() {
    ServiceManagerNative.ensureServerStarted();
  }

  public boolean isEngineLaunched() {
    String engineProcessName = getEngineProcessName();
    ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    for (ActivityManager.RunningAppProcessInfo info : am.getRunningAppProcesses()) {
      if (info.processName.endsWith(engineProcessName)) {
        return true;
      }
    }
    return false;
  }

  public String getEngineProcessName() {
    return context.getString(R.string.engine_process_name);
  }

  public void initialize(VirtualInitializer initializer) {
    if (initializer == null) {
      throw new IllegalStateException("Initializer = NULL");
    }
    switch (processType) {
      case Main:
        initializer.onMainProcess();
        break;
      case VAppClient:
        initializer.onVirtualProcess();
        break;
      case Server:
        initializer.onServerProcess();
        break;
      case CHILD:
        initializer.onChildProcess();
        break;
      default:
    }
  }

  private void detectProcessType() {
    // Host package name
    hostPkgName = context.getApplicationInfo().packageName;
    // Main process name
    mainProcessName = context.getApplicationInfo().processName;
    // Current process name
    processName = ActivityThread.getProcessName.call(mainThread);
    if (processName.equals(mainProcessName)) {
      processType = ProcessType.Main;
      // io.virtualapp:x
    } else if (processName.endsWith(Constants.SERVER_PROCESS_NAME)) {
      processType = ProcessType.Server;
    } else if (VActivityManager.get().isAppProcess(processName)) {
      processType = ProcessType.VAppClient;
    } else {
      processType = ProcessType.CHILD;
    }
    if (isVAppProcess()) {
      systemPid = VActivityManager.get().getSystemPid();
    }
  }

  private IAppManager getService() {
    return singleton.get();
  }

  /**
   * @return If the current process is used to VA.
   */
  public boolean isVAppProcess() {
    return ProcessType.VAppClient == processType;
  }

  /**
   * @return If the current process is the main. 主进程即继承VA的app
   */
  public boolean isMainProcess() {
    return ProcessType.Main == processType;
  }

  /**
   * @return If the current process is the child. 子进程即VA虚拟安装的app
   */
  public boolean isChildProcess() {
    return ProcessType.CHILD == processType;
  }

  /**
   * @return If the current process is the server. io.virtualapp:x 进程，用于虚拟各种系统服务
   */
  public boolean isServerProcess() {
    return ProcessType.Server == processType;
  }

  /**
   * @return the <em>actual</em> process name 在主进程中可能有多个进程在运行，比如app本身，多开的app进程
   */
  public String getProcessName() {
    return processName;
  }

  /**
   * @return the <em>Main</em> process name 主进程
   */
  public String getMainProcessName() {
    return mainProcessName;
  }

  /**
   * Optimize the Dalvik-Cache for the specified package.
   * 优化指定包名的Dalvik缓存
   *
   * @param pkg package name
   */
  @Deprecated public void preOpt(String pkg) throws IOException {
    InstalledAppInfo info = getInstalledAppInfo(pkg, 0);
    if (info != null && !info.dependSystem) {
      DexFile.loadDex(info.apkPath, info.getOdexFile().getPath(), 0).close();
    }
  }

  /**
   * Is the specified app running in foreground / background?
   * 指定的应用是否还在运行中？
   *
   * @param packageName package name
   * @param userId user id
   * @return if the specified app running in foreground / background.
   */
  public boolean isAppRunning(String packageName, int userId) {
    return VActivityManager.get().isAppRunning(packageName, userId);
  }

  public InstallResult installPackage(String apkPath, int flags) {
    try {
      return getService().installPackage(apkPath, flags);
    } catch (RemoteException e) {
      return VirtualRuntime.crash(e);
    }
  }

  public void addVisibleOutsidePackage(String pkg) {
    try {
      getService().addVisibleOutsidePackage(pkg);
    } catch (RemoteException e) {
      VirtualRuntime.crash(e);
    }
  }

  public void removeVisibleOutsidePackage(String pkg) {
    try {
      getService().removeVisibleOutsidePackage(pkg);
    } catch (RemoteException e) {
      VirtualRuntime.crash(e);
    }
  }

  public boolean isOutsidePackageVisible(String pkg) {
    try {
      return getService().isOutsidePackageVisible(pkg);
    } catch (RemoteException e) {
      return VirtualRuntime.crash(e);
    }
  }

  public boolean isAppInstalled(String pkg) {
    try {
      return getService().isAppInstalled(pkg);
    } catch (RemoteException e) {
      return VirtualRuntime.crash(e);
    }
  }

  public boolean isPackageLaunchable(String packageName) {
    InstalledAppInfo info = getInstalledAppInfo(packageName, 0);
    return info != null && getLaunchIntent(packageName, info.getInstalledUsers()[0]) != null;
  }

  public Intent getLaunchIntent(String packageName, int userId) {
    VPackageManager pm = VPackageManager.get();
    Intent intentToResolve = new Intent(Intent.ACTION_MAIN);
    intentToResolve.addCategory(Intent.CATEGORY_INFO);
    intentToResolve.setPackage(packageName);
    List<ResolveInfo> ris =
        pm.queryIntentActivities(intentToResolve, intentToResolve.resolveType(context), 0, userId);

    // Otherwise, try to find a main launcher activity.
    if (ris == null || ris.size() <= 0) {
      // reuse the intent instance
      intentToResolve.removeCategory(Intent.CATEGORY_INFO);
      intentToResolve.addCategory(Intent.CATEGORY_LAUNCHER);
      intentToResolve.setPackage(packageName);
      ris = pm.queryIntentActivities(intentToResolve, intentToResolve.resolveType(context), 0,
          userId);
    }
    if (ris == null || ris.size() <= 0) {
      return null;
    }
    Intent intent = new Intent(intentToResolve);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    intent.setClassName(ris.get(0).activityInfo.packageName, ris.get(0).activityInfo.name);
    return intent;
  }

  public boolean createShortcut(int userId, String packageName, OnEmitShortcutListener listener) {
    return createShortcut(userId, packageName, null, listener);
  }

  public boolean createShortcut(int userId, String packageName, Intent splash,
      OnEmitShortcutListener listener) {
    InstalledAppInfo setting = getInstalledAppInfo(packageName, 0);
    if (setting == null) {
      return false;
    }
    ApplicationInfo appInfo = setting.getApplicationInfo(userId);
    PackageManager pm = context.getPackageManager();
    String name;
    Bitmap icon;
    try {
      CharSequence sequence = appInfo.loadLabel(pm);
      name = sequence.toString();
      icon = BitmapUtils.drawableToBitmap(appInfo.loadIcon(pm));
    } catch (Throwable e) {
      return false;
    }
    if (listener != null) {
      String newName = listener.getName(name);
      if (newName != null) {
        name = newName;
      }
      Bitmap newIcon = listener.getIcon(icon);
      if (newIcon != null) {
        icon = newIcon;
      }
    }
    Intent targetIntent = getLaunchIntent(packageName, userId);
    if (targetIntent == null) {
      return false;
    }
    Intent shortcutIntent = new Intent();
    shortcutIntent.setClassName(getHostPkg(), Constants.SHORTCUT_PROXY_ACTIVITY_NAME);
    shortcutIntent.addCategory(Intent.CATEGORY_DEFAULT);
    if (splash != null) {
      shortcutIntent.putExtra("_VA_|_splash_", splash.toUri(0));
    }
    shortcutIntent.putExtra("_VA_|_intent_", targetIntent);
    shortcutIntent.putExtra("_VA_|_uri_", targetIntent.toUri(0));
    shortcutIntent.putExtra("_VA_|_user_id_", userId);

    Intent addIntent = new Intent();
    addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
    addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);
    addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON, icon);
    addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
    context.sendBroadcast(addIntent);
    return true;
  }

  public boolean removeShortcut(int userId, String packageName, Intent splash,
      OnEmitShortcutListener listener) {
    InstalledAppInfo setting = getInstalledAppInfo(packageName, 0);
    if (setting == null) {
      return false;
    }
    ApplicationInfo appInfo = setting.getApplicationInfo(userId);
    PackageManager pm = context.getPackageManager();
    String name;
    try {
      CharSequence sequence = appInfo.loadLabel(pm);
      name = sequence.toString();
    } catch (Throwable e) {
      return false;
    }
    if (listener != null) {
      String newName = listener.getName(name);
      if (newName != null) {
        name = newName;
      }
    }
    Intent targetIntent = getLaunchIntent(packageName, userId);
    if (targetIntent == null) {
      return false;
    }
    Intent shortcutIntent = new Intent();
    shortcutIntent.setClassName(getHostPkg(), Constants.SHORTCUT_PROXY_ACTIVITY_NAME);
    shortcutIntent.addCategory(Intent.CATEGORY_DEFAULT);
    if (splash != null) {
      shortcutIntent.putExtra("_VA_|_splash_", splash.toUri(0));
    }
    shortcutIntent.putExtra("_VA_|_intent_", targetIntent);
    shortcutIntent.putExtra("_VA_|_uri_", targetIntent.toUri(0));
    shortcutIntent.putExtra("_VA_|_user_id_", VUserHandle.myUserId());

    Intent addIntent = new Intent();
    addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
    addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);
    addIntent.setAction("com.android.launcher.action.UNINSTALL_SHORTCUT");
    context.sendBroadcast(addIntent);
    return true;
  }

  public void setUiCallback(Intent intent, IUiCallback callback) {
    if (callback != null) {
      Bundle bundle = new Bundle();
      BundleCompat.putBinder(bundle, "_VA_|_ui_callback_", callback.asBinder());
      intent.putExtra("_VA_|_sender_", bundle);
    }
  }

  public InstalledAppInfo getInstalledAppInfo(String pkg, int flags) {
    try {
      return getService().getInstalledAppInfo(pkg, flags);
    } catch (RemoteException e) {
      return VirtualRuntime.crash(e);
    }
  }

  public int getInstalledAppCount() {
    try {
      return getService().getInstalledAppCount();
    } catch (RemoteException e) {
      return VirtualRuntime.crash(e);
    }
  }

  public boolean isStartup() {
    return isStartUp;
  }

  public boolean uninstallPackageAsUser(String pkgName, int userId) {
    try {
      return getService().uninstallPackageAsUser(pkgName, userId);
    } catch (RemoteException e) {
      // Ignore
    }
    return false;
  }

  public boolean uninstallPackage(String pkgName) {
    try {
      return getService().uninstallPackage(pkgName);
    } catch (RemoteException e) {
      // Ignore
    }
    return false;
  }

  public Resources getResources(String pkg) throws Resources.NotFoundException {
    InstalledAppInfo installedAppInfo = getInstalledAppInfo(pkg, 0);
    if (installedAppInfo != null) {
      AssetManager assets = mirror.android.content.res.AssetManager.ctor.newInstance();
      mirror.android.content.res.AssetManager.addAssetPath.call(assets, installedAppInfo.apkPath);
      Resources hostRes = context.getResources();
      return new Resources(assets, hostRes.getDisplayMetrics(), hostRes.getConfiguration());
    }
    throw new Resources.NotFoundException(pkg);
  }

  public synchronized ActivityInfo resolveActivityInfo(Intent intent, int userId) {
    ActivityInfo activityInfo = null;
    if (intent.getComponent() == null) {
      ResolveInfo resolveInfo =
          VPackageManager.get().resolveIntent(intent, intent.getType(), 0, userId);
      if (resolveInfo != null && resolveInfo.activityInfo != null) {
        activityInfo = resolveInfo.activityInfo;
        intent.setClassName(activityInfo.packageName, activityInfo.name);
      }
    } else {
      activityInfo = resolveActivityInfo(intent.getComponent(), userId);
    }
    if (activityInfo != null) {
      if (activityInfo.targetActivity != null) {
        ComponentName componentName =
            new ComponentName(activityInfo.packageName, activityInfo.targetActivity);
        activityInfo = VPackageManager.get().getActivityInfo(componentName, 0, userId);
        intent.setComponent(componentName);
      }
    }
    return activityInfo;
  }

  public ActivityInfo resolveActivityInfo(ComponentName componentName, int userId) {
    return VPackageManager.get().getActivityInfo(componentName, 0, userId);
  }

  public ServiceInfo resolveServiceInfo(Intent intent, int userId) {
    ServiceInfo serviceInfo = null;
    ResolveInfo resolveInfo =
        VPackageManager.get().resolveService(intent, intent.getType(), 0, userId);
    if (resolveInfo != null) {
      serviceInfo = resolveInfo.serviceInfo;
    }
    return serviceInfo;
  }

  public void killApp(String pkg, int userId) {
    VActivityManager.get().killAppByPkg(pkg, userId);
  }

  public void killAllApps() {
    VActivityManager.get().killAllApps();
  }

  public List<InstalledAppInfo> getInstalledApps(int flags) {
    try {
      return getService().getInstalledApps(flags);
    } catch (RemoteException e) {
      return VirtualRuntime.crash(e);
    }
  }

  public List<InstalledAppInfo> getInstalledAppsAsUser(int userId, int flags) {
    try {
      return getService().getInstalledAppsAsUser(userId, flags);
    } catch (RemoteException e) {
      return VirtualRuntime.crash(e);
    }
  }

  public void clearAppRequestListener() {
    try {
      getService().clearAppRequestListener();
    } catch (RemoteException e) {
      e.printStackTrace();
    }
  }

  public void scanApps() {
    try {
      getService().scanApps();
    } catch (RemoteException e) {
      // Ignore
    }
  }

  public IAppRequestListener getAppRequestListener() {
    try {
      return getService().getAppRequestListener();
    } catch (RemoteException e) {
      return VirtualRuntime.crash(e);
    }
  }

  public void setAppRequestListener(final AppRequestListener listener) {
    IAppRequestListener inner = new IAppRequestListener.Stub() {
      @Override public void onRequestInstall(final String path) {
        VirtualRuntime.getUIHandler().post(new Runnable() {
          @Override public void run() {
            listener.onRequestInstall(path);
          }
        });
      }

      @Override public void onRequestUninstall(final String pkg) {
        VirtualRuntime.getUIHandler().post(new Runnable() {
          @Override public void run() {
            listener.onRequestUninstall(pkg);
          }
        });
      }
    };
    try {
      getService().setAppRequestListener(inner);
    } catch (RemoteException e) {
      e.printStackTrace();
    }
  }

  public boolean isPackageLaunched(int userId, String packageName) {
    try {
      return getService().isPackageLaunched(userId, packageName);
    } catch (RemoteException e) {
      return VirtualRuntime.crash(e);
    }
  }

  public void setPackageHidden(int userId, String packageName, boolean hidden) {
    try {
      getService().setPackageHidden(userId, packageName, hidden);
    } catch (RemoteException e) {
      e.printStackTrace();
    }
  }

  public boolean installPackageAsUser(int userId, String packageName) {
    try {
      return getService().installPackageAsUser(userId, packageName);
    } catch (RemoteException e) {
      return VirtualRuntime.crash(e);
    }
  }

  public boolean isAppInstalledAsUser(int userId, String packageName) {
    try {
      return getService().isAppInstalledAsUser(userId, packageName);
    } catch (RemoteException e) {
      return VirtualRuntime.crash(e);
    }
  }

  public int[] getPackageInstalledUsers(String packageName) {
    try {
      return getService().getPackageInstalledUsers(packageName);
    } catch (RemoteException e) {
      return VirtualRuntime.crash(e);
    }
  }

  public void registerObserver(IPackageObserver observer) {
    try {
      getService().registerObserver(observer);
    } catch (RemoteException e) {
      VirtualRuntime.crash(e);
    }
  }

  public void unregisterObserver(IPackageObserver observer) {
    try {
      getService().unregisterObserver(observer);
    } catch (RemoteException e) {
      VirtualRuntime.crash(e);
    }
  }

  public boolean isOutsideInstalled(String packageName) {
    try {
      return unHookPackageManager.getApplicationInfo(packageName, 0) != null;
    } catch (PackageManager.NameNotFoundException e) {
      // Ignore
    }
    return false;
  }

  public int getSystemPid() {
    return systemPid;
  }

  /**
   * Process type
   */
  private enum ProcessType {
    /**
     * Server process
     */
    Server,
    /**
     * Virtual app process
     */
    VAppClient,
    /**
     * Main process
     */
    Main,
    /**
     * Child process
     */
    CHILD
  }

  public interface AppRequestListener {
    void onRequestInstall(String path);

    void onRequestUninstall(String pkg);
  }

  public interface OnEmitShortcutListener {
    Bitmap getIcon(Bitmap originIcon);

    String getName(String originName);
  }

  public abstract static class UiCallback extends IUiCallback.Stub {
  }

  public abstract static class PackageObserver extends IPackageObserver.Stub {
  }

  public static abstract class VirtualInitializer {
    public void onMainProcess() {
    }

    public void onVirtualProcess() {
    }

    public void onServerProcess() {
    }

    public void onChildProcess() {
    }
  }
}
