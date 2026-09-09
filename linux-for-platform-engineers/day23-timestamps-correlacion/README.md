# W4D4 — Timestamps y correlación: ¿qué ocurrió primero?

## 1. Idea principal

En troubleshooting no basta con encontrar errores.

Un incidente puede generar una cadena de eventos:

```text
error
↓
restart
↓
otro error
↓
5xx
↓
servicio caído
```

La tarea es reconstruir la secuencia:

> **¿Cuál ocurrió primero y cuál fue consecuencia?**

Para esto utilizamos **timestamps** y correlacionamos evidencia de diferentes fuentes.

---

## 2. Timestamp

Un **timestamp** es una marca de tiempo asociada a un evento.

Ejemplo:

```text
2026-09-08T18:31:02-06:00
```

El timestamp permite determinar **cuándo ocurrió o fue registrado un evento**.

En troubleshooting sirve principalmente para ordenar acontecimientos.

```text
18:31:01 → evento A
18:31:02 → evento B
18:31:03 → evento C
```

Por lo tanto:

```text
A ocurrió antes que B
B ocurrió antes que C
```

---

## 3. Orden temporal ≠ causalidad

Que un evento ocurra antes que otro **no demuestra automáticamente** que lo haya causado.

Ejemplo:

```text
18:40:01 → timeout
18:40:02 → 502
```

Podemos afirmar:

```text
timeout ocurrió antes que 502
```

Pero todavía debemos investigar para determinar si:

```text
timeout → provocó → 502
```

es realmente una explicación válida.

Primero tenemos una **relación temporal**.

Después necesitamos contexto y evidencia para formular una hipótesis.

---

## 4. Correlación

**Correlacionar** significa relacionar diferentes evidencias que tienen algún vínculo relevante.

Podemos correlacionar:

* timestamps;
* eventos;
* servicio involucrado;
* contexto;
* comportamiento observado;
* diferentes fuentes de logs.

Ejemplo:

```text
access.log
10:31:01 → POST /login

error.log
10:31:02 → upstream timed out

journald
10:31:02 → evento relacionado con nginx
```

Las fuentes aportan información diferente, pero juntas permiten reconstruir mejor lo ocurrido.

---

## 5. Timeline

Una **timeline** es una secuencia ordenada de eventos según el momento en que ocurrieron.

Ejemplo:

```text
18:05:10 → POST /api/login
18:05:11 → upstream timed out
18:05:12 → 502
18:05:15 → nginx restart
18:05:20 → nginx vuelve a estar activo
```

La timeline permite identificar:

1. primera señal;
2. eventos posteriores;
3. posibles consecuencias;
4. relación probable entre acontecimientos.

---

## 6. Primera falla

La primera falla no necesariamente es:

> "La primera línea que contiene la palabra error."

Debemos buscar:

> **El primer evento anómalo relevante dentro de la secuencia del incidente.**

También debemos revisar si existe alguna señal anterior en otra fuente.

Ejemplo:

```text
18:39:58 → backend comenzó a rechazar conexiones
18:40:02 → nginx upstream timed out
18:40:03 → 502
```

En este caso, el `timeout` puede ser un error registrado por nginx, pero probablemente **no sea la primera falla del incidente**.

---

## 7. Consecuencias

No todos los eventos posteriores son necesariamente causas.

Podemos tener:

```text
problema
↓
error
↓
síntoma
↓
consecuencia
↓
acción de recuperación
```

Ejemplo:

```text
backend no responde
↓
nginx espera
↓
timeout
↓
502
↓
restart
```

Aquí el `502` podría ser una consecuencia del problema anterior y el `restart` una acción posterior de recuperación.

---

## 8. Comando principal

```bash
journalctl -u nginx \
  --since "6 hours ago" \
  --output=short-iso \
  --no-pager
```

### Componentes

```text
journalctl
```

Consulta los registros administrados por journald.

```text
-u nginx
```

Limita la consulta a la unidad `nginx`.

```text
--since "6 hours ago"
```

Limita la búsqueda a las últimas seis horas.

```text
--output=short-iso
```

Utiliza un formato de salida con timestamps adecuados para analizar la secuencia temporal.

```text
--no-pager
```

Muestra directamente la salida en la terminal.

### Objetivo

Obtener una vista temporal de los eventos registrados para nginx.

---

## 9. Revisar error.log

```bash
sudo tail -n 500 /var/log/nginx/error.log
```

### Componentes

```text
sudo
```

Ejecuta el comando con privilegios elevados.

```text
tail
```

Muestra las últimas líneas de un archivo.

```text
-n 500
```

Solicita las últimas 500 líneas.

```text
/var/log/nginx/error.log
```

Archivo de errores de nginx.

### Objetivo

Obtener suficiente contexto del `error.log` para comparar sus timestamps con los eventos de journald.

---

## 10. Comparación de fuentes

Las fuentes no son equivalentes.

```text
journald
↓
comportamiento del servicio como unidad

error.log
↓
errores y condiciones anómalas de nginx

access.log
↓
peticiones recibidas por nginx
```

Por eso podemos construir una timeline combinando evidencia:

```text
             INCIDENTE
                 ↓
        ┌────────┼────────┐
        ↓        ↓        ↓
   access.log error.log journald
        │        │        │
        └────────┼────────┘
                 ↓
             TIMELINE
                 ↓
            CORRELACIÓN
                 ↓
             HIPÓTESIS
```

---

## 11. Modelo de análisis

El proceso aprendido es:

```text
incidente
↓
obtener evidencia
↓
obtener timestamps
↓
ordenar eventos
↓
identificar primera señal
↓
identificar eventos posteriores
↓
identificar consecuencias
↓
formular hipótesis
↓
comprobar hipótesis
↓
conclusión
```

---

## 12. Hipótesis

Una hipótesis debe relacionar eventos utilizando la evidencia disponible.

Formato:

> **"X probablemente provocó Y porque..."**

Ejemplo:

> El timeout probablemente provocó los 502 porque el timeout apareció primero y los 502 comenzaron inmediatamente después.

La palabra **"probablemente"** es importante porque una secuencia temporal por sí sola no demuestra causalidad.

---

## 13. Entregable de troubleshooting

Para documentar el incidente:

### Primera falla

Identificar el primer evento anómalo relevante.

### Consecuencia 1

Identificar qué ocurrió posteriormente y que posiblemente esté relacionado con la primera falla.

### Consecuencia 2

Identificar otro evento posterior que pueda ser consecuencia del problema.

### Timeline

Ordenar los eventos por timestamp:

```text
HH:MM:SS → evento 1
HH:MM:SS → evento 2
HH:MM:SS → evento 3
HH:MM:SS → evento 4
```

### Hipótesis

Explicar la relación probable:

> **"X probablemente provocó Y porque..."**

---

## 14. Idea clave del día

> **Encontrar un error no es lo mismo que explicar un incidente.**

El troubleshooting importante comienza cuando dejamos de preguntar solamente:

```text
¿Qué error apareció?
```

y empezamos a preguntar:

```text
¿Qué ocurrió primero?
¿Qué ocurrió después?
¿Qué parece consecuencia?
¿Qué relación existe entre los eventos?
¿Qué evidencia respalda esa hipótesis?
```

### Principio del día

```text
EVENTO
↓
TIMESTAMP
↓
ORDEN TEMPORAL
↓
CORRELACIÓN
↓
HIPÓTESIS
↓
COMPROBACIÓN
```

**Objetivo aprendido:** utilizar timestamps y múltiples fuentes de logs para reconstruir la secuencia de un incidente y distinguir entre una posible causa, un síntoma y una consecuencia.
