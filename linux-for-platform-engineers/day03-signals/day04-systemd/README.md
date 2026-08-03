# Día 4 – systemd, systemctl y journalctl

## Objetivo

Comprender cómo Linux administra los servicios del sistema mediante **systemd**, aprender a inspeccionar su estado utilizando **systemctl** y consultar los registros (logs) mediante **journalctl** para diagnosticar problemas en producción.

---

# Conceptos aprendidos

## ¿Qué ocurre cuando inicia Linux?

Cuando se enciende una computadora ocurre, de manera simplificada, la siguiente secuencia:

```text
Encendido del equipo
        │
        ▼
BIOS / UEFI
        │
        ▼
Bootloader (GRUB)
        │
        ▼
Kernel de Linux
        │
        ▼
PID 1 (systemd)
        │
        ▼
Inicio de servicios del sistema
```

Una vez que el kernel ha sido cargado, necesita un primer proceso que continúe con el arranque del sistema.

En la mayoría de las distribuciones Linux modernas, ese proceso es **systemd**.

---

# ¿Qué es systemd?

**systemd** es el sistema de inicialización (*init system*) utilizado por la mayoría de las distribuciones Linux actuales.

Es el primer proceso del espacio de usuario (PID 1) y es responsable de administrar el ciclo de vida de los servicios y otros recursos del sistema.

Entre sus responsabilidades se encuentran:

- iniciar servicios durante el arranque.
- detener servicios.
- reiniciar servicios cuando fallan.
- gestionar dependencias entre servicios.
- registrar eventos del sistema.
- supervisar procesos pertenecientes a un servicio.

---

# PID 1

Se verificó cuál es el proceso con PID 1 mediante:

```bash
ps -p 1
```

Resultado esperado:

```text
PID TTY          TIME CMD
1   ?        00:00:03 systemd
```

El proceso con PID 1 es especial porque todos los procesos del sistema descienden, directa o indirectamente, de él.

---

# ¿Qué es una Unit?

systemd no administra procesos directamente.

Administra **Units**.

Una Unit representa un recurso que systemd puede controlar.

Los tipos más comunes son:

| Tipo | Descripción |
|-------|-------------|
| `.service` | Servicios |
| `.socket` | Sockets |
| `.mount` | Sistemas de archivos montados |
| `.target` | Agrupaciones de Units |
| `.timer` | Tareas programadas |
| `.path` | Monitoreo de archivos y directorios |

Durante este laboratorio únicamente se trabajó con **Service Units**.

---

# ¿Qué es un Service?

Un **Service** es una Unit cuya función es describir cómo debe ejecutarse un proceso.

Ejemplo:

```text
nginx.service
```

No representa al proceso en sí.

Representa la configuración que indica a systemd:

- qué programa ejecutar.
- cómo iniciarlo.
- cuándo reiniciarlo.
- bajo qué usuario ejecutarlo.
- qué dependencias necesita.
- cómo debe finalizar.

Un mismo servicio puede contener uno o varios procesos.

Ejemplo:

```text
nginx.service
        │
        ▼
Master Process
        │
 ┌──────┴──────┐
 │             │
 ▼             ▼
Worker      Worker
```

---

# Ubicación de las Units

Las Units suelen almacenarse en alguno de estos directorios:

```text
/etc/systemd/system
```

o

```text
/usr/lib/systemd/system
```

Se listaron utilizando:

```bash
ls /etc/systemd/system
```

---

# systemctl

`systemctl` es la herramienta utilizada para comunicarse con systemd.

No administra procesos directamente.

La relación es la siguiente:

```text
systemctl
      │
      ▼
systemd
      │
      ▼
Units
      │
      ▼
Procesos
```

---

# Listar servicios

Comando utilizado:

```bash
systemctl list-units --type=service
```

Este comando muestra todas las Service Units actualmente conocidas por systemd.

Ejemplo:

```text
ssh.service

docker.service

NetworkManager.service
```

---

# Estado de un servicio

Se consultó el estado de un servicio mediante:

```bash
systemctl status <servicio>
```

Ejemplo:

```bash
systemctl status ssh
```

Información observada:

```text
Loaded

Active

Main PID

Tasks

Memory

CGroup
```

---

# Explicación de cada campo

## Loaded

Indica si systemd pudo cargar correctamente la definición de la Unit.

Valores comunes:

```text
loaded
```

```text
not-found
```

---

## Active

Representa el estado general del servicio.

Estados comunes:

```text
active
```

Servicio funcionando correctamente.

```text
inactive
```

Servicio detenido.

```text
failed
```

El servicio intentó iniciar y falló.

```text
activating
```

Se encuentra iniciándose.

```text
deactivating
```

Se encuentra apagándose.

---

## Main PID

PID del proceso principal administrado por systemd.

Permite localizar posteriormente el proceso mediante herramientas como:

```bash
ps
```

o

```bash
top
```

---

## Tasks

Cantidad de tareas (procesos o hilos) pertenecientes al servicio.

---

## Memory

Cantidad aproximada de memoria RAM utilizada por el servicio.

---

## CGroup

Indica el Control Group donde systemd agrupa los procesos del servicio.

Los **cgroups** permiten controlar y limitar recursos como:

- CPU
- memoria
- procesos

Esta tecnología es utilizada posteriormente por Docker y Kubernetes.

---

# journald

Los servicios administrados por systemd generan registros (logs).

Estos registros son almacenados por:

```text
systemd-journald
```

journald centraliza los eventos generados por el sistema operativo y los servicios.

---

# journalctl

`journalctl` es la herramienta utilizada para consultar el journal.

---

# Consultar los logs de un servicio

Comando utilizado:

```bash
journalctl -u <servicio>
```

Ejemplo:

```bash
journalctl -u ssh.service
```

El parámetro:

```text
-u
```

significa:

```text
Unit
```

y filtra únicamente los registros pertenecientes a esa Unit.

---

# Mostrar los últimos registros

```bash
journalctl -u ssh.service -n 50
```

Muestra únicamente las últimas 50 entradas.

---

# Seguir los logs en tiempo real

```bash
journalctl -u ssh.service -f
```

El parámetro:

```text
-f
```

equivale a:

```text
follow
```

El comando permanece abierto mostrando nuevos registros conforme aparecen.

---

# Mostrar únicamente el arranque actual

```bash
journalctl -u ssh.service -b
```

El parámetro:

```text
-b
```

significa:

```text
boot
```

y limita la consulta al arranque actual del sistema.

---

# Filtrar por prioridad

Consultar únicamente errores:

```bash
journalctl -p err
```

También pueden consultarse advertencias:

```bash
journalctl -p warning
```

---

# Laboratorio

## Paso 1

Listar los servicios disponibles.

```bash
systemctl list-units --type=service
```

---

## Paso 2

Seleccionar un servicio existente.

Ejemplos:

```text
ssh.service

cron.service

NetworkManager.service

docker.service
```

---

## Paso 3

Consultar su estado.

```bash
systemctl status <servicio>
```

Registrar:

- Loaded
- Active
- Main PID
- Memory
- CGroup

---

## Paso 4

Consultar los registros.

```bash
journalctl -u <servicio>
```

---

## Paso 5

Mostrar únicamente los últimos registros.

```bash
journalctl -u <servicio> -n 20
```

---

## Paso 6

Seguir los registros en tiempo real.

```bash
journalctl -u <servicio> -f
```

---

# Investigación de un incidente

Supongamos que un servicio deja de responder.

Antes de modificar código, el procedimiento recomendado es:

1. Verificar si el servicio está activo.

```bash
systemctl status <servicio>
```

2. Identificar el PID principal.

3. Consultar los registros.

```bash
journalctl -u <servicio>
```

4. Buscar mensajes de error.

```bash
journalctl -p err
```

5. Correlacionar la información obtenida con el estado del proceso y las métricas del sistema.

---

# Comandos utilizados

Listar servicios

```bash
systemctl list-units --type=service
```

Ver estado de un servicio

```bash
systemctl status <servicio>
```

Consultar logs

```bash
journalctl -u <servicio>
```

Últimos registros

```bash
journalctl -u <servicio> -n 50
```

Seguir logs en tiempo real

```bash
journalctl -u <servicio> -f
```

Logs del arranque actual

```bash
journalctl -u <servicio> -b
```

Mostrar errores

```bash
journalctl -p err
```

---

# Conclusiones

- `systemd` es el sistema de inicialización utilizado por la mayoría de las distribuciones Linux modernas.
- `systemctl` permite administrar y consultar el estado de las Units controladas por systemd.
- Una **Service Unit** define cómo debe ejecutarse un servicio, pero no es el proceso en sí.
- `journalctl` permite consultar los registros generados por el sistema y por los servicios administrados por systemd.
- El análisis conjunto de `systemctl` y `journalctl` constituye el primer paso para diagnosticar problemas en producción.

---

# Relación con Backend, Platform y SRE

Los servicios de infraestructura como PostgreSQL, Redis, Kafka, Nginx, Docker o una aplicación Spring Boot desplegada como servicio suelen ser administrados por systemd.

Ante un incidente, un ingeniero SRE normalmente sigue un flujo como el siguiente:

```text
Alerta
   │
   ▼
systemctl status
   │
   ▼
¿El servicio está activo?
   │
   ▼
journalctl
   │
   ▼
¿Existen errores?
   │
   ▼
Identificar causa raíz
```

Comprender este flujo permite reducir el tiempo de diagnóstico (MTTR) y constituye una habilidad fundamental para perfiles de Backend, Platform Engineering y Site Reliability Engineering.

