# Pagos en República Dominicana (AZUL + CardNET)

Este documento resume cómo activar pagos presenciales y links de pago en PraeCura usando proveedores comunes en RD.

## 1) Base obligatoria
- **Efectivo**: ya soportado en el módulo de `Pagos`.
- **Tarjeta presencial (POS)**: se registra como pago manual (método `CARD`).

## 2) Links de pago recomendados
### 2.1 AZUL Link de Pago
- AZUL provee un link de pago desde su portal (o por API según contrato).
- En PraeCura se registra ese link en la factura para seguimiento.

En la factura:
1. Abrir `Pagos` → seleccionar factura.
2. Click en **Crear link AZUL**.
3. Pegar URL del link y guardar.
4. Cuando el pago se confirme en AZUL, marcar como **Pagado** desde PraeCura.

### 2.2 CardNET Botón de Pago (online)
- CardNET permite crear una sesión de pago y redirigir al cliente.
- PraeCura integra el flujo con callback a:
  - `GET /billing/cardnet/return?SESSION=...`
  - `GET /billing/cardnet/cancel?SESSION=...`

## 3) Variables de configuración
Configurar en `application.yml` o por variables de entorno:

```
praecura:
  public-base-url: https://tu-dominio.com
  cardnet:
    enabled: true
    base-url: https://.../CardNetAPI
    authorize-url: https://.../authorize
    merchant-number: "..."
    merchant-terminal: "..."
    acquiring-institution-code: "..."
    merchant-name: "..."
    merchant-type: "..."
    currency-code: 214
    page-language: ESP
```

Notas:
- `public-base-url` debe ser accesible públicamente (para callbacks de CardNET).
- El formato exacto de `base-url/authorize-url` lo define CardNET en la documentación oficial.

## 4) Seguridad mínima (PCI)
- **Nunca** guardar CVV/PIN.
- Solo se guarda `last4`, referencia y proveedor.
- Toda acción queda auditada en `Reportes > Auditoría`.

## 5) Flujo recomendado
1. Cita completada → facturar.
2. Efectivo / POS → registrar pago manual.
3. Link AZUL/CardNET → crear link y registrar pago al confirmarse.

## 6) Checklist de producción
- Credenciales oficiales de AZUL y CardNET.
- Dominio público configurado.
- Certificado SSL válido.
- Pruebas de ida y vuelta en ambiente sandbox.

