# Día 16 — Propiedad, grupos y `umask`

## Objetivo

Entender cómo Linux relaciona **propietario**, **grupo** y **permisos**, cómo modificar la propiedad de archivos/directorios y cómo `umask` determina los permisos iniciales de los objetos nuevos.

---

# 1. Punto de partida: permisos + propiedad

En el día anterior vimos que una entrada como:

```text
-rw-r--r-- 1 pepe pepe 5 archivo.txt
```

contiene dos cosas distintas que no debemos confundir:

```text
-rw-r--r--
```

→ **tipo + permisos**

```text
pepe pepe
```

→ **propietario + grupo**

Los permisos se dividen en:

```text
rw- | r-- | r--
 ↑     ↑     ↑
user  group others
```

Por tanto, Linux necesita saber **quién es el propietario y qué grupo está asociado** para determinar qué bloque de permisos corresponde a un usuario.

---

# 2. Propietario (`owner`)

Todo archivo/directorio tiene un **usuario propietario**.

Ejemplo:

```text
-rw-r--r-- 1 pepe pepe archivo.txt
               ^    ^
               |    |
          propietario grupo
```

El propietario es una identidad de usuario asociada al objeto.

Esto es independiente de los permisos:

- `chmod` modifica permisos.
- `chown` modifica propiedad/grupo.

Cambiar el propietario **no cambia automáticamente** `rwx`.

---

# 3. Grupos

Un grupo permite administrar permisos para varios usuarios al mismo tiempo.

Ejemplo conceptual:

```text
grupo = developers

alice
bob
carlos
```

Un archivo podría ser:

```text
-rw-r----- 1 alice developers archivo.txt
```

Interpretación:

```text
owner  = alice
group  = developers

rw-    → alice
r--    → miembros del grupo developers
---    → others
```

Así, los permisos de grupo permiten compartir acceso sin tener que configurar cada usuario individualmente.

---

# 4. UID y GID

Linux identifica internamente usuarios y grupos mediante números.

## UID

**UID = User ID**

Identifica numéricamente a un usuario.

Por ejemplo:

```text
Uid: (1000/pepe)
```

significa:

```text
UID = 1000
usuario = pepe
```

## GID

**GID = Group ID**

Identifica numéricamente a un grupo.

Por ejemplo:

```text
Gid: (1000/pepe)
```

significa:

```text
GID = 1000
grupo = pepe
```

`ls -l` normalmente muestra los nombres:

```text
pepe pepe
```

mientras que `stat` puede mostrar tanto ID como nombre.

---

# 5. `id`: conocer nuestra identidad y grupos

```bash
whoami
```

muestra el usuario actual.

```bash
id
```

muestra información como:

```text
uid=...
gid=...
groups=...
```

Esto permite saber:

- qué usuario somos;
- cuál es nuestro UID;
- cuál es nuestro GID;
- a qué grupos pertenecemos.

Un usuario puede pertenecer a varios grupos.

Esto será importante porque el acceso de tipo `group` depende de la pertenencia al grupo asociado al archivo.

---

# 6. `chown`: cambiar propietario y/o grupo

`chown` significa **change owner**.

Sintaxis:

```bash
chown usuario archivo
```

o:

```bash
chown usuario:grupo archivo
```

Ejemplo:

```bash
sudo chown alice:developers archivo.txt
```

establece:

```text
owner = alice
group = developers
```

Los permisos `rwx` permanecen iguales.

También puede cambiarse solamente el grupo mediante:

```bash
sudo chown :developers archivo.txt
```

En este caso:

```text
owner → no cambia
group → developers
```

Cambiar la propiedad normalmente requiere privilegios adecuados.

> No hacer cambios de `chown` sobre archivos del sistema durante el laboratorio.

---

# 7. `chgrp`: cambiar únicamente el grupo

`chgrp` significa **change group**.

Sintaxis:

```bash
chgrp grupo archivo
```

Ejemplo:

```bash
chgrp developers archivo.txt
```

Si antes teníamos:

```text
-rw-r--r-- 1 pepe pepe archivo.txt
```

podemos obtener:

```text
-rw-r--r-- 1 pepe developers archivo.txt
```

