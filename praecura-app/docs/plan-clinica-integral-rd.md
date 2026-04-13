# Plan Maestro - Gestion Clinica Integral (RD)

Fecha de corte: 2026-02-06

## Objetivo
Convertir PraeCura en un sistema de clinica general integral, con flujo clinico-operativo-financiero completo, facturacion robusta, caja amigable y cumplimiento fiscal en Republica Dominicana (incluyendo e-CF DGII y representacion impresa con QR).

## Estado de ejecucion (2026-02-14)
- Fase 0 a Fase 11: implementadas y cerradas tecnicamente.
- Flujo integral operativo-clinico-financiero-fiscal disponible en la aplicacion.
- Cierre de calidad y evidencia documental generados para salida productiva.

## Estado actual (resumen tecnico)
- Completo: agenda, recepcion, citas, pacientes, flujo clinico, catalogos, reportes y auditoria.
- Completo: facturacion, pagos, caja, cuentas por cobrar, secuencias NCF/e-NCF, e-CF, links de pago e impresion fiscal.
- Completo: seguros/ARS, hospitalizacion, quirofano, enfermeria, farmacia, inventario y compras.
- Completo: branding white-label, permisos, aprobacion critica y BI ejecutivo con cierre operativo.
- Pendiente externo: firma de aprobacion operativa de negocio para salida final.

Referencias del codigo actual:
- /Users/baldwinr/DESARROLLO/Java/praecura-web/praecura-app/src/main/resources/templates/fragments/layout.html
- /Users/baldwinr/DESARROLLO/Java/praecura-web/praecura-app/src/main/java/com/baldwin/praecura/service/BillingService.java
- /Users/baldwinr/DESARROLLO/Java/praecura-web/praecura-app/src/main/resources/templates/billing/detail.html
- /Users/baldwinr/DESARROLLO/Java/praecura-web/praecura-app/src/main/resources/templates/billing/print.html
- /Users/baldwinr/DESARROLLO/Java/praecura-web/praecura-app/src/main/resources/templates/owner/branding.html

## Fases de ejecucion

### Fase 0 - Normalizacion UX y menu (rapida)
Objetivo: dejar el sistema coherente, sin ruido visual y con nomenclatura profesional.
- Ajustar ortografia y acentuacion en todo el menu y textos.
- Reordenar menu por dominio: Operacion, Clinico, Finanzas, Catalogos, Analitica, Administracion.
- Ocultar acciones no usadas por perfil y eliminar accesos redundantes.
- Estandarizar labels, placeholders y mensajes de error.

Criterio de cierre: no quedan textos sin acentos/terminologia inconsistente ni rutas duplicadas en navegacion.

### Fase 1 - Modelo de cargos clinicos (core de cobro integral)
Objetivo: cobrar todo lo que genera valor clinico, no solo citas.
- Crear concepto de "cargo" desacoplado de cita: consulta, procedimiento, laboratorio, imagen, medicamento, insumo, habitacion, honorarios, otros.
- Permitir multiples cargos por episodio/visita y facturacion consolidada o parcial.
- Soportar reglas de precio por sede, medico, horario, convenio y paquete.
- Soportar descuentos autorizados y recargos trazables.

Criterio de cierre: cajero puede facturar una visita completa con varios conceptos en un solo flujo.

### Fase 2 - Caja clinica POS (operacion diaria)
Objetivo: flujo de cobro rapido y seguro para caja.
- Pantalla unica de cobro con busqueda por paciente, expediente, telefono, cita o numero de factura.
- Carrito de cobro con desglose (subtotal, ITBIS, descuentos, total, abonado, balance).
- Medios de pago mixtos: efectivo, tarjeta, transferencia, link y combinados.
- Apertura/cierre/arqueo con diferencias justificadas y bitacora.
- Reimpresion y anulacion controlada por permisos.

Criterio de cierre: cajero puede cobrar, abonar, devolver y cerrar caja sin salir del modulo.

### Fase 3 - Facturacion fiscal RD (NCF + e-CF DGII)
Objetivo: cumplimiento fiscal dominicano end-to-end.
- Mantener NCF tradicional mientras se completa adopcion e-CF.
- Agregar entidad de documento fiscal electronico por factura (XML firmado, trackId, estado DGII, respuesta y acuse).
- Implementar flujo DGII: semilla, token, envio e-CF, consulta estado, acuse/validacion y anulacion cuando aplique.
- Manejar estados: pendiente, enviado, aceptado, aceptado condicional, rechazado, anulado.
- Reintentos automaticos y cola de contingencia cuando DGII no responda.

Criterio de cierre: cada factura fiscal se valida y traza contra DGII con evidencia auditable.

