# Día 10 — Diagnóstico de “conecto o no conecto”: TCP Connect + HTTP

## Objetivo del laboratorio

El objetivo de este laboratorio fue aprender a distinguir **en qué etapa falla una comunicación hacia un servicio**.

Cuando una aplicación intenta acceder a algo como:

```text
http://HOST:PUERTO
```

no ocurre una sola operación. Existen varias etapas que deben suceder en orden:

```text
DNS (si se utiliza un hostname)
        ↓
IP
        ↓
TCP connect
        ↓
conexión establecida
        ↓
HTTP request
        ↓
HTTP response
        ↓
status code / datos
```

La idea fundamental del día fue aprender a responder una pregunta muy importante durante un diagnóstico:

> **¿El problema está en establecer la conexión TCP o la conexión TCP funciona y el problema está posteriormente en HTTP?**

Esta distinción es fundamental en troubleshooting porque un error de conectividad TCP y un error HTTP pertenecen a etapas diferentes de la comunicación.

---

## 1. Relación con lo estudiado anteriormente

En el día anterior se estudió la relación:

```text
IP → puerto → socket → proceso
```

Se aprendió que un servicio de red puede estar asociado a un socket que permanece en estado:

```text
LISTEN
```

Por ejemplo:

```text
127.0.0.1:8080
```

significa que existe un socket TCP escuchando en el puerto `8080` de la interfaz loopback.

En este laboratorio se añadió una capa más al modelo.

Anteriormente se podía preguntar:

```text
¿Hay algún proceso escuchando en 127.0.0.1:8080?
```

Ahora podemos preguntar:

```text
¿Un cliente puede establecer una conexión TCP con
127.0.0.1:8080 y posteriormente intercambiar HTTP?
```

Esto permite pasar de observar solamente el estado del servidor a observar el recorrido completo desde el cliente hasta la aplicación.

---

## 2. Modelo conceptual: DNS → TCP → HTTP

Una petición HTTP no comienza realmente con HTTP.

Supongamos que hacemos:

```bash
curl http://example.com:8080
```

Conceptualmente tenemos:

```text
curl
 │
 │ 1. ¿Qué IP corresponde a example.com?
 ▼
DNS
 │
 ▼
IP
 │
 │ 2. ¿Puedo conectarme al puerto 8080?
 ▼
TCP connect
 │
 ▼
conexión TCP establecida
 │
 │ 3. Ahora sí puedo enviar HTTP
 ▼
HTTP request
 │
 ▼
HTTP response
```

Es importante **no mezclar estas etapas**.

Por ejemplo, si TCP no consigue establecerse, HTTP todavía no ha ocurrido.

No tiene sentido buscar primero un error HTTP `500` si ni siquiera se pudo establecer la conexión TCP.

---

## 3. DNS no siempre participa

En las pruebas de este laboratorio se utilizó:

```text
127.0.0.1
```

directamente.

Por tanto, no fue necesario resolver un nombre mediante DNS.

El flujo fue directamente:

```text
curl
 │
 ▼
127.0.0.1:8080
 │
 ▼
TCP
```

En cambio, si hubiéramos utilizado:

```bash
curl http://localhost:8080
```

o:

```bash
curl http://example.com:8080
```

podría existir primero una resolución de nombre:

```text
hostname
   ↓
DNS
   ↓
IP
```

Por eso el primer paso se expresa como:

```text
1. resolución DNS (si aplica)
```

y no como una etapa que obligatoriamente ocurre en todas las conexiones.

---

## 4. Preparación del experimento

Primero se comprobó qué sockets TCP estaban escuchando:

```bash
ss -lntp
```

La salida relevante fue:

```text
LISTEN       0         5                  127.0.0.1:8080              0.0.0.0:*        users:(("python3",pid=699,fd=3))
LISTEN       0         1000               10.255.255.254:53            0.0.0.0:*
LISTEN       0         4096               127.0.0.54:53                0.0.0.0:*
LISTEN       0         4096               127.0.0.53%lo:53             0.0.0.0:*
```

