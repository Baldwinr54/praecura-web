package com.baldwin.praecura.service;

import com.baldwin.praecura.entity.EcfStatus;
import com.baldwin.praecura.entity.ElectronicFiscalDocument;

public interface DgiiEcfClient {

  SubmitResult submit(ElectronicFiscalDocument document);

  TrackResult track(ElectronicFiscalDocument document);

  record SubmitResult(String trackId, EcfStatus status, String statusCode, String message) {
  }

  record TrackResult(EcfStatus status, String statusCode, String message) {
  }
}
