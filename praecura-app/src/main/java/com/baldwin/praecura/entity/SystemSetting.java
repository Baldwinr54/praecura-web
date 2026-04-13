package com.baldwin.praecura.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "system_settings")
@Getter
@Setter
public class SystemSetting {

  @Id
  @Column(name = "setting_key", nullable = false, length = 80)
  private String settingKey;

  @Column(name = "setting_value", length = 2000)
  private String settingValue;

  @Column(name = "updated_by", length = 120)
  private String updatedBy;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt = LocalDateTime.now();
}