La línea importante para nuestro experimento fue:

```text
LISTEN 0 5 127.0.0.1:8080 0.0.0.0:* users:(("python3",pid=699,fd=3))
```

Esto nos daba evidencia de que existía un socket TCP escuchando en `127.0.0.1:8080`.

---

## 5. Interpretación de `127.0.0.1:8080`

El endpoint:

```text
127.0.0.1:8080
```

está formado por:

```text
IP       puerto
 │          │
 ▼          ▼
127.0.0.1:8080
```

`127.0.0.1` es la dirección de **loopback**.

Representa al propio host.

Por tanto:

```text
127.0.0.1
```

permite que un proceso se comunique con otro proceso del mismo sistema mediante la pila de red, sin utilizar una interfaz física para salir hacia otra máquina.

El puerto:

```text
8080
```

identifica el endpoint TCP donde está escuchando el servicio.

---

## 6. Relación entre el socket y el proceso

La salida de `ss` mostraba:

```text
users:(("python3",pid=699,fd=3))
```

Esto permite relacionar:

```text
python3
   │
   ├── PID 699
   │
   └── FD 3
          │
          ▼
      socket TCP
          │
          ▼
    127.0.0.1:8080
```

Por tanto sabemos que el proceso Python con PID `699` posee el file descriptor `3` asociado al socket TCP que está escuchando en `127.0.0.1:8080`.

Esto conecta directamente con lo estudiado anteriormente sobre `lsof` y **file descriptors**.

---

## 7. Caso bueno: servidor disponible

Se ejecutó:

```bash
curl -v http://127.0.0.1:8080
```

La opción:

```text
-v
```

significa que `curl` muestra información detallada de la comunicación.

Esto resulta especialmente útil para diagnóstico porque permite observar:

* intento de conexión;
* establecimiento de la conexión;
* petición HTTP;
* headers;
* respuesta HTTP;
* status code.

---

## 8. Primera etapa de curl

La primera línea relevante fue:

```text
*   Trying 127.0.0.1:8080...
```

Esto significa que `curl` está intentando establecer una conexión con:

```text
127.0.0.1:8080
```

Todavía no estamos hablando de HTTP.

Estamos en la etapa de **conexión TCP**.

El flujo en este punto es:

```text
curl
 │
 ▼
127.0.0.1:8080
 │
 ▼
TCP connect
```

---

## 9. Conexión TCP establecida

Posteriormente apareció:

```text
* Established connection to 127.0.0.1 (127.0.0.1 port 8080) from 127.0.0.1 port 46312
```

Esta línea es especialmente importante.

Tenemos dos endpoints:

```text
Servidor
127.0.0.1:8080

Cliente
127.0.0.1:46312
```

El puerto `46312` es un puerto local utilizado por el cliente para esa conexión.

Por tanto, la conexión puede representarse como:

```text
cliente
127.0.0.1:46312
       │
       │ TCP
       ▼
servidor
127.0.0.1:8080
```

---

## 10. Puerto efímero

El puerto:

```text
46312
```

no era el puerto donde estaba escuchando el servidor.

El servidor estaba escuchando en:

```text
8080
```

El cliente utilizó:

```text
46312
```

como puerto local para esa conexión.

Esto es un **puerto efímero**: un puerto que el sistema operativo puede asignar temporalmente a una conexión iniciada por el cliente.

Por tanto:

```text
127.0.0.1:46312
```

representa el endpoint local de `curl`, mientras:

```text
127.0.0.1:8080
```

representa el endpoint del servidor.

---

## 11. La 4-tupla de la conexión

Una conexión TCP puede identificarse mediante:

```text
(source IP,
 source port,
 destination IP,
 destination port)
```

En este caso:

```text
(
  127.0.0.1,
  46312,
  127.0.0.1,
  8080
)
```

