package com.baldwin.praecura.repository;

import com.baldwin.praecura.entity.InventoryItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

  List<InventoryItem> findAllByOrderByNameAsc();

  List<InventoryItem> findByActiveTrueOrderByNameAsc();

  Optional<InventoryItem> findBySkuIgnoreCase(String sku);

  @Query("select i from InventoryItem i where lower(i.name) like lower(concat('%', ?1, '%')) order by i.name asc")
  List<InventoryItem> searchByName(String q);
}
