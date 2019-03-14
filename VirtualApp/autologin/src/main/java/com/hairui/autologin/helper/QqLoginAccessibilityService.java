package com.hairui.autologin.helper;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Bundle;
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
      AccessibilityNodeInfo info = getRootInActiveWindow();
      if (info == null) {
        return;
      }
      if (info.getChildCount() == 2) {
        goLoginPage();
      } else {
        goThrough(info);
      }
    }
  }

  private void goLoginPage() {
    Log.e(TAG, "获取当前页面元素:" + getRootInActiveWindow().getChild(0).toString());
    AccessibilityNodeInfo info = findViewByText("登 录", true);
    info.performAction(AccessibilityNodeInfo.ACTION_CLICK);
  }

  /**
   * 此处要登录
   */
  private boolean goThrough(AccessibilityNodeInfo info) {

    if (info.getChildCount() == 0) {

      if (info.toString().contains("请输入QQ号码或手机或邮箱")
          && info.toString().contains("QQ号/手机号/邮箱")
          && "android.widget.EditText".equals(info.getClassName())) {
        ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clipData = ClipData.newPlainText("account", "2103719357");
        clipboardManager.setPrimaryClip(clipData);
        info.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
        info.performAction(AccessibilityNodeInfo.ACTION_PASTE);
      } else if (info.toString().contains("密码 安全") && "android.widget.EditText".equals(
          info.getClassName())) {
        Bundle args = new Bundle();
        args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
            "xu13324506");
        info.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
      } else if (info.toString().contains("登 录") && "android.widget.Button".equals(
          info.getClassName())) {
        info.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
        AccessibilityNodeInfo parent = info;
        while (parent != null) {
          if (parent.isClickable()) {
            parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            break;
          }
          parent = parent.getParent();
        }
      }
    } else {
      for (int i = 0; i < info.getChildCount(); i++) {
        if (info.getChild(i) != null) {
          goThrough(info.getChild(i));
        }
      }
    }
    Log.e(TAG, "什么都没找到");
    return false;
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
    PWD("密码 安全"),
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
