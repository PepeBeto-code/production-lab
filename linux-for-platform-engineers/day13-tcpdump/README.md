# Día 13 — Paquetes con tcpdump: TCP handshake, HTTP request/response y cierre de conexión

## 1. Objetivo del día

El objetivo de este laboratorio fue observar directamente el tráfico de red generado durante una comunicación HTTP utilizando `tcpdump`.

Hasta el Día 12, el modelo mental era:

```text
hostname
    ↓
DNS query
    ↓
DNS resolver
    ↓
DNS response
    ↓
registro A / AAAA
    ↓
IP
    ↓
TCP connect
    ↓
HTTP request
    ↓
HTTP response
```

El Día 12 permitió entender con mayor detalle la parte de DNS: consultas, respuestas, resolver, registros `A` y `AAAA`, IPv4, IPv6, TTL y caché. También quedó establecido que, en el modelo estudiado, DNS proporciona la información necesaria para obtener una dirección IP antes de realizar la conexión TCP.

En este Día 13 se abrió la parte que anteriormente aparecía simplemente como:

```text
TCP connect
```

para observar qué ocurre realmente durante esa conexión.

El objetivo fue capturar evidencia de:

* TCP handshake:

  * SYN
  * SYN-ACK
  * ACK
* envío de un HTTP request;
* confirmaciones TCP;
* envío de un HTTP response;
* cuerpo de la respuesta HTTP;
* cierre de la conexión TCP.

El modelo observado fue:

```text
IP conocida
    ↓
Cliente inicia conexión TCP
    ↓
SYN
    ↓
Servidor responde
    ↓
SYN-ACK
    ↓
Cliente confirma
    ↓
ACK
    ↓
Conexión TCP establecida
    ↓
HTTP request
    ↓
ACK del servidor
    ↓
HTTP response
    ↓
ACK del cliente
    ↓
Cierre TCP
```

---

# 2. ¿Qué es tcpdump?

`tcpdump` es una herramienta de línea de comandos utilizada para capturar y observar tráfico de red.

Conceptualmente, cuando una aplicación genera tráfico:

```text
APLICACIÓN
    ↓
TCP
    ↓
paquetes
    ↓
interfaz de red
    ↓
red
```

normalmente no vemos directamente cada paquete.

Por ejemplo, al ejecutar:

```bash
curl http://127.0.0.1:8000
```

desde la terminal solamente vemos el resultado final de la petición.

Sin embargo, internamente ocurre una comunicación entre un cliente y un servidor.

Conceptualmente:

```text
curl
    ↓
TCP handshake
    ↓
HTTP request
    ↓
servidor
    ↓
HTTP response
    ↓
curl
```

`tcpdump` permite observar los paquetes generados durante este proceso.

Por lo tanto, en este laboratorio:

```text
curl
 │
 │ genera tráfico
 ▼
TCP / paquetes
 │
 ▼
interfaz de red
 │
 ▼
tcpdump observa el tráfico
```

`tcpdump` no genera la conexión.

La conexión fue generada por `curl`.

`tcpdump` solamente permitió observar los paquetes relacionados con esa comunicación.

---

# 3. Interfaces de red observadas

Antes de realizar la captura se ejecutó:

```bash
ip a
```

La salida mostró, entre otras, dos interfaces:

```text
lo
eth0
```

La interfaz utilizada para el laboratorio fue:

```text
lo
```

`lo` significa:

```text
loopback
```

La interfaz loopback permite realizar comunicaciones que permanecen dentro de la propia máquina.

En este laboratorio tanto el cliente como el servidor estaban ejecutándose en el mismo sistema.

El modelo fue:

```text
┌──────────────────────────────────────┐
│            MISMA MÁQUINA             │
│                                      │
│   curl                               │
│     │                                │
│     │ HTTP                           │
│     ▼                                │
│ 127.0.0.1                            │
│     │                                │
│     ▼                                │
│ interfaz lo                          │
│     │                                │
│     ▼                                │
│ servidor HTTP local                  │
│                                      │
└──────────────────────────────────────┘
```

