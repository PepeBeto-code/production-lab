# Día 2 – Monitoreo de procesos: CPU Bound vs I/O Bound

## Objetivo

Aprender a inspeccionar procesos en ejecución utilizando herramientas básicas de Linux para identificar consumo de CPU, memoria y tiempo de ejecución, así como distinguir entre procesos limitados por CPU (CPU Bound) y procesos limitados por operaciones de entrada/salida (I/O Bound).

---

# Conceptos aprendidos

## ¿Qué es un proceso?

Un proceso es una instancia de un programa en ejecución.

Cada proceso posee un identificador único llamado PID (Process ID), mediante el cual el sistema operativo puede administrarlo.

Ejemplos de procesos:

- Spring Boot
- PostgreSQL
- Redis
- Kafka
- Docker
- Chrome

Todos los procesos compiten por recursos del sistema como CPU, memoria y acceso a dispositivos.

---

# El comando ps

El comando `ps` permite obtener información sobre los procesos que existen en el sistema.

Comando utilizado:

```bash
ps -eo pid,%cpu,%mem,etime,time,comm
```

Resultado esperado:

```text
PID   %CPU %MEM ETIME     TIME     COMMAND
4213  99.8  0.0 00:01:20 00:01:19 yes
```

---

# Explicación de las columnas

## PID

Identificador único del proceso.

Ejemplo:

```text
4213
```

Permite administrar el proceso utilizando herramientas como `kill`.

---

## %CPU

Porcentaje de CPU consumido por el proceso.

Un valor cercano al 100% indica que el proceso está utilizando prácticamente un núcleo completo del procesador.

---

## %MEM

Porcentaje de memoria RAM utilizada por el proceso.

Es útil para identificar aplicaciones que consumen grandes cantidades de memoria.

---

## ETIME

Elapsed Time.

Representa cuánto tiempo lleva vivo el proceso desde que fue iniciado.

Ejemplo:

```text
01:25:10
```

Significa:

- 1 hora
- 25 minutos
- 10 segundos

No indica cuánto tiempo ha usado la CPU.

---

## TIME

CPU Time.

Representa únicamente el tiempo durante el cual el proceso realmente estuvo ejecutándose sobre la CPU.

Ejemplo:

```text
00:00:35
```

Esto significa que, aunque el proceso lleve varios minutos vivo, solamente ha utilizado 35 segundos de CPU.

---

# Diferencia entre ETIME y TIME

Supongamos el siguiente proceso:

```text
ETIME

05:00
```

```text
TIME

00:04
```

Interpretación:

El proceso lleva cinco minutos ejecutándose.

Sin embargo, solamente ha utilizado cuatro segundos de CPU.

Durante el resto del tiempo estuvo esperando algún recurso.

Ahora otro ejemplo:

```text
ETIME

05:00
```

```text
TIME

04:58
```

Interpretación:

El proceso pasó prácticamente todo el tiempo utilizando la CPU.

Es un fuerte indicio de un proceso CPU Bound.

---

# Ordenar procesos por consumo de CPU

Comando utilizado:

```bash
ps -eo pid,%cpu,%mem,etime,time,comm --sort=-%cpu
```

Observación:

Los procesos aparecen ordenados desde el que consume más CPU hasta el que consume menos.

Esta información es útil para identificar rápidamente procesos problemáticos durante un incidente.

---

# Ordenar procesos por consumo de memoria

Comando utilizado:

```bash
ps -eo pid,%cpu,%mem,etime,time,comm --sort=-%mem
```

Permite detectar procesos que consumen grandes cantidades de RAM.

---

# Monitoreo en tiempo real con top

Comando:

```bash
top
```

Información observada:

- utilización global de CPU
- utilización de memoria
- procesos activos
- porcentaje de CPU por proceso
- porcentaje de memoria por proceso
- tiempo acumulado de CPU

Atajos utilizados:

Ordenar por CPU:

```text
P
```

Ordenar por memoria:

```text
M
```

Salir:

```text
q
```

---

# htop

También se revisó la herramienta:

```bash
htop
```

Ventajas respecto a `top`:

- interfaz más clara
- barras de CPU y memoria
- búsqueda de procesos
- ordenamiento sencillo
- árbol de procesos
- posibilidad de enviar señales desde la interfaz

---

# pidstat

Herramienta perteneciente al paquete `sysstat`.

Comando:

```bash
pidstat 1
```

