# Week 1 — Production Incident Report #2

## 1. Incident Summary

### Reporte recibido

Se detectó un comportamiento anómalo en el servicio `production-worker.service`.

El servicio permanecía en estado `active (running)`, pero continuaba creando procesos `cat /dev/zero` que permanecían ejecutándose y consumiendo CPU.

El objetivo de la investigación fue determinar:

* qué proceso estaba generando los `cat`;
* por qué aparecían cada vez más procesos;
* cuánto CPU consumían;
* cuánto tiempo permanecían vivos;
* qué relación existía entre el proceso principal y sus hijos;
* y qué comportamiento del script estaba provocando la acumulación.

---

# 2. Initial State

El servicio fue inspeccionado mediante:

```bash
sudo systemctl status production-worker
```

El servicio aparecía como:

```text
● production-worker.service - Production Worker Challenge
     Loaded: loaded (/etc/systemd/system/production-worker.service; disabled; preset: enabled)
     Active: active (running)
     Main PID: 900 (production-work)
     Tasks: 22
     Memory: 31.4M
     CPU: 13min 46.827s
```

Lo más importante fue el `CGroup`, donde aparecía:

```text
├─900 /bin/bash /home/pepe/production-lab/week-tests/week01-challenge-2/production-worker.sh
├─907 cat /dev/zero
├─917 cat /dev/zero
├─920 cat /dev/zero
├─925 cat /dev/zero
├─928 cat /dev/zero
...
└─1000 sleep 5
```

Esto permitió establecer inicialmente que:

* `900` era el proceso principal del servicio.
* Los procesos `cat` eran hijos del proceso `900`.
* Existían múltiples instancias de `cat /dev/zero`.
* También existía un proceso `sleep 5`.

---

# 3. Service Investigation

## systemctl status

Comando utilizado:

```bash
sudo systemctl status production-worker
```

### Observación

El servicio estaba funcionando correctamente desde la perspectiva de systemd:

```text
Active: active (running)
```

Sin embargo, esto no significaba que su comportamiento interno fuera correcto.

El `CGroup` permitió observar que el servicio había generado numerosos procesos hijos.

### Conclusión parcial

El problema no era que systemd hubiera perdido el servicio.

El servicio estaba activo y ejecutando correctamente el script, pero el comportamiento del propio proceso era problemático.

---

# 4. Journal Investigation

Se utilizó:

```bash
journalctl -u production-worker
```

El journal inicialmente mostraba:

```text
Aug 09 09:12:44 pepe systemd[1]: Started production-worker.service - Production Worker Challenge.
```

Posteriormente, después de detener el servicio:

```text
Aug 09 10:08:28 pepe systemd[1]: Stopping production-worker.service - Production Worker Challenge...
Aug 09 10:08:28 pepe systemd[1]: production-worker.service: Deactivated successfully.
Aug 09 10:08:28 pepe systemd[1]: Stopped production-worker.service - Production Worker Challenge.
Aug 09 10:08:28 pepe systemd[1]: production-worker.service: Consumed 11h 2min 52.802s CPU time over 55min 44.457s wall clock time, 832M memory peak.
```

### Interpretación

`journalctl` confirmó:

* cuándo inició el servicio;
* cuándo fue detenido;
* que systemd consideró que terminó correctamente;
* y el consumo acumulado registrado por systemd.

El journal no mostraba cada creación de `cat`, porque esos procesos no estaban escribiendo mensajes al journal.

---

# 5. Process Investigation

Se utilizó:

```bash
ps -eo pid,ppid,%cpu,%mem,stat,etime,time,comm --sort=-%cpu
```

Durante la ejecución se observó:

```text
PID    PPID %CPU %MEM STAT     ELAPSED     TIME COMMAND
907     900 11.7  0.2 R          40:30  00:04:45 cat
917     900 11.5  0.2 R          40:25  00:04:39 cat
920     900 11.3  0.2 R          40:20  00:04:35 cat
925     900 11.2  0.2 R          40:15  00:04:30 cat
928     900 11.0  0.2 R          40:10  00:04:26 cat
...
1016    900  8.3  0.2 R          38:49  00:03:15 cat
1019    900  8.2  0.2 R          38:44  00:03:11 cat
1022    900  8.2  0.2 R          38:39  00:03:10 cat
```

## Análisis

La columna `PPID` fue especialmente importante.

Todos los `cat` tenían:

```text
PPID = 900
```

Esto significa que `900` era su proceso padre.

Por lo tanto, la estructura era:

