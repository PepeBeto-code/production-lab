# W4D2 — Encontrar el error real: búsqueda y filtros

## 1. Idea principal

> **Que exista mucho texto en los logs no significa que exista mucha evidencia útil.**

En troubleshooting no se trata de leer todos los logs. Se trata de **reducirlos hasta encontrar las líneas que puedan aportar evidencia sobre el incidente**.

El objetivo de hoy fue aprender a buscar pistas dentro de los registros utilizando `grep`.

---

## 2. De dónde venimos

El día anterior aprendimos a reducir los logs mediante:

```bash
journalctl -u <unidad>
```

→ filtrar por servicio/unidad.

```bash
--since / --until
```

→ filtrar por ventana temporal.

```bash
-n
```

→ limitar la cantidad de entradas.

```bash
-o short-iso
```

→ facilitar el análisis de timestamps.

Hoy agregamos:

```text
journalctl
    ↓
filtrar por servicio
    ↓
filtrar por tiempo
    ↓
buscar patrones/pistas
    ↓
analizar evidencia
```

---

# 3. grep

`grep` permite **buscar texto que coincida con un patrón** dentro de una entrada de texto.

Ejemplo:

```bash
grep "failed"
```

Busca líneas que contengan:

```text
failed
```

En troubleshooting sirve para reducir muchas líneas de logs a aquellas que contienen posibles indicadores de problemas.

---

# 4. Pipe `|`

El carácter:

```bash
|
```

se llama **pipe** o tubería.

Conecta la salida de un comando con la entrada de otro.

Ejemplo:

```bash
journalctl -u nginx | grep "failed"
```

Flujo:

```text
journalctl -u nginx
        ↓
     salida
        ↓
       |
        ↓
      grep
        ↓
líneas que coinciden
```

El pipe **no modifica los logs**. Solamente conecta comandos.

---

# 5. grep -i

```bash
grep -i
```

La opción:

```text
-i
```

hace que la búsqueda ignore diferencias entre mayúsculas y minúsculas.

Por ejemplo:

```bash
grep -i "error"
```

puede encontrar:

```text
error
ERROR
Error
```

Esto evita perder una pista porque el programa escribió la palabra con otra combinación de mayúsculas/minúsculas.

---

# 6. grep -E

```bash
grep -E
```

La opción `-E` permite utilizar **expresiones regulares extendidas**.

Para lo estudiado hoy, lo importante es poder utilizar:

```text
A|B|C
```

El `|` dentro del patrón significa:

```text
A O B O C
```

Ejemplo:

```bash
grep -E "error|failed"
```

Busca líneas que contengan `error` **o** `failed`.

---

# 7. grep -iE

Podemos combinar ambas opciones:

```bash
grep -iE
```

```text
-i → ignora mayúsculas/minúsculas
-E → permite expresiones regulares extendidas
```

Ejemplo:

```bash
grep -iE "error|failed"
```

Busca cualquiera de los patrones sin importar mayúsculas/minúsculas.

---

# 8. Comando principal del día

```bash
journalctl -u nginx --no-pager | grep -iE "error|fail|timeout|denied|exception|critical"
```

### Descomposición

```text
journalctl
```

Consulta el journal.

```text
-u nginx
```

Limita los registros a la unidad `nginx`.

```text
--no-pager
```

Muestra la salida directamente en la terminal.

```text
|
```

Pasa la salida de `journalctl` a `grep`.

```text
grep
```

Busca líneas que coincidan con un patrón.

```text
-i
```

Ignora mayúsculas/minúsculas.

```text
-E
```

Permite utilizar varios patrones mediante expresiones regulares extendidas.

```text
"error|fail|timeout|denied|exception|critical"
```

Busca cualquiera de esas pistas.

---

# 9. Pistas que buscamos

Estas palabras **no son automáticamente la causa raíz**. Son indicadores que ayudan a localizar líneas que merecen investigación.

### `error`

Indica que el programa registró algún tipo de error.

