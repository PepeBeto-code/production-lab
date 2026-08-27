# Día 15 — Permisos en Linux: archivos y directorios

## Objetivo

Entender cómo funcionan los permisos `r`, `w` y `x` en Linux para:

- archivos;
- directorios;
- usuario/propietario, grupo y otros;
- y comprobarlo mediante un laboratorio práctico.

También se revisó la diferencia entre `chmod` y `chown`, y se utilizaron `ls -l`, `ls -ld` y `stat` para observar permisos y metadatos.

---

# 1. Modelo básico de permisos

Linux necesita decidir qué puede hacer un usuario sobre un archivo o directorio.

Los permisos se organizan en **tres conjuntos**:

```text
rwx | rwx | rwx
 ↑      ↑      ↑
user   group  others
```

- **user (u):** propietario del archivo/directorio.
- **group (g):** usuarios pertenecientes al grupo asociado.
- **others (o):** los demás usuarios.

Cada conjunto tiene tres posiciones:

```text
r w x
│ │ └── execute
│ └──── write
└────── read
```

Cuando una posición contiene `-`, ese permiso no está concedido.

Por ejemplo:

```text
-r--r--r--
```

se interpreta como:

```text
- | r-- | r-- | r--
    user  group others
```

Por tanto:

- propietario: puede leer;
- grupo: puede leer;
- otros: pueden leer;
- ninguno tiene escritura ni ejecución.

El primer carácter (`-`) no es un permiso `rwx`: indica el tipo de objeto. `-` representa un archivo regular y `d` representa un directorio.

---

# 2. `r`, `w` y `x` en archivos

## `r` — read

En un archivo, `r` permite **leer su contenido**.

Ejemplo:

```bash
cat archivo.txt
```

Si el usuario no tiene permiso de lectura sobre el archivo, la lectura puede terminar en:

```text
Permission denied
```

## `w` — write

En un archivo, `w` permite **modificar su contenido**.

Una operación como:

```bash
echo "nuevo contenido" > archivo.txt
```

puede modificar/truncar el archivo.

`w` sobre el archivo se refiere al contenido del archivo.

## `x` — execute

En un archivo, `x` permite ejecutarlo como programa/script cuando corresponde.

Por ejemplo, un script con `#!` puede ejecutarse directamente:

```bash
./script.sh
```

Esto es diferente de invocar explícitamente al intérprete, por ejemplo:

```bash
bash script.sh
```

---

# 3. `r`, `w` y `x` en directorios

Los permisos de un directorio deben entenderse de forma diferente a los de un archivo.

## `r` — listar

En un directorio, `r` permite consultar/listar sus entradas.

Por ejemplo:

```bash
ls directorio/
```

Esto se refiere a poder conocer qué nombres/entradas contiene el directorio.

Tener `r` sobre el directorio no significa automáticamente tener permiso para leer el contenido de cada archivo que contiene.

## `w` — modificar entradas

En un directorio, `w` permite modificar sus entradas. Esto incluye operaciones como:

- crear;
- borrar;
- renombrar.

Éste fue el comportamiento comprobado en el laboratorio.

Un punto fundamental:

> **Para borrar un archivo interviene el permiso de escritura del directorio que contiene la entrada, no simplemente el permiso `w` del archivo.**

Por eso un archivo puede tener:

```text
-rw-r--r--
```

y aun así no poder eliminarse si el directorio no permite escritura.

## `x` — atravesar/acceder

En un directorio, `x` permite atravesarlo y acceder a los elementos de acuerdo con los demás permisos necesarios.

Por ejemplo, para llegar a:

```text
/home/pepe/proyecto/archivo.txt
```

Linux necesita poder atravesar los directorios de la ruta.

---

# 4. Diferencia fundamental: archivo vs directorio

| Permiso | Archivo | Directorio |
|---|---|---|
| `r` | Leer contenido | Listar entradas |
| `w` | Modificar/truncar contenido | Crear, borrar, renombrar entradas |
| `x` | Ejecutar | Atravesar/acceder |

La distinción más importante comprobada hoy fue:

```text
w sobre archivo
→ permite modificar su contenido

w sobre directorio
→ permite modificar las entradas del directorio
```

Por eso:

```text
permtest/
└── archivo.txt
```

