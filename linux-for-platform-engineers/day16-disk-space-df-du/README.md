# Día 4 — `df` vs `du`: espacio en disco

## Objetivo

Entender la diferencia entre:

```bash
df
```

y:

```bash
du
```

y aprender a usarlos para identificar cuánto espacio está ocupado y qué archivos/directorios son responsables de ese consumo.

---

## 1. `df` — espacio del filesystem

`df` significa **disk free**.

Muestra información sobre el espacio utilizado y disponible en los **filesystems/mounts**.

Comando utilizado:

```bash
df -h
```

`-h` = **human-readable**, muestra los tamaños en unidades fáciles de leer (`K`, `M`, `G`, etc.).

Columnas importantes:

```text
Filesystem
Size
Used
Avail
Use%
Mounted on
```

### Significado

* **Filesystem** → filesystem que estamos consultando.
* **Size** → tamaño total.
* **Used** → espacio utilizado.
* **Avail** → espacio disponible.
* **Use%** → porcentaje utilizado.
* **Mounted on** → punto del árbol donde está montado el filesystem.

### Idea fundamental

```text
df
↓
filesystem / mount
↓
"¿Qué tan lleno está este filesystem?"
```

---

## 2. `du` — uso de espacio por archivos/directorios

`du` significa **disk usage**.

A diferencia de `df`, `du` permite analizar cuánto espacio están utilizando archivos y directorios.

Ejemplo utilizado:

```bash
du -sh ~/labs/linux-month1/03-storage/*
```

Opciones:

```text
-s → summarize
-h → human-readable
```

`-s` proporciona un resumen por elemento en lugar de listar todo su contenido.

### Idea fundamental

```text
du
↓
archivos / directorios
↓
"¿Qué está consumiendo espacio?"
```

---

## 3. Diferencia principal

La diferencia que debemos recordar:

```text
df → filesystem
du → archivos/directorios
```

Otra forma de verlo:

```text
df
"¿Qué tan lleno está el filesystem?"

du
"¿Quién se está comiendo el espacio?"
```

Son herramientas **complementarias**, no sustitutas.

---

# 4. Laboratorio

Trabajamos en:

```bash
~/labs/linux-month1/03-storage/
```

Inicialmente ejecutamos:

```bash
du -sh .
```

Resultado:

```text
12K     .
```

El directorio completo ocupaba aproximadamente:

```text
12K
```

---

## 5. Crear un archivo de prueba

Creamos deliberadamente un archivo grande:

```bash
fallocate -l 200M archivo_prueba.bin
```

Después:

```bash
du -sh .
```

Resultado:

```text
201M    .
```

El directorio pasó de aproximadamente:

```text
12K
```

a:

```text
201M
```

Esto permitió observar directamente cómo un archivo grande aumenta el uso de espacio.

---

## 6. `ls` vs `du`

Comprobamos el archivo:

```bash
ls -lh archivo_prueba.bin
```

Resultado:

```text
-rw-r--r-- 1 pepe pepe 200M Sep  1 08:13 archivo_prueba.bin
```

`ls -lh` mostró:

```text
200M
```

Después:

```bash
du -h archivo_prueba.bin
```

mostró:

```text
201M    archivo_prueba.bin
```

La pequeña diferencia se debe a cómo se contabilizan/redondean las unidades y bloques de almacenamiento.

Modelo mental:

```text
ls
→ tamaño del archivo

du
→ espacio utilizado por el archivo
```

---

# 7. Encontrar al culpable con `du`

Ejecutamos:

```bash
du -sh ~/labs/linux-month1/03-storage/*
```

Resultado:

```text
201M    /home/pepe/labs/linux-month1/03-storage/archivo_prueba.bin
8.0K    /home/pepe/labs/linux-month1/03-storage/permtest
```

Por tanto:

```text
03-storage/
├── archivo_prueba.bin → 201M
└── permtest            → 8.0K
```

El responsable del consumo era claramente:

```text
archivo_prueba.bin
```

---

# 8. `--max-depth`

Utilizamos:

```bash
du -h --max-depth=2 .
```

Resultado:

```text
8.0K    ./permtest
201M    .
```

`--max-depth=2` permite limitar hasta qué profundidad de la jerarquía queremos que `du` muestre resultados.

Sirve para ir acotando la búsqueda:

```text
filesystem
    ↓
directorio
    ↓
subdirectorio
    ↓
archivo
```

Esto es especialmente útil cuando un sistema tiene muchísimos archivos y necesitamos localizar rápidamente qué parte está consumiendo espacio.

---

# 9. Comparación real `df` vs `du`

Ejecutamos:

```bash
df -h .
```

Resultado:

```text
Filesystem      Size  Used Avail Use% Mounted on
/dev/sdd       1007G  2.9G  953G   1% /
```

Esto significa que el filesystem donde está nuestro laboratorio es:

```text
/dev/sdd
```

montado en:

```text
/
```

y tiene aproximadamente:

```text
1007G → tamaño total
2.9G  → utilizados
953G  → disponibles
1%    → utilización
```

En cambio:

```bash
du -sh .
```

mostró:

```text
201M    .
```

No existe contradicción:

```text
df
→ 2.9G utilizados en TODO el filesystem /

du
→ 201M utilizados por nuestro directorio 03-storage
```

Nuestro laboratorio representa solamente una parte del espacio utilizado por `/`.

---

# 10. ¿Por qué `df` y `du` pueden no coincidir?

Esta es una parte importante para diagnóstico de sistemas.

`df` trabaja desde la perspectiva del **filesystem**:

```text
df
↓
espacio ocupado en el filesystem
```

`du` trabaja recorriendo archivos/directorios:

```text
du
↓
archivos/directorios encontrados
```

Por eso pueden existir diferencias.

### Mounts/filesystems diferentes

Un directorio puede contener otro filesystem montado.

Visualmente:

```text
/home
└── pepe
    └── datos  ← otro filesystem
```

Aunque todo parezca parte del mismo árbol de directorios:

```text
/home
```

y:

```text
/home/pepe/datos
```

pueden pertenecer a filesystems diferentes.

Esto puede hacer que una búsqueda con `du` no se interprete correctamente si no tenemos en cuenta los mounts.

---

## 11. Otra causa importante: archivos eliminados pero abiertos

Un caso clásico:

```text
proceso
   ↓
tiene abierto un archivo grande
   ↓
archivo es eliminado con rm
   ↓
el nombre desaparece
   ↓
el proceso todavía mantiene abierto el archivo
```

Entonces puede ocurrir:

```text
df → sigue contando el espacio ocupado
du → ya no encuentra el archivo por su nombre
```

Resultado:

```text
df → disco aparentemente lleno

du → no encuentra dónde están esos GB
```

Esto explica situaciones reales como:

> "Borré el log, pero el disco sigue lleno."

En estos casos, herramientas como `lsof` permiten investigar archivos eliminados que todavía están abiertos por procesos.

---

# 12. Experimento controlado

Creamos:

```bash
fallocate -l 200M archivo_prueba.bin
```

Observamos:

```bash
du -sh .
```

```text
201M
```

Y:

```bash
ls -lh archivo_prueba.bin
```

```text
200M
```

Después de terminar el experimento, eliminamos el archivo:

```bash
rm archivo_prueba.bin
```

Y comprobamos nuevamente:

```bash
du -sh .
```

Deberíamos regresar aproximadamente al estado inicial:

```text
12K
```

---

# 13. Comandos principales del día

```bash
# Ver espacio de los filesystems
df -h

# Ver el filesystem donde está el directorio actual
df -h .

# Ver uso total del directorio actual
du -sh .

# Ver uso de cada elemento dentro de un directorio
du -sh ~/labs/linux-month1/03-storage/*

# Buscar consumo bajando en la jerarquía
du -h --max-depth=2 .

# Ver tamaño de un archivo
ls -lh archivo_prueba.bin

# Ver espacio utilizado por un archivo
du -h archivo_prueba.bin

# Crear archivo de prueba de 200 MB
fallocate -l 200M archivo_prueba.bin

# Eliminar archivo de prueba
rm archivo_prueba.bin
```

---

# 14. Modelo mental final

Ante un problema de espacio:

```text
"El disco está lleno"
        ↓
       df
        ↓
¿Qué filesystem está lleno?
        ↓
       du
        ↓
¿Qué directorio/archivo está consumiendo el espacio?
```

Por tanto:

```text
df → detectar el filesystem problemático

du → localizar el consumo dentro de los directorios
```

Y si:

```text
df >> du
```

hay que investigar causas como:

* otros mounts/filesystems;
* archivos eliminados pero todavía abiertos;
* diferencias en lo que cada herramienta está contabilizando.

---

## Conclusión

El laboratorio demostró con datos reales que `df` y `du` responden preguntas diferentes.

Nuestro sistema mostró:

```text
df -h .
/dev/sdd
1007G total
2.9G usados
953G disponibles
1%
```

Mientras nuestro laboratorio mostró:

```text
du -sh .
201M
```

Los aproximadamente **2.9 GB de `df` corresponden al filesystem completo `/`**, mientras que los **201 MB de `du` corresponden únicamente a `03-storage`**.

El archivo de prueba:

```text
archivo_prueba.bin
```

fue el principal consumidor dentro del laboratorio y permitió comprobar experimentalmente cómo `du` localiza el uso de espacio.

### Concepto que debe quedar grabado

```text
df = filesystem / mount
du = archivos / directorios

df → "¿qué tan lleno está?"
du → "¿quién lo está llenando?"
```
