# Day 05 - strace: Entendiendo qué hace realmente un proceso

## Objetivo

Aprender cómo observar la interacción entre un proceso y el kernel mediante **System Calls (syscalls)** utilizando la herramienta **strace**.

El objetivo de este laboratorio **no** es memorizar todas las syscalls existentes, sino comprender que cualquier programa necesita solicitar servicios al kernel para acceder a archivos, memoria, red, procesos o dispositivos.

---

# ¿Qué es una System Call?

Un proceso en Linux **no puede acceder directamente al hardware**.

No puede:

- leer un archivo
- escribir en pantalla
- abrir una conexión TCP
- reservar memoria física

Para realizar cualquiera de estas acciones debe solicitarlo al Kernel mediante una **System Call**.

Podemos imaginarlo así:

```
Proceso
   │
   │ Solicitud
   ▼
Kernel
   │
   │ Acceso al hardware
   ▼
Disco / Red / Memoria / CPU
```

El kernel es el único que tiene acceso directo al hardware.

Los programas únicamente solicitan operaciones.

---

# ¿Qué es strace?

`strace` significa:

```
System Call Trace
```

Es una herramienta que intercepta todas las llamadas que un proceso realiza al kernel.

No muestra:

- variables
- clases
- funciones
- objetos

Muestra únicamente las **syscalls**.

Por ello funciona con cualquier lenguaje:

- C
- Java
- Go
- Python
- NodeJS
- Rust

Todos terminan comunicándose con el mismo kernel.

---

# Instalación

Ubuntu / Debian

```bash
sudo apt install strace
```

Verificar instalación

```bash
strace -V
```

---

# Primer laboratorio

Se ejecutó:

```bash
strace cat README.md
```

El resultado contiene cientos de líneas.

Esto ocurre porque antes de ejecutar la lógica principal del programa, Linux necesita:

- cargar bibliotecas compartidas (.so)
- reservar memoria
- cargar configuración
- preparar el entorno de ejecución

Por ello las primeras líneas del `strace` normalmente no corresponden al trabajo principal del programa.

---

# Filtrar syscalls

Para reducir el ruido puede filtrarse únicamente un conjunto de llamadas:

```bash
strace -e trace=openat,read,write,close cat README.md
```

Esto permite concentrarse únicamente en operaciones relacionadas con archivos.

---

# Observación importante

Durante este laboratorio se esperaba encontrar una secuencia clásica como:

```
open()
↓

read()
↓

write()
↓

close()
```

Sin embargo, la implementación de `cat` instalada en el sistema utilizó una optimización distinta.

La salida observada fue similar a:

```
statx()

↓

openat()

↓

fstat()

↓

splice()

↓

pipe()

↓

splice()

↓

close()
```

Esto demuestra una idea importante:

> **No todos los programas utilizan las mismas syscalls para realizar una tarea.**

La implementación depende del programa y de las optimizaciones disponibles.

---

# Syscalls observadas

## statx()

Obtiene información sobre un archivo.

Ejemplo:

- tamaño
- permisos
- propietario
- fechas

No abre el archivo.

Solo consulta sus metadatos.

---

## openat()

Solicita al kernel abrir un archivo.

Ejemplo:

```text
openat(...,"README.md",O_RDONLY)=3
```

El kernel devuelve un **File Descriptor**.

En este caso:

```
3
```

Ese descriptor identifica al archivo durante el resto de la ejecución.

---

## read()

Lee datos desde un descriptor de archivo.

Aunque apareció muchas veces al cargar bibliotecas del sistema, el archivo `README.md` no fue leído mediante `read()` sino mediante `splice()`.

---

## write()

Escribe datos hacia un descriptor.

Normalmente:

```
1
```

representa:

```
stdout
```

es decir, la terminal.

En este laboratorio prácticamente no apareció porque `cat` utilizó `splice()`.

---

## splice()

Transfiere datos entre dos descriptores sin realizar copias innecesarias entre espacio de usuario y kernel.

En este caso el flujo fue aproximadamente:

```
README.md

↓

Pipe

↓

stdout
```

Es una optimización utilizada por algunas versiones modernas de `cat`.

