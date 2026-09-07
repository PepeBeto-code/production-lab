# W4D3 — Journald vs `/var/log`: dos fuentes de evidencia

## 1. Idea principal

Los logs no necesariamente se encuentran en un único lugar ni tienen todos el mismo propósito.

Durante el troubleshooting podemos tener diferentes **fuentes de evidencia**, entre ellas:

* `journald`
* archivos dentro de `/var/log/`

La fuente que conviene consultar depende de **qué información estamos intentando obtener**.

El objetivo de hoy fue aprender a diferenciar estas fuentes y entender qué evidencia aporta cada una.

---

## 2. Journald

`journald` es el componente de `systemd` encargado de recopilar y administrar registros.

Los registros pueden consultarse mediante:

```bash
journalctl
```

Para consultar específicamente los registros asociados con nginx:

```bash
journalctl -u nginx
```

La opción:

```text
-u nginx
```

permite limitar la consulta a la unidad `nginx`.

También podemos limitar por cantidad:

```bash
journalctl -u nginx -n 100 --no-pager
```

Donde:

* `-u nginx` → registros de nginx.
* `-n 100` → últimas 100 entradas.
* `--no-pager` → muestra directamente la salida en la terminal.

Esto continúa el modelo aprendido anteriormente:

```text
journalctl
 ↓
servicio
 ↓
tiempo
 ↓
pistas
 ↓
contexto
 ↓
correlación
```

---

## 3. `/var/log`

`/var/log` es un directorio donde pueden existir diferentes archivos de registro.

No es un único log.

Dentro pueden existir logs de diferentes servicios y componentes.

En nginx encontramos normalmente:

```text
/var/log/nginx/
├── access.log
└── error.log
```

La existencia exacta de archivos depende de la configuración y del sistema.

---

## 4. `access.log`

Archivo:

```text
/var/log/nginx/access.log
```

Está orientado principalmente a registrar las **peticiones que recibe nginx**.

Puede proporcionar evidencia sobre cosas como:

* qué solicitudes llegaron;
* qué rutas fueron solicitadas;
* qué métodos HTTP se utilizaron;
* qué respuestas produjo nginx.

Ejemplos conceptuales:

```text
GET /
GET /login
POST /login
GET /api/users
```

Por lo tanto:

```text
access.log
     ↓
¿qué peticiones recibió nginx?
```

Es especialmente útil cuando necesitamos reconstruir la actividad que estaba ocurriendo en el servidor.

---

## 5. `error.log`

Archivo:

```text
/var/log/nginx/error.log
```

Está orientado a registrar errores y otras condiciones anómalas relacionadas con nginx.

Puede contener pistas como:

```text
connection failed
permission denied
upstream timed out
```

Por lo tanto:

```text
error.log
     ↓
¿qué problemas o condiciones anómalas registró nginx?
```

Es una fuente especialmente relevante cuando investigamos por qué nginx tuvo algún problema.

Sin embargo, encontrar un error no significa automáticamente haber encontrado la causa raíz.

La línea todavía debe analizarse en contexto y relacionarse con timestamps y otros eventos.

---

## 6. Comandos utilizados

Para inspeccionar los archivos existentes:

```bash
ls -lah /var/log/nginx/
```

Para revisar las últimas 100 líneas del log de errores:

```bash
sudo tail -n 100 /var/log/nginx/error.log
```

Para revisar las últimas 100 líneas del log de acceso:

```bash
sudo tail -n 100 /var/log/nginx/access.log
```

Para comparar con los registros de `journald`:

```bash
journalctl -u nginx -n 100 --no-pager
```

---

## 7. `tail`

`tail` permite mostrar las últimas líneas de un archivo.

La opción:

```text
-n 100
```

indica que queremos mostrar las últimas 100 líneas.

Por ejemplo:

```bash
sudo tail -n 100 /var/log/nginx/error.log
```

Esto permite reducir la cantidad de información que debemos revisar en lugar de leer un archivo completo potencialmente muy grande.

Es otra forma de aplicar el principio aprendido anteriormente:

> No leer logs indiscriminadamente; reducir la cantidad de evidencia hasta obtener información útil.

---

## 8. Log rotation

Los archivos de logs pueden crecer continuamente.

Para evitar que un único archivo crezca indefinidamente, puede utilizarse **log rotation**.

Podemos encontrar archivos como:

```text
error.log
error.log.1
error.log.2.gz
```

Conceptualmente:

```text
error.log
    ↓
log actual

error.log.1
    ↓
generación anterior

error.log.2.gz
    ↓
generación más antigua y comprimida
```

La extensión:

```text
.gz
```

indica que el archivo está comprimido mediante gzip.

Esto es importante para troubleshooting porque el evento que buscamos puede no encontrarse en el archivo actual.

