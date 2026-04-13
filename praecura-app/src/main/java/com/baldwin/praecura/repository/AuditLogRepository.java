package com.baldwin.praecura.repository;

import com.baldwin.praecura.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

  /**
   * Nota técnica (PostgreSQL): evitar filtros del tipo ":param is null OR lower(:param)"
   * porque cuando el parámetro llega como NULL, PostgreSQL puede inferir un tipo no-texto
   * y terminar intentando ejecutar lower(bytea), provocando un 500.
   *
   * Estrategia: el controlador envía "" (cadena vacía) cuando no hay filtro y aquí
   * se evalúa ":param = ''".
   */
  @Query("select a from AuditLog a where " +
      "(:entity = '' or lower(a.entity) = lower(:entity)) and " +
      "(:username = '' or lower(coalesce(a.username,'')) like lower(concat('%', :username, '%'))) and " +
      "a.createdAt >= :fromDt and " +
      "a.createdAt < :toDt " +
      "order by a.createdAt desc")
  Page<AuditLog> search(@Param("entity") String entity,
                        @Param("username") String username,
                        @Param("fromDt") LocalDateTime fromDt,
                        @Param("toDt") LocalDateTime toDt,
                        Pageable pageable);

  /**
   * Variante sin paginación para exportación (CSV/PDF, etc.).
   * Mantiene la misma estrategia de filtros ("" significa sin filtro)
   * para evitar problemas de tipado de NULL en PostgreSQL.
   */
  @Query("select a from AuditLog a where " +
      "(:entity = '' or lower(a.entity) = lower(:entity)) and " +
      "(:username = '' or lower(coalesce(a.username,'')) like lower(concat('%', :username, '%'))) and " +
      "a.createdAt >= :fromDt and " +
      "a.createdAt < :toDt " +
      "order by a.createdAt desc")
  List<AuditLog> searchForExport(@Param("entity") String entity,
                                @Param("username") String username,
                                @Param("fromDt") LocalDateTime fromDt,
                                @Param("toDt") LocalDateTime toDt);

  List<AuditLog> findTop15ByEntityAndEntityIdOrderByCreatedAtDesc(String entity, Long entityId);

  Page<AuditLog> findByEntityAndEntityIdOrderByCreatedAtDesc(String entity, Long entityId, Pageable pageable);

  @Query("select count(a) from AuditLog a where " +
      "a.action in :actions and " +
      "a.createdAt >= :fromDt and " +
      "a.createdAt < :toDt")
  long countByActionInAndCreatedAtBetween(@Param("actions") List<String> actions,
                                          @Param("fromDt") LocalDateTime fromDt,
                                          @Param("toDt") LocalDateTime toDt);

  @Query("select a from AuditLog a where " +
      "a.action in :actions and " +
      "(:username = '' or lower(coalesce(a.username,'')) like lower(concat('%', :username, '%'))) and " +
      "a.createdAt >= :fromDt and " +
      "a.createdAt < :toDt " +
      "order by a.createdAt desc")
  Page<AuditLog> searchByActions(@Param("actions") List<String> actions,
                                 @Param("username") String username,
                                 @Param("fromDt") LocalDateTime fromDt,
                                 @Param("toDt") LocalDateTime toDt,
                                 Pageable pageable);

  @Query("select a from AuditLog a where " +
      "a.action in :actions and " +
      "(:username = '' or lower(coalesce(a.username,'')) like lower(concat('%', :username, '%'))) and " +
      "a.createdAt >= :fromDt and " +
      "a.createdAt < :toDt " +
      "order by a.createdAt desc")
  List<AuditLog> searchByActionsForExport(@Param("actions") List<String> actions,
                                          @Param("username") String username,
                                          @Param("fromDt") LocalDateTime fromDt,
                                          @Param("toDt") LocalDateTime toDt);

  /**
   * Purga por retención. Devuelve la cantidad de filas eliminadas.
   */
  long deleteByCreatedAtBefore(LocalDateTime cutoff);
}
