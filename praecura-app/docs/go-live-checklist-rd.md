# Go-Live Checklist - Republica Dominicana

Fecha de cierre tecnico: 2026-02-14

## 1. Configuracion
- [x] Nombre comercial, RNC y datos de empresa configurables en branding.
- [x] Serie fiscal/NCF configurable y vigente por secuencia activa.
- [x] Parametros de e-CF DGII disponibles para ambiente objetivo.
- [x] Usuarios admin y operativos soportados por el modulo de acceso.

## 2. Validaciones funcionales
- [x] Agenda/recepcion operativas.
- [x] Facturacion/caja operativas.
- [x] ARS/seguros operativo basico.
- [x] Clinico avanzado operativo basico (SOAP, diagnosticos y ordenes).
- [x] Hospitalizacion/quirofano/enfermeria operativo basico.
- [x] Farmacia/inventario/compras operativo basico.

## 3. Validaciones de cumplimiento fiscal
- [x] Factura incluye identificacion fiscal correcta.
- [x] NCF/e-NCF se asigna segun flujo disponible.
- [x] QR de verificacion visible en impresion.
- [x] Flujo e-CF (enviar/consultar estado) habilitado.

## 4. Seguridad y control
- [x] Admin puede acceder a todos los modulos.
- [x] Usuario no admin bloqueado en secciones administrativas.
- [x] Doble aprobacion activa para acciones criticas.
- [x] Auditoria de eventos principales visible.

## 5. Operacion y monitoreo
- [x] Alertas operativas habilitadas.
- [x] Cierre diario automatizado o programado.
- [x] Procedimiento de respaldo definido.
- [x] Procedimiento de contingencia definido.

## 6. Evidencia tecnica de cierre
- [x] Suite de pruebas `./mvnw -q test` en verde.
- [x] Migraciones Flyway aplicadas hasta `V51`.
- [x] Sin bloqueantes tecnicos abiertos para salida.

## 7. Criterio de salida productiva
- [x] UAT tecnico documentado en `docs/uat-clinica-integral.md`.
- [x] Cero defectos criticos abiertos en la fase de cierre tecnico.
- [ ] Responsable de negocio autoriza salida final (firma operativa).

## 8. Firma de aprobacion operativa
- Responsable de negocio: ____________________
- Fecha: ____________________
- Observaciones: ____________________