Es una pista general y normalmente necesita contexto.

### `fail`

Relacionada con una operación que no tuvo éxito.

Ejemplo:

```text
connection failed
```

### `timeout`

Indica que una operación esperó una respuesta durante cierto tiempo y no la obtuvo dentro del límite esperado.

Puede apuntar, dependiendo del contexto, a problemas de comunicación, red, dependencias o respuestas demasiado lentas.

### `denied`

Indica que una operación fue rechazada.

Ejemplo:

```text
permission denied
```

Puede llevar a investigar permisos o acceso a algún recurso.

### `exception`

Indica que el programa registró una condición excepcional durante su ejecución.

Puede ser una pista importante, pero necesita contexto.

### `critical`

Indica una condición que el propio programa considera particularmente grave.

También es una pista, no una prueba automática de causa raíz.

---

# 10. `fail` en lugar de solamente `failed`

El patrón utilizado es:

```text
fail
```

en lugar de:

```text
failed
```

porque permite encontrar diferentes palabras que contienen esa parte:

```text
failed
failure
failing
```

Es una estrategia útil cuando buscamos una familia de términos relacionados.

---

# 11. Acotar por tiempo + buscar pistas

Podemos combinar lo aprendido ayer con lo aprendido hoy:

```bash
journalctl -u nginx --since "30 min ago" --no-pager \
| grep -iE "error|fail|timeout|denied|exception|critical"
```

Este comando realiza tres reducciones:

```text
TODOS LOS LOGS
      ↓
nginx
      ↓
últimos 30 minutos
      ↓
líneas con posibles pistas
```

Por lo tanto:

```text
-u
```

responde:

> ¿De qué servicio quiero evidencia?

```text
--since
```

responde:

> ¿De qué periodo quiero evidencia?

```text
grep
```

responde:

> ¿Qué tipo de líneas me interesa revisar?

---

# 12. La `\`

En:

```bash
journalctl -u nginx --since "30 min ago" --no-pager \
| grep -iE "error|fail|timeout|denied|exception|critical"
```

la:

```text
\
```

permite continuar el mismo comando en la siguiente línea.

También se puede escribir todo en una sola línea:

```bash
journalctl -u nginx --since "30 min ago" --no-pager | grep -iE "error|fail|timeout|denied|exception|critical"
```

El significado es el mismo.

---

# 13. `error.log` vs `access.log`

Nginx puede registrar información con propósitos diferentes.

## error.log

Orientado a:

> **qué salió mal / pistas relacionadas con la causa**

Puede contener información sobre errores y otras condiciones anómalas.

Ejemplo conceptual:

```text
connection failed
permission denied
upstream timed out
```

Cuando investigamos por qué nginx tuvo un problema, `error.log` puede ser especialmente relevante.

---

## access.log

Orientado a:

> **qué peticiones recibió nginx**

Ejemplo conceptual:

```text
GET /
GET /login
POST /login
GET /api/users
```

Sirve para saber qué solicitudes llegaron al servidor.

---

## Diferencia para memorizar

```text
error.log
    ↓
qué salió mal
    ↓
pistas de causa
```

```text
access.log
    ↓
qué peticiones recibió nginx
```

---

# 14. Ruido vs evidencia

Un log puede contener miles de líneas.

No todas tienen el mismo valor diagnóstico.

Por ejemplo:

```text
worker process started
request received
client connected
```

pueden ser información normal.

Mientras que:

```text
connection failed
permission denied
upstream timeout
```

pueden ser pistas que merecen investigación.

Por eso:

> **Filtrar logs es convertir una gran cantidad de texto en un conjunto pequeño de candidatos a evidencia.**

---

# 15. Encontrar un error ≠ encontrar la causa raíz

Esto es fundamental.

Encontrar:

```text
ERROR
```

no demuestra que hayamos encontrado la causa raíz.

El filtro:

```bash
grep -iE "error|fail|timeout|denied|exception|critical"
```

