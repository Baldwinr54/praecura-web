package com.baldwin.praecura.repository;

import com.baldwin.praecura.entity.SurgerySchedule;
import com.baldwin.praecura.entity.SurgeryStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SurgeryScheduleRepository extends JpaRepository<SurgerySchedule, Long> {

  List<SurgerySchedule> findAllByOrderByScheduledAtDesc();

  List<SurgerySchedule> findByStatusOrderByScheduledAtAsc(SurgeryStatus status);

  List<SurgerySchedule> findByScheduledAtBetweenOrderByScheduledAtAsc(LocalDateTime from, LocalDateTime to);
}
