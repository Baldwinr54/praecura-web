package com.baldwin.praecura.service;

import com.baldwin.praecura.dto.SpecialtyForm;
import com.baldwin.praecura.entity.Specialty;
import com.baldwin.praecura.repository.SpecialtyRepository;
import jakarta.transaction.Transactional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SpecialtyService {

  private final SpecialtyRepository specialtyRepository;

  public SpecialtyService(SpecialtyRepository specialtyRepository) {
    this.specialtyRepository = specialtyRepository;
  }

  public Page<Specialty> list(String q, Pageable pageable) {
    String query = q == null ? "" : q.trim();
    return specialtyRepository.search(query, pageable);
  }

  @Cacheable(value = "selectSpecialties", sync = true)
  public List<Specialty> listActive() {
    return specialtyRepository.findAllActive();
  }

  public Specialty getOrThrow(Long id) {
    return specialtyRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Especialidad no encontrada"));
  }

  public boolean existsByNameIgnoreCase(String name) {
    return specialtyRepository.existsByNameIgnoreCase(name == null ? "" : name.trim());
  }

  @Transactional
  @CacheEvict(value = {"selectSpecialties"}, allEntries = true)
  public Specialty create(SpecialtyForm form) {
    Specialty s = new Specialty();
    String name = normalizeName(form.getName());
    s.setName(name);
    s.setActive(form.isActive());
    validateUniqueActiveName(name, null, s.isActive());
    return specialtyRepository.save(s);
  }

  @Transactional
  @CacheEvict(value = {"selectSpecialties"}, allEntries = true)
  public Specialty update(Long id, SpecialtyForm form) {
    Specialty s = getOrThrow(id);
    String name = normalizeName(form.getName());
    s.setName(name);
    s.setActive(form.isActive());
    validateUniqueActiveName(name, id, s.isActive());
    return specialtyRepository.save(s);
  }

  @Transactional
  @CacheEvict(value = {"selectSpecialties"}, allEntries = true)
  public Specialty toggle(Long id) {
    Specialty s = getOrThrow(id);
    s.setActive(!s.isActive());
    return specialtyRepository.save(s);
  }

  // ---------------------------------------------------------------------
  // Compatibility helpers (kept to minimize changes across layers)
  // ---------------------------------------------------------------------
  public Page<Specialty> search(String q, PageRequest pageRequest) {
    return list(q, pageRequest);
  }

  public Specialty findById(Long id) {
    return getOrThrow(id);
  }

  @Transactional
  public Specialty saveOrUpdate(SpecialtyForm form) {
    if (form == null) {
      throw new IllegalArgumentException("Formulario inválido");
    }
    if (form.getId() == null) {
      return create(form);
    }
    return update(form.getId(), form);
  }

  @Transactional
  public Specialty toggleActive(Long id) {
    return toggle(id);
  }

  private String normalizeName(String name) {
    if (name == null) return "";
    String t = name.trim().replaceAll("\\s+", " ");
    return t;
  }

  private void validateUniqueActiveName(String name, Long excludeId, boolean active) {
    if (!active) return;
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("El nombre es obligatorio.");
    }
    if (specialtyRepository.existsNameOtherActive(name, excludeId)) {
      throw new IllegalArgumentException("Ya existe una especialidad activa con ese nombre.");
    }
  }
}
