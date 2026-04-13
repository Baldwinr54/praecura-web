package com.baldwin.praecura.service;

import com.baldwin.praecura.dto.MedicalServiceForm;
import com.baldwin.praecura.entity.MedicalService;
import com.baldwin.praecura.repository.MedicalServiceRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MedicalServiceService {

  private final MedicalServiceRepository medicalServiceRepository;
  private final AuditService auditService;

  public MedicalServiceService(MedicalServiceRepository medicalServiceRepository, AuditService auditService) {
    this.medicalServiceRepository = medicalServiceRepository;
    this.auditService = auditService;
  }

  public Page<MedicalService> search(String q, Pageable pageable) {
    if (q == null || q.trim().isEmpty()) {
      return medicalServiceRepository.findActive(pageable);
    }
    return medicalServiceRepository.searchActive(q.trim(), pageable);
  }

  public MedicalService findById(Long id) {
    return medicalServiceRepository.findById(id).orElseThrow();
  }

  @Cacheable(value = "selectServices", sync = true)
  public List<MedicalService> listActiveForSelect() {
    return medicalServiceRepository.findActiveForSelect();
  }

  @CacheEvict(value = {"selectServices"}, allEntries = true)
  public MedicalService saveOrUpdate(MedicalServiceForm form) {
    MedicalService s = (form.getId() != null) ? findById(form.getId()) : new MedicalService();
    s.setName(form.getName());
    s.setDurationMinutes(form.getDurationMinutes());
    s.setPrice(form.getPrice());
    s.setActive(true);

    MedicalService saved = medicalServiceRepository.save(s);
    auditService.log(form.getId() == null ? "CREATE" : "UPDATE", "MedicalService", saved.getId(), saved.getName());
    return saved;
  }

  @CacheEvict(value = {"selectServices"}, allEntries = true)
  public void deactivate(Long id) {
    MedicalService s = findById(id);
    s.setActive(false);
    medicalServiceRepository.save(s);
    auditService.log("DEACTIVATE", "MedicalService", id, s.getName());
  }
}
