# W4D1 — journalctl basics

## 1. Comandos utilizados

### Identificar servicios disponibles

```bash
systemctl list-units --type=service
```

Permite listar las unidades de tipo `service` que están disponibles/cargadas en systemd, para identificar qué servicio se puede utilizar para consultar sus logs.

### Consultar los últimos registros de una unidad

```bash
journalctl -u <unidad> -n 100 --no-pager
```

* `journalctl` → consulta el journal.
* `-u <unidad>` → filtra los registros asociados a una unidad de systemd.
* `-n 100` → muestra las últimas 100 entradas.
* `--no-pager` → muestra la salida directamente en la terminal.

### Consultar registros de una unidad durante la última hora

```bash
journalctl -u <unidad> --since "1 hour ago" --no-pager
```

* `--since` → establece desde qué momento se muestran los registros.
* `"1 hour ago"` → indica una hora antes del momento actual.

### Consultar la última hora con timestamps ISO

```bash
journalctl -u <unidad> --since "1 hour ago" -o short-iso --no-pager
```

* `-o` → selecciona el formato de salida.
* `short-iso` → muestra timestamps en un formato ISO más explícito.

### Seguir registros en tiempo real

```bash
journalctl -u <unidad> -f
```

* `-f` → `follow`.
* Mantiene la consulta abierta y muestra nuevos registros conforme aparecen.

---

## 2. Hallazgos concretos

### Hallazgo 1 — Los logs pueden consultarse por unidad

`journalctl` permite filtrar el journal mediante:

```bash
journalctl -u <unidad>
```

Esto evita tener que revisar todos los registros del sistema cuando el incidente está relacionado con un servicio específico.

---

### Hallazgo 2 — Los registros pueden limitarse temporalmente

Con:

```bash
--since
--until
```

es posible construir una ventana temporal para investigar qué ocurrió durante un periodo concreto.

Esto permite relacionar los eventos del sistema con la línea temporal de un incidente.

---

### Hallazgo 3 — Los logs pueden observarse tanto históricamente como en tiempo real

Con:

```bash
journalctl -u <unidad>
```

se consultan registros que ya existen.

Con:

```bash
journalctl -u <unidad> -f
```

se siguen los nuevos registros conforme aparecen.

Por lo tanto, `journalctl` sirve tanto para reconstruir lo que ocurrió como para observar qué está ocurriendo durante una prueba.

---

## 3. Qué información dio cada comando

| Comando                                                               | Información obtenida                                                                                           |
| --------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------- |
| `systemctl list-units --type=service`                                 | Qué unidades de tipo servicio están disponibles/cargadas.                                                      |
| `journalctl -u <unidad> -n 100 --no-pager`                            | Los últimos 100 registros asociados con la unidad.                                                             |
| `journalctl -u <unidad> --since "1 hour ago" --no-pager`              | Los registros de la unidad ocurridos durante la última hora.                                                   |
| `journalctl -u <unidad> --since "1 hour ago" -o short-iso --no-pager` | Los mismos registros con timestamps en formato `short-iso`, facilitando la construcción de una línea temporal. |
| `journalctl -u <unidad> -f`                                           | Nuevos registros de la unidad conforme aparecen en tiempo real.                                                |

---

## 4. Conceptos principales

### `journald`

Componente de systemd encargado de recopilar y gestionar los registros del journal.

### `journal`

Conjunto de registros de eventos que pueden ser consultados posteriormente.

### `journalctl`

Herramienta utilizada para consultar esos registros.

### Unidad (`unit`)

Objeto que systemd administra. Para este tema interesan principalmente las unidades de tipo:

```text
.service
```

### `-u`

Filtra los registros por una unidad específica.

### `--since`

Define el inicio de la ventana temporal.

### `--until`

Define el final de la ventana temporal.

### `-n`

Limita la cantidad de entradas mostradas.

### `-f`

Sigue los registros en tiempo real.

### `-o short-iso`

Cambia el formato de salida para mostrar timestamps ISO explícitos.

---

## 5. Idea principal

`journalctl` permite convertir los logs del sistema en evidencia útil para troubleshooting.

El patrón aprendido es:

```text
INCIDENTE
   ↓
IDENTIFICAR SERVICIO
   ↓
journalctl -u <unidad>
   ↓
FILTRAR POR TIEMPO
   ↓
--since / --until
   ↓
LIMITAR RESULTADOS
   ↓
-n
   ↓
ANALIZAR TIMESTAMPS
   ↓
-o short-iso
   ↓
SEGUIR EVENTOS EN TIEMPO REAL
   ↓
-f
```

Esto complementa el modelo de diagnóstico aprendido anteriormente:

```text
SÍNTOMA
   ↓
HIPÓTESIS
   ↓
COMPROBACIÓN
   ↓
EVIDENCIA
   ↓
CORRELACIÓN
   ↓
CONCLUSIÓN
```

Los comandos de storage (`df`, `df -i`, `du`, `ls`, `findmnt`) permiten investigar el estado del sistema; `journalctl` permite investigar **qué eventos quedaron registrados y cuándo ocurrieron**.

> **Principio del día:** los logs son evidencia. `journalctl` permite reducir esa evidencia por servicio, tiempo y cantidad de resultados para investigar un incidente de forma más precisa.