si se quita `w` de `permtest/`, no se puede crear o eliminar `archivo.txt`, aunque `archivo.txt` tenga permiso de escritura.

---

# 5. `chmod` vs `chown`

## `chmod`

`chmod` cambia los **permisos**.

```bash
chmod ...
```

Afecta `r`, `w` y `x`.

Ejemplo usado en el laboratorio:

```bash
chmod u-w ~/labs/linux-month1/03-storage/permtest
```

Esto significa:

```text
u
↓
user/propietario

-w
↓
quitar write
```

Para restaurarlo:

```bash
chmod u+w ~/labs/linux-month1/03-storage/permtest
```

## `chown`

`chown` cambia el **propietario** y/o grupo asociado.

Conceptualmente:

```text
chmod
→ permisos

chown
→ propietario/grupo
```

No son equivalentes.

---

# 6. `ls -l` y `ls -ld`

`ls -l` muestra información detallada de archivos.

Ejemplo del laboratorio:

```text
-rw-r--r-- 1 pepe pepe 5 Aug 26 19:25 archivo.txt
```

La primera parte:

```text
-rw-r--r--
```

son los permisos/tipo.

En el laboratorio, el archivo pertenecía a:

```text
pepe pepe
```

y tenía:

```text
-rw-r--r--
```

Es decir, `0644`:

```text
rw- | r-- | r--
```

- propietario: lectura + escritura;
- grupo: lectura;
- otros: lectura.

Para inspeccionar el directorio usamos:

```bash
ls -ld ~/labs/linux-month1/03-storage/permtest
```

La `d` de:

```text
drwxr-xr-x
```

indica que es un directorio.

La opción `-d` hace que `ls` muestre la información del propio directorio en lugar de listar su contenido.

---

# 7. `stat`

`stat` muestra información detallada sobre un archivo o directorio, incluyendo permisos, propietario, tamaño, inode y timestamps.

En el laboratorio:

```bash
stat ~/labs/linux-month1/03-storage/permtest/archivo.txt
```

produjo información relevante como:

```text
File: /home/pepe/labs/linux-month1/03-storage/permtest/archivo.txt
size: 5
Blocks: 8
IO Block: 4096
regular file
Device: 8,48
Inode: 52810
Links: 1
Access: (0644/-rw-r--r--)
Uid: (1000/pepe)
Gid: (1000/pepe)
```

Lo importante para este laboratorio fue principalmente:

```text
Access: (0644/-rw-r--r--)
```

que permite relacionar la representación numérica `0644` con los permisos `-rw-r--r--`.

También se observó que el archivo tenía:

```text
size: 5
```

porque `hola` ocupa 5 bytes incluyendo el salto de línea producido por `echo`.

---

# 8. Laboratorio práctico

## 8.1 Crear el entorno

Se creó:

```bash
mkdir -p ~/labs/linux-month1/03-storage/permtest
```

Después:

```bash
echo "hola" > ~/labs/linux-month1/03-storage/permtest/archivo.txt
```

Se comprobó que el contenido era:

```text
hola
```

---

## 8.2 Estado inicial

Directorio:

```text
drwxr-xr-x 2 pepe pepe 4096 ... /home/pepe/labs/linux-month1/03-storage/permtest
```

Permisos:

```text
rwx | r-x | r-x
 u     g     o
```

Archivo:

```text
-rw-r--r-- 1 pepe pepe 5 ... archivo.txt
```

Permisos:

```text
rw- | r-- | r--
 u     g     o
```

El `stat` confirmó:

```text
Access: (0644/-rw-r--r--)
```

---

# 9. Experimento: quitar `w` al directorio

Se ejecutó:

```bash
chmod u-w ~/labs/linux-month1/03-storage/permtest
```

El directorio pasó de:

```text
drwxr-xr-x
```

a:

```text
dr-xr-xr-x
```

Es decir, al propietario se le quitó `w`.

Después se intentó crear:

```bash
touch ~/labs/linux-month1/03-storage/permtest/nuevo.txt
```

Resultado:

```text
Permission denied
```

También se intentó borrar el archivo existente:

```bash
rm ~/labs/linux-month1/03-storage/permtest/archivo.txt
```

Resultado:

```text
Permission denied
```

### Conclusión experimental

