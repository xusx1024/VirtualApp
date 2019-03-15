package com.hairui.javaproxy.dynamic;

import com.hairui.javaproxy.Shopping;
import com.hairui.javaproxy.statik.ShoppingImpl;
import java.lang.reflect.Proxy;

/**
 * Fun:
 *
 * @author sxx.xu@shunwang.com on 3/15/2019.
 */
public class Test {
  public static void main(String[] args) {
    Shopping shopping = new ShoppingImpl();
    shopping.doShopping(200);
    shopping =
        (Shopping) Proxy.newProxyInstance(Shopping.class.getClassLoader(), shopping.getClass().getInterfaces(),
            new ShoppingHandler(shopping));
    shopping.doShopping(200);
  }
}
