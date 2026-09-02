# Día 5 — Inodos (`df -i`) + cómo provocar el caso típico

## Objetivo

Entender que un filesystem tiene **dos recursos distintos relacionados con el almacenamiento**:

```text
1. Espacio en bytes
2. Inodos
```

Y que cualquiera de los dos puede agotarse de manera independiente.

El concepto fundamental del día:

```text
espacio en bytes ≠ inodos
```

---

# 1. Recordatorio del Día 4

El día anterior aprendimos:

```text
df → filesystem / mount
du → archivos y directorios
```

`df -h` permite saber cuánto espacio en bytes está utilizado y disponible en un filesystem:

```bash
df -h
```

Mientras que `du` permite investigar cuánto espacio están consumiendo archivos y directorios:

```bash
du -sh .
```

Hoy agregamos una segunda dimensión al análisis:

```text
df -h
→ espacio en bytes

df -ih
→ inodos
```

---

# 2. ¿Qué es un inode?

Un **inode** (index node) es una estructura de datos utilizada por el filesystem para representar un archivo y almacenar información sobre él.

Entre la información asociada al archivo se encuentran, de forma simplificada:

* permisos;
* propietario;
* grupo;
* timestamps;
* tamaño;
* referencias necesarias para localizar sus datos.

Un modelo simplificado:

```text
archivo.txt
     │
     ▼
   inode
     │
     ├── permisos
     ├── propietario
     ├── grupo
     ├── timestamps
     ├── tamaño
     └── referencias a datos
```

El inode **no es el contenido del archivo**.

Es la estructura que contiene metadata e información necesaria para gestionar ese archivo.

---

# 3. Nombre del archivo e inode

El nombre que vemos:

```text
archivo.txt
```

no es exactamente el inode.

Conceptualmente, el directorio mantiene una relación entre el nombre y el inode:

```text
archivo.txt → inode 12345
foto.jpg    → inode 12346
log.txt     → inode 12347
```

Por eso debemos distinguir:

```text
nombre del archivo
        ↓
inode
        ↓
datos del archivo
```

---

# 4. ¿Por qué los inodos son un recurso?

Cada archivo necesita recursos del filesystem para ser representado.

Por eso crear:

```text
archivo 1
archivo 2
archivo 3
...
```

consume inodos.

Crear:

```text
10,000 archivos
```

significa utilizar aproximadamente:

```text
10,000 inodos
```

La cantidad exacta observada puede verse afectada por las estructuras adicionales del filesystem, por lo que lo importante es observar el incremento y no asumir que será exactamente una relación de 1:1 en la salida global.

---

# 5. Dos recursos independientes

Un filesystem tiene, entre otras cosas, un límite de:

```text
espacio disponible en bytes
```

y una cantidad finita de:

```text
inodos disponibles
```

Son recursos diferentes.

Podemos representarlo así:

```text
FILESYSTEM
│
├── espacio en bytes
│
└── inodos
```

Por lo tanto, es posible tener:

```text
muchos GB libres
+
muy pocos inodos libres
```

o incluso:

```text
muchos GB libres
+
0 inodos libres
```

En ese último caso todavía existe espacio en bytes, pero no hay inodos disponibles para crear nuevos archivos.

---

# 6. `df -i`

El comando:

```bash
df -i
```

muestra información sobre la utilización de inodos de los filesystems.

Mientras:

```bash
df -h
```

se utiliza para observar el espacio en bytes de manera legible,

```bash
df -i
```

se enfoca en los inodos.

Para verlo de manera human-readable:

```bash
df -ih
```

---

# 7. Columnas de `df -ih`

Una salida puede tener una estructura como:

```text
Filesystem     Inodes  IUsed   IFree IUse% Mounted on
/dev/sdd       67.2M   1.2M    66M     2% /
```

### `Filesystem`

Filesystem que estamos consultando.

Ejemplo:

```text
/dev/sdd
```

### `Inodes`

Cantidad total de inodos disponibles en ese filesystem.

### `IUsed`

Cantidad de inodos utilizados.

### `IFree`

Cantidad de inodos libres.

### `IUse%`

Porcentaje de utilización de los inodos.

### `Mounted on`

Punto de montaje del filesystem.

