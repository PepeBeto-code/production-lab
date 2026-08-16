# Día 11 — HTTP profundo con evidencia: status codes, headers y latencia

## Objetivo del laboratorio

El objetivo de este laboratorio es profundizar en HTTP a partir del punto exacto donde terminó el Día 10.

En el Día 10 se aprendió a distinguir entre:

```text
DNS
 ↓
IP
 ↓
TCP connect
 ↓
conexión TCP establecida
 ↓
HTTP request
 ↓
HTTP response
 ↓
status code / body
```

La idea fundamental del Día 10 fue que una petición HTTP no puede ocurrir si antes no se consiguió establecer la conexión TCP.

Por ejemplo:

```text
127.0.0.1:8080
     │
     ▼
TCP connect ✓
     │
     ▼
HTTP GET /
     │
     ▼
HTTP 200 OK
```

mientras que:

```text
127.0.0.1:9999
     │
     ▼
TCP connect ✗
     │
     ▼
Connection refused
     │
     ▼
HTTP no ocurrió
```

En este día se añade una capa de análisis más profunda.

Ya no solamente queremos responder:

> ¿TCP conecta?

Ahora queremos responder:

> ¿Qué ocurrió después de establecer TCP?

Y específicamente:

* ¿Qué request HTTP envió el cliente?
* ¿Qué response HTTP recibió?
* ¿Qué status code devolvió el servidor?
* ¿Qué headers contiene la respuesta?
* ¿Qué tamaño tiene el body?
* ¿Cuánto tiempo tardó la operación completa?
* ¿Qué podemos inferir de toda esa evidencia durante un diagnóstico?

El objetivo final es pasar de una observación superficial como:

```text
curl funcionó
```

a una observación mucho más precisa:

```text
TCP connection establecida.
Se envió una petición HTTP GET.
El servidor respondió mediante HTTP.
La respuesta tuvo status 200.
La respuesta contenía determinados headers.
El body tenía determinado tamaño.
La operación completa tardó aproximadamente X milisegundos.
```

Esto constituye una base importante para troubleshooting y posteriormente para observabilidad y análisis de performance.

---

# 1. Punto de partida: lo aprendido en el Día 10

En el Día 10 se estudió que una comunicación hacia un servicio no es una única operación.

Conceptualmente:

```text
hostname
   │
   ▼
DNS
   │
   ▼
IP
   │
   ▼
IP:puerto
   │
   ▼
TCP connect
   │
   ▼
TCP ESTABLISHED
   │
   ▼
HTTP request
   │
   ▼
HTTP response
   │
   ├── status code
   ├── headers
   └── body
```

No todas las conexiones requieren DNS.

Cuando se utiliza directamente:

```text
127.0.0.1
```

no es necesario resolver un hostname mediante DNS.

Por ejemplo:

```bash
curl http://127.0.0.1:8080
```

puede comenzar directamente con la conexión hacia:

```text
127.0.0.1:8080
```

En cambio:

```bash
curl http://localhost:8080
```

requiere resolver `localhost` según los mecanismos de resolución del sistema.

---

# 2. La distinción fundamental: TCP vs HTTP

Esta distinción continúa siendo el fundamento de todo el laboratorio.

TCP y HTTP pertenecen a capas diferentes.

Podemos representar la relación de manera simplificada:

```text
HTTP
 │
 │ bytes de HTTP
 ▼
TCP
 │
 │ segmentos TCP
 ▼
IP
```

HTTP es un protocolo de aplicación.

TCP es un protocolo de transporte.

TCP no sabe que los bytes transportados representan:

```text
GET /
Host:
User-Agent:
```

Para TCP esos son simplemente datos.

El significado de esos bytes como una petición HTTP lo proporciona HTTP.

Por eso:

```text
TCP
 │
 └── transporta bytes
```

mientras:

```text
HTTP
 │
 ├── GET
 ├── path
 ├── headers
 ├── status code
 └── body
```

Esta separación es fundamental para troubleshooting.

Si TCP falla:

```text
TCP connect
     X
```

HTTP todavía no comenzó.

Si TCP funciona:

```text
TCP connect
     ✓
     │
     ▼
HTTP
```

entonces ya podemos analizar HTTP.

---

# 3. De "conecta o no conecta" a "¿qué ocurrió después?"

En el Día 10 la pregunta principal era:

> ¿Podemos establecer la conexión TCP?

Ahora la pregunta se amplía:

> Una vez establecida la conexión TCP, ¿qué ocurrió a nivel HTTP?

Por ejemplo, estas dos situaciones son completamente diferentes.

## Caso 1: fallo TCP

```text
curl
 │
 ▼
127.0.0.1:9999
 │
 ▼
TCP connect
 │
 X
 │
Connection refused
```

