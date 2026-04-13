package com.baldwin.praecura.service;

import com.baldwin.praecura.dto.ClinicResourceForm;
import com.baldwin.praecura.entity.ClinicResource;
import com.baldwin.praecura.entity.ClinicSite;
import com.baldwin.praecura.repository.ClinicResourceRepository;
import com.baldwin.praecura.repository.ClinicSiteRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClinicResourceService {

  private final ClinicResourceRepository clinicResourceRepository;
  private final ClinicSiteRepository clinicSiteRepository;
  private final AuditService auditService;

  public ClinicResourceService(ClinicResourceRepository clinicResourceRepository,
                               ClinicSiteRepository clinicSiteRepository,
                               AuditService auditService) {
    this.clinicResourceRepository = clinicResourceRepository;
    this.clinicSiteRepository = clinicSiteRepository;
    this.auditService = auditService;
  }

  public Page<ClinicResource> search(String q, Long siteId, Pageable pageable) {
    return clinicResourceRepository.searchActive(q == null ? "" : q.trim(), siteId, pageable);
  }

  public ClinicResource findById(Long id) {
    return clinicResourceRepository.findById(id).orElseThrow();
  }

  @Cacheable(value = "selectResources", sync = true)
  public List<ClinicResource> listActiveForSelect() {
    return clinicResourceRepository.findActiveForSelect();
  }

  @Cacheable(value = "selectResourcesBySite", sync = true)
  public List<ClinicResource> listActiveForSite(Long siteId) {
    if (siteId == null) return clinicResourceRepository.findActiveForSelect();
    return clinicResourceRepository.findActiveForSite(siteId);
  }

  @CacheEvict(value = {"selectResources", "selectResourcesBySite"}, allEntries = true)
  public ClinicResource saveOrUpdate(ClinicResourceForm form) {
    ClinicResource r = (form.getId() != null) ? findById(form.getId()) : new ClinicResource();

    ClinicSite site = clinicSiteRepository.findById(form.getSiteId())
        .orElseThrow(() -> new IllegalArgumentException("Sede no existe"));
    if (!site.isActive()) {
      throw new IllegalArgumentException("No se puede usar una sede inactiva.");
    }

    r.setName(form.getName());
    r.setType(form.getType());
    r.setSite(site);
    r.setNotes(blankToNull(form.getNotes()));
    r.setActive(true);

    ClinicResource saved = clinicResourceRepository.save(r);
    auditService.log(form.getId() == null ? "CREATE" : "UPDATE", "ClinicResource", saved.getId(), saved.getName());
    return saved;
  }

  @CacheEvict(value = {"selectResources", "selectResourcesBySite"}, allEntries = true)
  public void deactivate(Long id) {
    ClinicResource r = findById(id);
    r.setActive(false);
    clinicResourceRepository.save(r);
    auditService.log("DEACTIVATE", "ClinicResource", id, r.getName());
  }

  private String blankToNull(String v) {
    if (v == null) return null;
    String t = v.trim();
    return t.isEmpty() ? null : t;
  }
}
