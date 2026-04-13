package com.baldwin.praecura.repository;

import com.baldwin.praecura.entity.PharmacyDispensationItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PharmacyDispensationItemRepository extends JpaRepository<PharmacyDispensationItem, Long> {

  List<PharmacyDispensationItem> findByDispensationIdOrderByIdAsc(Long dispensationId);
}
