package com.baldwin.praecura.config;

/**
 * Contexto inmutable por-request, usado principalmente para logging/correlación.
 */
public final class RequestContext {

  private final String requestId;
  private final String ipAddress;
  private final String userAgent;

  public RequestContext(String requestId, String ipAddress, String userAgent) {
    this.requestId = requestId;
    this.ipAddress = ipAddress;
    this.userAgent = userAgent;
  }

  public String getRequestId() {
    return requestId;
  }

  public String getIpAddress() {
    return ipAddress;
  }

  public String getUserAgent() {
    return userAgent;
  }
}
