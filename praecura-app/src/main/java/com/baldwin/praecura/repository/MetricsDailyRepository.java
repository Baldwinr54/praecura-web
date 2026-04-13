package com.baldwin.praecura.repository;

import com.baldwin.praecura.entity.MetricsDaily;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MetricsDailyRepository extends JpaRepository<MetricsDaily, LocalDate> {
  List<MetricsDaily> findByDayBetweenOrderByDayAsc(LocalDate from, LocalDate to);

  @Query("select m from MetricsDaily m where m.day >= :from and m.day <= :to order by m.day asc")
  List<MetricsDaily> findRange(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
