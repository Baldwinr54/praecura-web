package com.baldwin.praecura.repository;

import com.baldwin.praecura.entity.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PatientRepository extends JpaRepository<Patient, Long> {

  @Query(""" 
      select p from Patient p 
      where p.active = true 
        and (:q is null or :q = '' or (
             lower(p.fullName) like lower(concat('%', :q, '%'))
          or lower(coalesce(p.phone,'')) like lower(concat('%', :q, '%'))
          or lower(coalesce(p.cedula,'')) like lower(concat('%', :q, '%'))
          or lower(coalesce(p.email,'')) like lower(concat('%', :q, '%'))
        ))
      order by p.id desc
      """)
  Page<Patient> searchActive(@Param("q") String q, Pageable pageable);

  @Query(""" 
      select p from Patient p
      where (:includeInactive = true or p.active = true)
        and (:name is null or :name = '' or lower(p.fullName) like lower(concat('%', :name, '%')))
        and (:cedula is null or :cedula = '' or lower(coalesce(p.cedula,'')) like lower(concat('%', :cedula, '%')))
        and (:phone is null or :phone = '' or lower(coalesce(p.phone,'')) like lower(concat('%', :phone, '%')))
        and (:email is null or :email = '' or lower(coalesce(p.email,'')) like lower(concat('%', :email, '%')))
        and (:q is null or :q = '' or (
             lower(p.fullName) like lower(concat('%', :q, '%'))
          or lower(coalesce(p.phone,'')) like lower(concat('%', :q, '%'))
          or lower(coalesce(p.cedula,'')) like lower(concat('%', :q, '%'))
          or lower(coalesce(p.email,'')) like lower(concat('%', :q, '%'))
        ))
        and (:missingPhone is null or :missingPhone = false or p.phone is null or p.phone = '')
        and (:missingEmail is null or :missingEmail = false or p.email is null or p.email = '')
      order by p.fullName asc
      """)
  Page<Patient> searchAdvanced(@Param("q") String q,
                              @Param("name") String name,
                              @Param("cedula") String cedula,
                              @Param("phone") String phone,
                              @Param("email") String email,
                              @Param("missingPhone") Boolean missingPhone,
                              @Param("missingEmail") Boolean missingEmail,
                              @Param("includeInactive") boolean includeInactive,
                              Pageable pageable);

  @Query(""" 
      select p from Patient p 
      where p.active = true 
      order by p.id desc
      """)
  Page<Patient> findActive(Pageable pageable);

  @Query(""" 
      select p from Patient p 
      where p.active = true 
      order by p.fullName asc
      """)
  List<Patient> findActiveForSelect();

  boolean existsByCedulaIgnoreCaseAndActiveTrue(String cedula);

  @Query(""" 
      select (count(p) > 0) 
      from Patient p 
      where p.active = true 
        and lower(p.cedula) = lower(:cedula) 
        and (:excludeId is null or p.id <> :excludeId)
      """)
  boolean existsCedulaOther(@Param("cedula") String cedula, @Param("excludeId") Long excludeId);

  boolean existsByEmailIgnoreCaseAndActiveTrue(String email);

  @Query(""" 
      select (count(p) > 0) 
      from Patient p 
      where p.active = true 
        and lower(p.email) = lower(:email)
        and (:excludeId is null or p.id <> :excludeId)
      """)
  boolean existsEmailOther(@Param("email") String email, @Param("excludeId") Long excludeId);
}