Permite observar estadísticas de procesos cada segundo.

Es útil para monitorear:

- CPU
- memoria
- operaciones de entrada/salida

---

# Concepto de CPU Bound

Un proceso CPU Bound pasa la mayor parte de su tiempo ejecutando instrucciones sobre el procesador.

Características:

- utilización muy alta de CPU
- TIME aumenta casi al mismo ritmo que ETIME
- pocas esperas por recursos externos

Ejemplos reales:

- compresión de archivos
- criptografía
- renderizado
- cálculos matemáticos intensivos

---

# Concepto de I/O Bound

Un proceso I/O Bound pasa gran parte de su tiempo esperando recursos externos.

Ejemplos:

- disco
- red
- PostgreSQL
- Redis
- Kafka

Características:

- bajo uso de CPU
- ETIME aumenta rápidamente
- TIME aumenta lentamente

El cuello de botella no es el procesador, sino la espera por datos.

---

# Laboratorio

## Experimento 1 – CPU Bound

Proceso ejecutado:

```bash
yes > /dev/null
```

### Explicación

`yes` imprime continuamente el carácter "y".

Al redirigir la salida hacia `/dev/null`, toda la salida es descartada.

Sin embargo, el proceso continúa ejecutando instrucciones sin detenerse.

---

### Observaciones

Con `top` se observó:

- el proceso permanecía en los primeros lugares.
- el uso de CPU permanecía cercano al 100%.
- la memoria utilizada prácticamente no cambiaba.
- el tiempo de CPU (`TIME`) aumentaba continuamente.
- el proceso nunca entraba en espera.

Interpretación:

El proceso es completamente CPU Bound.

---

## Experimento 2 – Copia de un archivo grande

Proceso ejecutado:

```bash
cp archivo_grande.iso copia.iso
```

Observaciones:

- utilización de CPU relativamente baja.
- el proceso permanecía gran parte del tiempo esperando al dispositivo de almacenamiento.
- el cuello de botella era el acceso al disco.

Interpretación:

Proceso I/O Bound.

---

# Comparación

| Característica | CPU Bound | I/O Bound |
|----------------|-----------|-----------|
| Uso de CPU | Muy alto | Bajo o moderado |
| TIME | Crece rápidamente | Crece lentamente |
| ETIME | Similar a TIME | Mucho mayor que TIME |
| Espera recursos | Muy poca | Muy frecuente |
| Cuello de botella | Procesador | Disco, red o base de datos |

---

# Comandos utilizados

Mostrar procesos

```bash
ps -eo pid,%cpu,%mem,etime,time,comm
```

Ordenar por CPU

```bash
ps -eo pid,%cpu,%mem,etime,time,comm --sort=-%cpu
```

Ordenar por memoria

```bash
ps -eo pid,%cpu,%mem,etime,time,comm --sort=-%mem
```

Monitoreo en tiempo real

```bash
top
```

Monitor avanzado

```bash
htop
```

Estadísticas por proceso

```bash
pidstat 1
```

Proceso CPU Bound

```bash
yes > /dev/null
```

---

# Conclusiones

- `ps` permite inspeccionar procesos y analizar el uso de CPU, memoria y tiempo de ejecución.
- `top` proporciona monitoreo en tiempo real para detectar procesos problemáticos.
- `ETIME` representa el tiempo total de vida del proceso.
- `TIME` representa únicamente el tiempo acumulado de CPU.
- Un proceso CPU Bound utiliza la mayor parte del tiempo ejecutando instrucciones.
- Un proceso I/O Bound pasa gran parte del tiempo esperando recursos externos.
- Antes de optimizar una aplicación es indispensable identificar cuál es el recurso que limita su rendimiento.

---

# Relación con Backend, Platform y SRE

Estas herramientas constituyen el primer nivel de diagnóstico durante un incidente en producción.

Ejemplos:

- Un servicio Spring Boot responde lentamente. Antes de modificar el código, debe verificarse si el proceso está limitado por CPU o por operaciones de entrada/salida.
- Si el uso de CPU es bajo pero la latencia es alta, el problema probablemente se encuentre en PostgreSQL, Redis, Kafka, el almacenamiento o la red.
- La capacidad para diferenciar procesos CPU Bound e I/O Bound permite formular hipótesis correctas antes de iniciar una investigación más profunda con herramientas como `jstack`, `jcmd`, OpenTelemetry, Prometheus o Grafana.