Esto confirmó que:

```text
w del directorio
→ controla la modificación de sus entradas
→ crear
→ borrar
→ renombrar
```

Por tanto, tener `w` en:

```text
archivo.txt
```

no basta para eliminarlo.

Para eliminar la entrada del directorio se necesita poder modificar el directorio que la contiene.

Finalmente se restauró el permiso:

```bash
chmod u+w ~/labs/linux-month1/03-storage/permtest
```

---

# 10. Experimento de lectura del archivo

La segunda parte del laboratorio consiste en comprobar `r` sobre el archivo.

Se quita `r` al propietario:

```bash
chmod u-r ~/labs/linux-month1/03-storage/permtest/archivo.txt
```

Después se intenta:

```bash
cat ~/labs/linux-month1/03-storage/permtest/archivo.txt
```

El resultado esperado es:

```text
Permission denied
```

Esto demuestra directamente:

```text
r sobre archivo
→ permite leer su contenido
```

Después se restaura:

```bash
chmod u+r ~/labs/linux-month1/03-storage/permtest/archivo.txt
```

Y:

```bash
cat ~/labs/linux-month1/03-storage/permtest/archivo.txt
```

vuelve a mostrar:

```text
hola
```

---

# 11. Modelo mental final

Para archivos:

```text
r → puedo leer el contenido
w → puedo modificar/truncar el contenido
x → puedo ejecutar
```

Para directorios:

```text
r → puedo listar entradas
w → puedo crear/borrar/renombrar entradas
x → puedo atravesar/acceder
```

Y los tres bloques:

```text
rwx | rwx | rwx
 ↑      ↑      ↑
user   group  others
```

responden:

```text
¿Qué puede hacer el propietario?
¿Qué puede hacer alguien del grupo?
¿Qué puede hacer cualquier otro usuario?
```

La idea operacional más importante del laboratorio fue:

```text
operación
   ↓
¿sobre qué objeto se está actuando?
   ↓
archivo o directorio
   ↓
¿qué permiso necesita esa operación?
```

Especialmente:

```text
borrar archivo
    ↓
se modifica la entrada del directorio
    ↓
necesito permiso de escritura sobre el directorio
```

No debemos confundirlo con:

```text
modificar contenido del archivo
    ↓
necesito permiso de escritura sobre el archivo
```

---

# 12. Comandos utilizados

```bash
# Crear laboratorio
mkdir -p ~/labs/linux-month1/03-storage/permtest

# Crear archivo
echo "hola" > ~/labs/linux-month1/03-storage/permtest/archivo.txt

# Leer archivo
cat ~/labs/linux-month1/03-storage/permtest/archivo.txt

# Ver permisos del directorio
ls -ld ~/labs/linux-month1/03-storage/permtest

# Ver contenido y permisos
ls -l ~/labs/linux-month1/03-storage/permtest

# Ver metadatos
stat ~/labs/linux-month1/03-storage/permtest/archivo.txt

# Quitar escritura al propietario del directorio
chmod u-w ~/labs/linux-month1/03-storage/permtest

# Restaurar escritura
chmod u+w ~/labs/linux-month1/03-storage/permtest

# Quitar lectura al propietario del archivo
chmod u-r ~/labs/linux-month1/03-storage/permtest/archivo.txt

# Restaurar lectura
chmod u+r ~/labs/linux-month1/03-storage/permtest/archivo.txt

# Crear archivo durante la prueba
touch ~/labs/linux-month1/03-storage/permtest/nuevo.txt

# Eliminar archivo durante la prueba
rm ~/labs/linux-month1/03-storage/permtest/archivo.txt
```

---

# Conclusión del Día 15

El objetivo principal no era memorizar comandos, sino entender **qué está protegiendo cada permiso y sobre qué objeto se aplica**.

La distinción crítica es:

```text
ARCHIVO
r → leer contenido
w → modificar contenido
x → ejecutar

DIRECTORIO
r → listar
w → crear/borrar/renombrar entradas
x → atravesar/acceder
```

El laboratorio confirmó especialmente que **el permiso de escritura del directorio controla la creación y eliminación de sus entradas**, independientemente del permiso de escritura que tenga el archivo.

Con esto queda cubierta la parte práctica central del Día 15.
