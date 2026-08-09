# Week 1 - Production Lab Challenge Report

## 1. Objetivo

El objetivo de este challenge fue investigar el comportamiento de varios procesos Linux utilizando evidencia del propio sistema, en lugar de asumir su comportamiento por el nombre del proceso.

Se analizaron principalmente:

- `problematic-worker`
- `normal-worker.sh`
- `io-worker.sh`
- `cat`
- `sleep`

Las herramientas utilizadas fueron:

```bash
ps
top
strace
lsof
```

El análisis se enfocó principalmente en:

- Consumo de CPU.
- Estado del proceso.
- Relación padre-hijo.
- Tiempo transcurrido desde el inicio.
- Tiempo real de CPU consumido.
- Llamadas al sistema.
- Archivos y recursos abiertos.

---

## 2. Estado inicial

Para observar los procesos se utilizó:

```bash
ps -eo pid,ppid,%cpu,%mem,stat,etime,time,comm --sort=-%cpu
```

Una de las observaciones obtenidas fue:

```text
    PID    PPID %CPU %MEM STAT     ELAPSED     TIME COMMAND
   1295    1176 99.9  0.1 R       01:20:20 01:20:20 problematic-wor
   1317    1312 99.9  0.2 R       01:20:13 01:20:11 cat
   1270    1176  0.0  0.1 S       01:20:27 00:00:04 normal-worker.s
   1312    1176  0.0  0.1 S       01:20:13 00:00:00 io-worker.sh
   6870    1270  0.0  0.2 S          00:00 00:00:00 sleep
```

La primera observación importante fue que existían dos procesos consumiendo prácticamente un núcleo completo de CPU:

```text
problematic-worker -> ~100%
cat                -> ~100%
```

Mientras tanto, los workers aparecían con muy poco consumo de CPU.

Sin embargo, no se tomó `%CPU` como evidencia suficiente. Fue necesario analizar el tiempo, los estados y la relación entre los procesos.

---

## 3. Análisis mediante `ps`

### 3.1. `PID`

El `PID` identifica de forma única al proceso.

Por ejemplo:

```text
problematic-worker -> PID 1295
io-worker.sh       -> PID 1312
cat                -> PID 1317
```

Esto permitió investigar cada proceso individualmente.

### 3.2. `PPID`

El `PPID` identifica al proceso padre.

La relación:

```text
PID  1317
PPID 1312
```

indica:

```text
io-worker.sh (1312)
       |
       +-- cat (1317)
```

Por lo tanto, `cat` es hijo de `io-worker.sh`.

Esto fue fundamental para entender por qué el `TIME` de ambos procesos era diferente.

### 3.3. `%CPU`

Los valores observados fueron aproximadamente:

```text
problematic-worker -> 99.9%
cat                -> 99.9%
io-worker.sh       -> 0.0%
normal-worker.sh   -> 0.0%
```

Los dos primeros eran los principales consumidores de CPU.

### 3.4. `STAT`

Se observaron principalmente dos estados:

```text
R
S
```

- `R` corresponde a un proceso ejecutándose o listo para ejecutarse.
- `S` corresponde a un proceso dormido o bloqueado de forma interrumpible.

Por ejemplo:

```text
problematic-worker -> R
cat                -> R
io-worker.sh       -> S
normal-worker.sh   -> S
sleep              -> S
```

Sin embargo, el estado `S` por sí solo no explica por qué está esperando.

Un proceso puede estar en `S` esperando diferentes cosas.

---

## 4. Análisis de `ELAPSED` y `TIME`

Esta fue una de las partes más importantes del challenge.

### `ELAPSED`

Representa cuánto tiempo lleva existiendo aproximadamente el proceso.

### `TIME`

Representa cuánto tiempo de CPU ha consumido el proceso.

Por lo tanto, comparar ambos valores permite obtener información sobre su comportamiento.

### 4.1. `problematic-worker`

```text
ELAPSED = 01:20:20
TIME    = 01:20:20
```

Los valores son prácticamente iguales:

```text
TIME ~= ELAPSED
```

Esto indica que el proceso ha pasado gran parte de su vida consumiendo CPU.

Además:

```text
%CPU ~= 100%
STAT = R
```

Por lo tanto, el proceso presenta un comportamiento claramente **CPU-bound**.

### 4.2. `cat`

```text
ELAPSED = 01:20:13
TIME    = 01:20:11
```

Nuevamente:

```text
TIME ~= ELAPSED
```

Y:

```text
%CPU ~= 100%
STAT = R
```

Esto indica que `cat` también está ejecutando trabajo de CPU de manera prácticamente continua.

### 4.3. `normal-worker.sh`

```text
ELAPSED = 01:20:27
TIME    = 00:00:04
```

Aquí la diferencia es enorme:

```text
ELAPSED >> TIME
```

El proceso lleva mucho tiempo existiendo, pero ha utilizado muy poco tiempo de CPU.

Esto es consistente con un proceso que pasa gran parte de su existencia esperando. En este caso, el comportamiento está relacionado con la ejecución de `sleep`.

### 4.4. `io-worker.sh`

```text
ELAPSED = 01:20:13
TIME    = 00:00:00
```

El proceso lleva más de una hora existiendo, pero prácticamente no ha utilizado CPU.

La investigación posterior con `strace` permitió explicar esta diferencia: el proceso está esperando al proceso hijo mediante `wait4()`.

---

## 5. Análisis mediante `top`

También se utilizó:

```bash
top
```

La salida mostró:

```text
PID    S   %CPU   %MEM   TIME+       COMMAND

1295   R   100.0   0.1   46:36.74    problematic-wor
1317   R   100.0   0.3   46:27.74    cat
1312   S     0.0   0.1    0:00.00    io-worker.sh
```

La información de `top` confirmó lo observado mediante `ps`.

Los procesos que requerían investigación eran principalmente:

```text
problematic-worker
cat
```

Ambos utilizaban aproximadamente el 100% de CPU.

---

## 6. Análisis de `normal-worker.sh` y `sleep`

El proceso `normal-worker.sh` aparece normalmente como:

```text
S
```

Esto sucede porque pasa gran parte de su tiempo esperando mediante `sleep`.

El proceso `sleep` también aparece como:

```text
S
```

porque permanece dormido durante el intervalo solicitado.

Que un proceso esté en `S` no significa necesariamente que sea I/O-bound.

En este caso:

```text
normal-worker.sh -> espera mediante sleep
sleep            -> espera temporal
```

No existe evidencia para clasificarlos simplemente como I/O-bound.

Además, que `sleep` tenga memoria residente no significa que esté consumiendo CPU.

Un proceso puede mostrar:

```text
%CPU = 0
RES > 0
```

Esto ocurre porque continúa existiendo y mantiene memoria residente aunque esté dormido.

---

## 7. Investigación de `io-worker.sh`

El proceso recibió inicialmente el nombre:

```text
io-worker.sh
```

con la intención de representar un worker relacionado con I/O.

Sin embargo, la investigación demostró que el nombre no describe correctamente el comportamiento observado.

La relación entre procesos era:

```text
io-worker.sh
     |
     +-- cat
```

El proceso `cat` tenía:

```text
%CPU ~= 100%
TIME ~= ELAPSED
STAT = R
```

Mientras que el padre tenía:

```text
%CPU ~= 0%
TIME ~= 0
STAT = S
```

Por lo tanto, el padre no estaba realizando el trabajo intensivo de CPU.

---

## 8. Por qué `cat` no termina

El comando ejecutado por el worker utiliza:

```bash
cat /dev/zero > /dev/null
```

`/dev/zero` proporciona datos continuamente.

Por lo tanto, `cat` puede continuar ejecutando un ciclo equivalente conceptualmente a:

```text
read(/dev/zero)
write(/dev/null)
read(/dev/zero)
write(/dev/null)
...
```

El proceso no alcanza un final de archivo normal. Por eso, el mismo proceso `cat` continúa existiendo.

El `while` del script no avanza a una nueva iteración mientras espera que ese proceso `cat` termine.

---

## 9. Investigación mediante `strace`

Se utilizó `strace` para observar las llamadas al sistema realizadas por los procesos.

El objetivo fue comprobar qué estaba haciendo realmente cada proceso, en lugar de inferirlo únicamente mediante `ps` y `top`.