No existe una response HTTP.

No tenemos:

```text
HTTP/1.1 404
```

ni:

```text
HTTP/1.1 500
```

porque nunca se llegó hasta HTTP.

---

## Caso 2: error HTTP

Supongamos:

```text
curl http://127.0.0.1:8080/no-existe
```

y obtenemos:

```text
HTTP/1.0 404 Not Found
```

Aquí sabemos que:

```text
TCP connect ✓
HTTP request ✓
HTTP response ✓
```

El problema ocurrió después de establecer TCP.

Esta distinción es una de las ideas principales que debe conservarse para futuros diagnósticos.

---

# 4. HTTP request y HTTP response

Una comunicación HTTP tiene, conceptualmente, dos partes principales:

```text
request
response
```

La request es enviada por el cliente.

La response es enviada por el servidor.

Por ejemplo:

```text
CLIENTE                         SERVIDOR

HTTP request
────────────────────────────────►

HTTP response
◄────────────────────────────────
```

Una petición podría comenzar con:

```text
GET / HTTP/1.1
```

y la respuesta podría comenzar con:

```text
HTTP/1.0 200 OK
```

La petición y la respuesta son mensajes HTTP diferentes.

---

# 5. HTTP request

Una petición HTTP puede contener elementos como:

```text
GET / HTTP/1.1
Host: 127.0.0.1:8080
User-Agent: curl/8.18.0
Accept: */*
```

Podemos dividir la primera línea:

```text
GET / HTTP/1.1
│   │ │
│   │ └── versión HTTP
│   │
│   └──── recurso solicitado
│
└──────── método HTTP
```

## GET

`GET` es un método HTTP.

Indica que el cliente está solicitando obtener un recurso.

## `/`

Representa la ruta solicitada.

En:

```text
GET / HTTP/1.1
```

el cliente está solicitando el recurso raíz `/`.

## HTTP/1.1

Indica la versión del protocolo HTTP utilizada en esa petición.

---

# 6. Headers HTTP

Después de la línea inicial pueden aparecer headers.

Por ejemplo:

```text
Host: 127.0.0.1:8080
User-Agent: curl/8.18.0
Accept: */*
```

Los headers son metadatos asociados con la petición o respuesta HTTP.

Es importante recordar:

```text
TCP no interpreta estos headers como HTTP.
```

TCP simplemente transporta los bytes.

HTTP es quien define que:

```text
Host:
User-Agent:
Accept:
```

tienen determinados significados.

---

# 7. `curl -i`

La opción:

```bash
curl -i URL
```

hace que curl incluya los headers de la respuesta en la salida.

Por ejemplo:

```bash
curl -i http://127.0.0.1:8080/
```

puede producir una estructura semejante a:

```text
HTTP/1.0 200 OK
Server: SimpleHTTP/0.6 Python/3.14.4
Date: ...
Content-type: text/html; charset=utf-8
Content-Length: 3835

<!DOCTYPE HTML>
<html>
...
</html>
```

La diferencia principal respecto a ejecutar simplemente:

```bash
curl http://127.0.0.1:8080/
```

es que `-i` muestra también los headers de la response.

Sin `-i`:

```text
body
```

Con `-i`:

```text
status line
headers
body
```

La línea vacía separa los headers del body:

```text
Content-Length: 3835

<!DOCTYPE HTML>
```

Por tanto, podemos pensar:

```text
HTTP response
 │
 ├── status line
 │
 ├── headers
 │
 └── body
```

---

# 8. `curl -v`

La opción:

```bash
curl -v URL
```

significa `verbose`.

Su objetivo es mostrar información detallada de la operación.

Es especialmente útil para troubleshooting porque permite observar diferentes etapas de la comunicación.

En el Día 10 se observó, por ejemplo:

```text
* Trying 127.0.0.1:8080...
```

Después:

```text
* Established connection to 127.0.0.1
```

Después:

```text
> GET / HTTP/1.1
```

Y posteriormente:

```text
< HTTP/1.0 200 OK
```

La salida de `curl -v` utiliza símbolos que ayudan a distinguir la dirección de los datos.

## `*`

Información de diagnóstico de curl.

Ejemplo:

```text
* Trying 127.0.0.1:8080...
```

## `>`

Datos enviados por el cliente.

Ejemplo:

```text
> GET / HTTP/1.1
> Host: 127.0.0.1:8080
> User-Agent: curl/8.18.0
```

## `<`

Datos recibidos del servidor.

Ejemplo:

```text
< HTTP/1.0 200 OK
< Server: SimpleHTTP/0.6 Python/3.14.4
< Content-Length: 3835
```

Por tanto:

```text
* = información de curl

> = cliente → servidor

< = servidor → cliente
```

Esta distinción es muy útil al leer una salida verbose.