---

## pipe()

Crea un canal de comunicación temporal.

En este laboratorio fue utilizado como paso intermedio para realizar la copia mediante `splice()`.

---

## close()

Libera un descriptor previamente abierto.

Después de terminar la operación se observaron llamadas similares a:

```text
close(5)

close(4)

close(3)
```

Indicando que todos los recursos fueron liberados correctamente.

---

# Segundo laboratorio

Se ejecutó:

```bash
strace echo Hola
```

Objetivo:

Observar cómo un proceso escribe datos hacia la terminal.

La syscall más importante es:

```
write()
```

El proceso solicita al kernel escribir el texto en el descriptor correspondiente a `stdout`.

---

# Tercer laboratorio

Se ejecutó:

```bash
strace sleep 3
```

Objetivo:

Observar que un proceso puede solicitar al kernel permanecer suspendido durante un intervalo de tiempo.

La syscall observada fue:

```
clock_nanosleep()
```

---

# Cuarto laboratorio

Se ejecutó:

```bash
strace curl https://example.com
```

Durante este laboratorio aparecieron muchas syscalls relacionadas con acceso a archivos y red.

Sin embargo, **todavía no se estudiaron TCP, DNS, HTTP ni TLS**, por lo que el objetivo de este laboratorio no fue analizar dichas llamadas en detalle.

Lo importante fue comprender que un programa que utiliza la red realiza syscalls diferentes a las utilizadas por un programa que únicamente lee archivos.

Estos conceptos serán retomados durante el módulo de Redes.

---

# Ideas clave aprendidas

Un proceso no accede directamente al hardware.

Siempre solicita operaciones al kernel.

Las syscalls representan ese mecanismo de comunicación.

`strace` permite observar dichas solicitudes en tiempo real.

No existe una única secuencia de syscalls para realizar una tarea.

Diferentes implementaciones pueden utilizar estrategias distintas (`read/write`, `splice`, `mmap`, `sendfile`, etc.).

El objetivo de `strace` no es memorizar syscalls, sino entender **qué recurso está utilizando un proceso**.

---

# ¿Cómo piensa un SRE?

Cuando una aplicación falla, un SRE no comienza revisando el código fuente.

Primero obtiene evidencia.

Preguntas típicas:

- ¿El programa abrió el archivo correctamente?
- ¿El archivo existe?
- ¿Qué recursos está intentando utilizar?
- ¿Está accediendo al disco?
- ¿Está utilizando memoria?
- ¿Está intentando comunicarse por red?
- ¿Está cerrando correctamente sus recursos?

`strace` permite responder estas preguntas incluso sin tener acceso al código fuente.

---

# Comandos utilizados

Instalar

```bash
sudo apt install strace
```

Versión

```bash
strace -V
```

Traza completa

```bash
strace cat README.md
```

Filtrar syscalls

```bash
strace -e trace=openat,read,write,close cat README.md
```

Escribir en terminal

```bash
strace echo Hola
```

Suspender un proceso

```bash
strace sleep 3
```

Programa que utiliza la red

```bash
strace curl https://example.com
```

---

# Relación con Backend, Platform y SRE

`strace` es una de las herramientas más utilizadas para investigar procesos en sistemas Linux.

Permite obtener evidencia sobre el comportamiento real de una aplicación sin necesidad de revisar su código fuente.

En etapas posteriores del plan de estudio, este conocimiento se combinará con:

- TCP/IP
- DNS
- HTTP
- TLS
- Docker
- PostgreSQL
- JVM
- Kubernetes

para analizar incidentes reales en aplicaciones distribuidas.

---

# Conclusiones

Durante este laboratorio se comprendió que un proceso no interactúa directamente con el hardware, sino mediante **System Calls** realizadas al kernel.

También se comprobó que programas aparentemente sencillos, como `cat`, pueden utilizar implementaciones optimizadas (`splice`) en lugar del patrón clásico `read()` + `write()`.

La principal habilidad adquirida no consiste en memorizar syscalls, sino en interpretar qué recursos está utilizando un proceso y comenzar a obtener evidencia sobre su comportamiento.
