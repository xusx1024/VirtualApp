package com.lody.virtual.helper.ipcbus;

/**
 * 进程间通信的单例，T标识各种系统服务。
 * 比如AccountManagerService.账户管理服务。
 *
 * @author Lody
 */
public class IPCSingleton<T> {

  private Class<?> ipcClass;
  private T instance;

  public IPCSingleton(Class<?> ipcClass) {
    this.ipcClass = ipcClass;
  }

  public T get() {
    if (instance == null) {
      synchronized (this) {
        if (instance == null) {
          instance = IPCBus.get(ipcClass);
        }
      }
    }
    return instance;
  }
}
