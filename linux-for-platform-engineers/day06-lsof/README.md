# Day 06 - lsof: Inspeccionando los recursos abiertos de un proceso

## Objetivo

Aprender a utilizar **lsof (List Open Files)** para inspeccionar todos los recursos que un proceso mantiene abiertos.

Al finalizar este laboratorio serás capaz de:

- Entender qué significa realmente "archivo abierto" en Linux.
- Comprender qué es un **File Descriptor (FD)**.
- Inspeccionar los recursos abiertos por un proceso.
- Identificar qué proceso está utilizando un puerto.
- Interpretar correctamente la salida de `lsof`.
- Comprender la diferencia entre `ps`, `strace` y `lsof`.

---

# ¿Qué es lsof?

`lsof` significa:

```
List Open Files
```

Su función es mostrar **todos los recursos abiertos por un proceso**.

Aunque el nombre dice "archivos", en Linux un recurso abierto no siempre es un archivo tradicional.

Puede ser:

- Un archivo de texto.
- Un directorio.
- Un socket TCP.
- Un socket UNIX.
- Una terminal.
- Un pipe.
- Un dispositivo del sistema.

Por ello `lsof` es una herramienta fundamental para diagnosticar aplicaciones en ejecución.

---

# El concepto más importante del día

Antes de aprender `lsof`, es necesario entender cómo trabaja un proceso.

Cuando un programa necesita utilizar algún recurso, **no accede directamente a él**.

Solicita al kernel que lo abra.

Por ejemplo:

```
Proceso

↓

Quiero abrir README.md

↓

Kernel

↓

Descriptor 3
```

A partir de ese momento el programa ya no utiliza el nombre del archivo.

Utiliza únicamente el descriptor.

```
Descriptor 3

↓

README.md
```

Lo mismo ocurre con cualquier otro recurso.

```
Descriptor 0

↓

stdin
```

```
Descriptor 1

↓

stdout
```

```
Descriptor 2

↓

stderr
```

Más adelante el proceso puede abrir:

```
Descriptor 3

↓

README.md
```

```
Descriptor 4

↓

application.log
```

```
Descriptor 5

↓

Socket TCP
```

Todos son simplemente recursos abiertos.

---

# ¿Qué hace realmente lsof?

`lsof` únicamente le pregunta al kernel:

> ¿Qué recursos tiene abiertos este proceso?

Y el kernel responde con una lista similar a:

```
Proceso Java

Descriptor 0 → stdin

Descriptor 1 → stdout

Descriptor 2 → stderr

Descriptor 3 → README.md

Descriptor 4 → application.log

Descriptor 5 → Socket TCP
```

Eso es exactamente lo que muestra `lsof`.

No modifica el proceso.

No cambia su comportamiento.

Simplemente toma una fotografía del estado actual de sus recursos abiertos.

---

# Instalación

Ubuntu / Debian

```bash
sudo apt install lsof
```

Verificar instalación

```bash
lsof -v
```

---

# Ver todos los recursos abiertos

```bash
lsof
```

Este comando muestra todos los recursos abiertos por todos los procesos del sistema.

La salida puede contener cientos o miles de líneas.

---

# Ver únicamente un proceso

```bash
lsof -p <PID>
```

Ejemplo:

```bash
lsof -p 5321
```

Aquí únicamente se muestran los recursos abiertos por ese proceso.

---

# Explicación de las columnas

Ejemplo de salida:

```
COMMAND PID USER FD TYPE DEVICE SIZE/OFF NODE NAME
```

Cada columna tiene un significado específico.

---

## COMMAND

Programa propietario del recurso.

Ejemplo:

```
java
```

---

## PID

Identificador del proceso.

Ejemplo:

```
5231
```

---

## USER

Usuario propietario del proceso.

Ejemplo:

```
pepe
```

---

## FD (File Descriptor)

Indica qué descriptor utiliza el proceso para acceder al recurso.

Ejemplos:

```
0u
```

Descriptor estándar de entrada.

```
1u
```

Salida estándar.

```
2u
```

Salida de errores.

También pueden aparecer:

```
3r
```

Descriptor 3 abierto en modo lectura.

```
5w
```

Descriptor 5 abierto en modo escritura.

```
7u
```

Descriptor abierto para lectura y escritura.

---

## TYPE

Tipo de recurso.

Los más comunes son:

| Tipo | Significado |
|-------|-------------|
| REG | Archivo normal |
| DIR | Directorio |
| CHR | Dispositivo de caracteres |
| FIFO | Pipe |
| IPv4 | Socket IPv4 |
| IPv6 | Socket IPv6 |
| unix | Socket UNIX |

---

## NAME

Nombre del recurso.

Ejemplos:

```
README.md
```

```
application.log
```

```
TCP *:8080 (LISTEN)
```

```
/dev/pts/0
```

Esta suele ser la columna más útil durante una investigación.

---

# Recursos especiales

Al ejecutar:

