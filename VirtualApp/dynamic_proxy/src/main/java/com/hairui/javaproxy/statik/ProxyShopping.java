package com.hairui.javaproxy.statik;

import com.hairui.javaproxy.Shopping;

/**
 * Fun:
 *
 * @author sxx.xu@shunwang.com on 3/15/2019.
 */
public class ProxyShopping implements Shopping {
  Shopping base;

  public ProxyShopping(Shopping base) {
    this.base = base;
  }

  /**
   * 购物行为
   */
  @Override public Object[] doShopping(long money) {
    long readMoney = money / 2;

    Object[] things = base.doShopping(readMoney);

    return things;
  }
}