La dirección IPv4 utilizada fue:

```text
127.0.0.1
```

Por lo tanto, el tráfico observado fue local y no dependió de una conexión externa a Internet.

Esto permitió construir un laboratorio controlado.

---

# 4. Preparación del servidor HTTP local

Se inició un servidor HTTP local en el puerto:

```text
8000
```

mediante:

```bash
python3 -m http.server 8000
```

El escenario creado fue:

```text
CLIENTE                     SERVIDOR

curl                        Python HTTP server
 │                                 ▲
 │                                 │
 └──────── 127.0.0.1:8000 ─────────┘
```

El cliente se conectó a:

```text
127.0.0.1:8000
```

Es importante distinguir:

```text
127.0.0.1
```

es la dirección IP utilizada en este laboratorio.

Mientras:

```text
8000
```

es el puerto en el que estaba escuchando el servidor HTTP local.

Por lo tanto, el destino completo utilizado por el cliente fue:

```text
127.0.0.1:8000
```

---

# 5. Comando utilizado para capturar el tráfico

Se ejecutó:

```bash
sudo tcpdump -i lo -n -A tcp port 8000
```

Cada parte tuvo un propósito.

## `sudo`

Se utilizó para ejecutar `tcpdump` con permisos suficientes para realizar la captura.

## `tcpdump`

Es la herramienta encargada de capturar y mostrar los paquetes.

## `-i lo`

La opción:

```text
-i
```

permite indicar la interfaz que se quiere observar.

En este caso:

```text
-i lo
```

significa:

```text
capturar tráfico en la interfaz loopback
```

Esto fue correcto porque el cliente y el servidor estaban comunicándose mediante:

```text
127.0.0.1
```

dentro de la misma máquina.

## `-n`

La opción:

```text
-n
```

evita convertir direcciones y puertos en nombres.

Esto permite observar directamente valores numéricos como:

```text
127.0.0.1
8000
41408
```

en lugar de intentar resolver nombres.

## `-A`

La opción:

```text
-A
```

permite mostrar el contenido de los paquetes en formato ASCII cuando es posible.

Gracias a esto fue posible observar directamente contenido como:

```text
GET / HTTP/1.1
```

y:

```text
HTTP/1.0 200 OK
```

Además, fue posible ver el contenido HTML enviado por el servidor.

## `tcp port 8000`

Esta parte fue un filtro.

Indica que solamente queremos observar tráfico TCP relacionado con el puerto:

```text
8000
```

Sin este filtro, podrían aparecer muchos otros paquetes y el análisis sería más difícil.

Por lo tanto:

```text
tcp port 8000
```

significa conceptualmente:

```text
Mostrar únicamente tráfico TCP
relacionado con el puerto 8000
```

El comando completo fue:

```text
tcpdump
│
├── -i lo
│     observar interfaz loopback
│
├── -n
│     no resolver nombres
│
├── -A
│     mostrar contenido ASCII cuando sea posible
│
└── tcp port 8000
      filtrar tráfico TCP relacionado con el puerto 8000
```

---

# 6. Generación del tráfico

Mientras `tcpdump` estaba capturando paquetes, se ejecutó:

```bash
curl http://127.0.0.1:8000
```

Esto generó una conexión entre:

```text
CLIENTE

127.0.0.1:41408
```

y:

```text
SERVIDOR

127.0.0.1:8000
```

El puerto:

```text
41408
```

fue utilizado por el lado cliente en esa conexión concreta.

Por lo tanto, la comunicación observada fue:

```text
127.0.0.1:41408
        │
        │ TCP
        ▼
127.0.0.1:8000
```

---

# 7. TCP three-way handshake observado

La captura mostró primero:

```text
127.0.0.1.41408 > 127.0.0.1.8000: Flags [S]
```

Después:

```text
127.0.0.1.8000 > 127.0.0.1.41408: Flags [S.]
```

Y finalmente:

```text
127.0.0.1.41408 > 127.0.0.1.8000: Flags [.]
```

Estas tres líneas corresponden al TCP three-way handshake.