Esto es importante porque demuestra que el puerto `8080` por sí solo no representa una conexión TCP individual.

Puede existir una conexión:

```text
127.0.0.1:46312 → 127.0.0.1:8080
```

y posteriormente otra:

```text
127.0.0.1:51266 → 127.0.0.1:8080
```

Ambas utilizan el mismo puerto de servidor, pero son conexiones diferentes.

---

## 12. ¿Qué ocurrió entre `Trying` y `Established`?

Aunque `curl -v` no mostró cada paquete TCP individual, entre ambas líneas tuvo que ocurrir el establecimiento de la conexión TCP.

Conceptualmente:

```text
Cliente                              Servidor

SYN ------------------------------>

     <-------------------------- SYN-ACK

ACK ------------------------------>
```

Después del handshake, ambos lados pueden considerar establecida la conexión TCP.

El punto importante para este laboratorio es:

```text
Trying
   ↓
TCP connect
   ↓
Established
```

Solo después de esto tiene sentido comenzar a hablar de HTTP.

---

## 13. curl comienza a utilizar HTTP

Después de establecer la conexión apareció:

```text
* using HTTP/1.x
```

Esto indica que `curl` va a utilizar HTTP sobre la conexión TCP que acaba de establecer.

Aquí cambia la capa conceptual:

```text
TCP
 ↓
conexión establecida
 ↓
HTTP
```

---

## 14. Petición HTTP

La petición enviada fue:

```text
> GET / HTTP/1.1
```

Podemos dividirla:

### `GET`

Es el método HTTP.

Indica que el cliente quiere obtener un recurso.

### `/`

Es la ruta solicitada.

Representa la raíz del servidor HTTP.

### `HTTP/1.1`

Es la versión del protocolo HTTP utilizada en la petición.

Por tanto:

```text
GET / HTTP/1.1
```

significa conceptualmente:

> El cliente solicita mediante HTTP el recurso `/` utilizando el método `GET`.

---

## 15. Headers de la petición

También se observaron:

```text
> Host: 127.0.0.1:8080
> User-Agent: curl/8.18.0
> Accept: */*
```

Estos son **headers HTTP**.

Es importante distinguirlos de TCP.

TCP no interpreta:

```text
GET /
Host:
User-Agent:
```

Para TCP todo eso son simplemente **bytes que están siendo transportados**.

El significado de esos bytes como HTTP lo interpreta el servidor HTTP.

Por tanto:

```text
TCP
 │
 ├── transporta bytes
 │
 └── no sabe que los bytes representan HTTP
```

Mientras:

```text
HTTP
 │
 ├── GET
 ├── Host
 ├── User-Agent
 └── Accept
```

---

## 16. La petición terminó de enviarse

Después apareció:

```text
* Request completely sent off
```

En este momento `curl` ya había enviado la petición HTTP.

El flujo era:

```text
curl
 │
 │ HTTP request
 ▼
TCP
 │
 ▼
Python HTTP server
```

Ahora el cliente esperaba la respuesta.

---

## 17. Respuesta HTTP

El servidor respondió:

```text
< HTTP/1.0 200 OK
```

Aquí tenemos:

```text
HTTP/1.0
```

la versión HTTP utilizada por el servidor en la respuesta.

Y:

```text
200
```

es el status code.

`200` indica que la petición fue procesada correctamente.

Por tanto, el caso bueno completo fue:

```text
TCP connection
       ✓
       ↓
HTTP request
       ✓
       ↓
HTTP response
       ✓
       ↓
200 OK
```

---

## 18. HTTP/1.1 en la petición y HTTP/1.0 en la respuesta

Un detalle observado fue:

```text
> GET / HTTP/1.1
```

pero el servidor respondió:

```text
< HTTP/1.0 200 OK
```

Esto no significa que TCP haya cambiado de versión.

Son versiones del protocolo HTTP.

En este experimento se utilizó el servidor HTTP sencillo de Python:

```text
SimpleHTTP/0.6 Python/3.14.4
```