Lo que cambió fue:

```text
grupo:
pepe → developers
```

Los permisos siguen siendo:

```text
rw-r--r--
```

---

# 8. Diferencia entre `chmod`, `chown`, `chgrp` y `umask`

| Comando/concepto | Función |
|---|---|
| `chmod` | Cambia permisos `rwx` de un objeto existente |
| `chown` | Cambia propietario y/o grupo |
| `chgrp` | Cambia el grupo |
| `umask` | Define qué permisos se bloquean al crear nuevos objetos |

Modelo mental:

```text
                    ARCHIVO
                       |
        +--------------+--------------+
        |              |              |
        ↓              ↓              ↓
  propietario        grupo         permisos
        |              |              |
      chown        chown/chgrp       chmod
                                      ↑
                              umask al crear
```

La diferencia fundamental:

```text
chmod
→ "¿Qué permisos tiene?"

chown/chgrp
→ "¿De quién es y qué grupo tiene?"

umask
→ "¿Con qué permisos iniciales nace?"
```

---

# 9. ¿Dónde aparece esto en sistemas reales?

En servidores Linux es común encontrar archivos asociados a usuarios/grupos de servicios, por ejemplo:

```text
root
www-data
postgres
nginx
```

La propiedad y los permisos son importantes para aplicaciones, logs, configuraciones y otros recursos.

Ante un:

```text
Permission denied
```

una de las primeras comprobaciones debe ser:

```bash
ls -l archivo
```

y preguntarse:

```text
¿Quién es el propietario?
¿Qué grupo tiene?
¿Qué usuario ejecuta el proceso?
¿Qué permisos tiene?
```

Esto conecta directamente permisos con procesos y servicios.

---

# 10. ¿Qué es `umask`?

`umask` es una **máscara de permisos de creación**.

No representa directamente los permisos finales de un archivo.

Su función es indicar qué permisos **no deben concederse inicialmente** cuando un proceso crea un objeto.

Modelo:

```text
proceso
   ↓
crea objeto
   ↓
permisos base solicitados
   ↓
se aplica umask
   ↓
permisos iniciales finales
```

Por eso un archivo puede nacer con permisos como:

```text
-rw-r--r--
```

sin ejecutar `chmod` después de crearlo.

---

# 11. La `umask` pertenece al proceso

La `umask` es una configuración utilizada por el **proceso** al crear nuevos objetos.

Por eso no debemos pensar:

```text
umask = propiedad permanente del archivo
```

sino:

```text
proceso
  ↓
tiene una umask
  ↓
crea archivo/directorio
  ↓
la umask influye en sus permisos iniciales
```

Puede haber diferentes procesos con diferentes `umask`.

Esto será especialmente importante posteriormente con servicios, `systemd`, contenedores y aplicaciones.

---

# 12. Consultar la `umask`

Comando:

```bash
umask
```

En nuestro entorno obtuvimos:

```text
0022
```

Por tanto:

```text
umask = 0022
```

La `umask` no es el permiso final.

**No significa que el archivo nuevo tendrá `0022`.**

Es una máscara que bloquea determinados bits de permisos.

---

# 13. Relación numérica de permisos

Ya conocemos:

```text
r = 4
w = 2
x = 1
```

Por ejemplo:

```text
7 = rwx
6 = rw-
5 = r-x
4 = r--
3 = -wx
2 = -w-
1 = --x
0 = ---
```

Así:

```text
0644
```

significa:

```text
6 → rw-
4 → r--
4 → r--
```

o:

```text
rw- | r-- | r--
```

---

# 14. Archivos nuevos y `umask`

Para archivos regulares, el conjunto de permisos base normalmente se considera:

```text
0666
```

es decir:

```text
rw-rw-rw-
```

Los permisos de ejecución (`x`) no se conceden normalmente como parte de ese conjunto base.

Con nuestra:

```text
umask = 0022
```

el resultado típico es:

```text
0666
 ↓
umask 0022
 ↓
0644
```

Resultado:

```text
-rw-r--r--
```

Interpretación:

```text
owner  → rw-
group  → r--
others → r--
```

La idea importante es que `umask` **restringe** permisos iniciales; no agrega permisos.