## Paso 1 — SYN

La primera línea fue:

```text
Flags [S]
```

La letra:

```text
S
```

representa:

```text
SYN
```

El cliente inició la conexión:

```text
CLIENTE
127.0.0.1:41408

SYN [S]
────────────────────►

SERVIDOR
127.0.0.1:8000
```

## Paso 2 — SYN-ACK

Después apareció:

```text
Flags [S.]
```

En la salida observada:

```text
S
```

representa:

```text
SYN
```

Mientras:

```text
.
```

representa:

```text
ACK
```

Por lo tanto:

```text
[S.]
```

representa:

```text
SYN + ACK
```

El servidor respondió:

```text
CLIENTE                         SERVIDOR

              SYN + ACK [S.]
◄────────────────────────────────────
```

## Paso 3 — ACK

Finalmente apareció:

```text
Flags [.]
```

El punto representa:

```text
ACK
```

El cliente confirmó la respuesta del servidor:

```text
CLIENTE                         SERVIDOR

ACK [.]
────────────────────────────────────►
```

Después de estos tres pasos:

```text
[S]
 ↓
[S.]
 ↓
[.]
```

la conexión TCP quedó establecida.

Conceptualmente:

```text
CLIENTE                         SERVIDOR

SYN [S]
────────────────────────────────►

                    SYN + ACK [S.]
◄────────────────────────────────

ACK [.]
────────────────────────────────►

========= TCP ESTABLECIDO =========
```

---

# 8. `length 0` durante el handshake

Los paquetes del handshake mostraron:

```text
length 0
```

Esto significa que en esos paquetes observados no había datos de aplicación como:

```text
GET / HTTP/1.1
```

Todavía no se estaba enviando el HTTP request.

Primero ocurrió:

```text
TCP handshake
    ↓
conexión establecida
    ↓
HTTP request
```

Esto permite observar directamente la separación conceptual entre TCP y HTTP.

---

# 9. HTTP request observado

Después del handshake apareció:

```text
127.0.0.1.41408 > 127.0.0.1.8000: Flags [P.]
```

con:

```text
length 78
```

Y gracias a:

```text
-A
```

fue posible observar:

```http
GET / HTTP/1.1
Host: 127.0.0.1:8000
User-Agent: curl/8.18.0
Accept: */*
```

Este fue el HTTP request real generado por:

```bash
curl http://127.0.0.1:8000
```

---

# 10. ¿Qué significa `[P.]`?

La captura mostró:

```text
[P.]
```

Esto contiene dos flags:

```text
P
.
```

`P` representa:

```text
PSH
```

El punto representa:

```text
ACK
```

Por lo tanto:

```text
[P.]
```

representa:

```text
PSH + ACK
```

Sin embargo, es importante no cometer esta simplificación:

```text
[P.] = HTTP request
```

Esto no es necesariamente correcto.

El flag TCP:

```text
[P.]
```

por sí mismo no significa:

```text
"esto es una petición HTTP"
```

En esta captura sabemos que el paquete contiene un HTTP request porque sus datos muestran:

```http
GET / HTTP/1.1
```

Por lo tanto, la interpretación correcta es:

```text
[P.]
+
datos TCP
+
GET / HTTP/1.1
=
paquete TCP que transporta un HTTP request
```

El paquete observado fue:

```text
CLIENTE
127.0.0.1:41408

[P.]
+
GET / HTTP/1.1
+
headers HTTP
────────────────────────────►

SERVIDOR
127.0.0.1:8000
```

---

# 11. ACK del servidor al recibir el request

Después del request apareció:

```text
127.0.0.1.8000 > 127.0.0.1.41408: Flags [.]
```

con:

```text
length 0
```

Esto no fue todavía el HTTP response.

El servidor simplemente confirmó la recepción de los datos enviados por el cliente.

Conceptualmente:

```text
CLIENTE                         SERVIDOR

HTTP request
[P.]
────────────────────────────────────►

                    ACK [.]
◄────────────────────────────────────
```

Por lo tanto, un paquete:

```text
[.]
```

con:

```text
length 0
```

puede representar simplemente una confirmación TCP sin transportar contenido HTTP.

Esto demuestra nuevamente que:

```text
ACK TCP
```

y:

```text
HTTP response
```

son conceptos diferentes.

---

# 12. Inicio del HTTP response

Después apareció:

```text
127.0.0.1.8000 > 127.0.0.1.41408: Flags [P.]
```

con:

```text
length 156
```

El contenido observado fue:

```http
HTTP/1.0 200 OK
Server: SimpleHTTP/0.6 Python/3.14.4
Date: Thu, 20 Aug 2026 11:29:12 GMT
Content-type: text/html; charset=utf-8
Content-Length: 3835
```

Esto fue el inicio del HTTP response.

El contenido indica:

```text
HTTP/1.0 200 OK
```

La respuesta HTTP fue exitosa según ese código de estado.

Además, aparecieron diferentes headers.

Por ejemplo:

```text
Server:
```

indica información proporcionada por el servidor.

```text
Content-type:
```

indica el tipo de contenido:

```text
text/html; charset=utf-8
```

Y:

```text
Content-Length: 3835
```

indica el tamaño anunciado para el contenido de la respuesta.

---

# 13. El HTTP response también fue confirmado mediante ACK

Después de recibir los primeros datos del response apareció:

```text
127.0.0.1.41408 > 127.0.0.1.8000: Flags [.]
```

con:

```text
length 0
```

Este paquete fue una confirmación TCP.

Conceptualmente:

```text
SERVIDOR

[P.]
+
HTTP/1.0 200 OK
+
headers
────────────────────────────►

CLIENTE

ACK [.]
────────────────────────────►
```

La idea importante es:

```text
HTTP envía contenido
dentro de
TCP
```

y TCP tiene su propio mecanismo de confirmación mediante ACK.

---

# 14. El body de la respuesta HTTP

Posteriormente apareció otro paquete:

```text
127.0.0.1.8000 > 127.0.0.1.41408: Flags [P.]
```

con:

```text
length 3835
```

Y su contenido comenzaba con:

```html
<!DOCTYPE HTML>
<html lang="en">
<head>
...
```

Este contenido correspondió al body de la respuesta HTTP.

Por lo tanto, en esta captura concreta, la respuesta se pudo observar conceptualmente como:

```text
HTTP RESPONSE

┌─────────────────────────────────────┐
│ Headers                             │
│                                     │
│ HTTP/1.0 200 OK                     │
│ Server: ...                         │
│ Content-Type: text/html             │
│ Content-Length: 3835                │
└─────────────────────────────────────┘
                  ↓
┌─────────────────────────────────────┐
│ Body                                │
│                                     │
│ <!DOCTYPE HTML>                     │
│ <html>                              │
│ ...                                 │
│ contenido HTML                      │
└─────────────────────────────────────┘
```

La relación observada fue especialmente interesante porque primero el servidor anunció:

```text
Content-Length: 3835
```

y posteriormente apareció el contenido HTML con:

```text
length 3835
```

Esto corresponde a lo observado en esta ejecución concreta.

---

# 15. ACK después del body

Después de recibir el contenido HTML apareció:

```text
127.0.0.1.41408 > 127.0.0.1.8000: Flags [.]
```

con:

```text
ack 3992
```

y:

```text
length 0
```

El cliente confirmó la recepción de los datos enviados por el servidor.

Conceptualmente:

```text
SERVIDOR

HTML
[P.]
length 3835
────────────────────────────►

CLIENTE

ACK [.]
────────────────────────────►
```

---

# 16. Cierre de la conexión TCP

Además del objetivo principal del laboratorio, la captura mostró cómo terminó la conexión TCP.

Primero apareció:

```text
127.0.0.1.8000 > 127.0.0.1.41408: Flags [F.]
```

La letra:

```text
F
```

representa:

```text
FIN
```

Mientras:

```text
.
```

representa:

```text
ACK
```

Por lo tanto:

```text
[F.]
```

representa:

```text
FIN + ACK
```

