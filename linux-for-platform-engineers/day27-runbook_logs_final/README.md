# W4 — Runbook final: Troubleshooting de logs

## Objetivo

Investigar problemas de cualquier servicio Linux de forma sistemática, usando los logs como evidencia y no como una lista de líneas que simplemente hay que leer.

El modelo mental es:

```text
Problema
   ↓
Pregunta concreta
   ↓
Evidencia
   ↓
Acotar búsqueda
   ↓
Ordenar eventos
   ↓
Correlacionar señales
   ↓
Formular hipótesis
   ↓
Comprobar hipótesis
   ↓
Conclusión
```

> **Nginx es solamente un ejemplo. Este runbook es genérico para cualquier servicio Linux administrado por systemd.**

---

## 1. Diagnóstico rápido

### Pregunta

> ¿Cuál es el estado actual del servicio?

```bash
systemctl status <unidad>
```

Ejemplo:

```bash
systemctl status nginx
```

### Estados principales

```text
Active: active (running)
```

El servicio está activo y su proceso está ejecutándose.

```text
Active: inactive
```

El servicio no está activo. No implica por sí solo que exista un fallo.

```text
Active: failed
```

systemd considera que la unidad terminó en un estado de fallo.

### Importante

`failed` describe el estado observado, no necesariamente la causa raíz.

Ejemplo:

```text
configuración inválida
        ↓
la aplicación falla al iniciar
        ↓
systemd registra failure
        ↓
servicio = failed
```

Por lo tanto:

```text
failed ≠ causa raíz
```

`systemctl status` proporciona una primera pieza de evidencia. Si existe un problema, hay que investigar los logs para determinar por qué ocurrió.

---

## 2. Recolección de evidencia

Una vez conocido el estado, consultar los eventos relacionados con la unidad.

### Últimas 100 entradas

```bash
journalctl -u <unidad> -n 100
```

Ejemplo:

```bash
journalctl -u nginx -n 100
```

Descomposición:

- `journalctl`: consulta el journal.
- `-u <unidad>`: limita la búsqueda a la unidad indicada.
- `-n 100`: muestra las últimas 100 entradas.

La idea es reducir el espacio de búsqueda:

```text
todo el journal
      ↓
unidad concreta
      ↓
últimas 100 entradas
```

### Acotar por tiempo

```bash
journalctl -u <unidad> --since "30 min ago"
```

Pregunta que responde:

> ¿Qué eventos de esta unidad ocurrieron desde hace aproximadamente 30 minutos?

### Definir una ventana temporal

```bash
journalctl -u <unidad>   --since "..."   --until "..."
```

Pregunta que responde:

> ¿Qué ocurrió dentro de este intervalo concreto?

### Advertencia

El intervalo debe corresponder al incidente.

No usar automáticamente:

```bash
--since "15 min ago"
```

Si el incidente ocurrió horas antes, el filtro puede eliminar precisamente la evidencia que buscamos.

> Un filtro correctamente construido puede producir evidencia insuficiente si el intervalo temporal no corresponde al incidente.

---

## 3. Búsqueda inteligente

Una vez acotados servicio y tiempo, buscar señales relevantes.

### Keywords útiles

```text
error
failed
timeout
permission
denied
exception
critical
no such file
```

La lista no es una regla absoluta. Las señales deben elegirse de acuerdo con la pregunta y el tipo de incidente.

### Ejemplo

```bash
journalctl -u <unidad> --since "30 min ago" | grep -iE "error|failed|timeout|denied"
```

### Descomposición

`grep`:

> Busca líneas que coincidan con un patrón.

`-i`:

> Ignora mayúsculas y minúsculas.

`-E`:

> Permite expresiones regulares extendidas.

Dentro del patrón:

```text
error|failed|timeout|denied
```

el `|` significa **OR**:

```text
error
   O
failed
   O
timeout
   O
denied
```

La tubería de shell:

```text
journalctl ... | grep ...
```

tiene otro significado: conecta la salida de `journalctl` con la entrada de `grep`.

Conceptualmente:

```text
journalctl
    ↓
salida de logs
    ↓
grep
    ↓
líneas relevantes
```

### No buscar solamente `error`

Buscar únicamente:

```bash
grep -i "error"
```

puede perder señales más específicas, por ejemplo:

```text
failed
timeout
permission denied
unknown directive
exception
critical
no such file
```

La búsqueda debe conservar las señales necesarias para responder la pregunta actual.

---

## 4. Correlación

Encontrar una línea de error no significa automáticamente haber encontrado la causa raíz.

### Preguntas

```text
¿Qué ocurrió primero?
¿Qué ocurrió después?
¿Cuál parece ser la causa?
¿Cuál parece ser consecuencia?
¿Coinciden los timestamps?
¿Hay otra fuente de evidencia?
```

### Timestamps

Los timestamps permiten:

- ordenar acontecimientos;
- construir una línea temporal;
- comparar eventos de diferentes fuentes;
- distinguir señales iniciales de consecuencias posteriores.

Ejemplo:

```text
10:15:01  configuración modificada
10:15:03  error de configuración
10:15:03  proceso no puede iniciar
10:15:04  systemd registra failure
```

La secuencia sugiere:

```text
configuración inválida
        ↓
error
        ↓
fallo del proceso
        ↓
estado failed
```

Por eso el último evento observado no necesariamente es la causa.

### Comparar fuentes

```text
journald
    ↓
/var/log/*
    ↓
estado del servicio
```

Una fuente puede mostrar el estado, otra el error específico y otra aportar contexto adicional.

El objetivo es construir una explicación respaldada por varias piezas de evidencia cuando sea necesario.

---

## 5. Clasificación de la causa

Después de recolectar y correlacionar evidencia, formular una hipótesis y clasificarla principalmente como:

```text
Configuración
Permisos
Recursos
Red
Filesystem
Dependencias
Aplicación / bug
```

### Configuración

Señales posibles:

```text
unknown directive
invalid configuration
syntax error
```

Hipótesis:

> Existe un problema en la configuración.

### Permisos

Señal posible:

```text
permission denied
```

La investigación debe continuar para determinar qué recurso, usuario, proceso u operación está involucrado.

### Recursos

Investigar problemas relacionados con recursos disponibles del sistema, por ejemplo:

```text
memoria
CPU
espacio
descriptores
```

### Red

Señales posibles:

```text
timeout
connection refused
connection reset
```

La señal orienta la investigación, pero no demuestra por sí sola dónde está el problema.

### Filesystem

Señales posibles:

```text
no such file
read-only filesystem
```

También pueden investigarse problemas de espacio, inodos o acceso a archivos.

### Dependencias

Un servicio puede depender de otros componentes:

```text
base de datos
otro servicio
DNS
servicio externo
archivo de configuración
certificado
```

Si una dependencia falla, el servicio principal puede mostrar solamente el síntoma.

### Aplicación / bug

La causa puede estar en el comportamiento o código de la propia aplicación.

Ejemplos de señales:

```text
exception
unexpected state
internal error
```

---

## 6. Acción y verificación

Una hipótesis debe conducir a una acción relacionada con la evidencia.

Modelo:

```text
evidencia
   ↓
hipótesis
   ↓
acción dirigida
```

Evitar el patrón:

```text
problema
   ↓
reiniciar todo
   ↓
esperar que funcione
```

Una acción de troubleshooting debe buscar resolver o comprobar la hipótesis planteada.

### Después de la acción

Volver a comprobar el estado:

```bash
systemctl status <unidad>
```

Y revisar nuevamente los logs:

```bash
journalctl -u <unidad> ...
```

La verificación debe buscar evidencia nueva que permita responder:

> **¿La evidencia demuestra que el servicio se recuperó?**

No basta con saber que se ejecutó una acción de reparación.

---

## 7. Criterio de recuperación

Una recuperación puede comprobarse mediante varias piezas de evidencia.

### Nivel 1 — Estado

```bash
systemctl status <unidad>
```

Comprobar que el servicio esté en el estado esperado.

### Nivel 2 — Logs

Volver a revisar los logs y comprobar si el error investigado:

- desapareció;
- dejó de repetirse;
- o fue reemplazado por evidencia que indique otro problema.

### Nivel 3 — Comportamiento esperado

Cuando sea posible, comprobar que el servicio realiza realmente la función esperada.

Por ejemplo, dependiendo del servicio:

```text
responde a una solicitud
acepta una conexión
procesa una operación
se comunica con una dependencia
```

La idea es:

```text
estado correcto
    +
logs coherentes
    +
comportamiento esperado
```

La evidencia disponible dependerá del servicio y del incidente.

---

## 8. Runbook operativo

### Paso 1 — Definir el problema

Antes de ejecutar comandos:

```text
¿Qué está fallando exactamente?
¿Desde cuándo?
¿Qué comportamiento se esperaba?
¿Qué comportamiento se observa?
```

Convertir el problema en una pregunta concreta.

---

### Paso 2 — Revisar el estado actual

```bash
systemctl status <unidad>
```

Determinar:

```text
active
inactive
failed
```

No asumir que `failed` es la causa.

---

### Paso 3 — Obtener evidencia del journal

```bash
journalctl -u <unidad> -n 100
```

Si conocemos aproximadamente cuándo comenzó:

```bash
journalctl -u <unidad> --since "30 min ago"
```

Si conocemos una ventana específica:

```bash
journalctl -u <unidad>   --since "..."   --until "..."
```

---

### Paso 4 — Reducir el ruido

Elegir keywords relacionadas con la pregunta:

```bash
journalctl -u <unidad> --since "30 min ago" | grep -iE "error|failed|timeout|denied"
```

