package com.baldwin.praecura.repository;

import com.baldwin.praecura.entity.DailyOperationalClosing;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyOperationalClosingRepository extends JpaRepository<DailyOperationalClosing, Long> {

  Optional<DailyOperationalClosing> findByClosingDate(LocalDate closingDate);

  List<DailyOperationalClosing> findTop30ByOrderByClosingDateDesc();
}
