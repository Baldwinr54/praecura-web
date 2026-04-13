package com.baldwin.praecura.controller;

import com.baldwin.praecura.config.RequestContextHolder;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Health endpoint ligero (sin dependencias extra).
 * Útil para verificaciones rápidas en despliegue / Docker.
 */
@RestController
public class SystemController {

  @GetMapping("/system/health")
  public Map<String, Object> health() {
    var ctx = RequestContextHolder.get();

    Map<String, Object> out = new LinkedHashMap<>();
    out.put("status", "UP");
    out.put("timestamp", Instant.now().toString());
    if (ctx != null) {
      out.put("requestId", ctx.getRequestId());
    }
    return out;
  }
}