---

## 10. `strace` de `io-worker.sh`

Entre las llamadas observadas se encontró:

```c
openat(AT_FDCWD, "./io-worker.sh", O_RDONLY) = 3
```

El script es abierto.

Posteriormente:

```c
read(3, "#!/bin/bash\n\nwhile true\ndo    c"..., 80) = 62
```

Bash lee el contenido del script.

Después busca el ejecutable `cat`:

```c
newfstatat(... "/usr/local/sbin/cat", ...) = -1 ENOENT
newfstatat(... "/usr/local/bin/cat", ...) = -1 ENOENT
newfstatat(... "/usr/sbin/cat", ...) = -1 ENOENT
newfstatat(... "/usr/bin/cat", ...) = 0
```

Finalmente encuentra:

```text
/usr/bin/cat
```

---

## 11. Creación del proceso hijo

Una de las llamadas más importantes fue:

```c
clone(...) = 3774
```

Esta llamada corresponde a la creación del proceso hijo que posteriormente ejecutará el comando.

Conceptualmente:

```text
io-worker.sh
      |
      | clone()
      v
 proceso hijo
      |
      v
     cat
```

Esto confirma que `cat` es un proceso independiente.

Por ello, su tiempo de CPU se contabiliza separadamente del padre.

---

## 12. `wait4()`

Después de crear el hijo apareció:

```c
wait4(-1, ..., 0, NULL)
```

Esta fue una de las evidencias más importantes.

El proceso padre está esperando al hijo.

Por eso:

```text
io-worker.sh
%CPU ~= 0
TIME ~= 0
STAT = S
```

Mientras tanto:

```text
cat
%CPU ~= 100
TIME ~= ELAPSED
STAT = R
```

La espera del padre no es una espera de I/O, sino una espera de proceso.

Por ello, no se debe clasificar `io-worker.sh` como I/O-bound simplemente porque esté esperando.

La regla obtenida durante el challenge fue:

> Estar esperando no significa automáticamente ser I/O-bound. I/O-bound significa que el progreso del trabajo está limitado principalmente por operaciones de entrada y salida.

---

## 13. `strace` de `cat`

Al investigar `cat` aparecieron numerosas llamadas repetitivas relacionadas con:

```c
read()
write()
```

Esto corresponde al flujo conceptual:

```text
/dev/zero
    |
    | read()
    v
   cat
    |
    | write()
    v
/dev/null
```

Posteriormente, el ciclo continúa:

```text
read()
write()
read()
write()
...
```

La cantidad de llamadas observadas explica por qué `strace` puede producir una salida extremadamente grande.

---

## 14. `read()` y `write()` no significan automáticamente I/O-bound

El análisis permitió corregir una interpretación inicial.

Un proceso puede ejecutar:

```c
read()
write()
```

y aun así presentar un comportamiento CPU-bound.

En este caso:

```bash
cat /dev/zero > /dev/null
```

mostró:

```text
%CPU ~= 100%
TIME ~= ELAPSED
STAT = R
```

Por lo tanto, la presencia de syscalls de I/O no es suficiente para determinar que un proceso sea I/O-bound.

Hay que determinar si la I/O provoca una espera significativa y constituye el recurso que limita el progreso.

---

## 15. Investigación adicional mediante `lsof`

También se ejecutó:

```bash
lsof -p 1312
```

La salida relevante incluyó:

```text
io-worker 1312 pepe cwd    DIR   ... /home/pepe/production-lab/week-tests/week01-challenge
io-worker 1312 pepe rtd    DIR   ... /
io-worker 1312 pepe txt    REG   ... /usr/bin/bash
```

También se observó:

```text
io-worker 1312 pepe   0u   CHR   ... /dev/pts/4
io-worker 1312 pepe   1u   CHR   ... /dev/pts/4
io-worker 1312 pepe   2u   CHR   ... /dev/pts/4
```

Además:

```text
io-worker 1312 pepe 255r REG ... /home/pepe/production-lab/week-tests/week01-challenge/io-worker.sh
```

---

## 16. Interpretación de `lsof`

Los descriptores:

```text
0 -> stdin
1 -> stdout
2 -> stderr
```

estaban asociados a:

```text
/dev/pts/4
```

que corresponde a la terminal.

El descriptor:

```text
255r
```

correspondía al propio archivo:

```text
io-worker.sh
```

El proceso Bash mantiene abierto el script mientras lo está ejecutando.

También aparecieron bibliotecas compartidas y otros archivos utilizados por Bash.

No se encontró mediante este `lsof` un recurso abierto que explicara el consumo de CPU observado.

Por lo tanto, `lsof` funcionó como una herramienta complementaria de inspección, pero no fue necesario para establecer la causa del comportamiento observado.

---

## 17. Comparación final

| Característica | `normal-worker.sh` | `problematic-worker` | `io-worker.sh` | `cat` |
|---|---|---|---|---|
| PID | 1270 | 1295 | 1312 | 1317 |
| PPID | 1176 | 1176 | 1176 | 1312 |
| CPU | ~0% | ~100% | ~0% | ~100% |
| `ELAPSED` | Alto | Alto | Alto | Alto |
| `TIME` | Bajo | Aproximadamente igual a `ELAPSED` | Aproximadamente 0 | Aproximadamente igual a `ELAPSED` |
| `STAT` observado | `S` | `R` | `S` | `R` |
| Comportamiento | Espera mediante `sleep` | Ejecución continua de CPU | Espera al hijo | Ejecución continua |
| Syscalls relevantes | Relacionadas con `sleep` y el shell | No investigadas a fondo | `clone()` y `wait4()` | `read()` y `write()` |
| Recursos abiertos | No investigados | No investigados | Script, terminal y bibliotecas | No incluidos en este análisis |
| Clasificación | Proceso predominantemente en espera | CPU-bound | Padre esperando a un hijo | CPU-bound |

---

## 18. Hipótesis inicial

La hipótesis inicial era que existía una diferencia entre los workers:

```text
problematic-worker
-> trabajo intensivo de CPU

normal-worker.sh
-> trabajo periódico con esperas

io-worker.sh
-> supuesto trabajo de I/O
```

La investigación demostró que esta clasificación inicial no era completamente correcta.

---

## 19. Evidencia

### 19.1. `problematic-worker`

```text
%CPU ~= 100%
TIME ~= ELAPSED
STAT = R
```

**Conclusión:** CPU-bound.

### 19.2. `cat`

```text
%CPU ~= 100%
TIME ~= ELAPSED
STAT = R
```

Mediante `strace`:

```text
read()
write()
read()
write()
...
```

**Conclusión:** CPU-bound.

### 19.3. `io-worker.sh`

Mediante `strace`:

```text
clone()
...
wait4()
```

**Conclusión:** el padre está bloqueado esperando al proceso hijo.

---

## 20. Causa del comportamiento observado

### 20.1. `problematic-worker`

La causa del alto consumo de CPU es que el proceso ejecuta trabajo continuamente, sin permanecer bloqueado esperando recursos externos.

La evidencia principal es:

```text
%CPU ~= 100%
TIME ~= ELAPSED
STAT = R
```

### 20.2. `cat`

El proceso ejecuta:

```text
read(/dev/zero)
write(/dev/null)
```

de forma repetitiva.

Debido a que `/dev/zero` proporciona datos continuamente, `cat` no termina y continúa ejecutando el ciclo.

Esto explica por qué `TIME` continúa aumentando.

### 20.3. `io-worker.sh`

El padre no acumula tiempo de CPU porque no está realizando el trabajo intensivo.

Después de crear el hijo mediante:

```c
clone()
```

termina esperando mediante:

```c
wait4()
```

Por lo tanto, la CPU utilizada por `cat` pertenece al proceso hijo y no se acumula dentro del `TIME` del padre.

---

## 21. Resolución

Este challenge no representó un incidente operativo que requiriera una acción de recuperación.

Por lo tanto, no se ejecutó una resolución mediante `kill`, `SIGTERM` o `SIGKILL` como parte del diagnóstico final.

La finalidad fue identificar y explicar el comportamiento de los procesos.

---

## 22. Verificación

La explicación se verificó mediante varias fuentes de evidencia independientes:

```text
ps
|
+-> Identificó PID, PPID, CPU, TIME, ELAPSED y STAT

top
|
+-> Confirmó los procesos que consumían CPU

strace
|
+-> Mostró clone() y wait4() en el padre

strace de cat
|
+-> Mostró read() y write() repetitivos

lsof
|
+-> Mostró los archivos y recursos abiertos por io-worker.sh
```

La evidencia fue consistente entre las herramientas.

---

## 23. Lessons Learned

### 23.1. `ps`

Aprendí a utilizar `PID` y `PPID` para reconstruir relaciones entre procesos.

También aprendí que:

```text
ELAPSED != TIME
```

Comparar ambos valores ayuda a determinar cuánto tiempo ha pasado el proceso ejecutándose en la CPU frente al tiempo durante el cual simplemente ha existido.

### 23.2. `top`

Aprendí a utilizarlo como una primera herramienta de observación para identificar rápidamente:

- Procesos con alto `%CPU`.
- Estado del proceso.
- Uso de memoria.
- Tiempo de CPU.
- Procesos potencialmente sospechosos.

Sin embargo, `top` no explica por sí solo la causa del comportamiento.

### 23.3. Estados de proceso

Aprendí que:

```text
R
```

indica que un proceso está ejecutándose o listo para ejecutarse, mientras que:

```text
S
```

indica un estado de espera o sueño interrumpible.

También aprendí que `S` no significa automáticamente I/O-bound.

Es necesario determinar qué está esperando el proceso.

### 23.4. `strace`

Aprendí que `strace` permite observar las llamadas al sistema realizadas por un proceso.

Esto permite pasar de:

> El proceso está consumiendo CPU.

a preguntas más profundas como:

> ¿Qué está haciendo realmente el proceso?

En este challenge fueron especialmente importantes:

```c
clone()
wait4()
read()
write()
openat()
access()
```

También aprendí que una gran cantidad de llamadas `read()` y `write()` no demuestra automáticamente que un proceso sea I/O-bound.

### 23.5. `lsof`

Aprendí que `lsof` permite investigar qué archivos y otros recursos mantiene abiertos un proceso.

En el caso analizado permitió observar:

```text
stdin
stdout
stderr
```

asociados a la terminal, además del propio script:

```text
io-worker.sh
```

Sin embargo, en este challenge no fue necesario para determinar la causa del consumo de CPU.

---

## 24. Conclusión final

El challenge mostró cómo investigar el comportamiento de los procesos desde diferentes niveles.

Primero se utilizó `top` para identificar los procesos que consumían CPU.

Después se utilizó `ps` para obtener información más precisa y relacionar los procesos mediante `PID` y `PPID`.

La comparación entre `ELAPSED` y `TIME` permitió distinguir los procesos que utilizaban CPU continuamente de aquellos que pasaban gran parte de su vida esperando.

Posteriormente, `strace` permitió observar el comportamiento interno desde el punto de vista de las llamadas al sistema.

El caso más importante fue:

```text
io-worker.sh
      |
      | clone()
      v
     cat
      |
      | read()
      | write()
      | read()
      | write()
      | ...
      v
   CPU ~= 100%

io-worker.sh
      |
      v
    wait4()
      |
      v
   espera a cat
```

Esto explicó por qué el padre podía permanecer en:

```text
%CPU = 0
TIME = 0
STAT = S
```

mientras el hijo acumulaba prácticamente todo el tiempo de CPU.

También se comprobó que el nombre `io-worker.sh` no debe tomarse como evidencia de que el proceso sea I/O-bound. El nombre se conserva para el laboratorio, pero la documentación debe dejar claro que el comportamiento real observado fue el de un proceso padre esperando a un hijo CPU-bound.

Finalmente, `lsof` permitió complementar la investigación mostrando los archivos, terminales y bibliotecas asociados al proceso, aunque no fue necesario para explicar el problema.

La lección principal del ejercicio fue:

> **En una investigación de sistemas no se debe confiar únicamente en el nombre del proceso, en `%CPU` o en una hipótesis inicial. Hay que reunir evidencia, relacionar los procesos y comprobar qué está haciendo realmente el sistema.**