```text
production-worker (PID 900)
│
├── cat (PID 907)
├── cat (PID 917)
├── cat (PID 920)
├── cat (PID 925)
├── ...
└── sleep
```

Cada `cat` era un proceso independiente.

No se trataba de un mismo proceso `cat` cuyo CPU se estuviera acumulando.

---

# 6. Understanding ETIME vs TIME

En los procesos `cat` se observó, por ejemplo:

```text
ETIME = 40:30
TIME  = 04:45
```

`ETIME` representa cuánto tiempo ha transcurrido desde que nació el proceso.

`TIME` representa cuánto tiempo de CPU ha utilizado el proceso.

Por ejemplo:

```text
ETIME = 40 minutos
TIME  = 4 minutos
```

significa que el proceso lleva aproximadamente 40 minutos existiendo, pero solamente ha consumido alrededor de 4 minutos de CPU.

La diferencia entre ambos permitió distinguir:

```text
tiempo de vida
```

de:

```text
tiempo realmente ejecutándose en CPU
```

---

# 7. Why Were More Children Being Created?

Se inspeccionó el script:

```bash
sudo cat /home/pepe/production-lab/week-tests/week01-challenge-2/production-worker.sh
```

El contenido relevante era:

```bash
#!/bin/bash

LOG="/tmp/production-worker.log"

echo "$(date) - production-worker started" >> "$LOG"

while true
do
    echo "$(date) - worker processing..." >> "$LOG"

    cat /dev/zero > /dev/null &

    sleep 5
done
```

La línea crítica era:

```bash
cat /dev/zero > /dev/null &
```

El operador `&` ejecuta el comando en segundo plano.

Por lo tanto, el shell crea el `cat` y continúa con la siguiente instrucción:

```bash
sleep 5
```

Después de cinco segundos vuelve al inicio del `while` y crea otro `cat`.

El comportamiento puede representarse así:

```text
production-worker
       │
       ├── cat #1 ────────────────────────────────>
       │
       └── sleep 5
              │
              └── termina
       
       production-worker
       │
       ├── cat #1 ────────────────────────────────>
       ├── cat #2 ────────────────────────────────>
       │
       └── sleep 5

       production-worker
       │
       ├── cat #1 ────────────────────────────────>
       ├── cat #2 ────────────────────────────────>
       ├── cat #3 ────────────────────────────────>
       │
       └── sleep 5
```

Los `cat` permanecen vivos porque `/dev/zero` proporciona datos continuamente.

Por lo tanto, cada nueva iteración crea otro proceso que no termina por sí mismo.

---

# 8. lsof Investigation

Se utilizó:

```bash
lsof -p 900
```

La salida mostró, entre otras cosas:

```text
productio 900 pepe cwd    DIR   8,48     4096 ... /home/pepe/production-lab/week-tests/week01-challenge-2
productio 900 pepe rtd    DIR   8,48     4096 ... /
productio 900 pepe txt    REG   8,48  1540520 ... /usr/bin/bash
```

Y:

```text
productio 900 pepe   0r   CHR   1,3   ... /dev/null
productio 900 pepe   1u   unix  ...    type=STREAM (CONNECTED)
productio 900 pepe   2u   unix  ...    type=STREAM (CONNECTED)
```

También aparecía:

```text
productio 900 pepe 255r REG ... /home/pepe/production-lab/week-tests/week01-challenge-2/production-worker.sh
```

## Interpretación

`lsof` permitió confirmar los recursos abiertos por el proceso principal.

Entre ellos estaban:

* su directorio de trabajo;
* el ejecutable `/usr/bin/bash`;
* `/dev/null`;
* sus descriptores de entrada/salida;
* y el propio script.

Sin embargo, `lsof` sobre el PID principal no fue la herramienta que permitió descubrir la causa raíz.

La información decisiva provino de `systemctl`, `ps` y posteriormente `strace`.

---

# 9. strace Investigation

Se adjuntó `strace` al proceso principal:

```bash
sudo strace -p 3521
```

Una de las llamadas fundamentales observadas fue:

```text
clone(...) = 3684
```

Posteriormente apareció:

```text
SIGCHLD {si_signo=SIGCHLD, si_code=CLD_EXITED, si_pid=3684, ...}
```

y:

```text
wait4(...) = 3684
```

Más adelante volvieron a aparecer:

```text
clone(...) = 3685
clone(...) = 3686
clone(...) = 3687
clone(...) = 3688
clone(...) = 3689
```

## Interpretación

`clone()` permitió observar la creación de nuevos procesos.

El patrón:

