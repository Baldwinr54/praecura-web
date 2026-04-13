# Runbook Operativo - Produccion

## 1. Inicio diario
1. Verificar estado de aplicacion (`/system/health`).
2. Confirmar que el cierre del dia anterior este finalizado.
3. Revisar alertas operativas abiertas en BI ejecutivo.
4. Confirmar apertura de caja por sede si aplica.

## 2. Flujo operativo diario
- Recepcion: check-in y estado de cola.
- Clinico: registro SOAP, diagnosticos y ordenes.
- Caja/facturacion: cobro por cargos clinicos y servicios.
- ARS: autorizaciones y reclamos del dia.
- Farmacia: control de stock critico y dispensacion.

## 3. Controles de seguridad
- Usuario admin: acceso total.
- Usuario no admin: acceso limitado segun rol/permisos.
- Acciones criticas (anular/reembolsar/nota de credito): doble aprobacion.

## 4. Operacion fiscal RD
- Validar secuencias NCF activas.
- Operar e-CF en cola (preparar/enviar/sincronizar).
- Revisar estados rechazados/errores y reintentos.
- Confirmar que facturas imprimen QR y datos legales.

## 5. Cierre diario
1. Generar cierre diario desde BI.
2. Revisar total cobrado, reembolsos y pendiente.
3. Validar alertas abiertas.
4. Finalizar cierre con observaciones.
5. Archivar reporte del dia.

## 6. Incidencias
- Error funcional: registrar modulo, usuario, hora y pasos.
- Error 500: registrar `X-Request-Id` y stack trace.
- Incidente fiscal: pausar acciones manuales y escalar a responsable financiero.
