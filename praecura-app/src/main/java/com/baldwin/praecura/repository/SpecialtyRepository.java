package com.baldwin.praecura.repository;

import com.baldwin.praecura.entity.Specialty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpecialtyRepository extends JpaRepository<Specialty, Long> {

  boolean existsByNameIgnoreCase(String name);

  Optional<Specialty> findByNameIgnoreCase(String name);

  @Query("select s from Specialty s where (:q is null or :q = '' or lower(s.name) like lower(concat('%', :q, '%'))) order by s.name asc")
  Page<Specialty> search(@Param("q") String q, Pageable pageable);

  @Query("select s from Specialty s where s.active = true order by s.name asc")
  List<Specialty> findAllActive();

  @Query(""" 
      select (count(s) > 0)
      from Specialty s
      where s.active = true
        and lower(trim(s.name)) = lower(trim(:name))
        and (:excludeId is null or s.id <> :excludeId)
      """)
  boolean existsNameOtherActive(@Param("name") String name, @Param("excludeId") Long excludeId);

}