Por ejemplo:

```text
/
```

---

# 8. `df -h` vs `df -ih`

Ahora tenemos dos comandos que debemos distinguir claramente:

```bash
df -h
```

Pregunta:

> ¿Cuánto espacio en bytes está utilizado/disponible?

Mientras:

```bash
df -ih
```

pregunta:

> ¿Cuántos inodos están utilizados/disponibles?

Modelo mental:

```text
df -h
→ BYTES

df -ih
→ INODOS
```

---

# 9. Archivo grande vs muchos archivos pequeños

Este es el experimento conceptual principal del día.

## Caso 1 — archivo grande

Ayer utilizamos:

```bash
fallocate -l 200M archivo_prueba.bin
```

Esto creó:

```text
1 archivo
200 MB
```

El efecto principal fue:

```text
espacio en bytes ↑↑
```

Pero solamente se creó un archivo, por lo que el incremento en inodos fue pequeño:

```text
inodos ↑ muy poco
```

Conceptualmente:

```text
200 MB
↓
1 archivo
↓
mucho espacio en bytes
↓
muy pocos inodos
```

---

## Caso 2 — miles de archivos pequeños

Hoy creamos muchos archivos vacíos:

```bash
mkdir inode-test
```

y:

```bash
touch inode-test/file_{00001..10000}
```

Esto crea aproximadamente:

```text
10,000 archivos
```

Cada uno tiene:

```text
0 bytes de contenido
```

pero cada archivo necesita ser representado por el filesystem.

Por tanto:

```text
10,000 archivos
↓
muchos inodos utilizados
```

El objetivo es observar que:

```text
inodos ↑↑
```

mientras que el contenido de los archivos prácticamente no aporta datos.

---

# 10. ¿Por qué un archivo de 0 bytes utiliza recursos?

`touch` puede crear un archivo vacío:

```bash
touch archivo
```

Podemos observar que su tamaño de contenido es:

```text
0 bytes
```

Pero:

```text
0 bytes de contenido
```

no significa:

```text
0 recursos del filesystem
```

El filesystem todavía necesita mantener información para representar ese archivo.

Conceptualmente:

```text
archivo vacío
    │
    ├── inode
    │    ├── permisos
    │    ├── propietario
    │    ├── timestamps
    │    └── metadata
    │
    └── entrada dentro del directorio
```

Por eso miles de archivos vacíos siguen consumiendo recursos del filesystem.

---

# 11. Laboratorio

Directorio utilizado:

```bash
cd ~/labs/linux-month1/03-storage/
```

## Estado inicial

Primero observamos el espacio en bytes:

```bash
df -h .
```

Después los inodos:

```bash
df -ih .
```

Estos valores representan nuestro:

```text
ANTES
```

---

## Crear directorio de prueba

```bash
mkdir inode-test
```

---

## Crear 10,000 archivos vacíos

```bash
touch inode-test/file_{00001..10000}
```

El patrón:

```text
file_{00001..10000}
```

genera nombres:

```text
file_00001
file_00002
file_00003
...
file_10000
```

y `touch` crea esos archivos.

---

## Comprobar cantidad de archivos

```bash
find inode-test -type f | wc -l
```

Descomposición:

```text
find
→ busca dentro de la jerarquía

-type f
→ solamente archivos normales

|
→ envía la salida al siguiente comando

wc -l
→ cuenta líneas
```

Resultado esperado:

```text
10000
```

---

# 12. Medir los inodos después

Ejecutamos:

```bash
df -ih .
```

Ahora comparamos:

```text
ANTES
IUsed
IFree
IUse%

        ↓

DESPUÉS
IUsed
IFree
IUse%
```

Esperamos observar:

```text
IUsed ↑
IFree ↓
IUse% ↑
```

porque acabamos de crear miles de archivos.

---

# 13. Medir nuevamente el espacio en bytes

También ejecutamos:

```bash
df -h .
```

Esto nos permite comparar ambos recursos:

```text
df -h .
→ espacio en bytes

df -ih .
→ inodos
```

La idea del experimento es comprobar que crear miles de archivos pequeños tiene un impacto mucho más evidente en el uso de inodos que crear un archivo grande.

---