```bash
lsof -p <PID>
```

es común encontrar entradas como:

```
cwd
```

Current Working Directory.

Directorio actual del proceso.

---

```
txt
```

Ejecutable cargado en memoria.

Por ejemplo:

```
/usr/bin/java
```

---

```
mem
```

Bibliotecas compartidas cargadas por el proceso.

Ejemplo:

```
libc.so
```

---

```
0
```

stdin.

---

```
1
```

stdout.

---

```
2
```

stderr.

---

# Buscar procesos utilizando un puerto

Otra de las funciones más útiles de `lsof` consiste en inspeccionar conexiones de red.

Ejemplo:

```bash
lsof -i :8080
```

El parámetro `-i` indica que únicamente queremos mostrar conexiones de red.

Si existe un proceso utilizando ese puerto veremos algo parecido a:

```
COMMAND PID USER FD TYPE NAME

java 8451 pepe 15u IPv4 TCP *:8080 (LISTEN)
```

Lo anterior puede traducirse como:

- El proceso Java
- Tiene abierto el descriptor 15
- Ese descriptor corresponde a un socket IPv4
- Está escuchando conexiones en el puerto 8080

---

# Laboratorio realizado

## Preparación

Se creó un laboratorio sencillo en Java.

Archivos:

```
README.md
```

```
config.txt
```

Programa:

```
Main.java
```

El programa realiza la siguiente secuencia:

```
Inicio

↓

Esperar

↓

Abrir README.md

↓

Esperar

↓

Abrir config.txt

↓

Esperar

↓

Cerrar README.md

↓

Esperar

↓

Cerrar config.txt

↓

Esperar

↓

Finalizar
```

Durante cada pausa se ejecutó:

```bash
lsof -p <PID>
```

para observar los recursos abiertos.

---

# Observaciones

## Primera ejecución

Antes de abrir archivos únicamente aparecían:

- cwd
- txt
- mem
- stdin
- stdout
- stderr

Todavía no existían descriptores asociados a:

```
README.md
```

ni

```
config.txt
```

---

## Después de abrir README.md

Apareció un nuevo descriptor asociado al archivo.

Esto confirmó que el kernel creó un nuevo File Descriptor para ese recurso.

---

## Después de abrir config.txt

Se observó un segundo descriptor correspondiente a ese archivo.

Ahora ambos archivos permanecían abiertos simultáneamente.

---

## Después de cerrar README.md

El descriptor desapareció de la salida de `lsof`.

Esto demostró que el recurso había sido liberado.

---

## Después de cerrar config.txt

También desapareció.

El proceso volvió a tener únicamente sus recursos básicos abiertos.

---

# Comparación con herramientas anteriores

## ps

Responde:

> ¿Qué procesos existen?

---

## top

Responde:

> ¿Qué proceso consume CPU o memoria?

---

## systemctl

Responde:

> ¿Cuál es el estado de un servicio?

---

## journalctl

Responde:

> ¿Qué registró el servicio?

---

## strace

Responde:

> ¿Qué llamadas al sistema realiza un proceso?

---

## lsof

Responde:

> ¿Qué recursos tiene abiertos un proceso en este momento?

---

# Comandos utilizados

Instalación

```bash
sudo apt install lsof
```

Versión

```bash
lsof -v
```

Todos los recursos abiertos

```bash
lsof
```

Recursos abiertos por un proceso

```bash
lsof -p <PID>
```

Buscar un puerto

```bash
lsof -i :8080
```

---

# Aplicaciones prácticas

`lsof` es una herramienta ampliamente utilizada por administradores de sistemas y SRE para responder preguntas como:

- ¿Qué proceso está utilizando el puerto 8080?
- ¿Qué archivos mantiene abiertos una aplicación?
- ¿Una aplicación abrió correctamente su archivo de configuración?
- ¿Qué recursos siguen abiertos antes de finalizar un proceso?
- ¿Qué conexiones de red mantiene activas una aplicación?

---

# Ideas clave aprendidas

- Un proceso no trabaja directamente con archivos, sino con **File Descriptors**.
- Un descriptor representa un recurso abierto administrado por el kernel.
- Los sockets, pipes, terminales y archivos normales pueden inspeccionarse con `lsof`.
- `lsof` muestra el estado actual de los recursos abiertos por un proceso.
- Observar cómo aparecen y desaparecen descriptores al abrir y cerrar archivos permite comprender mejor el ciclo de vida de los recursos en Linux.

---

# Conclusiones

Durante este laboratorio se comprendió que un proceso mantiene una tabla de recursos abiertos administrada por el kernel.

Cada recurso recibe un **File Descriptor**, que es el identificador utilizado por el proceso para acceder a él.

`lsof` permite inspeccionar esa tabla en tiempo real, convirtiéndose en una herramienta esencial para diagnosticar aplicaciones, investigar procesos y analizar el uso de archivos, puertos y conexiones de red.