---

# 9. Diferencia conceptual entre `curl -i` y `curl -v`

No deben tratarse como sinónimos.

## `curl -i`

Está orientado principalmente a observar la response HTTP:

```text
status
headers
body
```

## `curl -v`

Está orientado a obtener información detallada de toda la operación, incluyendo aspectos de conexión y los datos HTTP enviados y recibidos.

Mentalmente:

```text
curl -i
    │
    └── ¿qué respondió HTTP?

curl -v
    │
    └── ¿qué ocurrió durante la comunicación?
```

Para troubleshooting, `curl -v` suele proporcionar una visión mucho más amplia.

---

# 10. Status line

Una response HTTP comienza con una status line.

Por ejemplo:

```text
HTTP/1.1 200 OK
```

Puede dividirse así:

```text
HTTP/1.1 200 OK
   │      │    │
   │      │    └── texto descriptivo
   │      │
   │      └─────── status code
   │
   └────────────── versión HTTP
```

El elemento más importante para este laboratorio es:

```text
200
```

el status code.

---

# 11. Status codes

Los status codes permiten que el servidor comunique de manera estandarizada el resultado de una petición HTTP.

Las principales familias son:

```text
1xx → información
2xx → éxito
3xx → redirección
4xx → problema asociado con la solicitud
5xx → error del servidor
```

No todos los códigos posibles serán estudiados hoy.

Los relevantes para este laboratorio son:

```text
200
301
404
500
```

---

# 12. `200 OK`

El código:

```text
200
```

pertenece a la familia `2xx`.

Representa una respuesta exitosa.

Por ejemplo:

```text
HTTP/1.1 200 OK
```

En nuestro laboratorio, el flujo sería:

```text
TCP connection
     ✓
     │
     ▼
HTTP request
     ✓
     │
     ▼
HTTP response
     ✓
     │
     ▼
200 OK
```

Por tanto, recibir un `200` demuestra que hemos llegado mucho más allá de TCP.

---

# 13. Qué significa realmente un 200

Un `200 OK` significa, en términos generales, que la petición HTTP fue procesada correctamente.

No debe interpretarse como:

> "Toda la arquitectura está perfectamente sana."

El status code únicamente describe el resultado de esa operación HTTP según lo que el servidor decidió comunicar.

Por ejemplo, una arquitectura podría ser:

```text
cliente
   │
   ▼
load balancer
   │
   ▼
backend
   │
   ▼
database
```

Un backend podría devolver `200` aunque existan problemas en otros componentes que no afecten esa petición concreta.

Por eso:

```text
200
```

es evidencia útil, pero no constituye por sí mismo un diagnóstico completo del sistema.

---

# 14. `301 Moved Permanently`

El código:

```text
301
```

pertenece a la familia `3xx`.

Representa una redirección permanente.

Una response podría tener:

```text
HTTP/1.1 301 Moved Permanently
Location: https://example.com/
```

El header:

```text
Location:
```

indica la ubicación a la que se dirige al cliente.

Conceptualmente:

```text
CLIENTE
   │
   │ GET / 
   ▼
SERVIDOR
   │
   │ 301
   │ Location: ...
   ▼
CLIENTE
   │
   │ nueva request
   ▼
NUEVA UBICACIÓN
```

Una redirección puede afectar la latencia porque puede provocar una operación adicional.

De forma simplificada:

```text
request inicial
       │
       ▼
301 response
       │
       ▼
segunda request
       │
       ▼
segunda response
```

Por tanto, al analizar tiempos hay que saber si estamos observando únicamente una request o una secuencia que involucra redirecciones.

---

# 15. `404 Not Found`

El código:

```text
404
```

pertenece a la familia `4xx`.

Representa que el recurso solicitado no fue encontrado.

Por ejemplo:

```bash
curl -i http://127.0.0.1:8080/no-existe
```

puede devolver:

```text
HTTP/1.0 404 File not found
```

Lo importante para el diagnóstico es que existe una response HTTP.

Por tanto:

```text
TCP connect
     ✓
     │
     ▼
HTTP request
     ✓
     │
     ▼
HTTP response
     ✓
     │
     ▼
404
```

Un `404` no representa un fallo de TCP.

Al contrario: la existencia de una response HTTP demuestra que la comunicación llegó hasta el servidor HTTP.

---

# 16. `500 Internal Server Error`

El código:

```text
500
```

pertenece a la familia `5xx`.

Representa un error del lado del servidor durante el procesamiento de la petición.

Conceptualmente:

```text
TCP connect
     ✓
     │
     ▼
HTTP request
     ✓
     │
     ▼
servidor / aplicación
     │
     X
     │
     ▼
HTTP 500
```

La existencia de:

```text
HTTP/1.1 500 Internal Server Error
```

demuestra que se llegó a HTTP.

No significa que sepamos automáticamente qué causó el problema.

Para determinar la causa real podrían ser necesarios:

* logs;
* stack traces;
* métricas;
* tracing;
* información del backend;
* información de bases de datos;
* etc.

El status code localiza aproximadamente la etapa, pero no necesariamente proporciona la causa raíz.

---

# 17. Comparación de los casos

Una comparación importante es:

## Connection refused

```text
127.0.0.1:9999
       │
       ▼
TCP connect
       X
       │
       ▼
Connection refused
```

Aquí:

```text
TCP ✗
HTTP no ocurrió
```

## 404

```text
127.0.0.1:8080/no-existe
       │
       ▼
TCP connect
       ✓
       │
       ▼
HTTP request
       ✓
       │
       ▼
HTTP response
       ✓
       │
       ▼
404
```

Aquí:

```text
TCP ✓
HTTP ✓
recurso no encontrado
```

## 500

```text
127.0.0.1:8080/error
       │
       ▼
TCP connect
       ✓
       │
       ▼
HTTP request
       ✓
       │
       ▼
servidor
       X
       │
       ▼
500
```

Aquí:

```text
TCP ✓
HTTP ✓
procesamiento del servidor falló
```

Esta clasificación es fundamental para troubleshooting.

---

# 18. Header `Server`

Un response puede incluir:

```text
Server: SimpleHTTP/0.6 Python/3.14.4
```

`Server` es un response header que proporciona información sobre el software que generó la respuesta.

En el laboratorio del Día 10:

```text
Server: SimpleHTTP/0.6 Python/3.14.4
```

permitió identificar el servidor HTTP sencillo de Python utilizado en el experimento.

Sin embargo, este header no debe interpretarse como una descripción completa de toda la arquitectura.

Podría existir, por ejemplo:

```text
cliente
   │
   ▼
nginx
   │
   ▼
Spring Boot
   │
   ▼
PostgreSQL
```

En ese escenario, distintos componentes podrían participar en el procesamiento.

Por tanto:

```text
Server: ...
```

es una pieza de evidencia, no necesariamente una descripción completa de todos los componentes involucrados.

---

# 19. Header `Date`

Otro header observado fue:

```text
Date: Sat, 15 Aug 2026 16:06:49 GMT
```

Este header proporciona la fecha/hora asociada con la respuesta HTTP.

Puede ser útil para relacionar aproximadamente una respuesta con eventos registrados en logs.

Por ejemplo:

```text
HTTP response
Date: ...
```

puede compararse con timestamps de logs.

Sin embargo, al hacer correlaciones temporales en sistemas distribuidos hay que considerar:

* relojes de máquinas diferentes;
* sincronización de tiempo;
* zonas horarias;
* retrasos de red;
* diferencias entre el instante de generación y recepción.

Por tanto, `Date` es evidencia temporal útil, pero no debe interpretarse aisladamente como una medición exacta de latencia.

---

# 20. Header `Content-Length`

Un response puede contener:

```text
Content-Length: 3835
```

Este header indica el tamaño del cuerpo de la respuesta en bytes.

Conceptualmente:

```text
HTTP response
 │
 ├── headers
 │
 └── body
       │
       └── 3835 bytes
```

Esto es relevante para performance.

Dos requests podrían producir:

```text
Content-Length: 500
```

y:

```text
Content-Length: 5000000
```

respectivamente.

Aunque ambos devuelvan:

```text
200 OK
```

el volumen de datos transferidos es muy diferente.

Por tanto, al analizar latencia no basta con mirar únicamente el status code.

También puede ser necesario conocer el tamaño de la respuesta.

---

# 21. Header `Connection: keep-alive`

Un response HTTP puede contener:

```text
Connection: keep-alive
```

La idea general de una conexión persistente es permitir reutilizar una conexión TCP para múltiples intercambios HTTP, evitando tener que establecer una nueva conexión para cada request.

Sin reutilización:

```text
TCP connect
    │
HTTP request
    │
HTTP response
    │
TCP close
```

Después:

```text
TCP connect
    │
HTTP request
    │
HTTP response
    │
TCP close
```

Con reutilización:

```text
TCP connect
    │
    ├── HTTP request
    ├── HTTP response
    ├── HTTP request
    ├── HTTP response
    ├── HTTP request
    └── HTTP response
```

La ventaja conceptual es que el costo de establecer la conexión puede reutilizarse entre múltiples requests.

Esto puede ser importante para reducir latencia y overhead cuando existen muchas requests hacia el mismo servidor.

---

# 22. Relación entre keep-alive y TCP

Es importante no pensar que `keep-alive` elimina TCP.

La conexión sigue siendo TCP.

