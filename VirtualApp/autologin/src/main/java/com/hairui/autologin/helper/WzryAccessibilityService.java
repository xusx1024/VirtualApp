package com.hairui.autologin.helper;

import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

/**
 * Fun: 王者荣耀UI
 *
 * @author sxx.xu@shunwang.com on 3/13/2019.
 */
public class WzryAccessibilityService extends BaseAccessibilityService {

  private static final String TAG = WzryAccessibilityService.class.getSimpleName();
  private static final String LOGOUT = "注销";
  private static final String QQ_LOGIN = "与QQ好友玩";
  private String mPkgName;
  private int times = 0;
  private int totalTimeCost = 0;

  @Override public void onAccessibilityEvent(AccessibilityEvent event) {
    mPkgName = event.getPackageName().toString();
    Log.e(TAG, mPkgName);
    if (PackageName.WZRY.getPkgName().equals(mPkgName)) {
      goThrough();
    }
  }

  /**
   * 此处要遍历"注销" "与QQ好友玩"
   */
  private boolean goThrough() {
    times++;
    Log.e(TAG, "获取当前页面元素:" + getRootInActiveWindow().getChildCount());
    AccessibilityNodeInfo node = findViewByText(LOGOUT);
    while (node == null && times < 20) {
      try {
        int t = times * 300;
        totalTimeCost += t;
        Thread.sleep(t);
        Log.e(TAG, "第" + times + "次没有找到注销或者QQ登录相关元素," + t + "豪秒后将重试");
        goThrough();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    if (node == null) {
      Log.e(TAG, "耗时" + totalTimeCost + "毫秒，没有找到注销或者QQ登录相关元素");
      return false;
    }

    if (node.getText() != null && node.getText().toString().contains(LOGOUT)) {
      performViewClick(node);
      Log.e(TAG, "王者荣耀已登录账号注销成功！");
      return true;
    }

    node = findViewByText(QQ_LOGIN);
    if (node.getText() != null && node.getText().toString().contains(QQ_LOGIN)) {
      performViewClick(node);
      Log.e(TAG, "点击与QQ好友玩！");
      return true;
    }
    Log.e(TAG, "没有找到注销或者QQ登录相关元素！");
    return false;
  }
}