No buscar solamente `error` por costumbre.

---

### Paso 5 — Ordenar y correlacionar

Preguntar:

```text
¿Qué ocurrió primero?
¿Qué ocurrió después?
¿Cuál es la señal más específica?
¿Cuál parece ser causa?
¿Cuál parece ser consecuencia?
¿Los timestamps coinciden?
¿Existe otra fuente de evidencia?
```

Comparar cuando sea necesario:

```text
journald
/var/log/*
estado del servicio
```

---

### Paso 6 — Formular hipótesis

Clasificar principalmente como:

```text
Configuración
Permisos
Recursos
Red
Filesystem
Dependencias
Aplicación / bug
```

La clasificación es una hipótesis de trabajo, no una conclusión automática.

---

### Paso 7 — Comprobar

Realizar una acción dirigida por la hipótesis.

Después, volver a observar:

```bash
systemctl status <unidad>
```

y revisar nuevamente los logs.

---

### Paso 8 — Concluir

La investigación termina cuando existe evidencia suficiente para explicar:

```text
qué ocurrió
    ↓
por qué ocurrió
    ↓
qué acción se tomó
    ↓
qué evidencia demuestra el resultado
```

---

## 9. Anti-patrones que este runbook evita

### Leer todo el historial

```bash
journalctl -u <unidad>
```

sin una pregunta o intervalo concreto.

### Buscar solamente `error`

```bash
grep -i "error"
```

y asumir que todas las señales relevantes contienen esa palabra.

### Ignorar timestamps

Encontrar un error sin determinar cuándo ocurrió.

### Confundir consecuencia con causa

Interpretar:

```text
failed
```

como causa raíz sin investigar qué provocó el fallo.

### Copiar logs sin interpretarlos

Más líneas no equivalen a mejor diagnóstico.

### Ejecutar comandos sin una pregunta

Un comando debe responder una pregunta concreta o producir evidencia para una hipótesis.

### Reparar sin verificar

Ejecutar una acción y asumir que el problema quedó resuelto.

---

## 10. Modelo mental final

```text
PROBLEMA
   ↓
¿Qué necesito averiguar?
   ↓
PREGUNTA CONCRETA
   ↓
¿Qué evidencia puede responderla?
   ↓
EVIDENCIA
   ↓
¿Puedo reducir el espacio de búsqueda?
   ↓
ACOTAR
   ↓
¿En qué orden ocurrieron los eventos?
   ↓
ORDENAR
   ↓
¿Las señales pertenecen al mismo incidente?
   ↓
CORRELACIONAR
   ↓
¿Qué explicación encaja con la evidencia?
   ↓
HIPÓTESIS
   ↓
¿Cómo puedo comprobarla?
   ↓
ACCIÓN / PRUEBA
   ↓
¿La nueva evidencia confirma o rechaza la hipótesis?
   ↓
VERIFICACIÓN
   ↓
CONCLUSIÓN
```

> **Troubleshooting no consiste en encontrar "la línea roja". Consiste en construir una explicación respaldada por evidencia.**

---

## 11. Ejemplo mínimo con nginx

Nginx solamente sirve como ejemplo de aplicación del runbook.

### Estado

```bash
systemctl status nginx
```

### Logs recientes

```bash
journalctl -u nginx -n 100
```

### Acotar por tiempo

```bash
journalctl -u nginx --since "30 min ago"
```

### Buscar señales

```bash
journalctl -u nginx --since "30 min ago" | grep -iE "emerg|error|failed|unknown directive"
```

### Otra fuente de evidencia

```bash
sudo grep -iE "emerg|error|failed|unknown directive" /var/log/nginx/error.log | tail -n 50
```

El objetivo no es memorizar esta secuencia para nginx.

El objetivo es reconocer el patrón:

```text
estado
  ↓
logs
  ↓
tiempo
  ↓
señales
  ↓
correlación
  ↓
hipótesis
  ↓
acción
  ↓
verificación
```

---

## Conclusión

Este runbook consolida el trabajo de W4 sobre troubleshooting de logs.

La habilidad principal no es memorizar comandos, sino decidir:

```text
qué quiero averiguar
        ↓
qué evidencia necesito
        ↓
dónde encontrarla
        ↓
cómo reducir el ruido
        ↓
cómo relacionarla con el resto de la evidencia
        ↓
cómo comprobar la hipótesis
        ↓
cómo demostrar que el servicio se recuperó
```

El runbook queda diseñado para reutilizarse con diferentes servicios Linux y no depende de nginx.

El laboratorio práctico específico no necesita repetirse artificialmente mientras el entorno no tenga suficiente complejidad. El valor del ejercicio está ahora en consolidar el criterio de investigación y aplicarlo posteriormente sobre sistemas con más servicios, dependencias, fuentes de logs y tipos de fallo.