Lo que cambia es cuánto tiempo se mantiene disponible y si puede reutilizarse.

Sin reutilización:

```text
TCP connection
    │
    └── una interacción
```

Con reutilización:

```text
TCP connection
    │
    ├── interacción 1
    ├── interacción 2
    └── interacción 3
```

Por tanto:

```text
HTTP keep-alive
```

no es un reemplazo de TCP.

Es una estrategia de reutilización de una conexión TCP existente para múltiples intercambios HTTP.

---

# 23. Relación con el servidor utilizado en el Día 10

En el Día 10 se utilizó un servidor HTTP sencillo de Python.

Se observó:

```text
> GET / HTTP/1.1
```

pero el servidor respondió:

```text
< HTTP/1.0 200 OK
```

Esto demuestra que no debemos asumir que la request y response necesariamente muestran exactamente la misma versión HTTP.

Son mensajes HTTP diferentes:

```text
request
    │
    └── HTTP/1.1

response
    │
    └── HTTP/1.0
```

Esto tampoco significa que TCP haya cambiado de versión.

HTTP y TCP son protocolos diferentes.

---

# 24. Latencia

Hasta este punto la pregunta principal era:

```text
¿funcionó?
```

Ahora añadimos otra:

```text
¿cuánto tardó?
```

Un endpoint puede devolver:

```text
200 OK
```

y tardar:

```text
5 ms
```

o:

```text
5 segundos
```

En ambos casos:

```text
status = 200
```

pero operacionalmente son situaciones muy diferentes.

Por eso necesitamos observar también el tiempo.

---

# 25. Qué significa latencia

Latencia es el tiempo que transcurre entre determinados puntos de inicio y finalización de una operación.

En este laboratorio queremos medir aproximadamente el tiempo total observado por `curl`:

```text
inicio de curl
     │
     ▼
operación
     │
     ├── conexión TCP
     ├── request HTTP
     ├── procesamiento
     ├── response
     └── transferencia
     │
     ▼
fin
```

La medición depende de exactamente qué herramienta y métrica utilicemos.

Por eso debemos evitar afirmar simplemente:

> "la aplicación tardó X ms"

si la medición realmente corresponde al tiempo total observado por el cliente.

---

# 26. Comando para medir `time_total`

El comando propuesto es:

```bash
time curl -o /dev/null -s -w "%{time_total}\n" http://127.0.0.1:8080/
```

Podemos dividirlo:

```text
time
curl
-o /dev/null
-s
-w "%{time_total}\n"
URL
```

Cada parte tiene un propósito distinto.

---

# 27. `-o /dev/null`

La opción:

```bash
-o /dev/null
```

hace que curl envíe el body descargado hacia `/dev/null`.

`/dev/null` es un dispositivo especial de Unix/Linux que descarta los datos que recibe.

Conceptualmente:

```text
curl
 │
 │ body
 ▼
/dev/null
 │
 X
```

Esto es útil porque en una medición de latencia no queremos llenar la terminal con el contenido de la respuesta.

---

# 28. `-s`

La opción:

```bash
-s
```

significa `silent`.

Evita mostrar el progreso normal de curl.

Esto hace que la salida sea más limpia y adecuada para una medición.

---

# 29. `-w`

La opción:

```bash
-w
```

corresponde a `write-out`.

Permite indicarle a curl que imprima información adicional al finalizar la operación.

Por ejemplo:

```bash
-w "%{time_total}\n"
```

le pide que escriba el valor de:

```text
time_total
```

seguido de un salto de línea.

---

# 30. `%{time_total}`

La variable:

```text
%{time_total}
```

representa el tiempo total de la operación observado por curl.

El valor se expresa en segundos.

Por ejemplo:

```text
0.003421
```

equivale aproximadamente a:

```text
3.421 ms
```

porque:

```text
1 segundo = 1000 milisegundos
```

Por tanto:

```text
0.003421 s × 1000
=
3.421 ms
```

---

# 31. ¿Qué incluye `time_total`?

No debemos interpretar `time_total` simplemente como:

> tiempo de ejecución del código de la aplicación.

Es el tiempo total observado por curl para la operación.

Dependiendo del escenario puede involucrar diferentes componentes:

```text
DNS
TCP connection
TLS
request
espera de respuesta
transferencia
```

En nuestro experimento local:

```text
127.0.0.1
```

no estamos haciendo una resolución DNS porque utilizamos directamente una IP.

Además, si usamos HTTP sin TLS, no existe handshake TLS.

Por tanto, conceptualmente nuestro experimento es aproximadamente:

```text
curl
 │
 ▼
TCP connect
 │
 ▼
HTTP request
 │
 ▼
procesamiento del servidor
 │
 ▼
HTTP response
 │
 ▼
transferencia
```

