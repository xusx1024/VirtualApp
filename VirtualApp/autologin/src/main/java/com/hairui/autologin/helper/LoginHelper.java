package com.hairui.autologin.helper;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.view.accessibility.AccessibilityEvent;

/**
 * Fun: 无障碍服务登录账号
 *
 * 服务连接 -> 事件触发 -> 事件中断 -> 断开连接
 *
 * @author sxx.xu@shunwang.com on 3/13/2019.
 */
public class LoginHelper extends AccessibilityService {

  private AccessibilityServiceInfo info = new AccessibilityServiceInfo();

  /**
   * This method is a part of the {@link AccessibilityService} lifecycle and is
   * called after the system has successfully bound to the service. If is
   * convenient to use this method for setting the {@link AccessibilityServiceInfo}.
   *
   * @see AccessibilityServiceInfo
   * @see #setServiceInfo(AccessibilityServiceInfo)
   */
  @Override protected void onServiceConnected() {
    super.onServiceConnected();
    // 事件过滤
    info.eventTypes = AccessibilityEvent.TYPE_VIEW_CLICKED | AccessibilityEvent.TYPE_VIEW_FOCUSED;
    // 应用过滤
    info.packageNames =
        new String[] { "com.hairui.autologin", "com.tencent.mobileqq", "com.tencent.tmgp.sgame" };
    // 无障碍反馈
    info.feedbackType = AccessibilityServiceInfo.FEEDBACK_VISUAL;
    info.notificationTimeout = 1000;

    setServiceInfo(info);
  }

  /**
   * Callback for {@link AccessibilityEvent}s.
   *
   * @param event The new event. This event is owned by the caller and cannot be used after
   * this method returns. Services wishing to use the event after this method returns should
   * make a copy.
   */
  @Override public void onAccessibilityEvent(AccessibilityEvent event) {

  }

  /**
   * Callback for interrupting the accessibility feedback.
   */
  @Override public void onInterrupt() {

  }

  /**
   * Called when all clients have disconnected from a particular interface
   * published by the service.  The default implementation does nothing and
   * returns false.
   *
   * @param intent The Intent that was used to bind to this service,
   * as given to {@link Context#bindService
   * Context.bindService}.  Note that any extras that were included with
   * the Intent at that point will <em>not</em> be seen here.
   * @return Return true if you would like to have the service's
   * {@link #onRebind} method later called when new clients bind to it.
   */
  @Override public boolean onUnbind(Intent intent) {
    return super.onUnbind(intent);
  }
}
