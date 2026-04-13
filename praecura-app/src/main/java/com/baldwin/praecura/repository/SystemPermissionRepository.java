package com.baldwin.praecura.repository;

import com.baldwin.praecura.entity.PermissionModule;
import com.baldwin.praecura.entity.SystemPermission;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemPermissionRepository extends JpaRepository<SystemPermission, Long> {

  List<SystemPermission> findByActiveTrueOrderByModuleAscNameAsc();

  Optional<SystemPermission> findByCode(String code);

  List<SystemPermission> findByModuleOrderByNameAsc(PermissionModule module);
}
