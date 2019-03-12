package com.hairui.autologin.kit;

import java.util.Random;

/**
 * Fun:
 *
 * @author sxx.xu@shunwang.com on 3/12/2019.
 */
public class JavaKit {
  /**
   * 生成随机数当作getItemID
   * n ： 需要的长度
   */
  public static String fakeToken(int n) {
    String val = "";
    Random random = new Random();
    for (int i = 0; i < n; i++) {
      String str = random.nextInt(2) % 2 == 0 ? "num" : "char";
      // 产生字母
      if ("char".equalsIgnoreCase(str)) {
        int nextInt = random.nextInt(2) % 2 == 0 ? 65 : 97;
        // System.out.println(nextInt + "!!!!"); 1,0,1,1,1,0,0
        val += (char) (nextInt + random.nextInt(26));
        // 产生数字
      } else if ("num".equalsIgnoreCase(str)) {
        val += String.valueOf(random.nextInt(10));
      }
    }
    return val;
  }
}
