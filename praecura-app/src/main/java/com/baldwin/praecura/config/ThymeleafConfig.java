package com.baldwin.praecura.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.extras.java8time.dialect.Java8TimeDialect;

/**
 * Enables Thymeleaf Java 8 time dialect so templates can use #temporals.*
 * (e.g., formatting LocalDateTime in Agenda/Reporte de Auditoría).
 */
@Configuration
public class ThymeleafConfig {

	@Bean
	public Java8TimeDialect java8TimeDialect() {
		return new Java8TimeDialect();
	}
}
