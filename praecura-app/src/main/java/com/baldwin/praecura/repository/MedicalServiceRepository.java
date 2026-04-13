package com.baldwin.praecura.repository;

import com.baldwin.praecura.entity.MedicalService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MedicalServiceRepository extends JpaRepository<MedicalService, Long> {

  @Query("select s from MedicalService s where s.active = true and (" +
      " lower(s.name) like lower(concat('%', :q, '%'))" +
      ") order by s.id desc")
  Page<MedicalService> searchActive(@Param("q") String q, Pageable pageable);

  @Query("select s from MedicalService s where s.active = true order by s.id desc")
  Page<MedicalService> findActive(Pageable pageable);

  @Query("select s from MedicalService s where s.active = true order by s.name asc")
  List<MedicalService> findActiveForSelect();
}