funciona como una herramienta de **búsqueda/triage inicial**.

La línea encontrada todavía debe analizarse:

```text
línea encontrada
      ↓
leer contexto
      ↓
ver timestamp
      ↓
comparar con el incidente
      ↓
correlacionar eventos
      ↓
formular/comprobar hipótesis
```

Por ejemplo:

```text
07:31:02 → POST /login
07:31:03 → upstream timeout
07:31:04 → request failed
```

La secuencia temporal aporta más información que simplemente encontrar la palabra `timeout`.

---

# 16. Modelo mental de hoy

No pensar:

> "Tengo que buscar la palabra error."

Pensar:

> **"Tengo mucha evidencia y necesito reducirla hasta encontrar eventos que puedan explicar el síntoma."**

Modelo:

```text
INCIDENTE
   ↓
IDENTIFICAR SERVICIO
   ↓
journalctl -u
   ↓
ACOTAR TIEMPO
   ↓
--since / --until
   ↓
BUSCAR PISTAS
   ↓
grep -iE
   ↓
ANALIZAR CONTEXTO
   ↓
ANALIZAR TIMESTAMPS
   ↓
CORRELACIONAR
   ↓
COMPROBAR HIPÓTESIS
   ↓
CONCLUSIÓN
```

---

# 17. Relación con el modelo de troubleshooting

El modelo general sigue siendo:

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

Lo nuevo de hoy está principalmente en cómo encontrar la **evidencia útil**:

```text
journalctl
    ↓
servicio
    ↓
tiempo
    ↓
grep
    ↓
pistas
```

---

# 18. Comandos importantes

### Consultar logs de una unidad

```bash
journalctl -u <unidad>
```

### Consultar sin paginador

```bash
journalctl -u <unidad> --no-pager
```

### Últimos 30 minutos

```bash
journalctl -u <unidad> --since "30 min ago"
```

### Buscar una palabra

```bash
journalctl -u <unidad> | grep "failed"
```

### Ignorar mayúsculas/minúsculas

```bash
journalctl -u <unidad> | grep -i "error"
```

### Buscar múltiples pistas

```bash
journalctl -u <unidad> | grep -iE "error|fail|timeout|denied|exception|critical"
```

### Buscar múltiples pistas dentro de una ventana temporal

```bash
journalctl -u <unidad> --since "30 min ago" --no-pager \
| grep -iE "error|fail|timeout|denied|exception|critical"
```

---

# 19. Entregable del día

Archivo:

```text
W4D2_log_search.md
```

Debe contener:

1. **5 líneas/pistas relevantes**
2. **Qué ocurrió**
3. **Por qué esa línea es relevante**

La idea no es simplemente copiar cinco líneas que contengan `error`.

Para cada pista hay que poder explicar:

```text
Pista encontrada
      ↓
¿Qué ocurrió?
      ↓
¿Por qué podría ser relevante?
```

---

# 20. Resumen final

### Lo nuevo de hoy

* `grep` permite buscar patrones dentro de texto.
* `|` conecta la salida de un comando con otro.
* `grep -i` ignora mayúsculas/minúsculas.
* `grep -E` permite utilizar patrones extendidos.
* `A|B|C` significa A O B O C dentro del patrón.
* `error`, `fail`, `timeout`, `denied`, `exception` y `critical` son pistas útiles para una búsqueda inicial.
* `journalctl` y `grep` pueden combinarse.
* También podemos combinar filtrado por servicio + tiempo + contenido.
* `error.log` se enfoca en errores/pistas de problemas.
* `access.log` se enfoca en las peticiones recibidas.
* Encontrar una línea con `error` **no significa automáticamente haber encontrado la causa raíz**.
* Las líneas deben analizarse en contexto y correlacionarse con timestamps y otros eventos.

## Principio del día

> **No leas los logs indiscriminadamente. Filtra la evidencia hasta encontrar las líneas que puedan explicar el síntoma.**
