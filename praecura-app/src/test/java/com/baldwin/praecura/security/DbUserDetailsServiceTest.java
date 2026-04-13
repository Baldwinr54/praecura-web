package com.baldwin.praecura.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.baldwin.praecura.entity.AppUser;
import com.baldwin.praecura.entity.Role;
import com.baldwin.praecura.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

@ExtendWith(MockitoExtension.class)
class DbUserDetailsServiceTest {

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private DbUserDetailsService dbUserDetailsService;

  @Test
  void normalizesLegacyRolePrefixWhenBuildingAuthorities() {
    AppUser user = AppUser.builder()
        .id(1L)
        .username("admin")
        .passwordHash("$2a$10$test")
        .role(new Role(1L, "ROLE_ADMIN"))
        .enabled(true)
        .build();
    when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

    UserDetails userDetails = dbUserDetailsService.loadUserByUsername("admin");

    assertThat(userDetails.getAuthorities())
        .extracting("authority")
        .contains("ROLE_ADMIN")
        .doesNotContain("ROLE_ROLE_ADMIN");
  }
}
