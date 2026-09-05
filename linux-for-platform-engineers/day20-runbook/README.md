# W3 Report --- Storage Troubleshooting

## 1. Escenario

Se simuló un incidente de almacenamiento en el que una aplicación que
funcionaba correctamente el día anterior comienza a fallar porque no
puede escribir sus archivos de log.

La idea del ejercicio fue diagnosticar el problema sin asumir desde el
principio que el disco estaba lleno.

El escenario se trabajó sobre una carpeta de laboratorio:

``` bash
cd ~/labs/linux-month1/03-storage
mkdir -p capstone/logs
touch capstone/logs/app.log
touch capstone/logs/error.log
```

El directorio utilizado para simular los logs fue:

``` text
capstone/logs/
```

------------------------------------------------------------------------

## 2. Síntoma

El síntoma simulado fue:

``` text
La aplicación no puede escribir sus logs.
```

Este mensaje por sí solo no indica la causa.

Las posibles causas consideradas fueron:

-   Falta de espacio en bytes.
-   Falta de inodos.
-   Permisos incorrectos.
-   Propietario o grupo incorrectos.
-   Filesystem montado como `read-only`.
-   Ruta inexistente.

El diagnóstico debía separar estas posibilidades mediante comandos y
evidencia.

------------------------------------------------------------------------

## 3. Hipótesis iniciales

### Hipótesis 1 --- No hay espacio en bytes

Un filesystem puede impedir nuevas escrituras cuando se queda sin
espacio disponible.

Se comprueba con:

``` bash
df -h .
```

`df` muestra el estado del filesystem y `-h` lo presenta en formato
legible.

La pregunta es:

> ¿Hay suficiente espacio disponible en bytes?

------------------------------------------------------------------------

### Hipótesis 2 --- No hay inodos

Tener espacio libre en bytes no garantiza que se puedan crear nuevos
archivos.

También puede agotarse el número de inodos disponibles.

Se comprueba con:

``` bash
df -ih .
```

La diferencia fundamental es:

``` text
df -h
→ bytes

df -ih
→ inodos
```

------------------------------------------------------------------------

### Hipótesis 3 --- El directorio no tiene permisos de escritura

Para crear un nuevo archivo dentro de un directorio, son importantes los
permisos del directorio que lo contiene.

Se comprueba con:

``` bash
ls -ld capstone/logs
```

También se pueden revisar los archivos existentes con:

``` bash
ls -l capstone/logs
```

------------------------------------------------------------------------

### Hipótesis 4 --- El filesystem está montado como `read-only`

Aunque los permisos Unix sean correctos, un filesystem montado como `ro`
puede impedir escrituras.

Se puede investigar con:

``` bash
findmnt .
```

Las opciones relevantes incluyen:

``` text
rw
→ read-write

ro
→ read-only
```

------------------------------------------------------------------------

### Hipótesis 5 --- La ruta no existe

Una ruta inexistente produce un problema diferente:

``` text
No such file or directory
```

Esto se distingue de:

``` text
Permission denied
```

La estructura del laboratorio se creó con:

``` bash
mkdir -p capstone/logs
```

por lo que en este escenario la ruta sí existe.

------------------------------------------------------------------------

## 4. Evidencia

### 4.1 `df -h`

Comando:

``` bash
df -h .
```

Propósito:

> Comprobar el espacio disponible en bytes del filesystem que contiene
> el laboratorio.

`df` muestra información del filesystem completo, no solamente de una
carpeta específica.

Conceptualmente:

``` text
Size
→ capacidad total

Used
→ espacio utilizado

Avail
→ espacio disponible

Use%
→ porcentaje utilizado

Mounted on
→ punto de montaje
```

En este escenario, el objetivo de esta comprobación es descartar la
hipótesis de que el filesystem esté lleno.

**Evidencia esperada:** el filesystem dispone de espacio libre
suficiente.

**Interpretación:** la falta de bytes no explica por sí sola el fallo.

------------------------------------------------------------------------

### 4.2 `df -ih`

Comando:

``` bash
df -ih .
```

Propósito:

> Comprobar la disponibilidad de inodos.

Los inodos son un recurso diferente del espacio en bytes.

Es posible tener:

``` text
bytes disponibles
+
inodos agotados
```

y aun así no poder crear nuevos archivos.

**Evidencia esperada:** existen inodos disponibles.

**Interpretación:** el agotamiento de inodos no explica el fallo
simulado.