que aparece en:

```text
< Server: SimpleHTTP/0.6 Python/3.14.4
```

El punto importante es que una petición HTTP y su respuesta pueden tener características/versiones HTTP determinadas por el comportamiento del cliente y servidor.

---

## 19. Headers de la respuesta

La respuesta contenía:

```text
< Server: SimpleHTTP/0.6 Python/3.14.4
< Date: Sat, 15 Aug 2026 16:06:49 GMT
< Content-type: text/html; charset=utf-8
< Content-Length: 3835
```

Estos son headers HTTP de respuesta.

Por ejemplo:

```text
Content-type: text/html; charset=utf-8
```

indica que el cuerpo de la respuesta es HTML codificado en UTF-8.

Mientras:

```text
Content-Length: 3835
```

indica el tamaño del cuerpo de respuesta.

---

## 20. El cuerpo de la respuesta

Finalmente `curl` mostró:

```html
<!DOCTYPE HTML>
<html lang="en">
...
```

El servidor Python estaba sirviendo el directorio actual mediante HTTP.

Por eso la respuesta contenía una página:

```text
Directory listing for /
```

seguida de archivos y directorios.

El flujo completo fue:

```text
Python HTTP server
       │
       ▼
directorio actual
       │
       ▼
genera respuesta HTML
       │
       ▼
HTTP response
       │
       ▼
curl muestra el body
```

El hecho de que aparecieran elementos como:

```text
Desktop/
Documents/
Downloads/
```

se debe a que el servidor HTTP estaba sirviendo el directorio desde el que se ejecutó.

En este entorno se estaba trabajando desde:

```text
/mnt/c/Users/josew
```

por lo que el directorio expuesto mediante el servidor incluía contenido del filesystem de Windows accesible desde WSL.

---

## 21. Resultado del caso bueno

El caso bueno demostró:

```text
Destino:
127.0.0.1:8080

       ↓

TCP connect
       ✓

       ↓

Established connection
       ✓

       ↓

HTTP GET /
       ✓

       ↓

HTTP response
       ✓

       ↓

HTTP 200 OK
       ✓

       ↓

HTML body
       ✓
```

Por tanto, podemos concluir que:

* existe un servicio escuchando en `127.0.0.1:8080`;
* la conexión TCP pudo establecerse;
* la aplicación HTTP recibió la petición;
* la aplicación respondió;
* la respuesta fue `200 OK`.

---

## 22. Caso malo: puerto cerrado

Para el segundo caso se ejecutó:

```bash
curl -v http://127.0.0.1:9999
```

No existía un servicio escuchando en ese puerto.

La salida comenzó con:

```text
*   Trying 127.0.0.1:9999...
```

Nuevamente, esto representa el intento de establecer la conexión TCP.

Después apareció:

```text
* connect to 127.0.0.1 port 9999 from 127.0.0.1 port 46728 failed: Connection refused
```

Aquí la conexión falló.

El cliente utilizó:

```text
127.0.0.1:46728
```

como puerto local.

El destino era:

```text
127.0.0.1:9999
```

Por tanto:

```text
cliente
127.0.0.1:46728
       │
       │ TCP connect
       ▼
127.0.0.1:9999
       X
```

El resultado fue:

```text
Connection refused
```

---

## 23. Lo más importante del caso malo: nunca llegamos a HTTP

Después apareció:

```text
* Failed to connect to 127.0.0.1 port 9999 after 350 ms: Could not connect to server
```

y finalmente:

```text
curl: (7) Failed to connect to 127.0.0.1 port 9999 after 350 ms: Could not connect to server
```

No apareció ninguna línea como:

```text
> GET / HTTP/1.1
```

ni:

```text
< HTTP/1.1 200 OK
```

ni ningún otro status HTTP.

Esto es fundamental.

El flujo fue:

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
 │
 STOP