En esta captura, el servidor inició el cierre.

Conceptualmente:

```text
SERVIDOR

FIN + ACK
[F.]
────────────────────────────►

CLIENTE
```

Después apareció:

```text
127.0.0.1.41408 > 127.0.0.1.8000: Flags [F.]
```

El cliente también envió:

```text
FIN + ACK
```

Finalmente apareció:

```text
127.0.0.1.8000 > 127.0.0.1.41408: Flags [.]
```

que fue la confirmación final observada.

El cierre visto en esta ejecución fue:

```text
SERVIDOR                         CLIENTE

FIN + ACK [F.]
────────────────────────────────►

                    FIN + ACK [F.]
◄────────────────────────────────

ACK [.]
────────────────────────────────►
```

---

# 17. Captura completa simplificada

La comunicación completa observada fue:

```text
CLIENTE
127.0.0.1:41408

SERVIDOR
127.0.0.1:8000


[S]
SYN
────────────────────────────────────►

[S.]
SYN + ACK
◄────────────────────────────────────

[.]
ACK
────────────────────────────────────►

========== TCP CONNECTION ESTABLISHED ==========

[P.]
PSH + ACK

GET / HTTP/1.1
Host: 127.0.0.1:8000
User-Agent: curl/8.18.0
Accept: */*
────────────────────────────────────►

[.]
ACK
◄────────────────────────────────────

[P.]

HTTP/1.0 200 OK
Server: ...
Content-Type: text/html
Content-Length: 3835
◄────────────────────────────────────

[.]
ACK
────────────────────────────────────►

[P.]

<!DOCTYPE HTML>
<html>
...
body HTML
◄────────────────────────────────────

[.]
ACK
────────────────────────────────────►

=============== TCP CONNECTION CLOSE ===============

[F.]
FIN + ACK
◄────────────────────────────────────

[F.]
FIN + ACK
────────────────────────────────────►

[.]
ACK
◄────────────────────────────────────
```

---

# 18. Diferencia fundamental: TCP vs HTTP

Uno de los aprendizajes más importantes del laboratorio fue observar directamente que TCP y HTTP no son lo mismo.

TCP se encargó de establecer y gestionar la comunicación.

HTTP fue el protocolo cuyos mensajes viajaron dentro de esa comunicación.

El modelo observado fue:

```text
TCP

├── establecer conexión
│
│   SYN
│   SYN-ACK
│   ACK
│
├── transportar datos
│
│   HTTP request
│   │
│   └── GET / HTTP/1.1
│
│   HTTP response
│   │
│   ├── HTTP/1.0 200 OK
│   ├── headers
│   └── HTML
│
└── cerrar conexión
    │
    ├── FIN
    ├── FIN
    └── ACK
```

Por lo tanto, el flujo completo observado fue:

```text
curl
    ↓
TCP handshake
    ↓
TCP connection established
    ↓
HTTP request
    ↓
TCP ACK
    ↓
HTTP response headers
    ↓
TCP ACK
    ↓
HTTP response body
    ↓
TCP ACK
    ↓
TCP connection close
```

---

# 19. Importancia de `length`

La salida de `tcpdump` permitió utilizar:

```text
length
```

como una pista para diferenciar paquetes sin datos de paquetes que transportaban contenido.

Por ejemplo, durante el handshake aparecieron:

```text
length 0
```

También aparecieron paquetes ACK con:

```text
length 0
```

Estos paquetes no mostraban contenido HTTP.

Sin embargo, el request mostró:

```text
length 78
```

y contenía:

```http
GET / HTTP/1.1
```

Posteriormente aparecieron:

```text
length 156
```

con:

```http
HTTP/1.0 200 OK
```

y headers.

Después:

```text
length 3835
```

con el contenido HTML.

Por lo tanto, en esta captura:

```text
length 0
```

permitió identificar paquetes sin datos de aplicación visibles.

Mientras que un valor mayor que cero acompañado de contenido como:

```http
GET / HTTP/1.1
```

o:

```http
HTTP/1.0 200 OK
```

permitió observar paquetes TCP transportando datos HTTP.

