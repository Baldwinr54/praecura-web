package com.baldwin.praecura.service;

import com.baldwin.praecura.dto.ClinicSiteForm;
import com.baldwin.praecura.entity.ClinicSite;
import com.baldwin.praecura.repository.ClinicSiteRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClinicSiteService {

  private final ClinicSiteRepository clinicSiteRepository;
  private final AuditService auditService;

  public ClinicSiteService(ClinicSiteRepository clinicSiteRepository, AuditService auditService) {
    this.clinicSiteRepository = clinicSiteRepository;
    this.auditService = auditService;
  }

  public Page<ClinicSite> search(String q, Pageable pageable) {
    if (q == null || q.trim().isEmpty()) {
      return clinicSiteRepository.findActive(pageable);
    }
    return clinicSiteRepository.searchActive(q.trim(), pageable);
  }

  public ClinicSite findById(Long id) {
    return clinicSiteRepository.findById(id).orElseThrow();
  }

  @Cacheable(value = "selectSites", sync = true)
  public List<ClinicSite> listActiveForSelect() {
    return clinicSiteRepository.findActiveForSelect();
  }

  @CacheEvict(value = {"selectSites"}, allEntries = true)
  public ClinicSite saveOrUpdate(ClinicSiteForm form) {
    ClinicSite s = (form.getId() != null) ? findById(form.getId()) : new ClinicSite();
    s.setName(form.getName());
    s.setCode(blankToNull(form.getCode()));
    s.setAddress(blankToNull(form.getAddress()));
    s.setPhone(blankToNull(form.getPhone()));
    s.setNotes(blankToNull(form.getNotes()));
    s.setActive(true);

    ClinicSite saved = clinicSiteRepository.save(s);
    auditService.log(form.getId() == null ? "CREATE" : "UPDATE", "ClinicSite", saved.getId(), saved.getName());
    return saved;
  }

  @CacheEvict(value = {"selectSites"}, allEntries = true)
  public void deactivate(Long id) {
    ClinicSite s = findById(id);
    s.setActive(false);
    clinicSiteRepository.save(s);
    auditService.log("DEACTIVATE", "ClinicSite", id, s.getName());
  }

  private String blankToNull(String v) {
    if (v == null) return null;
    String t = v.trim();
    return t.isEmpty() ? null : t;
  }
}
