# Día 3 – Señales y ciclo de vida de procesos en Linux

## Objetivo

Comprender cómo Linux controla el ciclo de vida de los procesos mediante señales, cuándo utilizar cada una y cómo interpretar el comportamiento de un proceso durante su terminación.

---

# Conceptos aprendidos

## ¿Qué es una señal?

Una señal (signal) es un mecanismo de comunicación asíncrona utilizado por el kernel para notificar a un proceso que ocurrió un evento.

Una señal no contiene información compleja; simplemente indica que el proceso debe reaccionar ante determinado evento.

Ejemplos:

- El usuario presiona `Ctrl + C`.
- Un administrador solicita detener un proceso.
- Un proceso debe recargar su configuración.
- El sistema necesita finalizar un proceso.

Las señales son enviadas por el kernel o por otro proceso.

---

# El comando kill

El nombre del comando puede resultar confuso.

`kill` no "mata" procesos directamente.

Lo único que hace es enviar una señal a un proceso identificado por su PID.

Sintaxis:

```bash
kill PID
```

Por defecto envía:

```text
SIGTERM (15)
```

---

# Obtener el PID de un proceso

Se inició un proceso de prueba:

```bash
sleep 1000
```

Se obtuvo el PID utilizando:

```bash
ps aux | grep sleep
```

o

```bash
ps -ef | grep sleep
```

Resultado observado:

```text
PID: XXXXX
```

---

# Señales utilizadas

## SIGINT (2)

Comando:

```bash
kill -SIGINT PID
```

o

```bash
kill -2 PID
```

También puede enviarse presionando:

```text
Ctrl + C
```

### Comportamiento observado

El proceso terminó inmediatamente.

Esta señal representa una interrupción enviada normalmente por el usuario desde la terminal.

---

## SIGTERM (15)

Comando:

```bash
kill PID
```

o

```bash
kill -SIGTERM PID
```

### Comportamiento observado

El proceso terminó correctamente.

SIGTERM solicita al proceso finalizar de forma ordenada.

Permite que la aplicación:

- cierre conexiones
- guarde información pendiente
- libere memoria
- termine procesos internos

Este tipo de cierre se conoce como:

```text
Graceful Shutdown
```

---

## SIGKILL (9)

Comando:

```bash
kill -9 PID
```

o

```bash
kill -SIGKILL PID
```

### Comportamiento observado

El proceso desapareció inmediatamente.

No tuvo oportunidad de ejecutar código de limpieza.

SIGKILL no puede ser ignorada ni capturada por una aplicación.

El kernel elimina directamente el proceso.

---

# Diferencias entre SIGTERM y SIGKILL

| SIGTERM | SIGKILL |
|----------|----------|
| Permite finalizar correctamente | Finaliza inmediatamente |
| Puede ser capturada | No puede capturarse |
| Permite liberar recursos | No ejecuta limpieza |
| Recomendado como primera opción | Último recurso |
| Utilizado por Docker y Kubernetes para apagados normales | Utilizado cuando el proceso no responde |

---

# ¿Qué hace Ctrl + C?

Al presionar:

```text
Ctrl + C
```

la terminal no mata directamente el proceso.

Lo que realmente ocurre es:

```text
Terminal
      │
      ▼
Kernel
      │
      ▼
envía SIGINT
      │
      ▼
Proceso
```

El proceso decide cómo responder a esa señal.

---

# El comando wait

Se creó el siguiente script:

```bash
#!/bin/bash

sleep 5 &

echo "PID: $!"

wait

echo "Terminó"
```

## Explicación

1. `sleep 5 &` crea un proceso hijo.
2. `$!` imprime el PID del último proceso ejecutado en segundo plano.
3. `wait` bloquea el script hasta que el hijo termina.
4. Una vez finalizado el hijo, el script continúa.

Resultado observado:

```text
PID: XXXX

(espera 5 segundos)

Terminó
```

---

# Procesos Zombie

Un proceso zombie es un proceso que ya terminó su ejecución pero cuyo proceso padre todavía no ha leído su estado de salida mediante `wait()`.

Características:

- No consume CPU.
- No ejecuta instrucciones.
- Conserva únicamente información mínima en la tabla de procesos.
- Desaparece cuando el padre ejecuta `wait()`.

Estado en Linux:

```text
Z
```

Generalmente muchos procesos zombie indican que una aplicación tiene un error al gestionar sus procesos hijos.

---

# Flujo del ciclo de vida

Proceso creado

↓

Proceso ejecutándose

↓

Recibe una señal

↓

SIGINT
Usuario interrumpe

o

SIGTERM
Solicitud de cierre ordenado

o

SIGKILL
Finalización inmediata

↓

Proceso termina

↓

Kernel conserva temporalmente el código de salida

↓

El proceso padre ejecuta wait()

↓

El proceso desaparece completamente

---

# Comandos utilizados

Listar procesos

```bash
ps aux
```

Buscar proceso

```bash
ps aux | grep sleep
```

Listar señales

```bash
kill -l
```

Enviar SIGTERM

```bash
kill PID
```

Enviar SIGINT

```bash
kill -SIGINT PID
```

Enviar SIGKILL

```bash
kill -SIGKILL PID
```

Esperar procesos hijos

```bash
wait
```

---

# Conclusiones

- Linux controla el ciclo de vida de los procesos mediante señales.
- `kill` únicamente envía señales; no elimina procesos directamente.
- `SIGTERM` debe utilizarse como primera opción porque permite un cierre ordenado.
- `SIGKILL` debe reservarse para procesos que no responden.
- `Ctrl + C` envía `SIGINT`, no finaliza directamente el proceso.
- `wait()` permite al proceso padre recoger el estado de salida de sus hijos.
- Los procesos zombie aparecen cuando un proceso padre no recoge el estado de finalización de sus procesos hijos.

---

# Relación con Backend, Platform y SRE

Estos conceptos son fundamentales para comprender el comportamiento de aplicaciones en producción.

Ejemplos:

- Spring Boot recibe `SIGTERM` durante un despliegue para finalizar peticiones activas.
- Docker envía `SIGTERM` antes de detener un contenedor.
- Kubernetes espera un tiempo configurable (`terminationGracePeriodSeconds`) para permitir un apagado ordenado antes de enviar `SIGKILL`.
- Un ingeniero SRE debe saber identificar cuándo un proceso terminó correctamente, cuándo fue forzado a terminar y cómo interpretar procesos zombie durante el diagnóstico de incidentes.