```

Por tanto:

> **No hubo una petición HTTP porque nunca se consiguió establecer la conexión TCP.**

Esta es precisamente la distinción que buscaba enseñar el laboratorio.

---

## 24. Comparación de ambos casos

### Caso bueno

```text
127.0.0.1:8080
       │
       ▼
TCP connect
       ✓
       │
       ▼
Established
       │
       ▼
HTTP GET /
       ✓
       │
       ▼
HTTP 200 OK
       ✓
```

### Caso malo

```text
127.0.0.1:9999
       │
       ▼
TCP connect
       X
       │
       ▼
Connection refused

HTTP
NO SE ALCANZÓ
```

Esta comparación es el principal aprendizaje del laboratorio.

---

## 25. Segundo experimento: observar las conexiones con `ss`

Después de comprobar los dos casos con `curl`, se utilizó:

```bash
watch -n 0.1 'ss -antp'
```

El objetivo era intentar observar las conexiones TCP mientras `curl` estaba ejecutándose.

La opción:

```text
watch -n 0.1
```

hace que el comando se ejecute repetidamente cada `0.1` segundos.

Por tanto:

```bash
watch -n 0.1 'ss -antp'
```

permite observar continuamente los estados de los sockets TCP.

---

## 26. Por qué no se alcanzó a observar `ESTAB`

Se esperaba potencialmente observar:

```text
ESTAB
```

pero las conexiones de `curl` eran extremadamente rápidas.

El ciclo era aproximadamente:

```text
SYN
 ↓
SYN-ACK
 ↓
ACK
 ↓
ESTABLISHED
 ↓
HTTP request
 ↓
HTTP response
 ↓
cierre
```

La fase `ESTABLISHED` podía durar tan poco que una herramienta que toma una fotografía cada `100 ms` podía no coincidir con ese instante.

Esto no significa que la conexión no haya estado establecida.

De hecho, `curl` había demostrado explícitamente:

```text
Established connection to 127.0.0.1
```

Por tanto:

> **No observar `ESTAB` con `watch` no significa que no haya existido el estado `ESTABLISHED`; significa que el muestreo no fue suficientemente rápido para capturarlo visualmente.**

---

## 27. Lo que sí se pudo observar: `TIME-WAIT`

La salida de `ss` mostró:

```text
TIME-WAIT    0    0    127.0.0.1:8080    127.0.0.1:51266
TIME-WAIT    0    0    127.0.0.1:8080    127.0.0.1:51316
TIME-WAIT    0    0    127.0.0.1:8080    127.0.0.1:53000
TIME-WAIT    0    0    127.0.0.1:8080    127.0.0.1:51274
TIME-WAIT    0    0    127.0.0.1:8080    127.0.0.1:52988
TIME-WAIT    0    0    127.0.0.1:8080    127.0.0.1:47024
TIME-WAIT    0    0    127.0.0.1:8080    127.0.0.1:51312
TIME-WAIT    0    0    127.0.0.1:8080    127.0.0.1:51292
TIME-WAIT    0    0    127.0.0.1:8080    127.0.0.1:51300
TIME-WAIT    0    0    127.0.0.1:8080    127.0.0.1:51290
```

Esto fue una observación importante porque permitió ver qué ocurre con las conexiones después de terminar la comunicación.

---

## 28. `TIME-WAIT` no es `LISTEN`

Es importante distinguir:

```text
LISTEN
```

de:

```text
TIME-WAIT
```

El servidor continuaba mostrando:

```text
LISTEN
127.0.0.1:8080
```

Esto significa que el socket de escucha continuaba disponible.

Al mismo tiempo existían varias conexiones anteriores en:

```text
TIME-WAIT
```

Por tanto podemos tener simultáneamente:

```text
LISTEN
127.0.0.1:8080
```

y:

```text
TIME-WAIT
127.0.0.1:8080 → 127.0.0.1:51266

TIME-WAIT
127.0.0.1:8080 → 127.0.0.1:51316