```text
clone()
   ↓
nuevo proceso
   ↓
SIGCHLD
   ↓
wait4()
```

permitió observar la creación y posterior recolección de procesos hijos.

Los diferentes valores devueltos por `clone()`:

```text
3684
3685
3686
3687
3688
3689
```

demuestran que se estaban creando procesos diferentes.

No se trataba de reutilizar el mismo PID.

---

# 10. Understanding SIGCHLD and wait4

Cuando un hijo termina, el padre recibe `SIGCHLD`.

Por ejemplo:

```text
SIGCHLD ... si_pid=3684
```

indica que el proceso hijo `3684` terminó.

Posteriormente:

```text
wait4(...) = 3684
```

indica que el proceso padre recogió ese hijo.

Esto permitió observar directamente la relación entre padre e hijos.

Sin embargo, no todos los hijos tenían el mismo comportamiento.

El `sleep 5` termina después de cinco segundos.

El `cat /dev/zero` no termina naturalmente porque continúa recibiendo datos de `/dev/zero`.

---

# 11. strace del cat

Al investigar directamente uno de los procesos `cat`, se observaron repetidamente llamadas de lectura y escritura:

```text
read(...)
write(...)
read(...)
write(...)
read(...)
write(...)
...
```

El comportamiento corresponde conceptualmente a:

```text
/dev/zero
    │
    │ read
    ▼
   cat
    │
    │ write
    ▼
/dev/null
```

Como `/dev/zero` proporciona datos continuamente y `/dev/null` acepta los datos, el `cat` puede continuar indefinidamente.

Esto explica por qué los procesos `cat` permanecían vivos.

---

# 12. CPU Investigation

Los `cat` aparecían en estado:

```text
R
```

y mostraban consumo significativo de CPU.

Por ejemplo:

```text
907  ... 11.7% ... R ... cat
917  ... 11.5% ... R ... cat
920  ... 11.3% ... R ... cat
```

Cada proceso tenía su propio consumo de CPU.

La acumulación de muchos `cat` provocaba que el consumo total de CPU del servicio aumentara considerablemente.

---

# 13. Why the Service Was the Root Cause

El servicio no tenía un problema porque systemd estuviera creando procesos incorrectamente.

Systemd solamente mantenía ejecutándose:

```text
production-worker.sh
```

El propio script contenía un:

```bash
while true
```

y dentro del ciclo:

```bash
cat /dev/zero > /dev/null &
sleep 5
```

Por lo tanto:

1. el servicio inicia el script;
2. el `while true` nunca termina;
3. cada cinco segundos se crea un nuevo `cat`;
4. el `cat` se ejecuta en segundo plano;
5. `/dev/zero` proporciona datos continuamente;
6. el `cat` no termina;
7. el siguiente ciclo crea otro `cat`;
8. los procesos se acumulan.

---

# 14. Hypothesis

### Hipótesis inicial

El servicio parecía estar generando procesos hijos continuamente y consumiendo cada vez más CPU.

La hipótesis fue que el problema estaba relacionado con la forma en que el script ejecutaba los procesos `cat`.

---

# 15. Evidence

La hipótesis fue respaldada por varias fuentes.

### systemctl

Mostró múltiples:

```text
cat /dev/zero
```

dentro del `CGroup` del servicio.

### ps

Mostró que los `cat` tenían el mismo:

```text
PPID = 900
```

por lo que eran hijos del proceso principal.

También mostró que cada `cat` tenía su propio:

```text
PID
%CPU
ETIME
TIME
STAT
```

### strace

Mostró múltiples:

```text
clone(...)
```

confirmando la creación de nuevos procesos.

### Código fuente

Mostró:

```bash
cat /dev/zero > /dev/null &
```

dentro de:

```bash
while true
```

y seguido de:

```bash
sleep 5
```

### strace del cat

Mostró operaciones repetitivas de:

```text
read()
write()
```

confirmando que el `cat` continuaba trabajando.

---

# 16. Root Cause

La causa raíz fue un proceso en segundo plano que nunca terminaba dentro de un ciclo infinito.

La combinación problemática fue:

```bash
while true
do
    cat /dev/zero > /dev/null &
    sleep 5
done
```

El `&` hace que el `cat` se ejecute en segundo plano.

El `sleep 5` solamente retrasa la siguiente iteración del ciclo.

No espera a que el `cat` termine.

Como el `cat` procesa:

```text
/dev/zero → /dev/null
```

continuamente, permanece ejecutándose.

Por ello, cada cinco segundos aparece un nuevo `cat` sin que desaparezcan los anteriores.

