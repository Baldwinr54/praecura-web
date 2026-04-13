package com.baldwin.praecura.service;

import com.baldwin.praecura.entity.FiscalSequence;
import com.baldwin.praecura.repository.FiscalSequenceRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FiscalSequenceService {

  private final FiscalSequenceRepository fiscalSequenceRepository;

  public FiscalSequenceService(FiscalSequenceRepository fiscalSequenceRepository) {
    this.fiscalSequenceRepository = fiscalSequenceRepository;
  }

  public List<FiscalSequence> listActive() {
    return fiscalSequenceRepository.findByActiveTrueOrderByTypeCodeAsc();
  }

  public List<FiscalSequence> listAll() {
    return fiscalSequenceRepository.findAll();
  }

  @Transactional
  public FiscalSequence create(FiscalSequence seq) {
    normalize(seq);
    if (seq.getStartNumber() <= 0) {
      throw new IllegalArgumentException("El rango inicial debe ser mayor a 0.");
    }
    if (seq.getNextNumber() <= 0) {
      seq.setNextNumber(seq.getStartNumber());
    }
    if (seq.getEndNumber() != null && seq.getEndNumber() < seq.getStartNumber()) {
      throw new IllegalArgumentException("El rango final no puede ser menor que el inicial.");
    }
    seq.setActive(true);
    seq.setCreatedAt(LocalDateTime.now());
    seq.setUpdatedAt(LocalDateTime.now());
    return fiscalSequenceRepository.save(seq);
  }

  @Transactional
  public void deactivate(Long id) {
    FiscalSequence seq = fiscalSequenceRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Secuencia fiscal no encontrada."));
    seq.setActive(false);
    seq.setUpdatedAt(LocalDateTime.now());
    fiscalSequenceRepository.save(seq);
  }

  @Transactional
  public String nextNcf(String typeCode) {
    String code = typeCode == null ? "" : typeCode.trim().toUpperCase();
    FiscalSequence seq = fiscalSequenceRepository.findActiveForUpdate(code)
        .orElseThrow(() -> new IllegalArgumentException("No existe una secuencia fiscal activa para " + code));

    if (seq.getExpiresAt() != null && seq.getExpiresAt().isBefore(LocalDate.now())) {
      throw new IllegalArgumentException("La secuencia " + code + " está vencida.");
    }

    long next = seq.getNextNumber();
    if (next <= 0) {
      throw new IllegalArgumentException("La secuencia " + code + " no tiene un número válido.");
    }
    if (seq.getEndNumber() != null && next > seq.getEndNumber()) {
      throw new IllegalArgumentException("La secuencia " + code + " agotó su rango.");
    }

    String ncf = code + pad(next, seq.getNumberLength());
    seq.setNextNumber(next + 1);
    seq.setUpdatedAt(LocalDateTime.now());
    fiscalSequenceRepository.save(seq);
    return ncf;
  }

  private void normalize(FiscalSequence seq) {
    if (seq.getTypeCode() != null) {
      seq.setTypeCode(seq.getTypeCode().trim().toUpperCase());
    }
    if (seq.getDescription() != null) {
      seq.setDescription(seq.getDescription().trim());
    }
    if (seq.getNumberLength() <= 0) {
      seq.setNumberLength(8);
    }
  }

  private String pad(long value, int length) {
    String s = String.valueOf(value);
    if (s.length() >= length) return s;
    return "0".repeat(length - s.length()) + s;
  }
}