---

# 15. Directorios nuevos y `umask`

Para directorios, el conjunto base normalmente es:

```text
0777
```

es decir:

```text
rwxrwxrwx
```

Con:

```text
umask = 0022
```

el resultado típico es:

```text
0777
 ↓
umask 0022
 ↓
0755
```

Resultado:

```text
drwxr-xr-x
```

Por eso, con una `umask` típica `022`, es común observar:

```text
archivo nuevo     → 0644 → -rw-r--r--
directorio nuevo  → 0755 → drwxr-xr-x
```

---

# 16. Experimento de `umask`

Primero se consulta la máscara actual:

```bash
umask
```

En nuestro caso:

```text
0022
```

Crear un archivo:

```bash
touch ~/labs/linux-month1/03-storage/permtest/umask-normal.txt
```

Comprobar:

```bash
ls -l ~/labs/linux-month1/03-storage/permtest/umask-normal.txt
```

Con `0022`, esperamos normalmente:

```text
-rw-r--r--
```

equivalente a:

```text
0644
```

---

# 17. Experimento con una `umask` más restrictiva

Se puede cambiar temporalmente la `umask` de la shell:

```bash
umask 077
```

Comprobar:

```bash
umask
```

Resultado:

```text
0077
```

Crear un archivo:

```bash
touch ~/labs/linux-month1/03-storage/permtest/umask-private.txt
```

Comprobar:

```bash
ls -l ~/labs/linux-month1/03-storage/permtest/umask-private.txt
```

El resultado esperado es:

```text
-rw-------
```

equivalente a:

```text
0600
```

Conceptualmente:

```text
owner  → rw-
group  → ---
others → ---
```

Es decir, solo el propietario tiene lectura/escritura.

---

# 18. Directorio con `umask 077`

Con:

```bash
umask 077
```

crear:

```bash
mkdir ~/labs/linux-month1/03-storage/permtest/umask-dir
```

y comprobar:

```bash
ls -ld ~/labs/linux-month1/03-storage/permtest/umask-dir
```

El resultado típico es:

```text
drwx------
```

equivalente a:

```text
0700
```

Esto demuestra nuevamente que la máscara afecta los permisos iniciales de objetos nuevos.

---

# 19. Restaurar la `umask`

Si la `umask` original era:

```text
0022
```

después del experimento:

```bash
umask 022
```

y comprobar:

```bash
umask
```

Debe volver a mostrar:

```text
0022
```

La configuración modificada afecta a la shell actual y a procesos hijos que hereden esa configuración.

---

# 20. Laboratorio de propiedad y grupos

## Consultar identidad

```bash
whoami
id
```

## Inspeccionar archivo

```bash
ls -l ~/labs/linux-month1/03-storage/permtest/archivo.txt
```

```bash
stat ~/labs/linux-month1/03-storage/permtest/archivo.txt
```

## Cambiar grupo

Usando un grupo al que pertenezcamos:

```bash
chgrp grupo archivo.txt
```

Comprobar:

```bash
ls -l archivo.txt
```

Observar:

```text
propietario → permanece igual
grupo       → cambia
permisos    → permanecen iguales
```

## Cambiar propietario/grupo

Sintaxis:

```bash
sudo chown usuario:grupo archivo.txt
```

Comprobar nuevamente con:

```bash
ls -l archivo.txt
```

---

# 21. Dos cambios de propiedad: qué debemos observar

El entregable pide documentar cambios de propiedad y explicar qué cambió en `ls -l`.

Ejemplo conceptual:

### Cambio 1 — grupo

Antes:

```text
-rw-r--r-- 1 pepe pepe archivo.txt
```

Después:

```text
-rw-r--r-- 1 pepe developers archivo.txt
```

Cambio:

```text
grupo:
pepe → developers
```

No cambiaron los permisos ni el propietario.

### Cambio 2 — propietario y grupo

Antes:

```text
-rw-r--r-- 1 pepe developers archivo.txt
```

Después:

```text
-rw-r--r-- 1 otro:usuario otro:grupo archivo.txt
```

La forma exacta depende de los usuarios/grupos disponibles en el laboratorio.

