package com.baldwin.praecura.repository;

import com.baldwin.praecura.entity.BedStatus;
import com.baldwin.praecura.entity.InpatientBed;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InpatientBedRepository extends JpaRepository<InpatientBed, Long> {

  List<InpatientBed> findAllByOrderBySiteNameAscCodeAsc();

  List<InpatientBed> findByActiveTrueOrderBySiteNameAscCodeAsc();

  List<InpatientBed> findByStatusOrderBySiteNameAscCodeAsc(BedStatus status);

  Optional<InpatientBed> findBySiteIdAndCodeIgnoreCase(Long siteId, String code);
}
