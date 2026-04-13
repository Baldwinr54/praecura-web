package com.baldwin.praecura.repository;

import com.baldwin.praecura.entity.SystemSetting;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemSettingRepository extends JpaRepository<SystemSetting, String> {

  List<SystemSetting> findBySettingKeyIn(Collection<String> keys);
}
