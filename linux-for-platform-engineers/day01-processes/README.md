# Día 1 – Introducción a procesos en Linux

## Objetivo

Comprender qué es un proceso, cómo el sistema operativo representa los programas en ejecución y aprender a inspeccionar los procesos utilizando herramientas básicas de Linux.

---

# Conceptos aprendidos

## ¿Qué es un proceso?

Un proceso es una instancia de un programa en ejecución.

Cuando un programa es iniciado, Linux crea un proceso y le asigna recursos como:

- CPU
- Memoria
- Identificador (PID)
- Espacio de direcciones
- Variables de entorno
- Archivos abiertos

Cada proceso es administrado por el kernel.

---

## Programa vs Proceso

Es importante distinguir ambos conceptos.

### Programa

Es un archivo almacenado en el disco.

Ejemplos:

```text
/usr/bin/java

/usr/bin/bash

/usr/bin/python3
```

El programa no está ejecutándose.

---

### Proceso

Es la ejecución de un programa.

Ejemplo:

```text
java -jar api.jar
```

Cuando este comando se ejecuta, Linux crea un proceso.

Pueden existir múltiples procesos ejecutando el mismo programa.

Ejemplo:

```text
Google Chrome

PID 4102

Google Chrome

PID 4189

Google Chrome

PID 4250
```

Todos ejecutan el mismo programa, pero cada uno representa un proceso diferente.

---

# El Kernel

El kernel es el núcleo del sistema operativo.

Es responsable de:

- crear procesos
- asignar CPU
- administrar memoria
- administrar dispositivos
- enviar señales
- planificar la ejecución mediante el scheduler

Los procesos nunca acceden directamente al hardware.

Toda comunicación ocurre a través del kernel.

---

# PID (Process ID)

Cada proceso recibe un identificador único llamado:

```text
PID
```

Ejemplo:

```text
PID 3251
```

Este identificador permite:

- monitorear procesos
- enviar señales
- finalizar procesos
- obtener estadísticas

No pueden existir dos procesos con el mismo PID al mismo tiempo.

---

# PPID (Parent Process ID)

Todo proceso (excepto algunos procesos especiales del sistema) es creado por otro proceso.

El proceso que crea otro proceso se denomina:

```text
Proceso Padre
```

El nuevo proceso se denomina:

```text
Proceso Hijo
```

Ejemplo:

```text
bash

↓

java

↓

spring boot
```

Aquí:

- bash es padre de java
- java es padre de Spring Boot

---

# El árbol de procesos

Los procesos forman un árbol.

Puede visualizarse utilizando:

```bash
pstree
```

Ejemplo:

```text
systemd
 ├── NetworkManager
 ├── sshd
 ├── docker
 └── bash
      └── java
```

Este árbol permite comprender quién creó cada proceso.

---

# El proceso PID 1

En sistemas Linux modernos el primer proceso normalmente es:

```text
systemd
```

PID:

```text
1
```

Todos los procesos existentes descienden directa o indirectamente de este proceso.

Si un proceso queda sin padre, el sistema reasigna su padre a PID 1.

---

# Inspección de procesos

## ps

El comando:

```bash
ps
```

muestra procesos asociados a la terminal actual.

---

## ps aux

Comando:

```bash
ps aux
```

Muestra prácticamente todos los procesos del sistema.

Columnas principales:

### USER

Usuario propietario del proceso.

---

### PID

Identificador único del proceso.

---

### %CPU

Porcentaje de CPU utilizado.

---

### %MEM

Porcentaje de memoria utilizada.

---

### VSZ

Virtual Size.

Cantidad de memoria virtual reservada.

No representa necesariamente memoria física utilizada.

---

### RSS

Resident Set Size.

Cantidad de memoria física realmente ocupada.

---

### STAT

Estado actual del proceso.

Algunos estados comunes:

```text
R
Running
```

El proceso está ejecutándose.

```text
S
Sleeping
```

Está esperando algún evento.

```text
D
Uninterruptible Sleep
```

Esperando operaciones de entrada/salida.

```text
T
Stopped
```

Proceso detenido.

```text
Z
Zombie
```

Proceso terminado cuyo padre aún no recoge su estado.

---

### COMMAND

Comando utilizado para iniciar el proceso.

---

# Buscar procesos

Se utilizó:

```bash
ps aux | grep nombre
```

Ejemplo:

```bash
ps aux | grep java
```

Esto permite localizar procesos específicos.

---

# Filtrado mediante tuberías

El operador:

```bash
|
```

envía la salida de un comando como entrada del siguiente.

Ejemplo:

```bash
ps aux | grep postgres
```

Interpretación:

1. `ps aux` lista procesos.
2. `grep postgres` filtra únicamente aquellos que contienen la palabra "postgres".

---

# Visualización jerárquica

Comando:

```bash
pstree
```

Permite observar las relaciones padre-hijo entre procesos.

Esta vista facilita comprender cómo se originan los procesos dentro del sistema.

---

# Laboratorio

## Experimento 1

Listar todos los procesos:

```bash
ps aux
```

Observaciones:

- Se identificaron múltiples procesos del sistema.
- Cada proceso posee un PID único.
- Existen procesos pertenecientes a distintos usuarios.

---

## Experimento 2

Buscar un proceso específico.

Ejemplo:

```bash
ps aux | grep bash
```

Observaciones:

- Se encontró el proceso correspondiente a la terminal actual.
- También apareció el propio comando `grep`, ya que contiene la palabra buscada.

---

## Experimento 3

Visualizar el árbol de procesos.

```bash
pstree
```

Observaciones:

- Todos los procesos pertenecen a una estructura jerárquica.
- Se pudo identificar qué procesos son hijos de otros.

---

# Comandos utilizados

Listar procesos

```bash
ps
```

Listar todos los procesos

```bash
ps aux
```

Buscar un proceso

```bash
ps aux | grep nombre
```

Visualizar árbol de procesos

```bash
pstree
```

---

# Conclusiones

- Un proceso es un programa en ejecución administrado por el kernel.
- Cada proceso posee un PID único.
- Todo proceso tiene un proceso padre (PPID).
- Los procesos forman un árbol jerárquico.
- `ps` permite inspeccionar procesos activos.
- `grep` facilita localizar procesos específicos.
- `pstree` permite visualizar las relaciones padre-hijo entre procesos.

---

# Relación con Backend, Platform y SRE

Comprender los procesos es fundamental para diagnosticar aplicaciones en producción.

Ejemplos:

- Una aplicación Spring Boot corresponde a un proceso Java administrado por el kernel.
- PostgreSQL, Redis, Kafka y Nginx también son procesos independientes.
- Un ingeniero SRE debe ser capaz de localizar un proceso, identificar quién lo creó, inspeccionar su estado y relacionarlo con el resto del sistema antes de iniciar cualquier investigación.

Estos conceptos constituyen la base para temas posteriores como monitoreo de procesos, señales, concurrencia, observabilidad y diagnóstico de incidentes.

