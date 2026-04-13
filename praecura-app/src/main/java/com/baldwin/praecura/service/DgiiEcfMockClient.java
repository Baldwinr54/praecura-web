package com.baldwin.praecura.service;

import com.baldwin.praecura.entity.EcfStatus;
import com.baldwin.praecura.entity.ElectronicFiscalDocument;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "praecura.dgii", name = "mode", havingValue = "mock", matchIfMissing = true)
public class DgiiEcfMockClient implements DgiiEcfClient {

  @Value("${praecura.dgii.mock-auto-accept:true}")
  private boolean mockAutoAccept;

  @Override
  public SubmitResult submit(ElectronicFiscalDocument document) {
    String track = "MOCK-" + LocalDateTime.now().toLocalDate() + "-" + ThreadLocalRandom.current().nextInt(100000, 999999);
    return new SubmitResult(track, EcfStatus.SENT, "MOCK_SENT", "Envío mock aceptado para procesamiento.");
  }

  @Override
  public TrackResult track(ElectronicFiscalDocument document) {
    if (mockAutoAccept) {
      return new TrackResult(EcfStatus.ACCEPTED, "MOCK_ACCEPTED", "e-CF aceptado en modo mock.");
    }
    return new TrackResult(EcfStatus.SENT, "MOCK_PENDING", "e-CF aún en cola mock.");
  }
}
