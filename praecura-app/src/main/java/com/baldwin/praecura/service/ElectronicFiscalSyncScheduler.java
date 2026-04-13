package com.baldwin.praecura.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ElectronicFiscalSyncScheduler {

  private static final Logger log = LoggerFactory.getLogger(ElectronicFiscalSyncScheduler.class);

  private final ElectronicFiscalDocumentService electronicFiscalDocumentService;

  @Value("${praecura.dgii.poll-enabled:true}")
  private boolean pollEnabled;

  @Value("${praecura.dgii.poll-batch-size:30}")
  private int batchSize;

  public ElectronicFiscalSyncScheduler(ElectronicFiscalDocumentService electronicFiscalDocumentService) {
    this.electronicFiscalDocumentService = electronicFiscalDocumentService;
  }

  @Scheduled(fixedDelayString = "${praecura.dgii.poll-fixed-delay-ms:180000}")
  public void run() {
    if (!pollEnabled) return;
    try {
      ElectronicFiscalDocumentService.ProcessResult result = electronicFiscalDocumentService.processAutomaticSync(batchSize);
      if (result.syncedCount() > 0 || result.retriedCount() > 0) {
        log.info("e-CF sync job: synced={}, retried={}", result.syncedCount(), result.retriedCount());
      }
    } catch (Exception ex) {
      log.error("e-CF sync job failed: {}", ex.getMessage());
    }
  }
}
