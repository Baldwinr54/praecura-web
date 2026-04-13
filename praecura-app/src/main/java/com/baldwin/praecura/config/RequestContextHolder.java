package com.baldwin.praecura.config;

/**
 * ThreadLocal holder for request context.
 *
 * Important: always cleared by RequestContextFilter.
 */
public final class RequestContextHolder {

  private static final ThreadLocal<RequestContext> CTX = new ThreadLocal<>();

  private RequestContextHolder() {}

  public static void set(RequestContext context) {
    CTX.set(context);
  }

  public static RequestContext get() {
    return CTX.get();
  }

  public static void clear() {
    CTX.remove();
  }
}
