package com.hairui.autologin;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.hairui.autologin.helper.AccessibilityUtil;
import com.hairui.autologin.helper.BaseAccessibilityService;
import com.hairui.autologin.helper.QqLoginAccessibilityService;
import com.hairui.autologin.kit.JavaKit;
import com.hairui.autologin.kit.VUiKit;
import com.hairui.autologin.models.AppData;
import com.hairui.autologin.models.AppInfo;
import com.hairui.autologin.models.AppInfoLite;
import com.hairui.autologin.models.MultiplePackageAppData;
import com.hairui.autologin.models.PackageAppData;
import com.hairui.autologin.repo.AppDataSource;
import com.hairui.autologin.repo.AppRepository;
import com.hairui.autologin.repo.PackageAppDataStorage;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.os.VUserInfo;
import com.lody.virtual.os.VUserManager;
import com.lody.virtual.remote.InstallResult;
import com.lody.virtual.remote.InstalledAppInfo;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * 以王者荣耀(com.tencent.tmgp.sgame)为例尝试多开
 * 上号器
 *
 * @author sxx.xu
 */
public class MainActivity extends AppCompatActivity {

  private static final String TAG = MainActivity.class.getSimpleName();
  private static final String QQ_PKG_NAME = "com.tencent.mobileqq";
  private static final String WZRY_PKG_NAME = "com.tencent.tmgp.sgame";
  List<AppInfo> allLocalApps;
  List<AppData> localVirtualApps;
  BaseAccessibilityService service = BaseAccessibilityService.getInstance();
  private EditText mToken;
  private Button mLogin;
  private TextView mInfo;
  private ProgressBar mProgressBar;
  private StringBuilder mInfoContent = new StringBuilder();
  /**
   * 多开的软件们
   */
  private AppRepository mRepo;
  /**
   * 系统的软件们
   */
  private AppDataSource mRepository;
  private boolean hadFakeRequest = false;
  private boolean hasLocalApp = false;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    Toolbar toolbar = findViewById(R.id.toolbar);
    toolbar.setTitle("上号器");
    setSupportActionBar(toolbar);
    initView();
    fakeRequest();
    service.init(this);
    mLogin.setOnClickListener(v -> {
      if (hadFakeRequest) {
        clearNoteInfo();
      }
      CheckLocalData();
    });
  }

  @Override protected void onResume() {
    super.onResume();
    checkSDCard();
    checkAccessibilityPermission();
  }

  private void checkAccessibilityPermission() {
    boolean hasPermission =
        AccessibilityUtil.isSettingOpen(QqLoginAccessibilityService.class, MainActivity.this);
    if (!hasPermission) {
      AccessibilityUtil.checkSetting(MainActivity.this, QqLoginAccessibilityService.class);
    }
  }

  private void checkSDCard() {
    int permissionCheck =
        ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
    if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(this,
          new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE }, 100);
    }
  }

  /**
   * 本地查找
   */
  private void CheckLocalData() {
    mRepo = new AppRepository(this);
    mRepository = new AppRepository(this);
    showProgress();
    refreshNoteInfo("查找本地已多开应用列表。。。");
    PackageAppData appData = PackageAppDataStorage.get().acquire(WZRY_PKG_NAME);
    if (appData == null) {
      multiOpen();
    } else {
      refreshNoteInfo("找到应用：\n" + appData.toString());
      Log.e(TAG, appData.toString());

      refreshNoteInfo("启动游戏前准备。。。");
      handleOptApp(appData, appData.packageName, true);
    }
  }

  /**
   * 尝试多开
   */
  private void multiOpen() {
    refreshNoteInfo("查找应用列表。。。");
    mRepository.getInstalledApps(this).done(result -> {
      allLocalApps = result;
      for (AppInfo ai : allLocalApps) {
        if (WZRY_PKG_NAME.equals(ai.packageName)) {
          refreshNoteInfo("找到应用：\n" + ai.toString());
          Log.e(TAG, ai.toString());
          refreshNoteInfo("添加进多开列表。。。");
          addApp(new AppInfoLite(ai.packageName, ai.path, ai.fastOpen));
          break;
        }
      }
    });
  }

  /**
   * 多开游戏加入本地
   */
  private void addApp(AppInfoLite info) {
    class AddResult {
      private PackageAppData appData;
      private int userId;
      private boolean justEnableHidden;
    }
    AddResult addResult = new AddResult();
    VUiKit.defer().when(() -> {
      InstalledAppInfo installedAppInfo =
          VirtualCore.get().getInstalledAppInfo(info.packageName, 0);
      addResult.justEnableHidden = installedAppInfo != null;
      refreshNoteInfo("是否已安装？：" + addResult.justEnableHidden);
      refreshNoteInfo("启用多用户？：" + addResult.justEnableHidden);
      if (addResult.justEnableHidden) {
        int[] userIds = installedAppInfo.getInstalledUsers();
        int nextUserId = userIds.length;
                /*
                  Input : userIds = {0, 1, 3}
                  Output: nextUserId = 2
                 */
        for (int i = 0; i < userIds.length; i++) {
          if (userIds[i] != i) {
            nextUserId = i;
            break;
          }
        }
        addResult.userId = nextUserId;
        /**
         * 创建用户
         */
        if (VUserManager.get().getUserInfo(nextUserId) == null) {
          // user not exist, create it automatically.
          String nextUserName = "Space " + (nextUserId + 1);
          VUserInfo newUserInfo = VUserManager.get().createUser(nextUserName, VUserInfo.FLAG_ADMIN);
          if (newUserInfo == null) {
            throw new IllegalStateException();
          }
          refreshNoteInfo("创建多用户：" + newUserInfo.toString());
        }
        /**
         * 为创建的用户安装app
         * {@link VAppManagerService#installPackageAsUser(int, java.lang.String)}
         */
        boolean success = VirtualCore.get().installPackageAsUser(nextUserId, info.packageName);
        refreshNoteInfo("安装多开应用：" + success);
        if (!success) {
          throw new IllegalStateException();
        }
      } else {
        InstallResult res = mRepo.addVirtualApp(info);
        refreshNoteInfo("应用加入本地多开列表：" + res.isSuccess);
        if (!res.isSuccess) {
          throw new IllegalStateException();
        }
      }
    }).then((res) -> {
      addResult.appData = PackageAppDataStorage.get().acquire(info.packageName);
      refreshNoteInfo("从本地多开列表查询：" + addResult.appData.getName());
    }).done(res -> {
      /**
       * 多分身
       */
      boolean multipleVersion = addResult.justEnableHidden && addResult.userId != 0;
      refreshNoteInfo("是否是本地多个多开：" + multipleVersion);
      if (!multipleVersion) {
        PackageAppData data = addResult.appData;
        data.isLoading = true;
        handleOptApp(data, info.packageName, true);
      } else {
        MultiplePackageAppData data =
            new MultiplePackageAppData(addResult.appData, addResult.userId);
        data.isLoading = true;
        handleOptApp(data, info.packageName, false);
      }
    });
  }

  /**
   * dex 优化
   */
  private void handleOptApp(AppData data, String packageName, boolean needOpt) {
    VUiKit.defer().when(() -> {
      long time = System.currentTimeMillis();
      if (needOpt) {
        try {
          VirtualCore.get().preOpt(packageName);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      time = System.currentTimeMillis() - time;
      refreshNoteInfo("应用启用了odex？" + needOpt);
      refreshNoteInfo("准备odex时间:" + time + "ms");
      if (time < 3000L) {
        try {
          Thread.sleep(3000L - time);
        } catch (InterruptedException e) {
          refreshNoteInfo("准备odex异常:" + e.getLocalizedMessage());
          e.printStackTrace();
        }
      }
    }).done((res) -> {
      if (data instanceof PackageAppData) {
        ((PackageAppData) data).isLoading = false;
        ((PackageAppData) data).isFirstOpen = true;
        launchApp(data);
      } else if (data instanceof MultiplePackageAppData) {
        ((MultiplePackageAppData) data).isLoading = false;
        ((MultiplePackageAppData) data).isFirstOpen = true;
      }
    });
  }

  /**
   * 开启游戏
   */
  private void launchApp(AppData data) {
    hideProgress();
    refreshNoteInfo("启动游戏：" + data.getName());
    try {
      PackageAppData appData = (PackageAppData) data;
      Log.e(TAG, appData.toString());
      appData.isFirstOpen = false;
      int userId = 0;
      String pkg = appData.packageName;
      appData = PackageAppDataStorage.get().acquire(pkg);
      Intent intent = VirtualCore.get().getLaunchIntent(appData.packageName, userId);
      VirtualCore.get().setUiCallback(intent, new VirtualCore.UiCallback() {

        @Override public void onAppOpened(String packageName, int userId) throws RemoteException {
          // you can finish this
        }
      });
      PackageAppData finalAppData = appData;
      VUiKit.defer().when(() -> {
        if (!finalAppData.fastOpen) {
          try {
            VirtualCore.get().preOpt(finalAppData.packageName);
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
        VActivityManager.get().startActivity(intent, userId);
      });
    } catch (Throwable e) {
      e.printStackTrace();
      refreshNoteInfo("启动失败：" + e.getLocalizedMessage());
    }
  }

  private void refreshNoteInfo(String newInfo) {
    mInfoContent.append(newInfo);
    mInfoContent.append("\n");
    mInfo.setText(mInfoContent.toString());
  }

  private void clearNoteInfo() {
    mInfoContent = new StringBuilder();
    refreshNoteInfo("");
  }

  public void hideProgress() {
    mProgressBar.setVisibility(View.GONE);
  }

  public void showProgress() {
    mProgressBar.setVisibility(View.VISIBLE);
  }

  private void initView() {
    mToken = findViewById(R.id.textInputEditText);
    mLogin = findViewById(R.id.login);
    mInfo = findViewById(R.id.otherInfo);
    mInfo.setMovementMethod(ScrollingMovementMethod.getInstance());
    mProgressBar = findViewById(R.id.loading);
    mProgressBar.setVisibility(View.GONE);
    mToken.setText(JavaKit.fakeToken(18));
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    hideProgress();
    clearNoteInfo();
  }

  /**
   * 伪请求
   */
  private void fakeRequest() {
    showProgress();
    new Handler().postDelayed(() -> {
      hideProgress();
      refreshNoteInfo("上号码有效！");
      refreshNoteInfo("套餐：3h");
      long start = System.currentTimeMillis();
      long end = start + 60 * 60 * 1000 * 3;
      SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      refreshNoteInfo("开始时间：" + simpleDateFormat.format(start));
      refreshNoteInfo("结束时间：" + simpleDateFormat.format(end));
      refreshNoteInfo("操作系统：Android");
      refreshNoteInfo("游戏名称：王者荣耀");
      refreshNoteInfo("》》》》》》》》》信息有效，请点击上号");
      refreshNoteInfo("\n\n");
      hadFakeRequest = true;
    }, 3000);
  }
}
