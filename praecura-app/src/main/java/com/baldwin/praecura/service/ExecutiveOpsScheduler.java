package com.baldwin.praecura.service;

import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ExecutiveOpsScheduler {

  private final ExecutiveOpsService executiveOpsService;

  @Value("${praecura.ops.auto-generate-alerts:true}")
  private boolean autoGenerateAlerts;

  @Value("${praecura.ops.auto-generate-closing:true}")
  private boolean autoGenerateClosing;

  public ExecutiveOpsScheduler(ExecutiveOpsService executiveOpsService) {
    this.executiveOpsService = executiveOpsService;
  }

  @Scheduled(cron = "${praecura.ops.alerts-cron:0 */30 * * * *}")
  public void generateAlerts() {
    if (!autoGenerateAlerts) return;
    executiveOpsService.generateAlerts(LocalDate.now());
  }

  @Scheduled(cron = "${praecura.ops.closing-cron:0 10 23 * * *}")
  public void generateDailyClosing() {
    if (!autoGenerateClosing) return;
    executiveOpsService.generateDailyClosing(LocalDate.now(), "system");
  }
}