y `time_total` representa el tiempo total observado por curl para esa operación.

---

# 32. Por qué latencia y status code son dimensiones diferentes

Es incorrecto asumir:

```text
200 = rápido
404 = rápido
500 = lento
```

No existe una relación necesaria de ese tipo.

Podríamos obtener:

```text
200 → 50 ms
404 → 80 ms
500 → 20 ms
```

y esto no sería contradictorio.

El status code responde:

> ¿Cuál fue el resultado HTTP de la petición?

La latencia responde:

> ¿Cuánto tardó la operación medida?

Son observaciones diferentes.

---

# 33. Ejemplo conceptual

Podemos tener:

```text
GET /ok

TCP ✓
HTTP ✓
200 OK
time_total = 3 ms
```

y:

```text
GET /error

TCP ✓
HTTP ✓
500 Internal Server Error
time_total = 2 ms
```

El segundo endpoint puede ser más rápido porque detectar un error puede requerir menos procesamiento que generar una respuesta completa.

Por eso nunca debemos inferir causalidad únicamente a partir del status code y el tiempo observado.

---

# 34. Por qué esto es importante para SRE

Un servicio puede estar disponible pero ser lento.

Por ejemplo:

```text
HTTP 200
time_total = 4.8 s
```

Desde el punto de vista de disponibilidad:

```text
servicio respondió
```

Pero desde el punto de vista de performance:

```text
respuesta extremadamente lenta
```

Por eso, en sistemas reales, disponibilidad y latencia se estudian como dimensiones diferentes.

Este concepto será especialmente importante cuando posteriormente se estudien:

* métricas;
* percentiles;
* latencia p50;
* p95;
* p99;
* timeouts;
* tracing;
* performance tuning.

---

# 35. Evidencia vs interpretación

Una habilidad importante que empieza a desarrollarse aquí es separar:

```text
evidencia
```

de:

```text
interpretación
```

Por ejemplo:

### Evidencia

```text
< HTTP/1.0 404 File not found
```

### Interpretación

```text
La petición llegó al servidor HTTP y el recurso solicitado no fue encontrado.
```

La segunda afirmación está basada en la primera.

Otro ejemplo:

### Evidencia

```text
* Established connection to 127.0.0.1
```

### Interpretación

```text
La conexión TCP pudo establecerse.
```

Y:

### Evidencia

```text
Connection refused
```

### Interpretación

```text
El intento de establecimiento TCP falló y no se llegó a HTTP.
```

Este enfoque es importante para evitar conclusiones precipitadas durante troubleshooting.

---

# 36. Experimento 1 — observar headers y body con `curl -i`

El primer experimento consiste en ejecutar:

```bash
curl -i http://127.0.0.1:8080/
```

El objetivo es observar directamente:

```text
HTTP status line
response headers
body
```

Una respuesta puede tener una estructura como:

```text
HTTP/1.0 200 OK
Server: SimpleHTTP/0.6 Python/3.14.4
Date: ...
Content-type: text/html; charset=utf-8
Content-Length: 3835

<!DOCTYPE HTML>
<html>
...
</html>
```

La interpretación es:

```text
status
   ↓
200 OK

server
   ↓
SimpleHTTP/0.6 Python/3.14.4

content type
   ↓
text/html

content length
   ↓
3835 bytes

body
   ↓
HTML
```

---

# 37. Experimento 2 — observar la comunicación con `curl -v`

Ejecutar:

```bash
curl -v http://127.0.0.1:8080/
```

Permite observar el recorrido con mayor detalle.

Una salida típica puede incluir:

```text
* Trying 127.0.0.1:8080...
```

Esto representa el intento de conexión.

Después:

```text
* Established connection to 127.0.0.1 ...
```

Esto demuestra que la conexión TCP se estableció.

Después:

```text
> GET / HTTP/1.1
```

Esto demuestra que comenzó la comunicación HTTP.

Después:

```text
> Host: 127.0.0.1:8080
> User-Agent: curl/...
> Accept: */*
```

Estos son headers de request.

Finalmente:

```text
< HTTP/1.0 200 OK
```

indica el inicio de la response HTTP.

El flujo conceptual completo es:

```text
Trying
  │
  ▼
TCP connect
  │
  ▼
Established
  │
  ▼
HTTP request
  │
  ▼
HTTP response
  │
  ▼
status code
  │
  ▼
body
```

---

# 38. Experimento 3 — observar un 404

Para solicitar un recurso inexistente:

```bash
curl -i http://127.0.0.1:8080/esto-no-existe
```

La salida debe contener un status de la familia `404`.

El punto principal del experimento no es solamente obtener el número.

Es comprobar que:

```text
TCP connect ✓
HTTP request ✓
HTTP response ✓
404
```