El resultado es una acumulación progresiva de procesos y un aumento del consumo total de CPU.

---

# 17. Resolution

Se detuvo el servicio mediante systemd:

```bash
sudo systemctl stop production-worker
```

Esto provocó que el servicio pasara a:

```text
Active: inactive (dead)
```

Systemd registró:

```text
production-worker.service: Deactivated successfully.
production-worker.service: Stopped production-worker.service.
```

---

# 18. Verification

Después de detener el servicio se ejecutó:

```bash
ps -eo pid,ppid,%cpu,%mem,stat,etime,time,comm --sort=-%cpu
```

Los procesos `cat` pertenecientes al servicio ya no aparecían.

También se verificó:

```bash
systemctl status production-worker
```

y se obtuvo:

```text
Active: inactive (dead)
```

Finalmente:

```bash
journalctl -u production-worker
```

confirmó el evento de detención.

---

# 19. Lessons Learned

## systemctl

`systemctl status` permite conocer el estado de un servicio y observar información importante como:

* PID principal;
* cantidad de tareas;
* memoria;
* CPU;
* CGroup;
* procesos asociados al servicio.

También permite detener el servicio correctamente mediante:

```bash
sudo systemctl stop <service>
```

---

## journalctl

`journalctl -u <service>` permite consultar los eventos registrados por systemd para un servicio.

No necesariamente muestra todo lo que ocurre dentro de los procesos.

En este caso permitió observar:

```text
Started
Stopping
Deactivated successfully
Stopped
Consumed CPU time
```

pero no mostró cada creación de `cat`.

---

## ps

`ps` permitió analizar procesos individualmente.

Las columnas más útiles fueron:

```text
PID
PPID
%CPU
%MEM
STAT
ETIME
TIME
COMMAND
```

Especialmente importante fue `PPID`, porque permitió establecer la relación:

```text
900 production-worker
       │
       ├── 907 cat
       ├── 917 cat
       ├── 920 cat
       └── ...
```

---

## top

`top` permite observar el comportamiento de los procesos en tiempo real.

Fue útil para identificar que los `cat` estaban consumiendo CPU y que el número de procesos aumentaba.

---

## lsof

`lsof` permite observar los recursos abiertos por un proceso.

En este caso permitió inspeccionar:

* archivos;
* directorios;
* ejecutables;
* bibliotecas;
* descriptores de entrada/salida;
* `/dev/null`;
* y el propio script.

Sin embargo, no fue la herramienta principal para encontrar la causa raíz.

---

## strace

`strace` permitió observar directamente las llamadas al sistema realizadas por el proceso.

Las llamadas más importantes para este incidente fueron:

```text
clone()
SIGCHLD
wait4()
read()
write()
```

`clone()` permitió identificar la creación de nuevos procesos.

`SIGCHLD` indicó que un proceso hijo había terminado.

`wait4()` permitió observar cómo el padre recogía al hijo.

`read()` y `write()` mostraron el trabajo continuo de los `cat`.

---

# 20. Final Conclusion

El servicio `production-worker.service` estaba activo y funcionando desde el punto de vista de systemd, pero su comportamiento interno era defectuoso.

El proceso principal ejecutaba un ciclo infinito:

```bash
while true
```

Dentro del ciclo creaba:

```bash
cat /dev/zero > /dev/null &
```

El operador `&` hacía que cada `cat` se ejecutara en segundo plano.

Después el script ejecutaba:

```bash
sleep 5
```

y cinco segundos después volvía a crear otro `cat`.

El problema era que los `cat` no terminaban. Cada uno continuaba procesando datos de `/dev/zero` y escribiéndolos en `/dev/null`.

Mediante `ps` se comprobó que los `cat` eran procesos independientes y que todos tenían como padre al proceso principal del servicio.

Mediante `strace` se confirmó la creación de nuevos procesos mediante `clone()` y se observaron posteriormente señales `SIGCHLD` y llamadas `wait4()` para los procesos que sí terminaban, como `sleep`.

El `strace` de los `cat` mostró un flujo continuo de `read()` y `write()`, consistente con el procesamiento continuo de `/dev/zero`.

Finalmente, detener el servicio con:

```bash
sudo systemctl stop production-worker
```

eliminó el árbol de procesos asociado y confirmó que el consumo provenía del comportamiento generado por el servicio.

La causa raíz fue, por tanto, la creación repetitiva de procesos `cat` de larga duración dentro de un `while true`, sin esperar a que dichos procesos terminaran.