TIME-WAIT
127.0.0.1:8080 → 127.0.0.1:53000
```

El listener y las conexiones concretas son conceptos diferentes.

---

## 29. Interpretación de una entrada `TIME-WAIT`

Por ejemplo:

```text
TIME-WAIT
127.0.0.1:8080
127.0.0.1:51266
```

representa una conexión TCP concreta cuyos endpoints son:

```text
127.0.0.1:8080
```

y:

```text
127.0.0.1:51266
```

Podemos expresarla mediante la 4-tupla:

```text
(
    127.0.0.1,
    8080,
    127.0.0.1,
    51266
)
```

Otra entrada:

```text
TIME-WAIT
127.0.0.1:8080
127.0.0.1:51316
```

corresponde a otra conexión:

```text
(
    127.0.0.1,
    8080,
    127.0.0.1,
    51316
)
```

Aunque ambas utilizan el puerto `8080`, son conexiones diferentes porque el puerto del otro endpoint es diferente.

---

## 30. Relación entre `curl` y `TIME-WAIT`

Cada ejecución de:

```bash
curl http://127.0.0.1:8080
```

crea una conexión TCP.

Conceptualmente:

```text
curl #1
127.0.0.1:xxxxx → 127.0.0.1:8080
```

Después:

```text
curl #2
127.0.0.1:yyyyy → 127.0.0.1:8080
```

Después:

```text
curl #3
127.0.0.1:zzzzz → 127.0.0.1:8080
```

Cada conexión tiene su propia 4-tupla.

Al terminar, algunas conexiones pueden quedar temporalmente representadas como:

```text
TIME-WAIT
```

Por eso aparecieron múltiples puertos diferentes en la salida.

---

## 31. ¿Qué significa realmente `TIME-WAIT`?

`TIME-WAIT` es un estado de TCP asociado con el cierre de una conexión.

No significa:

> "El servidor está esperando a que el cliente responda."

Tampoco significa necesariamente:

> "Hay un problema."

Es un estado temporal que permite que TCP maneje correctamente el cierre de conexiones y evite que segmentos retrasados de una conexión anterior puedan confundirse con tráfico de una conexión posterior.

En este laboratorio solamente fue necesario reconocer que:

```text
conexión TCP
      ↓
cierre
      ↓
TIME-WAIT
```

La mecánica detallada de:

```text
FIN
ACK
FIN
ACK
```

y la razón exacta de la duración de `TIME-WAIT` se deja para un estudio posterior de TCP.

---

## 32. Una observación importante para troubleshooting

Ver:

```text
TIME-WAIT
```

no implica automáticamente que exista un problema.

Muchas conexiones cortas pueden producir muchas entradas:

```text
TIME-WAIT
TIME-WAIT
TIME-WAIT
...
```

Por ejemplo:

```text
cliente
 │
 ├── conexión
 ├── request
 ├── response
 └── cierre
```

repetido muchas veces.

Por eso, en un sistema real, encontrar `TIME-WAIT` es una pista que debe interpretarse en contexto, no una conclusión inmediata de que existe un fallo.

---

## 33. Modelo mental final

Al finalizar este laboratorio, el modelo de diagnóstico queda:

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
             ┌─────┴─────┐
             │           │
           falla       éxito
             │           │
             ▼           ▼
      refused/timeout  ESTABLISHED
                         │
                         ▼
                    HTTP request
                         │
                    ┌────┴────┐
                    │         │
                  falla     éxito
                    │         │
                    ▼         ▼
                problema   HTTP response
                HTTP/app        │
                                ▼
                            status code
                                │
                                ▼
                               body
```

Este modelo permite localizar aproximadamente en qué etapa de la comunicación está ocurriendo el problema.

---

## 34. Diagnóstico práctico

Ante un problema del tipo:

```text
"Mi aplicación no puede conectarse al servicio"
```

no debemos saltar directamente a HTTP.

Primero debemos separar las etapas.

### Pregunta 1: ¿El nombre resuelve?