Por ejemplo, si el incidente ocurrió anteriormente, la evidencia podría estar en:

```text
error.log.1
```

o en algún archivo rotado y comprimido.

Por lo tanto:

```text
no encontrar el evento en error.log
```

no significa necesariamente:

```text
el evento nunca ocurrió
```

También debemos considerar la rotación y la retención de logs.

---

## 9. Journald vs archivos de `/var/log`

Las dos fuentes pueden aportar evidencia diferente.

### Journald

Se consulta mediante:

```bash
journalctl
```

Puede ser especialmente útil para investigar el comportamiento de un servicio como unidad dentro del sistema.

Ejemplo:

```bash
journalctl -u nginx
```

Permite combinar fácilmente filtros como:

```bash
-u nginx
--since
--until
-n
```

y las búsquedas mediante `grep`.

---

### `error.log`

Está enfocado en:

```text
errores y condiciones anómalas de nginx
```

Puede ser especialmente útil para investigar pistas relacionadas con la causa de un problema.

---

### `access.log`

Está enfocado en:

```text
peticiones recibidas por nginx
```

Puede ser especialmente útil para reconstruir qué solicitudes estaban llegando al servidor.

---

## 10. No son fuentes equivalentes

No debemos asumir:

```text
journald = error.log = access.log
```

Cada fuente puede tener información, formato y propósito diferentes.

La pregunta correcta no es:

> "¿Cuál es el mejor lugar para buscar logs?"

La pregunta correcta es:

> **"¿Qué evidencia necesito y qué fuente puede proporcionármela?"**

---

## 11. Correlación entre fuentes

Las diferentes fuentes pueden utilizarse juntas.

Por ejemplo:

```text
access.log
10:31:01 → POST /login
```

Después:

```text
error.log
10:31:02 → upstream timed out
```

Y podemos encontrar información relacionada en:

```text
journald
10:31:02 → evento relacionado con nginx
```

La secuencia temporal puede proporcionar mucha más información que encontrar simplemente una línea que contenga:

```text
timeout
```

Esto conecta directamente con lo aprendido el día anterior:

```text
línea encontrada
 ↓
contexto
 ↓
timestamp
 ↓
correlación
 ↓
hipótesis
 ↓
comprobación
 ↓
conclusión
```

---

## 12. ¿Cuándo conviene mirar cada fuente?

No existe una regla universal.

### Journald

Conviene cuando queremos investigar eventos relacionados con el servicio y utilizar los filtros del journal para acotar:

```text
servicio
+
tiempo
+
contenido
```

Ejemplo:

```bash
journalctl -u nginx --since "30 min ago" --no-pager
```

---

### `error.log`

Conviene cuando queremos investigar específicamente los errores y condiciones anómalas que nginx registró.

```bash
sudo tail -n 100 /var/log/nginx/error.log
```

---

### `access.log`

Conviene cuando necesitamos saber qué peticiones estaba recibiendo nginx.

```bash
sudo tail -n 100 /var/log/nginx/access.log
```

---

## 13. Modelo mental

El modelo de troubleshooting se amplía:

```text
INCIDENTE
   ↓
¿QUÉ QUIERO SABER?
   ↓
IDENTIFICAR FUENTE DE EVIDENCIA
   ↓
CONSULTAR
   ↓
FILTRAR
   ↓
ANALIZAR CONTEXTO
   ↓
ANALIZAR TIMESTAMPS
   ↓
CORRELACIONAR CON OTRAS FUENTES
   ↓
COMPROBAR HIPÓTESIS
   ↓
CONCLUSIÓN
```

No debemos pensar:

```text
journald = bueno
/var/log = malo
```

ni:

```text
/var/log = bueno
journald = malo
```

Debemos pensar:

```text
fuente de evidencia
        ↓
qué información contiene
        ↓
qué pregunta estoy intentando responder
```

---

## 14. Conclusión del día

Hoy aprendí que los logs pueden existir en diferentes fuentes y que cada fuente puede aportar evidencia distinta.

`journald` es el sistema de registro administrado por `systemd` y se consulta mediante `journalctl`.

`/var/log` es un directorio que contiene diferentes archivos de logs.

En nginx:

```text
access.log
```

se enfoca principalmente en las peticiones recibidas.

```text
error.log
```

se enfoca principalmente en errores y condiciones anómalas.

También aprendí el concepto de **log rotation**, mediante el cual los logs actuales pueden convertirse en generaciones anteriores como:

```text
error.log.1
error.log.2.gz
```

Esto es importante porque una investigación no debe limitarse necesariamente al archivo actual.

### Principio del día

> **No existe una única fuente universal de evidencia. Hay que elegir la fuente según la pregunta que estamos intentando responder y, cuando sea necesario, correlacionar varias fuentes.**
