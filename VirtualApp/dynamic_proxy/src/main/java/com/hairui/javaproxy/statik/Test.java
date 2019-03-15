package com.hairui.javaproxy.statik;

import com.hairui.javaproxy.Shopping;

/**
 * Fun:
 *
 * @author sxx.xu@shunwang.com on 3/15/2019.
 */
public class Test {
  public static void main(String[] args) {
    Shopping shopping = new ShoppingImpl();
    shopping.doShopping(100);

    shopping = new ProxyShopping(shopping);
    shopping.doShopping(100);
  }
}