Por tanto, un `404` demuestra que sí llegamos al servidor HTTP.

---

# 39. Experimento 4 — preparar un endpoint 500

Para estudiar `500` necesitamos un servidor que genere deliberadamente una respuesta de error.

El laboratorio debe tener conceptualmente tres rutas:

```text
/ok
    → 200

/not-found
    → 404

/error
    → 500
```

El objetivo es que los tres casos ocurran después de establecer TCP.

Por tanto:

```text
/ok
    TCP ✓ → HTTP ✓ → 200

/not-found
    TCP ✓ → HTTP ✓ → 404

/error
    TCP ✓ → HTTP ✓ → 500
```

Esto permite comparar tres resultados HTTP diferentes sin mezclar un error de conectividad TCP con un error HTTP.

La implementación concreta del servidor se realizará como parte de la práctica y deberá analizarse línea por línea, en lugar de tratarse como código para copiar sin comprender.

---

# 40. Experimento 5 — medir `time_total`

Una vez disponibles los endpoints, podemos medir:

```bash
curl -o /dev/null -s -w "%{time_total}\n" \
  http://127.0.0.1:8080/ok
```

Después:

```bash
curl -o /dev/null -s -w "%{time_total}\n" \
  http://127.0.0.1:8080/not-found
```

Y finalmente:

```bash
curl -o /dev/null -s -w "%{time_total}\n" \
  http://127.0.0.1:8080/error
```

El objetivo es obtener una medición aproximada para cada caso.

Los valores reales deben registrarse a partir de la ejecución del laboratorio.

No deben inventarse ni asumirse.

---

# 41. El comando `time`

El ejercicio también utiliza:

```bash
time curl ...
```

`time` permite medir el tiempo de ejecución de un comando desde la perspectiva del shell/sistema.

Esto es diferente de:

```text
%{time_total}
```

que es una métrica proporcionada por curl sobre la operación que realizó.

Por eso conviene distinguir conceptualmente:

```text
time
    ↓
mide la ejecución del comando

curl -w "%{time_total}"
    ↓
muestra el tiempo total medido por curl
```

En un diagnóstico detallado pueden resultar útiles ambos enfoques, pero no representan necesariamente exactamente la misma cosa ni deben interpretarse como idénticos.

---

# 42. El entregable

El documento correspondiente al laboratorio será:

```text
W2D4_http_evidence.md
```

Debe registrar evidencia real obtenida durante la práctica.

Para cada status code se documentará:

```text
status
2 headers relevantes
tiempo total aproximado
interpretación
```

Una estructura posible es:

```text
Endpoint: /ok

Status:
200 OK

Headers:
Server: ...
Content-Length: ...

Tiempo total:
... ms

Interpretación:
...
```

Después:

```text
Endpoint: /not-found

Status:
404 ...

Headers:
...
...

Tiempo total:
... ms

Interpretación:
...
```

Y finalmente:

```text
Endpoint: /error

Status:
500 ...

Headers:
...
...

Tiempo total:
... ms

Interpretación:
...
```

Los valores deben proceder de las ejecuciones reales.

---

# 43. Modelo mental final del Día 11

El modelo acumulado queda:

```text
hostname
   │
   ▼
DNS (si aplica)
   │
   ▼
IP
   │
   ▼
IP:puerto
   │
   ▼
TCP connect
   │
   ├─────────────── falla
   │                   │
   │                   ▼
   │             refused / timeout
   │
   ▼
TCP ESTABLISHED
   │
   ▼
HTTP request
   │
   ├── method
   ├── path
   ├── HTTP version
   └── request headers
   │
   ▼
servidor / aplicación
   │
   ▼
HTTP response
   │
   ├── HTTP version
   ├── status code
   ├── response headers
   └── body
   │
   ▼
tiempo total observado
```

Ahora podemos localizar los problemas en más etapas.

Por ejemplo:

```text
Connection refused
```

apunta inicialmente hacia:

```text
TCP / listener / puerto / firewall / routing
```

Mientras:

```text
404
```

apunta hacia:

```text
HTTP / recurso solicitado
```

Y:

```text
500
```

apunta hacia:

```text
HTTP / aplicación / procesamiento del servidor
```

Mientras que:

```text
200 pero 5 segundos
```

indica:

```text
HTTP exitoso
pero posiblemente existe un problema de latencia/performance
```

---

# 44. Conclusiones

El aprendizaje principal del Día 11 es que una respuesta HTTP contiene mucha más información que simplemente un código como `200`.

Una respuesta HTTP puede analizarse mediante:

```text
status code
headers
body
timing
```

y cada elemento proporciona evidencia diferente.

`curl -i` permite observar principalmente:

```text
response
 ├── status
 ├── headers
 └── body
```