La idea que debe quedar documentada es:

```text
chown
→ propietario/grupo

chgrp
→ grupo

chmod
→ permisos
```

---

# 22. Ideas que NO hay que confundir

### Propiedad ≠ permisos

```text
pepe pepe
```

no son permisos.

Son:

```text
propietario grupo
```

Mientras:

```text
rw-r--r--
```

son permisos.

---

### `umask` ≠ permisos finales

```text
0022
```

no significa que el archivo tendrá permisos `0022`.

Es una máscara utilizada durante la creación.

Por ejemplo:

```text
archivo:
0666 + umask 0022 → 0644 (típicamente)

directorio:
0777 + umask 0022 → 0755 (típicamente)
```

---

### `chown` ≠ `chmod`

```text
chown
→ cambia de quién es

chmod
→ cambia qué puede hacer cada categoría
```

---

### `w` del archivo ≠ `w` del directorio

Esto viene del día anterior y es importante conservarlo:

```text
w en archivo
→ modificar contenido

w en directorio
→ modificar entradas:
   crear
   borrar
   renombrar
```

Por eso eliminar un archivo depende de los permisos del directorio que contiene su entrada.

---

# 23. Modelo mental completo

Un objeto Linux puede verse así:

```text
                    ARCHIVO / DIRECTORIO
                             |
            +----------------+----------------+
            |                |                |
            ↓                ↓                ↓
       propietario         grupo           permisos
            |                |                |
          UID              GID             rwx/u/g/o
```

Cuando un proceso intenta acceder:

```text
proceso
   ↓
¿qué usuario soy?
   ↓
¿soy el propietario?
   ↓
¿pertenezco al grupo correspondiente?
   ↓
¿qué bloque de permisos corresponde?
   ↓
¿la operación requiere r, w o x?
   ↓
¿está permitido?
```

Cuando crea un objeto:

```text
proceso
   ↓
umask
   ↓
permisos base
   ↓
permisos iniciales del objeto
```

Cuando queremos modificar la configuración posteriormente:

```text
chmod
→ permisos

chown
→ propietario/grupo

chgrp
→ grupo
```

---

# 24. Comandos principales del día

```bash
# Usuario actual
whoami

# UID, GID y grupos
id

# Ver permisos, propietario y grupo
ls -l archivo.txt

# Ver el propio directorio
ls -ld directorio/

# Ver metadatos detallados
stat archivo.txt

# Cambiar propietario
sudo chown usuario archivo.txt

# Cambiar propietario y grupo
sudo chown usuario:grupo archivo.txt

# Cambiar solamente el grupo
chown :grupo archivo.txt

# Cambiar grupo
chgrp grupo archivo.txt

# Consultar umask
umask

# Cambiar umask de la shell actual
umask 077

# Restaurar una umask de 022
umask 022

# Crear archivo
touch archivo.txt

# Crear directorio
mkdir directorio/
```

---

# 25. Conclusión

Hoy completamos la parte que faltaba en el modelo de permisos de Linux.

Ya no debemos pensar solamente:

```text
archivo → rwx
```

sino:

```text
archivo
   |
   +-- propietario
   |
   +-- grupo
   |
   +-- permisos
   |
   +-- otros metadatos
```

Los permisos se aplican según la relación entre el usuario que ejecuta el proceso y la propiedad del objeto:

```text
owner → permisos de user
group → permisos de group
others → permisos de others
```

Los comandos principales tienen responsabilidades diferentes:

```text
chmod  → permisos
chown  → propietario/grupo
chgrp  → grupo
umask  → permisos iniciales al crear
```

La `umask` que tenemos en nuestro entorno es:

```text
0022
```

y debe entenderse como **un filtro/máscara de creación**, no como los permisos finales.

Con una `umask` `0022`, típicamente:

```text
archivo nuevo
0666 → 0644

directorio nuevo
0777 → 0755
```

Finalmente, el concepto más importante para llevarse del día es:

> **Los permisos responden a "qué puede hacer cada categoría"; la propiedad y los grupos determinan quién cae dentro de esas categorías; y `umask` influye en los permisos con los que nacen los objetos nuevos.**
