package com.painless.pc.util;

import java.lang.reflect.Method;

import android.content.Context;

import com.painless.pc.singleton.Debug;

public final class ReflectionUtil {

  private final Object target;
  private final Class<?> clazz;

  public ReflectionUtil(Object manager) {
    target = manager;
    clazz = manager == null ? null : manager.getClass();
  }

  public ReflectionUtil(Object manager, Class<?> clazz) {
    target = manager;
    this.clazz = clazz;
  }

  public boolean isNull() {
    return target == null;
  }

  @SuppressWarnings("rawtypes")
  public Object invokeGetter(String method) {
    //	  Debug.log(target);
    //	  Debug.log(clazz);
    //	  for (Method m : clazz.getDeclaredMethods()) {
    //	    Debug.log(m.getName());
    //	  }
    try {
      final Class[] arrayOfClass = new Class[0];

      final Method localMethod = clazz.getDeclaredMethod(method, arrayOfClass);
      localMethod.setAccessible(true);
      return localMethod.invoke(target, null);
    } catch (final Throwable localException) {
      Debug.log(localException);
      return null;
    }
  }

  @SuppressWarnings("rawtypes")
  public Object invokeSetter(String method, Class paramType, Object paramValue) {
    return invokeSetter(method, paramType, paramValue, new boolean[1]);
  }

  public Object invokeSetter(String method, Class paramType, Object paramValue, boolean[] wasError) {
    try {
      final Class[] arrayOfClass = new Class[] { paramType };
      final Object[] arrayOfObject = new Object[] { paramValue };
      return getMethod(method, arrayOfClass).invoke(target, arrayOfObject);
    } catch (final Throwable e) {
      Debug.log(e);
      wasError[0] = true;
      return null;
    }
  }

  public Method getMethod(String method, Class... arrayOfClass) {
    try {
      final Method localMethod = clazz.getDeclaredMethod(method, arrayOfClass);
      localMethod.setAccessible(true);
      return localMethod;
    } catch (final Throwable e) {
      return null;
    }
  }

  public static ReflectionUtil get(Context context, String... services) {
    Object manager = null;
    if (services.length == 0) {
      services = new String[] { Context.CONNECTIVITY_SERVICE };
    }

    for (String service : services) {
      manager = getService(context, service);
      if (manager != null) {
        break;
      }
    }

    return new ReflectionUtil(manager);
  }

  private static Object getService(Context context, String service) {
    try {
      return context.getSystemService(service);
    } catch (final Throwable e) {
      return null;
    }
  }
}
