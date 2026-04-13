package com.baldwin.praecura.repository;

import com.baldwin.praecura.entity.AlertSeverity;
import com.baldwin.praecura.entity.AlertStatus;
import com.baldwin.praecura.entity.OperationalAlert;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperationalAlertRepository extends JpaRepository<OperationalAlert, Long> {

  List<OperationalAlert> findTop100ByOrderByDetectedAtDesc();

  List<OperationalAlert> findByStatusOrderByDetectedAtDesc(AlertStatus status);

  long countByStatus(AlertStatus status);

  long countByStatusAndSeverity(AlertStatus status, AlertSeverity severity);
}