---

# 20. No interpretar un flag aislado como un protocolo de aplicación

Una conclusión importante es que no se debe pensar:

```text
[P.] = HTTP request
```

ni:

```text
[P.] = HTTP response
```

`[P.]` representa flags TCP:

```text
P = PSH
. = ACK
```

Para identificar qué información está transportando un paquete, es necesario observar los datos.

En esta captura:

```text
[P.]
+
GET / HTTP/1.1
```

correspondió al HTTP request.

Mientras:

```text
[P.]
+
HTTP/1.0 200 OK
```

correspondió al HTTP response.

Y posteriormente:

```text
[P.]
+
<!DOCTYPE HTML>
```

transportó el body HTML.

Por lo tanto:

```text
FLAG TCP
≠
protocolo de aplicación
```

El flag forma parte de TCP.

El contenido transportado puede pertenecer a HTTP.

---

# 21. Filtro utilizado para evitar demasiado tráfico

Una parte importante del uso de `tcpdump` es filtrar el tráfico.

El filtro utilizado fue:

```text
tcp port 8000
```

Esto permitió reducir la cantidad de paquetes mostrados.

Sin ese filtro, podrían aparecer otros paquetes relacionados con diferentes procesos del sistema.

El modelo fue:

```text
todo el tráfico
│
├── DNS
├── otros procesos
├── conexiones diferentes
├── servicios del sistema
│
└── tráfico HTTP local puerto 8000
```

El filtro permitió concentrarse en:

```text
TCP
+
puerto 8000
```

Por lo tanto:

```bash
sudo tcpdump -i lo -n -A tcp port 8000
```

fue suficiente para observar específicamente el experimento que se estaba realizando.

---

# 22. Comandos utilizados

## Ver interfaces

```bash
ip a
```

Permitió identificar las interfaces disponibles.

Se observó:

```text
lo
eth0
```

Para este laboratorio se utilizó:

```text
lo
```

## Instalar tcpdump

```bash
sudo apt install tcpdump
```

## Comprobar la instalación

```bash
tcpdump --version
```

## Iniciar servidor HTTP local

```bash
python3 -m http.server 8000
```

## Capturar tráfico TCP

```bash
sudo tcpdump -i lo -n -A tcp port 8000
```

## Generar tráfico HTTP

```bash
curl http://127.0.0.1:8000
```

---

# 23. Resultado del laboratorio

Se logró observar evidencia real de:

## TCP handshake

```text
[S]
[S.]
[.]
```

Que corresponde a:

```text
SYN
 ↓
SYN-ACK
 ↓
ACK
```

## HTTP request

Se observó:

```http
GET / HTTP/1.1
Host: 127.0.0.1:8000
User-Agent: curl/8.18.0
Accept: */*
```

## Confirmación TCP del request

Se observó un:

```text
[.]
```

con:

```text
length 0
```

## HTTP response

Se observó:

```http
HTTP/1.0 200 OK
```

junto con headers como:

```text
Server:
Content-type:
Content-Length:
```

## Body de la respuesta

Se observó contenido:

```html
<!DOCTYPE HTML>
<html lang="en">
...
```

## Cierre de conexión

Se observaron:

```text
[F.]
[F.]
[.]
```

correspondientes al cierre observado de la conexión.

---

# 24. Ejercicio de razonamiento

## Pregunta 1

### ¿El handshake TCP apareció antes del HTTP request?

Sí.

La captura mostró:

```text
[S]
 ↓
[S.]
 ↓
[.]
 ↓
GET / HTTP/1.1
```

Por lo tanto, primero se estableció la conexión TCP y después se envió el request HTTP.

---

## Pregunta 2

### ¿`[P.]` significa automáticamente HTTP?

No.

`[P.]` representa:

```text
PSH + ACK
```

El contenido del paquete es lo que permitió identificar si estaba transportando un HTTP request, un HTTP response o contenido HTML.

---

## Pregunta 3

### ¿Todos los paquetes `[.]` eran HTTP responses?

No.

Los paquetes `[.]` observados con:

```text
length 0
```

fueron ACKs TCP sin contenido HTTP visible.

---

## Pregunta 4

### ¿Cómo se identificó el HTTP request?

Porque `tcpdump -A` mostró directamente:

```http
GET / HTTP/1.1
```

---

## Pregunta 5

### ¿Cómo se identificó el HTTP response?

Porque se observó:

```http
HTTP/1.0 200 OK
```

seguido de headers HTTP.

---

## Pregunta 6

### ¿La respuesta HTTP fue únicamente una línea?

No.

En la captura se observaron primero los headers:

```http
HTTP/1.0 200 OK
...
Content-Length: 3835
```

y posteriormente el body:

```html
<!DOCTYPE HTML>
<html>
...
```

---

## Pregunta 7

### ¿Después de enviar datos HTTP TCP continuó participando?

Sí.

Después de los datos se observaron ACKs TCP.

Esto muestra que TCP continúa gestionando la comunicación mientras HTTP transporta los mensajes de aplicación.

---

## Pregunta 8

### ¿También se observó el cierre de la conexión?

Sí.

Se observaron flags:

```text
[F.]
```

relacionados con:

```text
FIN + ACK
```

y un ACK final observado en la captura.

---

# 25. Conclusiones

Este laboratorio permitió pasar de un modelo abstracto:

```text
TCP connect
    ↓
HTTP request
    ↓
HTTP response
```

a observar directamente los paquetes relacionados con cada etapa.

Se comprobó que una comunicación HTTP puede observarse conceptualmente así:

```text
CLIENTE
│
├── SYN
│
├── recibe SYN-ACK
│
├── ACK
│
├── HTTP request
│
├── recibe HTTP response
│
├── ACKs TCP
│
└── cierre de conexión
```

La evidencia real mostró:

```text
[S]
```

para el SYN inicial.

```text
[S.]
```

para SYN + ACK.

```text
[.]
```

para ACK.

```text
[P.]
+
GET / HTTP/1.1
```

para un paquete TCP que transportó el HTTP request observado.

Posteriormente:

```text
[P.]
+
HTTP/1.0 200 OK
```

transportó el inicio del HTTP response.

Después:

```text
[P.]
+
HTML
```

transportó el body observado.

Finalmente aparecieron:

```text
[F.]
```

y:

```text
[.]
```

durante el cierre de la conexión.

---

# 26. Modelo mental final del Día 13

El modelo que debo conservar es:

```text
CLIENTE
│
│ SYN
▼
SERVIDOR
│
│ SYN-ACK
▼
CLIENTE
│
│ ACK
▼
CONEXIÓN TCP ESTABLECIDA
│
│ HTTP request
│
│ GET / HTTP/1.1
▼
SERVIDOR
│
│ ACK
▼
HTTP response
│
├── HTTP/1.0 200 OK
│
├── headers
│
└── body HTML
▼
ACKs TCP
▼
CIERRE TCP
├── FIN
├── FIN
└── ACK
```

La idea fundamental del día es:

> **TCP establece, administra y cierra la conexión. HTTP transporta los mensajes de aplicación dentro de esa comunicación. `tcpdump` permite observar los paquetes reales y relacionar directamente el TCP handshake, los ACKs, el HTTP request, el HTTP response y el cierre de la conexión.**

El flujo que antes se veía simplemente como:

```text
IP
 ↓
TCP connect
 ↓
HTTP request
 ↓
HTTP response
```

ahora puede entenderse con mayor detalle:

```text
IP
 ↓
TCP SYN
 ↓
TCP SYN-ACK
 ↓
TCP ACK
 ↓
TCP connection established
 ↓
HTTP request
 ↓
TCP ACK
 ↓
HTTP response headers
 ↓
TCP ACK
 ↓
HTTP response body
 ↓
TCP ACK
 ↓
TCP FIN / cierre
```

Con este laboratorio se obtuvo evidencia real de cada una de estas etapas utilizando:

```text
tcpdump
```

y generando tráfico controlado con:

```bash
curl http://127.0.0.1:8000
```
