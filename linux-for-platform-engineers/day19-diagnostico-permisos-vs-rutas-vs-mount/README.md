# Día 6 — Diagnóstico de “no puedo escribir”
## Permisos vs rutas vs mount/espacio

## Idea principal

Cuando una aplicación no puede escribir en Linux, **no asumir automáticamente que es falta de espacio**.

Las tres causas principales estudiadas hoy:

1. **Permisos**
2. **Ruta equivocada o inexistente**
3. **Filesystem / mount**
   - montado `read-only` (`ro`)
   - sin espacio en bytes
   - sin inodos

Modelo mental:

```text
NO PUEDO ESCRIBIR
        │
        ├── ¿Existe la ruta?
        │       └── NO → No such file or directory
        │
        ├── ¿Tengo permisos?
        │       └── NO → Permission denied
        │
        └── ¿El filesystem permite escribir?
                ├── ro
                ├── sin bytes
                └── sin inodos
```

---

## 1. Permisos

Para crear un archivo dentro de un directorio, son importantes los permisos del **directorio donde se creará el archivo**.

Ejemplo:

```bash
touch permissions-test/test.txt
```

Si el directorio existe pero no tiene permiso de escritura:

```text
Permission denied
```

### Comandos utilizados

Crear directorio:

```bash
mkdir permissions-test
```

Ver sus permisos:

```bash
ls -ld permissions-test
```

Quitar escritura al propietario:

```bash
chmod u-w permissions-test
```

Restaurarla:

```bash
chmod u+w permissions-test
```

### Conceptos

```text
chmod
→ change mode

u
→ user / propietario

w
→ write / escritura

-w
→ quitar escritura

+w
→ agregar escritura
```

Para un directorio, de forma simplificada:

```text
r → leer/listar entradas
w → modificar entradas (crear, eliminar, renombrar)
x → atravesar/acceder al directorio
```

Punto importante:

> Para crear `archivo.txt`, que todavía no existe, no puedes depender de los permisos del archivo. La capacidad de crear la nueva entrada depende del directorio que la contiene.

---

## 2. Ruta inexistente

Una ruta puede fallar aunque los permisos y el filesystem estén bien.

Ejemplo:

```bash
touch ruta-que-no-existe/test.txt
```

Si `ruta-que-no-existe/` no existe:

```text
No such file or directory
```

Esto es diferente de:

```text
Permission denied
```

### Crear la estructura

```bash
mkdir -p ruta-que-no-existe
```

`mkdir` crea directorios.

`-p` permite crear también los directorios padre necesarios.

Después:

```bash
touch ruta-que-no-existe/test.txt
```

puede funcionar.

### Diferencia clave

```text
Permission denied
→ la ruta existe, pero la operación no está autorizada.

No such file or directory
→ la ruta necesaria no existe.
```

---

## 3. Filesystem / mount

Aunque la ruta exista y los permisos sean correctos, el filesystem puede impedir la escritura.

Las condiciones principales estudiadas:

```text
rw
→ read-write
→ lectura y escritura

ro
→ read-only
→ solamente lectura
```

Un filesystem `ro` puede impedir escribir aunque los permisos Unix parezcan correctos.

### Ver el filesystem de una ruta

```bash
findmnt .
```

Permite identificar el mount/filesystem correspondiente al directorio y observar sus opciones.

Buscar especialmente:

```text
rw
```

o:

```text
ro
```

---

## 4. Verificar espacio en bytes

```bash
df -h .
```

`df` consulta el estado del filesystem.

`-h` = human-readable.

Pregunta:

> ¿Cuánto espacio en bytes está utilizado y disponible en el filesystem que contiene esta ruta?

Ejemplo conceptual:

```text
Size   Used   Avail   Use%
100G   70G    30G     70%
```

Aquí todavía existen bytes disponibles.

---

## 5. Verificar inodos

```bash
df -ih .
```

Pregunta:

> ¿Cuántos inodos están utilizados y disponibles en el filesystem que contiene esta ruta?

Recordatorio del Día 5:

```text
df -h
→ bytes

df -ih
→ inodos
```

Puede existir:

```text
mucho espacio en bytes
+
0 inodos libres
```

y entonces no será posible crear nuevos archivos.

---

## 6. `df` vs `du`

No responden la misma pregunta.

### `df`

```bash
df -h .
df -ih .
```

Observa el **filesystem completo**:

```text
df -h
→ espacio en bytes

df -ih
→ inodos
```

### `du`

```bash
du -sh directorio
```

Investiga cuánto espacio están consumiendo determinados archivos/directorios.

Modelo mental:

```text
df
↓
¿Cómo está el filesystem?

du
↓
¿Qué archivos/directorios están consumiendo espacio?
```

---

## 7. Procedimiento básico de diagnóstico

Cuando una aplicación dice:

```text
“No puedo escribir”
```

seguir este orden:

### 1. Comprobar la ruta

```bash
ls -ld /ruta
```

Preguntar:

> ¿Existe el directorio?

Si no existe, investigar la ruta/configuración.

---

### 2. Comprobar permisos

```bash
ls -ld /ruta
```

Preguntar:

> ¿El usuario/proceso tiene los permisos necesarios sobre el directorio?

Si no:

```text
Permission denied
```

---

### 3. Identificar el filesystem

```bash
findmnt /ruta
```

Preguntar:

> ¿Qué filesystem contiene esta ruta y con qué opciones está montado?

---

### 4. Comprobar bytes

```bash
df -h /ruta
```

Preguntar:

> ¿Hay espacio disponible?

---

### 5. Comprobar inodos

```bash
df -ih /ruta
```

Preguntar:

> ¿Hay inodos disponibles?

---

## 8. Árbol de diagnóstico

```text
                 NO PUEDO ESCRIBIR
                         │
                         ▼
                 ¿La ruta existe?
                    /          \
                  NO            SÍ
                  │              │
                  ▼              ▼
      No such file or      ¿Permisos?
         directory          /       \
                           NO        SÍ
                           │          │
                           ▼          ▼
                  Permission denied   │
                                      ▼
                             ¿Filesystem permite
                                escritura?
                                  /       \
                                NO         SÍ
                                │           │
                                ▼           ▼
                              read-only    │
                                            ▼
                                  ¿Hay espacio?
                                     │
                           ┌─────────┴─────────┐
                           │                   │
                         bytes              inodos
                           │                   │
                       df -h              df -ih
```

---

## 9. Comandos del día

```bash
# Entrar al laboratorio
cd ~/labs/linux-month1/03-storage/

# Directorio actual
pwd

# Ver permisos del directorio actual
ls -ld .

# Crear directorio de prueba
mkdir permissions-test

# Ver permisos
ls -ld permissions-test

# Crear archivo
touch permissions-test/test.txt

# Quitar escritura al propietario
chmod u-w permissions-test

# Intentar escribir después de quitar permisos
touch permissions-test/fail.txt

# Restaurar escritura
chmod u+w permissions-test

# Crear estructura de directorios
mkdir -p ruta-que-no-existe

# Crear archivo en la ruta
touch ruta-que-no-existe/test.txt

# Ver espacio en bytes
df -h .

# Ver inodos
df -ih .

# Identificar filesystem/mount y opciones
findmnt .

# Limpiar laboratorio
rm -rf permissions-test
rm -rf ruta-que-no-existe
```

---

## 10. Conexión con el Día 5

El Día 5 enseñó:

```text
FILESYSTEM
│
├── espacio en bytes
│      ↓
│    df -h
│
└── inodos
       ↓
     df -ih
```

El Día 6 convierte eso en troubleshooting:

```text
Aplicación no puede escribir
        ↓
¿ruta?
        ↓
¿permisos?
        ↓
¿filesystem?
        ↓
¿bytes?
        ↓
¿inodos?
```

La idea fundamental es **no confundir capas diferentes**.

---

## Conclusión

“No puedo escribir” no es un diagnóstico; es solamente el síntoma.

Las primeras preguntas deben separar:

```text
RUTA
→ ¿existe?

PERMISOS
→ ¿puedo escribir ahí?

FILESYSTEM
→ ¿está disponible para escritura?

BYTES
→ df -h

INODOS
→ df -ih

MOUNT
→ findmnt
```

Errores clave:

```text
Permission denied
→ problema de autorización/permisos

No such file or directory
→ problema de ruta inexistente

No space left on device
→ comprobar tanto df -h como df -ih
```

**Modelo mental final:**

```text
NO PUEDO ESCRIBIR
       │
       ├── RUTA
       │
       ├── PERMISOS
       │
       └── FILESYSTEM
              ├── ro/rw
              ├── bytes
              └── inodos
```
