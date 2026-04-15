# PraeCuraWeb

Sistema web clínico para la gestión de pacientes, citas, agenda médica, atención clínica, servicios, facturación, reportes y administración de usuarios.

## Objetivo Académico

Este proyecto fue desarrollado como entrega final de la asignatura **Desarrollo de Proyectos con Software Libre**, evidenciando la construcción de una aplicación funcional con módulos CRUD, integración persistente con base de datos libre, despliegue en servidor Linux virtualizado y publicación mediante servidor web.

## Resultados de Aprendizaje Evidenciados

Este proyecto evidencia el cumplimiento de los siguientes resultados de aprendizaje:

- **RA5:** empleo de lenguaje de programación y herramientas de desarrollo libre en la construcción de aplicaciones.
- **RA8 y RA9:** aplicación de procesos de diseño, codificación y uso de framework para optimizar el desarrollo de software.

## Tecnologías Utilizadas

- **Lenguaje Principal:** Java
- **Versión del Lenguaje Utilizada:** Java 21
- **Framework de Desarrollo:** Spring Boot
- **Tecnologías Complementarias:** Thymeleaf y Maven
- **Motor de Base de Datos:** PostgreSQL
- **Servidor Web / Reverse Proxy:** Apache
- **Sistema Operativo del Servidor:** Ubuntu Server
- **Entorno de Virtualización:** VirtualBox

## Arquitectura Utilizada

La solución fue implementada con la siguiente arquitectura tecnológica:

- Cliente web accesible desde navegador
- Aplicación desarrollada en Java con Spring Boot
- Persistencia de datos en PostgreSQL
- Publicación mediante Apache
- Despliegue en Ubuntu Server sobre VirtualBox

## Módulos Funcionales Principales

- Gestión de pacientes
- Gestión de doctores
- Agenda y citas médicas
- Atención clínica
- Servicios médicos
- Facturación y pagos
- Reportes
- Usuarios, roles y control de acceso

## Despliegue Realizado

La aplicación fue desplegada en **Ubuntu Server ARM64** ejecutándose en **VirtualBox**, utilizando **PostgreSQL** como motor de base de datos y **Apache** como servidor web / reverse proxy para la publicación de la aplicación.

## Evidencia de Integración del Sistema

El repositorio y la implementación permiten evidenciar:

- desarrollo de módulos funcionales tipo CRUD;
- integración persistente con PostgreSQL;
- despliegue sobre un servidor Linux virtualizado;
- publicación mediante Apache;
- funcionamiento real del sistema en entorno de servidor.

## Estructura del Repositorio

- `praecura-app/` : aplicación principal Spring Boot
- `scripts/` : scripts auxiliares
- `deploy/` : archivos de apoyo para despliegue en Ubuntu Server
- `docs/` : documentación y capturas
- `docker-compose.yml` : apoyo para ejecución local
- `setup_praecura_app.sh` : script auxiliar de inicialización local

## Archivos de Despliegue Incluidos

En la carpeta `deploy/` se incluyen los siguientes archivos de apoyo para la instalación y despliegue:

- `praecura_postgres_bootstrap.sql`
- `praecura.service`
- `praecura-apache.conf`
- `praecura.env.production.example`

## Seguridad y Configuración

Las credenciales reales y variables sensibles no se almacenan en este repositorio.  
El archivo `.env.example` contiene únicamente valores de ejemplo y la configuración real debe definirse fuera del repositorio.

## Integrantes del Grupo 7

- Rafael Estévez Espinal
- Eduardo García
- Luisanna Jiménez Reyes
- Jhonny Julio Morillo Novas
- Abimilet Peralta Grullon
- Daniel Esmill Pérez
- Baldwin Rodríguez Rodríguez
- Jason Jesús Tejada Monegro
- Rafael Antonio Vargas Jiménez
