package com.baldwin.praecura.repository;

import com.baldwin.praecura.entity.Doctor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

  long countByActiveTrue();

  @Query("select d from Doctor d where d.active = true and (" +
      " lower(d.fullName) like lower(concat('%', :q, '%'))" +
      " or lower(coalesce(d.specialtyText,'')) like lower(concat('%', :q, '%'))" +
      " or lower(coalesce(d.specialty.name,'')) like lower(concat('%', :q, '%'))" +
      " or lower(coalesce(d.licenseNo,'')) like lower(concat('%', :q, '%'))" +
      " or lower(coalesce(d.phone,'')) like lower(concat('%', :q, '%'))" +
      ") order by d.id desc")
  Page<Doctor> searchActive(@Param("q") String q, Pageable pageable);

  @Query("select d from Doctor d where d.active = true order by d.id desc")
  Page<Doctor> findActive(Pageable pageable);

  @Query("select d from Doctor d where d.active = true order by d.fullName asc")
  List<Doctor> findActiveForSelect();


@Query("select distinct d from Doctor d " +
    " left join fetch d.services s " +
    " left join fetch d.specialty sp " +
    " where d.id = :id")
Optional<Doctor> findByIdWithRelations(@Param("id") Long id);
}