------------------------------------------------------------------------

### 4.3 `du`

Comando:

``` bash
du -sh capstone/logs
```

Componentes:

``` text
du
→ disk usage

-s
→ summarize

-h
→ human-readable
```

Propósito:

> Saber cuánto espacio está consumiendo el directorio que contiene los
> logs.

Mientras `df` observa el filesystem, `du` permite investigar el consumo
de archivos y directorios.

Modelo:

``` text
df
→ ¿Cómo está el filesystem?

du
→ ¿Qué está consumiendo espacio?
```

**Evidencia esperada:** el directorio de logs utilizado en el
laboratorio tiene un consumo pequeño.

**Interpretación:** no existe evidencia de que este directorio esté
causando una presión importante de almacenamiento.

------------------------------------------------------------------------

### 4.4 `ls -ld`

Primero se provocó intencionalmente el problema:

``` bash
chmod u-w capstone/logs
```

Después se revisaron los permisos:

``` bash
ls -ld capstone/logs
```

El cambio elimina el permiso de escritura (`w`) del propietario del
directorio.

Después se intentó crear un nuevo archivo:

``` bash
touch capstone/logs/new.log
```

El resultado esperado es:

``` text
Permission denied
```

Esto proporciona evidencia directa de que el problema simulado está
relacionado con permisos.

------------------------------------------------------------------------

### 4.5 `ls -l`

También se puede revisar el contenido:

``` bash
ls -l capstone/logs
```

Este comando permite observar:

-   permisos,
-   propietario,
-   grupo,
-   tamaño,
-   nombre de los archivos.

Para diagnosticar una escritura fallida es importante distinguir entre:

``` text
archivo
```

y:

``` text
directorio que contiene al archivo
```

Si el archivo todavía no existe, se deben revisar especialmente los
permisos del directorio donde se intenta crearlo.

------------------------------------------------------------------------

## 5. Causa raíz

### Causa raíz: permisos de escritura del directorio

La causa raíz del escenario simulado fue la eliminación del permiso de
escritura del propietario sobre:

``` text
capstone/logs/
```

Se provocó mediante:

``` bash
chmod u-w capstone/logs
```

Después:

``` bash
touch capstone/logs/new.log
```

produjo:

``` text
Permission denied
```

La evidencia permite descartar las otras hipótesis principales del
escenario:

``` text
bytes
→ disponibles

inodos
→ disponibles

consumo de logs
→ pequeño

ruta
→ existe

permisos
→ incorrectos
```

Por lo tanto:

``` text
NO PUEDE ESCRIBIR LOGS
        ↓
el filesystem tiene recursos disponibles
        ↓
la ruta existe
        ↓
el directorio no permite escritura
        ↓
Permission denied
        ↓
CAUSA RAÍZ: permisos
```

------------------------------------------------------------------------

## 6. Corrección

Se restauró el permiso de escritura:

``` bash
chmod u+w capstone/logs
```

Se verificó:

``` bash
ls -ld capstone/logs
```

Y posteriormente:

``` bash
touch capstone/logs/new.log
```

La creación del archivo vuelve a ser posible.

------------------------------------------------------------------------

## 7. Runbook de diagnóstico de almacenamiento

Cuando una aplicación presenta:

``` text
"No puedo escribir"
```

no se debe asumir inmediatamente que el disco está lleno.

El procedimiento aprendido es:

### Paso 1 --- Revisar el filesystem

``` bash
df -h /ruta
```

Pregunta:

> ¿Hay espacio disponible en bytes?

------------------------------------------------------------------------

### Paso 2 --- Revisar inodos

``` bash
df -ih /ruta
```

Pregunta:

> ¿Hay inodos disponibles?

------------------------------------------------------------------------

### Paso 3 --- Investigar consumo

``` bash
du -sh /ruta
```

Pregunta:

> ¿Cuánto espacio consume esta carpeta?

Si se identifica presión de almacenamiento, se puede profundizar en sus
subdirectorios para localizar al consumidor.

------------------------------------------------------------------------

### Paso 4 --- Revisar permisos y propiedad

Para un directorio:

``` bash
ls -ld /ruta
```

Para su contenido:

``` bash
ls -l /ruta
```

Preguntas:

``` text
¿Quién es el propietario?

¿A qué grupo pertenece?

¿Qué permisos tiene?

¿El usuario que ejecuta el proceso puede escribir?
```

