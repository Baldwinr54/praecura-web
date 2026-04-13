package com.baldwin.praecura.repository;

import com.baldwin.praecura.entity.UserPermission;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPermissionRepository extends JpaRepository<UserPermission, Long> {

  List<UserPermission> findByUserIdOrderByPermissionModuleAscPermissionNameAsc(Long userId);

  Optional<UserPermission> findByUserIdAndPermissionCode(Long userId, String permissionCode);

  List<UserPermission> findByUserUsernameIgnoreCase(String username);
}