Si utilizamos un hostname:

```text
hostname → IP
```

### Pregunta 2: ¿Existe conectividad TCP?

```text
IP:puerto
     ↓
TCP connect
```

Herramientas útiles:

```bash
curl -v
```

o:

```bash
nc -vz HOST PUERTO
```

### Pregunta 3: ¿La aplicación responde mediante HTTP?

Una vez que TCP funciona:

```text
HTTP request
     ↓
HTTP response
```

Entonces podemos analizar:

```text
200
301
400
401
403
404
500
502
503
...
```

Pero un código HTTP solamente puede existir si llegamos hasta HTTP.

---

## 35. Evidencia obtenida durante el laboratorio

### Servicio disponible

```text
127.0.0.1:8080
```

se encontraba en:

```text
LISTEN
```

asociado con:

```text
python3
PID 699
FD 3
```

`curl` logró establecer la conexión:

```text
Established connection to 127.0.0.1
```

utilizando un puerto efímero:

```text
46312
```

Después envió:

```text
GET / HTTP/1.1
```

y recibió:

```text
HTTP/1.0 200 OK
```

seguido del contenido HTML.

### Puerto cerrado

Se intentó:

```bash
curl -v http://127.0.0.1:9999
```

El intento terminó con:

```text
Connection refused
```

No apareció ninguna petición HTTP.

Por tanto, el fallo ocurrió en:

```text
TCP connect
```

y no en HTTP.

---

## 36. Conclusiones

El principal aprendizaje del laboratorio fue:

> **"No puedo acceder al servicio" no identifica por sí mismo dónde está el problema. Hay que determinar en qué etapa falla la comunicación.**

Una conexión HTTP depende primero de que pueda establecerse la conexión TCP.

Por tanto:

```text
TCP falla
    ↓
HTTP ni siquiera comienza
```

mientras que:

```text
TCP funciona
    ↓
HTTP puede comenzar
    ↓
podemos obtener un status HTTP
```

Los dos experimentos demostraron claramente esta diferencia.

### Caso bueno

```text
TCP ✓
HTTP ✓
200 OK ✓
```

### Caso malo

```text
TCP ✗
HTTP no ocurrió
```

Además, el uso combinado de:

```bash
ss -lntp
```

y:

```bash
curl -v
```

permitió observar el problema desde dos perspectivas diferentes:

```text
ss
 ↓
estado de los sockets y proceso asociado
```

y:

```text
curl -v
 ↓
experiencia del cliente:
TCP + HTTP
```

Finalmente, `watch` junto con:

```bash
ss -antp
```

permitió observar conexiones anteriores en:

```text
TIME-WAIT
```

y relacionarlas con diferentes puertos efímeros y con la 4-tupla de cada conexión TCP.

---

## 37. Entregable — resumen operativo

### Caso bueno

`127.0.0.1:8080` tenía un servicio escuchando (`python3`, PID `699`).

`curl` consiguió establecer la conexión TCP.

El cliente utilizó un puerto efímero (`46312`).

Se envió:

```text
GET / HTTP/1.1
```

El servidor respondió:

```text
HTTP/1.0 200 OK
```

Se recibió correctamente el cuerpo HTML.

### Caso malo

`127.0.0.1:9999` no tenía un servicio aceptando conexiones.

El intento de TCP terminó en:

```text
Connection refused
```

No se llegó a enviar ninguna petición HTTP.

Por tanto, el fallo ocurrió en **TCP connect**, antes de HTTP.

### Idea clave para diagnóstico

```text
¿No conecta?
    ↓
Primero comprobar TCP.
    ↓
¿TCP conecta?
    │
    ├── NO → investigar conectividad / listener / puerto / routing / firewall
    │
    └── SÍ → entonces investigar HTTP / aplicación
```

Este es el modelo que debe conservarse para los siguientes laboratorios:

> **Antes de diagnosticar HTTP, hay que saber si realmente llegamos hasta HTTP.**