### Fase 4 - Representacion impresa legal (ticket + A4)
Objetivo: factura bonita, util y legal para paciente y auditoria.
- Mantener formato termico alargado (80mm) para caja.
- Agregar formato carta/A4 para administracion y aseguradoras.
- Incluir datos obligatorios: razon social, RNC, NCF/e-NCF, fecha, cliente, detalle, ITBIS, total, medio de pago.
- Incluir Codigo de Seguridad y QR de verificacion e-CF conforme guia DGII.

Criterio de cierre: factura impresa pasa revision fiscal y operativa interna.

### Fase 5 - Cuentas por cobrar, credito y seguros
Objetivo: control financiero real mas alla de cobro inmediato.
- Abonos, saldo pendiente, planes de pago y bloqueo por mora configurable.
- Notas de credito/debito con aprobacion.
- Integracion ARS: cobertura, copago, deducible, autorizaciones y glosas.
- Estado de cuenta por paciente, por ARS y por periodo.

Criterio de cierre: todo saldo pendiente tiene seguimiento, responsable y accion definida.

### Fase 6 - Clinico asistencial ampliado
Objetivo: evolucionar de registro basico a expediente clinico util.
- Episodio/encuentro clinico con SOAP y diagnosticos codificados.
- Ordenes clinicas: laboratorios, imagenes, procedimientos, recetas.
- Resultado de estudios y panel de seguimiento.
- Interconsultas y referimientos.
- Consentimientos por acto clinico.

Criterio de cierre: medico documenta, ordena y da seguimiento dentro del mismo sistema.

### Fase 7 - Hospitalizacion, quirofano y enfermeria
Objetivo: cubrir flujo intrahospitalario.
- Censo de camas y gestion de habitaciones.
- Programacion quirurgica, checklists y consumos de quirofano.
- Kardex de enfermeria, signos por turno y eventos adversos.
- Cargos automaticos por estancia y materiales.

Criterio de cierre: una internacion completa se opera y factura sin procesos manuales externos.

### Fase 8 - Farmacia, inventario y compras
Objetivo: controlar costo y trazabilidad de medicamentos/insumos.
- Kardex de inventario por lote y vencimiento.
- Dispensa vinculada a orden medica y/o factura.
- Minimos, reposicion, compras, recepcion y costo promedio.
- Alertas de vencimiento y ruptura de stock.

Criterio de cierre: no se dispensa ni factura inventario fuera de trazabilidad.

### Fase 9 - Seguridad, permisos y gobierno
Objetivo: pasar de 2 roles a seguridad empresarial.
- Matriz granular por modulo/accion (ver, crear, editar, anular, exportar, aprobar).
- Doble aprobacion para acciones criticas (anulaciones, devoluciones altas, cierre con diferencia).
- Trazabilidad completa de auditoria con contexto tecnico.
- Politicas de sesion, bloqueo, fuerza de contrasena y privacidad de datos.

Criterio de cierre: cada accion sensible queda controlada por permiso explicito.

### Fase 10 - Analitica ejecutiva y cumplimiento
Objetivo: tablero de direccion y control continuo.
- KPI clinicos: no-show, tiempos de espera, productividad, resolucion.
- KPI financieros: ingresos, recaudo, mora, devoluciones, margen por servicio.
- KPI fiscales: estado e-CF, pendientes DGII, secuencias por vencer.
- Alertas operativas y cierre diario automatizado.

Criterio de cierre: direccion opera por indicadores, no por percepcion.

### Fase 11 - Calidad, pruebas y salida a produccion
Objetivo: garantizar estabilidad antes de vender/implantar.
- Pruebas unitarias, integracion y regresion por flujo critico.
- UAT con caja real, medicos y administracion.
- Manuales de operacion por rol.
- Plan de despliegue, respaldo, recuperacion y monitoreo.

Criterio de cierre: release candidato aprobado con checklist funcional, fiscal y de seguridad.

## Orden recomendado inmediato (sin pausa)
1. Fase 0 y Fase 1.
2. Fase 2.
3. Fase 3 y Fase 4 (bloque fiscal DGII/e-CF).
4. Fase 5.
5. Fases 6, 7 y 8.
6. Fases 9, 10 y 11.

## Fuentes oficiales clave (para el bloque fiscal RD)
- DGII - Guia sobre facturacion electronica (enero 2025).
- DGII - Documentacion tecnica e-CF (XSD/servicios, actualizado en 2025).
- DGII - Modelos de representacion impresa de e-CF (incluye QR y codigo de seguridad).
- DGII - Formato de comprobantes fiscales electronicos e-CF v1.0.
