package com.baldwin.praecura.repository;

import com.baldwin.praecura.entity.ClinicResource;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClinicResourceRepository extends JpaRepository<ClinicResource, Long> {

  @Query("select r from ClinicResource r join r.site s " +
      "where r.active = true " +
      "and (:q is null or :q = '' or lower(r.name) like lower(concat('%', :q, '%'))) " +
      "and (:siteId is null or s.id = :siteId) " +
      "order by s.name asc, r.name asc")
  Page<ClinicResource> searchActive(@Param("q") String q,
                                    @Param("siteId") Long siteId,
                                    Pageable pageable);

  @Query("select r from ClinicResource r where r.active = true order by r.name asc")
  List<ClinicResource> findActiveForSelect();

  @Query("select r from ClinicResource r where r.active = true and r.site.id = :siteId order by r.name asc")
  List<ClinicResource> findActiveForSite(@Param("siteId") Long siteId);
}