------------------------------------------------------------------------

### Paso 5 --- Revisar el mount/filesystem

``` bash
findmnt /ruta
```

Pregunta:

> ¿El filesystem está montado como `rw` o `ro`?

------------------------------------------------------------------------

### Paso 6 --- Correlacionar con el síntoma

No basta con ejecutar comandos.

Hay que relacionar la evidencia:

``` text
síntoma
  ↓
hipótesis
  ↓
comando
  ↓
resultado
  ↓
interpretación
  ↓
descartar o confirmar
```

------------------------------------------------------------------------

## 8. Correlación con logs

Un log también es un archivo.

Por ejemplo:

``` text
/var/log/app.log
```

Una aplicación necesita poder escribir ese archivo.

Por eso, un error relacionado con logs puede tener su causa en
almacenamiento o permisos.

La relación puede verse así:

``` text
Aplicación
    │
    │ intenta escribir
    ▼
Archivo de log
    │
    ▼
Filesystem
    │
    ├── bytes
    ├── inodos
    └── permisos
```

Por ejemplo, si una aplicación comienza a mostrar errores de escritura y
al mismo tiempo:

``` text
df -h
→ filesystem casi lleno

du
→ logs ocupan una cantidad importante
```

existe evidencia que correlaciona ambos hechos y permite investigar una
posible presión de almacenamiento.

Sin embargo:

``` text
correlación ≠ causalidad automática
```

La conclusión debe basarse en evidencia adicional.

------------------------------------------------------------------------

## 9. Modelo mental final

El síntoma:

``` text
NO PUEDO ESCRIBIR
```

debe convertirse en un diagnóstico sistemático:

``` text
                 NO PUEDO ESCRIBIR
                         │
                         ▼
                 ¿La ruta existe?
                         │
                         ▼
                    ¿Permisos?
                         │
                         ▼
              ¿Filesystem permite escribir?
                         │
                  ┌──────┴──────┐
                  ▼             ▼
                 ro             rw
                                │
                         ┌──────┴──────┐
                         ▼             ▼
                       bytes         inodos
                         │             │
                       df -h         df -ih
                         │             │
                         └──────┬──────┘
                                ▼
                               du
                                │
                                ▼
                         correlación
                         con los logs
                                │
                                ▼
                           conclusión
```

------------------------------------------------------------------------

## 10. Comandos utilizados

``` bash
# Entrar al laboratorio
cd ~/labs/linux-month1/03-storage

# Comprobar ubicación
pwd

# Crear estructura del escenario
mkdir -p capstone/logs

# Crear logs de prueba
touch capstone/logs/app.log
touch capstone/logs/error.log

# Estado del filesystem: bytes
df -h .

# Estado del filesystem: inodos
df -ih .

# Uso de espacio de la carpeta de logs
du -sh capstone/logs

# Revisar permisos del directorio
ls -ld capstone/logs

# Revisar archivos y sus permisos/propiedad
ls -l capstone/logs

# Identificar filesystem y opciones de montaje
findmnt .

# Provocar fallo de permisos
chmod u-w capstone/logs

# Probar creación de un nuevo log
touch capstone/logs/new.log

# Restaurar permiso de escritura
chmod u+w capstone/logs

# Limpiar el laboratorio si ya no se necesita
rm -rf capstone
```

------------------------------------------------------------------------

## 11. Conclusión

El cierre de la semana consiste en entender que un problema de
almacenamiento no se diagnostica mirando únicamente los GB disponibles.

Cuando una aplicación no puede escribir, hay que separar diferentes
recursos y capas:

``` text
RUTA
→ ¿existe?

PERMISOS
→ ¿el proceso puede escribir?

FILESYSTEM
→ ¿está montado como rw?

BYTES
→ df -h

INODOS
→ df -ih

CONSUMO
→ du
```

Los logs son relevantes porque también son archivos. Una aplicación que
escribe logs depende de que el almacenamiento, los inodos, los permisos
y el filesystem estén funcionando correctamente.

El patrón de troubleshooting aprendido durante la semana es:

``` text
SÍNTOMA
   ↓
HIPÓTESIS
   ↓
COMANDO
   ↓
EVIDENCIA
   ↓
INTERPRETACIÓN
   ↓
CAUSA RAÍZ
```

La principal lección del capstone es:

> **No asumir la causa a partir del síntoma. Medir primero y concluir a
> partir de la evidencia.**