mientras `curl -v` permite observar con mayor detalle:

```text
conexión
request
response
```

Los status codes permiten clasificar el resultado HTTP:

```text
200 → éxito
301 → redirección
404 → recurso no encontrado
500 → error del servidor
```

Pero estos códigos solamente existen si la comunicación llegó a HTTP.

Por eso:

```text
Connection refused
```

y:

```text
HTTP 404
```

son problemas de naturaleza completamente diferente.

El primero ocurre antes de HTTP:

```text
TCP ✗
HTTP no ocurrió
```

El segundo ocurre después de que TCP funcionó:

```text
TCP ✓
HTTP ✓
404
```

Finalmente, se introdujo una segunda dimensión de análisis:

```text
latencia
```

Un `200` no necesariamente significa que el servicio tenga un buen comportamiento de performance.

Podemos tener:

```text
200 OK
```

pero con una latencia excesiva.

Por eso un diagnóstico más completo debe comenzar a considerar simultáneamente:

```text
¿Conectó?
     │
     ▼
¿Hubo HTTP?
     │
     ▼
¿Qué status devolvió?
     │
     ▼
¿Qué headers devolvió?
     │
     ▼
¿Cuánto contenido devolvió?
     │
     ▼
¿Cuánto tardó?
```

Este es el paso siguiente respecto al Día 10.

El Día 10 enseñó:

```text
¿Llegamos hasta HTTP?
```

El Día 11 empieza a enseñar:

```text
¿Qué ocurrió exactamente cuando llegamos a HTTP?
```

Y esa diferencia es fundamental para construir posteriormente un diagnóstico basado en evidencia.

---

# 45. Comandos clave del día

## Mostrar response headers + body

```bash
curl -i http://127.0.0.1:8080/
```

## Mostrar información detallada de la comunicación

```bash
curl -v http://127.0.0.1:8080/
```

## Solicitar una ruta inexistente

```bash
curl -i http://127.0.0.1:8080/esto-no-existe
```

## Medir el tiempo total observado por curl

```bash
curl -o /dev/null -s -w "%{time_total}\n" \
  http://127.0.0.1:8080/
```

## Combinarlo con `time`

```bash
time curl -o /dev/null -s -w "%{time_total}\n" \
  http://127.0.0.1:8080/
```

---

# 46. Preguntas que este laboratorio debe permitir responder

Al terminar este laboratorio se debe poder responder con claridad:

1. ¿Cuál es la diferencia entre `curl -i` y `curl -v`?

2. ¿Qué representa `HTTP/1.1 200 OK`?

3. ¿Qué es un status code?

4. ¿Qué diferencia existe entre las familias `2xx`, `3xx`, `4xx` y `5xx`?

5. ¿Por qué un `404` demuestra que TCP sí funcionó?

6. ¿Por qué un `500` ocurre en una etapa posterior a TCP?

7. ¿Qué información proporciona el header `Server`?

8. ¿Qué representa el header `Date`?

9. ¿Qué representa `Content-Length`?

10. ¿Qué significa conceptualmente `Connection: keep-alive`?

11. ¿Por qué una conexión persistente puede reducir overhead?

12. ¿Qué diferencia existe entre el status code y la latencia?

13. ¿Qué representa `%{time_total}`?

14. ¿Por qué `time_total` no debe interpretarse automáticamente como "tiempo que tardó el código de la aplicación"?

15. ¿Qué evidencia demuestra que una petición llegó hasta HTTP?

16. ¿Cómo distinguirías entre:

```text
Connection refused
```

y:

```text
HTTP 500
```

durante un diagnóstico?

17. ¿Por qué un servicio puede devolver `200 OK` y aun así presentar un problema de performance?

---

# 47. Idea clave para conservar

El modelo que debe conservarse de estos dos días es:

```text
NO PUEDO ACCEDER AL SERVICIO
             │
             ▼
       ¿DNS resuelve?
             │
             ▼
       ¿TCP conecta?
             │
        ┌────┴────┐
        │         │
       NO        SÍ
        │         │
        ▼         ▼
 investigar    ¿HTTP responde?
 TCP/network        │
              ┌─────┴─────┐
              │           │
             NO          SÍ
              │           │
              ▼           ▼
         investigar    status
         HTTP/app         │
                          ▼
                       headers
                          │
                          ▼
                         body
                          │
                          ▼
                       latencia
```

La regla fundamental sigue siendo:

> **Antes de diagnosticar HTTP, hay que saber si realmente llegamos hasta HTTP.**

Y una vez que sabemos que sí llegamos:

> **No debemos detenernos en el status code. También debemos observar headers, body, tamaño de respuesta y tiempo.**

Eso convierte una simple ejecución de `curl` en evidencia útil para diagnóstico.
