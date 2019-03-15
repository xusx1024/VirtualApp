package com.hairui.javaproxy.statik;

import com.hairui.javaproxy.Shopping;

/**
 * Fun:
 *
 * @author sxx.xu@shunwang.com on 3/15/2019.
 */
public class ShoppingImpl implements Shopping {
  /**
   * 购物行为
   */
  @Override public Object[] doShopping(long money) {
    System.out.println(money + "购买了a,b,c.");
    return new Object[] { "a", "b", "c" };
  }
}
