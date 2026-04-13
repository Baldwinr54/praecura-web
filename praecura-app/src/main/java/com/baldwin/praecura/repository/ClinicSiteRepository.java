package com.baldwin.praecura.repository;

import com.baldwin.praecura.entity.ClinicSite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClinicSiteRepository extends JpaRepository<ClinicSite, Long> {

  @Query("select s from ClinicSite s where s.active = true and (" +
      " :q is null or :q = '' or lower(s.name) like lower(concat('%', :q, '%'))" +
      " or lower(coalesce(s.code,'')) like lower(concat('%', :q, '%'))" +
      " ) order by s.name asc")
  Page<ClinicSite> searchActive(@Param("q") String q, Pageable pageable);

  @Query("select s from ClinicSite s where s.active = true order by s.name asc")
  Page<ClinicSite> findActive(Pageable pageable);

  @Query("select s from ClinicSite s where s.active = true order by s.name asc")
  List<ClinicSite> findActiveForSelect();
}
