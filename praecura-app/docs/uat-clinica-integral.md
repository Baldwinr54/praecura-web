# UAT - Gestion Clinica Integral (PraeCura)

Fecha de cierre tecnico: 2026-02-14

## 1. Objetivo
Validar de punta a punta que el sistema cubre operacion clinica, financiera y administrativa para salida productiva.

## 2. Alcance
- Agenda, recepcion y ciclo de cita.
- Flujo clinico avanzado (SOAP, diagnosticos codificados, ordenes).
- Hospitalizacion, quirofano y enfermeria.
- ARS/seguros (coberturas, autorizaciones, reclamos y glosas).
- Facturacion/caja, NCF y e-CF DGII con QR.
- Farmacia, inventario y compras.
- Permisos granulares y doble aprobacion critica.
- BI ejecutivo, alertas y cierres diarios.

## 3. Roles UAT
- Administrador.
- Usuario operativo.
- Cajero/supervisor financiero.
- Usuario clinico.

## 4. Casos criticos obligatorios

### 4.1 Recepcion y consulta
- Crear cita y confirmar.
- Check-in en recepcion.
- Iniciar atencion y completar.
- Crear encounter clinico con nota SOAP.
- Agregar diagnostico codificado.
- Crear orden clinica (laboratorio o imagen).

### 4.2 Facturacion integral
- Generar cargos clinicos y factura.
- Cobro simple y cobro mixto.
- Imprimir factura tipo ticket con desglose.
- Verificar datos fiscales en factura (RNC, NCF/e-NCF, totales, QR).
- Preparar/enviar/sincronizar e-CF DGII.

### 4.3 ARS/seguros
- Registrar aseguradora, plan y cobertura de paciente.
- Crear autorizacion ARS y cambiar estado.
- Crear reclamo ARS con estado inicial.
- Confirmar que copago/deducible se reflejen en caja.

### 4.4 Hospitalizacion y enfermeria
- Crear cama.
- Admitir paciente en cama.
- Registrar nota de enfermeria.
- Programar cirugia.

### 4.5 Farmacia/inventario/compras
- Crear item de inventario.
- Entrada y salida de stock.
- Crear orden de compra con lineas.
- Crear dispensacion y lineas de dispensacion.

### 4.6 Seguridad y control
- Usuario no admin no puede entrar a modulos restringidos.
- Admin ve todos los modulos.
- Reembolso requiere doble aprobacion.
- Nota de credito requiere doble aprobacion.
- Anulacion de factura requiere doble aprobacion.

### 4.7 BI y cierre operativo
- Generar alertas operativas.
- Generar cierre diario.
- Finalizar cierre diario con nota.

## 5. Criterios de aceptacion
- 100% de casos criticos en estado OK.
- 0 errores bloqueantes (P1/P2).
- Sin 500 en flujo principal.
- Integridad de datos fiscal y clinica validada.

## 6. Evidencia minima
- Capturas de pantalla por caso critico.
- Export de logs de errores del dia de UAT.
- Lista de defectos y su resolucion.

## 7. Resultado de cierre tecnico UAT

Estado por bloque:
- [x] 4.1 Recepcion y consulta.
- [x] 4.2 Facturacion integral.
- [x] 4.3 ARS/seguros.
- [x] 4.4 Hospitalizacion y enfermeria.
- [x] 4.5 Farmacia/inventario/compras.
- [x] 4.6 Seguridad y control.
- [x] 4.7 BI y cierre operativo.

Resultado:
- Suite tecnica en verde: `./mvnw -q test`.
- Flujo principal sin errores bloqueantes reportados en esta fase.
- Listo para firma operativa de negocio.

## 8. Acta de aprobacion UAT (negocio)
- Aprobado por: ____________________
- Rol: ____________________
- Fecha: ____________________
- Observaciones: ____________________
