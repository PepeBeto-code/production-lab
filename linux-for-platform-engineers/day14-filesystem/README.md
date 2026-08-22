# Día 14 — Filesystem y diagnóstico de almacenamiento

## Objetivo

Aprender a diagnosticar problemas de almacenamiento en Linux respondiendo, en orden:

1. ¿Qué filesystem tiene el problema?
2. ¿Es falta de espacio o de inodos?
3. ¿Qué directorio está consumiendo el espacio?
4. ¿Qué subdirectorio o archivo es el responsable?
5. ¿Por qué está creciendo?

La idea principal es **reducir progresivamente el área de búsqueda**, no memorizar comandos.

---

## Directorios importantes

- `/` → raíz de la jerarquía Linux.
- `/home` → archivos y espacio personal de los usuarios.
- `/etc` → configuración del sistema y servicios.
- `/tmp` → archivos temporales.
- `/var` → datos que cambian durante la operación del sistema.
  - `/var/log` → logs.
  - `/var/lib` → datos persistentes de servicios y aplicaciones.
  - `/var/cache` → cachés.
  - `/var/spool` → trabajos o datos en espera de ser procesados.

---

## Espacio vs inodos

Un filesystem tiene dos recursos importantes:

- **Espacio** → dónde se almacenan los datos.
- **Inodos** → estructuras necesarias para representar archivos.

Por eso pueden existir dos problemas diferentes:

### Mucho espacio usado, pocos inodos usados

Probablemente hay archivos o datos grandes:

- logs;
- backups;
- imágenes;
- archivos grandes.

### Poco espacio usado, muchos inodos usados

Probablemente existen muchísimos archivos pequeños.

Incluso puede haber espacio disponible y aun así no poder crear archivos si se agotan los inodos.

---

## `df -h`

```bash
df -h
```

Sirve para responder:

> ¿Cómo está el filesystem?

Columnas importantes:

- `Filesystem`
- `Size`
- `Used`
- `Avail`
- `Use%`
- `Mounted on`

`-h` significa **human-readable**.

---

## `df -ih`

```bash
df -ih
```

Sirve para revisar el uso de inodos.

- `-i` → inodos.
- `-h` → formato legible.

La diferencia principal:

```text
df -h   → ¿qué tan lleno está el almacenamiento?
df -ih  → ¿qué tan llenos están los inodos?
```

---

## `du`

`du` significa **disk usage**.

Mientras `df` nos dice cómo está un filesystem, `du` nos ayuda a descubrir:

> ¿Qué archivos o directorios están consumiendo el espacio?

---

## Comando principal del laboratorio

```bash
du -sh /var/* 2>/dev/null
```

Significado:

- `du` → uso de disco.
- `-s` → resumen.
- `-h` → formato legible.
- `/var/*` → cada elemento directamente dentro de `/var`.
- `2>/dev/null` → descarta los mensajes de error (`stderr`).

Importante: `2>/dev/null` no elimina el error; simplemente evita mostrarlo.

---

# Resultados de mi práctica

## Filesystem raíz

```text
/dev/sdd
1007G total
2.6G utilizados
954G disponibles
1% utilizado
Montado en /
```

Conclusión:

```text
El filesystem raíz no tiene un problema de espacio.
```

## Inodos

El filesystem `/` tenía aproximadamente:

```text
64M inodos totales
55K utilizados
1% utilizado
```

Conclusión:

```text
No existe un problema de agotamiento de inodos.
```

---

# Top 3 dentro de `/var`

Resultado de:

```bash
du -sh /var/* 2>/dev/null
```

Top 3:

```text
1. /var/log    404M
2. /var/lib    176M
3. /var/cache  122M
```

`/var/log` fue el directorio más grande dentro de `/var`.

Sin embargo:

```text
/var/log = 404M
Filesystem / = ~1007G
Uso total = 1%
```

Por lo tanto, **no representa actualmente un problema de almacenamiento**.

---

# Flujo de diagnóstico aprendido

```text
Problema o sospecha de almacenamiento
↓
df -h
↓
¿Qué filesystem está lleno?
↓
df -ih
↓
¿Es problema de espacio o de inodos?
↓
du
↓
¿Qué directorio consume más?
↓
du otra vez dentro de ese directorio
↓
¿Qué subdirectorio o archivo es responsable?
↓
Investigar por qué está creciendo
```

Ejemplo:

```text
Filesystem al 95%
↓
/var consume mucho
↓
/var/log consume mucho
↓
journal consume mucho
↓
Investigar qué está provocando su crecimiento
```

---

# Idea principal para recordar

No se trata de ejecutar comandos y copiar resultados.

El proceso correcto es:

> **Primero identificar el filesystem afectado, después determinar si el problema es espacio o inodos, y finalmente bajar por la jerarquía usando `du` hasta encontrar qué está consumiendo el almacenamiento y por qué.**

