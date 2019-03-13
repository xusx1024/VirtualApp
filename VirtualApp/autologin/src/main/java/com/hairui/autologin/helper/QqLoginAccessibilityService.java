package com.hairui.autologin.helper;

import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

/**
 * Fun: QQ登录
 * 此处区分 Webview & 原生QQUI
 *
 * @author sxx.xu@shunwang.com on 3/13/2019.
 */
public class QqLoginAccessibilityService extends BaseAccessibilityService {
  private static final String TAG = QqLoginAccessibilityService.class.getSimpleName();
  private String mPkgName;

  @Override public void onAccessibilityEvent(AccessibilityEvent event) {
    mPkgName = event.getPackageName().toString();
    Log.e(TAG, mPkgName);
    if (PackageName.QQ.getPkgName().equals(mPkgName)) {
      goThrough();
    }
  }

  /**
   * 此处要登录
   */
  private boolean goThrough() {
    int count = getRootInActiveWindow().getChildCount();
    Log.e(TAG, "获取当前页面元素:" + count);
    for (int i = 0; i < count; i++) {
      Log.e(TAG, "获取当前页面元素:" + getRootInActiveWindow().getChild(i).toString());
    }

    if (findViewByID(QQ.LOGIN1.getTxt()) != null && findViewByID(QQ.REGISTER.getTxt()) != null) {
      performViewClick(findViewByID(QQ.LOGIN1.getTxt()));
      Log.e(TAG, "登录、新用户页面点击成功！");
      return true;
    }

    AccessibilityNodeInfo node = findViewByText(QQ.ACCOUNT.getTxt());
    if (node == null) {
      Log.e(TAG, "登录页面输入账号元素未找到！");
      return false;
    } else {
      node.setText("2103719357");
    }

    node = findViewByID(QQ.PWD.getTxt());
    if (node == null) {
      Log.e(TAG, "登录页面密码元素未找到！");
      return false;
    } else {
      node.setText("xu13324506");
    }

    node = findViewByID(QQ.LOGIN2.getTxt());
    if (node == null) {
      Log.e(TAG, "登录页面登录按钮元素未找到！");
      return false;
    } else {
      performViewClick(node);
      return true;
    }
  }

  enum QQ {

    /** 登录注册页面的登录 */
    LOGIN1("btn_login"),
    /** 登录注册页面的注册 */
    REGISTER("btn_register"),
    /** 登录页面的登录 */
    LOGIN2("login"),
    /** 登录注册页面的账号 */
    ACCOUNT("QQ号/手机号/邮箱"),
    /** 登录注册页面的密码 */
    PWD("password"),
    ;
    private String txt;

    QQ(String btn_login) {
      this.txt = btn_login;
    }

    public String getTxt() {
      return txt;
    }

  }
}
