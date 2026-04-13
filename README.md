# PraeCuraWeb

Sistema web clínico para la gestión de pacientes, citas, agenda médica, atención clínica, servicios, facturación, reportes y administración de usuarios.

## Objetivo académico

Este proyecto fue desarrollado como entrega final de la asignatura Desarrollo de Proyectos con Software Libre, evidenciando:

- módulos funcionales tipo CRUD
- integración persistente con PostgreSQL
- despliegue en servidor Linux virtualizado
- publicación mediante Apache
- acceso remoto por SSH
- funcionamiento real del sistema

## Tecnologías utilizadas

- Java 21
- Spring Boot
- Thymeleaf
- Maven
- PostgreSQL
- Apache
- Ubuntu Server
- VirtualBox

## Módulos funcionales principales

- Gestión de pacientes
- Gestión de doctores
- Agenda y citas médicas
- Atención clínica
- Servicios médicos
- Facturación y pagos
- Reportes
- Usuarios, roles y control de acceso

## Despliegue realizado

La aplicación fue desplegada en Ubuntu Server ARM64 ejecutándose en VirtualBox, con PostgreSQL como base de datos y Apache como reverse proxy.

## Estructura del repositorio

- praecura-app/ : aplicación principal Spring Boot
- scripts/ : scripts auxiliares
- deploy/ : archivos de apoyo para despliegue en Ubuntu Server
- docs/ : documentación y capturas
- docker-compose.yml : apoyo para ejecución local
- setup_praecura_app.sh : script auxiliar de inicialización local

## Archivos de despliegue incluidos

En la carpeta deploy/ se incluyen:

- praecura_postgres_bootstrap.sql
- praecura.service
- praecura-apache.conf
- praecura.env.production.example

## Seguridad y configuración

Las credenciales reales y variables sensibles no se almacenan en este repositorio.
El archivo .env.example contiene únicamente valores de ejemplo.

## Autor

Baldwin Rodríguez
