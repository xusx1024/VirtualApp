package com.hairui.javaproxy.dynamic;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Fun:
 *
 * @author sxx.xu@shunwang.com on 3/15/2019.
 */
public class ShoppingHandler implements InvocationHandler {

  /**
   * 被代表的对象
   */
  Object original;

  public ShoppingHandler(Object original) {
    this.original = original;
  }

  @Override public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
    if ("doShopping".equals(method.getName())) {
      long money = (long) objects[0] / 2;
      Object[] things = (Object[]) method.invoke(original, money);
      return things;
    }
    return null;
  }
}