# 14. Observar cuánto ocupa el directorio

También podemos utilizar:

```bash
du -sh inode-test
```

Aunque los archivos sean vacíos:

```text
0 bytes de contenido
```

el directorio puede mostrar un consumo de espacio porque el filesystem necesita almacenar las estructuras y metadata necesarias para representar todas esas entradas.

Esto conecta con lo aprendido en el Día 4:

```text
du
→ espacio utilizado por archivos/directorios
```

y ahora:

```text
df -ih
→ utilización global de inodos del filesystem
```

---

# 15. El caso típico de inodos agotados

El caso que debemos reconocer en un sistema real es:

```text
df -h
```

muestra que todavía existe espacio disponible.

Pero:

```text
df -ih
```

muestra:

```text
IUse% = 100%
```

Esto significa:

```text
bytes disponibles
        ↓
       SÍ

inodos disponibles
        ↓
       NO
```

Por tanto, un sistema puede parecer tener espacio libre y aun así no poder crear nuevos archivos.

---

# 16. Cómo se produce

Un patrón típico es:

```text
aplicación
    │
    ├── crea archivo pequeño
    ├── crea archivo pequeño
    ├── crea archivo pequeño
    ├── crea archivo pequeño
    └── ...
             ↓
      miles/millones de archivos
             ↓
       muchos inodos usados
```

El problema no necesariamente es que los archivos sean grandes.

El problema puede ser simplemente:

```text
hay demasiados archivos
```

---

# 17. Diagnóstico

Cuando aparece un problema relacionado con:

```text
No space left on device
```

no debemos asumir automáticamente que se agotaron los bytes.

Debemos distinguir:

```bash
df -h
```

para espacio en bytes.

Y:

```bash
df -ih
```

para inodos.

Modelo mental:

```text
"¿El filesystem está lleno?"
          │
          ├── bytes → df -h
          │
          └── inodos → df -ih
```

---

# 18. Limpieza del laboratorio

Después del experimento:

```bash
rm -rf inode-test
```

Esto elimina el directorio de prueba y todos los archivos que contiene.

Después podemos comprobar:

```bash
df -ih .
```

y observar que la utilización de inodos regresa aproximadamente al nivel anterior.

---

# 19. Comandos utilizados

```bash
# Entrar al laboratorio
cd ~/labs/linux-month1/03-storage/

# Ver espacio en bytes
df -h .

# Ver inodos
df -ih .

# Crear directorio de prueba
mkdir inode-test

# Crear 10,000 archivos vacíos
touch inode-test/file_{00001..10000}

# Contar los archivos creados
find inode-test -type f | wc -l

# Ver espacio utilizado por el directorio
du -sh inode-test

# Eliminar el laboratorio
rm -rf inode-test
```

---

# 20. Modelo mental final

El concepto principal del Día 5:

```text
                    FILESYSTEM
                        │
             ┌──────────┴──────────┐
             │                     │
          ESPACIO                INODOS
          (bytes)              (cantidad)
             │                     │
           df -h                df -ih
```

Un archivo grande:

```text
archivo grande
      ↓
muchos bytes
      ↓
espacio ↑↑
inodos ↑ poco
```

Muchos archivos pequeños:

```text
miles de archivos
      ↓
muchos inodos
      ↓
inodos ↑↑
```

---

# Conclusión

El filesystem no tiene únicamente un límite de almacenamiento expresado en bytes. También tiene una cantidad finita de inodos.

Por eso existen dos situaciones diferentes:

```text
ESPACIO LLENO
→ se agotaron los bytes disponibles

INODOS LLENOS
→ se agotaron los inodos disponibles
```

Los comandos para distinguirlos son:

```bash
df -h
```

para espacio en bytes, y:

```bash
df -ih
```

para inodos.

El laboratorio demuestra experimentalmente la diferencia:

```text
archivo grande
→ consume principalmente espacio en bytes

miles de archivos pequeños/vacíos
→ consumen muchos inodos
```

El caso importante que debemos ser capaces de reconocer en producción es:

```text
df -h
→ todavía hay espacio

df -ih
→ IUse% = 100%
```

En ese escenario el problema es **agotamiento de inodos**, aunque todavía exista espacio en bytes.
