# Día 9 — Puertos, sockets, TCP y `ss`

## Objetivo del día

El objetivo de este laboratorio es extender el modelo de redes construido durante el Día 8.

Hasta ahora el modelo era, de forma simplificada:

```text
Aplicación
    │
    │ necesita comunicarse
    ▼
nombre
    │
    │ DNS
    ▼
IP destino
    │
    │ routing
    ▼
interfaz
    │
    ▼
gateway / red local
    │
    ▼
máquina destino
```

El Día 9 responde la pregunta que faltaba:

> **Si ya llegamos a la máquina destino, ¿a qué servicio o proceso dentro de esa máquina queremos comunicarnos?**

Una misma máquina puede ejecutar simultáneamente:

* nginx
* PostgreSQL
* SSH
* Java
* Redis
* Docker
* múltiples aplicaciones propias

Todos ellos pueden utilizar la red.

Por eso necesitamos agregar al modelo:

```text
IP
│
│ ¿qué máquina?
▼
puerto
│
│ ¿qué endpoint de transporte?
▼
socket
│
│ ¿qué objeto de comunicación utiliza el proceso?
▼
proceso
│
│
▼
servicio / aplicación
```

Además, estudiaremos cómo TCP mantiene el estado de las conexiones y cómo observar todo esto mediante `ss`.

---

# 1. IP y puerto resuelven problemas diferentes

Supongamos que una máquina tiene:

```text
192.168.1.20
```

Y dentro de ella existen:

```text
Nginx       → 80
PostgreSQL  → 5432
SSH         → 22
```

Desde otra máquina podemos tener:

```text
192.168.1.20:80
192.168.1.20:5432
192.168.1.20:22
```

La IP identifica al host:

```text
192.168.1.20
```

Mientras que el puerto identifica un punto de entrada de transporte en ese host:

```text
80
5432
22
```

Por eso es habitual pensar en un endpoint como:

```text
IP:puerto
```

Por ejemplo:

```text
192.168.1.20:80
```

La IP nos lleva hasta la máquina.

El puerto nos permite distinguir el servicio o endpoint de transporte al que queremos llegar dentro de ella.

---

# 2. ¿Qué es exactamente un puerto?

Un puerto TCP **no es un agujero físico**.

Es un número utilizado por TCP para identificar un punto extremo de comunicación dentro de un host.

El puerto tiene 16 bits:

```text
2^16 = 65536
```

Por lo tanto, existen valores:

```text
0 - 65535
```

Cuando vemos:

```text
127.0.0.1:8080
```

podemos separar:

```text
host:
127.0.0.1

puerto:
8080
```

La combinación de dirección IP y puerto forma parte de la identificación de un endpoint de red.

---

# 3. Puerto no significa proceso

Esta distinción es fundamental.

No debemos construir el modelo:

```text
puerto 8080 = proceso
```

Eso es incorrecto.

El puerto es un identificador utilizado por TCP/UDP.

Un proceso puede abrir un socket que esté asociado con un puerto.

El modelo correcto es aproximadamente:

```text
Proceso
   │
   │ utiliza
   ▼
socket
   │
   │ asociado a
   ▼
IP + puerto
```

Por ejemplo, `ss -lntp` puede mostrar:

```text
LISTEN 0 128 127.0.0.1:8080 0.0.0.0:* users:(("python",pid=1234,fd=3))
```

Aquí estamos viendo varias cosas distintas:

```text
LISTEN
```

Estado TCP.

```text
127.0.0.1:8080
```

Endpoint local.

```text
python
```

Proceso asociado.

```text
pid=1234
```

PID del proceso.

```text
fd=3
```

File descriptor utilizado por el proceso para ese socket.

Por eso debemos aprender a separar:

```text
puerto
socket
proceso
PID
file descriptor
estado TCP
```

---

# 4. ¿Qué es un socket?

El socket es una de las palabras centrales de este día.

Un socket es una abstracción proporcionada por el sistema operativo para que un proceso pueda comunicarse mediante la red.

No debemos reducirlo simplemente a:

```text
socket = IP + puerto
```

La IP y el puerto forman parte de la identificación del endpoint asociado al socket, pero el socket es el objeto de comunicación que el sistema operativo proporciona al proceso.

Desde el punto de vista de una aplicación, existen operaciones como:

```text
socket()
bind()
listen()
accept()
connect()
send()
recv()
close()
```

No es necesario memorizar todavía todas estas llamadas ni implementarlas directamente.

Lo importante es comprender conceptualmente qué papel desempeñan.

---

# 5. ¿Por qué existe el socket?

Imaginemos un programa servidor.

Quiere realizar algo como:

```text
esperar conexiones
       ↓
aceptar conexión
       ↓
recibir datos
       ↓
procesar
       ↓
responder
```

El programa necesita una forma de decirle al kernel:

> Quiero utilizar la red.

El kernel proporciona un socket.

El proceso obtiene un file descriptor que representa ese socket.

Por ejemplo:

```text
fd = 3
```

Desde la perspectiva del proceso, el descriptor `3` representa ese recurso de red.

Esto explica por qué Linux puede tratar los sockets como file descriptors.

---

# 6. File descriptors y sockets

Durante los días anteriores ya habíamos trabajado indirectamente con file descriptors.

Un proceso Linux normalmente tiene:

```text
0 → stdin
1 → stdout
2 → stderr
```

Y puede tener otros:

```text
3
4
5
...
```

Un socket también puede estar representado mediante un file descriptor.

Por ejemplo:

```text
Proceso Python
├── fd 0 → stdin
├── fd 1 → stdout
├── fd 2 → stderr
└── fd 3 → socket TCP
```

Esto conecta directamente con `lsof`.

Si hacemos:

```bash
lsof -p <PID>
```

podemos encontrar algo parecido a:

```text
python 1234 user 3u IPv4 ... TCP 127.0.0.1:8080 (LISTEN)
```

El:

```text
3u
```

nos indica que el proceso tiene abierto el file descriptor `3` para ese recurso.

Por tanto:

```text
proceso
   │
   └── fd 3
          │
          ▼
       socket
          │
          ▼
    TCP 127.0.0.1:8080
```

Esto conecta dos herramientas que ya hemos utilizado:

```text
lsof
```

y:

```text
ss
```

Ambas observan partes relacionadas del mismo sistema, pero desde perspectivas diferentes.

---

# 7. ¿Qué es un servidor?

En términos de red, "servidor" no necesariamente significa una computadora enorme dentro de un datacenter.

Un servidor puede ser simplemente:

> Un proceso que espera y atiende solicitudes de otros procesos mediante una interfaz de red.

Por ejemplo, cualquiera de estos puede actuar como servidor:

```text
python
java
nginx
postgres
```

Lo importante no es el tamaño de la máquina.

Lo importante es que exista un proceso que proporcione un servicio y atienda comunicaciones.

---

# 8. ¿Qué significa que un servidor "escucha"?

Aquí aparece el estado:

```text
LISTEN
```

Un servidor TCP realiza conceptualmente:

```text
socket()
   ↓
bind()
   ↓
listen()
   ↓
accept()
```

No necesitamos implementar estas llamadas todavía.

Necesitamos entender qué significa cada una.

## `socket()`

El proceso solicita al kernel un socket.

```text
Proceso
   ↓
kernel
   ↓
socket
```

## `bind()`

El proceso solicita asociar ese socket con una dirección y puerto.

Por ejemplo:

```text
127.0.0.1:8080
```

Conceptualmente:

```text
socket
   │
   └── bind()
          │
          ▼
    127.0.0.1:8080
```

## `listen()`

El proceso indica que quiere utilizar ese socket para aceptar conexiones TCP entrantes.

Entonces aparece el estado:

```text
LISTEN
```

## `accept()`

Cuando llega una conexión, el proceso puede aceptarla.

Aquí aparece una distinción fundamental:

> El socket que escucha y el socket utilizado para una conexión TCP concreta no representan la misma conexión.

Esta diferencia será especialmente importante al entender servidores concurrentes.

---

# 9. ¿Qué significa `LISTEN`?

`LISTEN` es un estado TCP asociado con un socket servidor que está preparado para recibir conexiones entrantes.

Por ejemplo:

```text
LISTEN 0 128 127.0.0.1:8080
```

Podemos leerlo aproximadamente como:

> Existe un socket TCP escuchando en `127.0.0.1:8080`.

Pero esto **no significa que exista un cliente conectado**.

Puede existir perfectamente:

```text
LISTEN
```

sin que exista ninguna conexión:

```text
ESTABLISHED
```

Por tanto:

```text
LISTEN ≠ cliente conectado
```

`LISTEN` significa:

> Estoy preparado para aceptar conexiones.

---

# 10. Servidor escuchando vs. conexión establecida

Supongamos que tenemos:

```text
Servidor
127.0.0.1:8080
```

Antes de que ningún cliente se conecte:

```text
Servidor
   │
   └── TCP LISTEN
       127.0.0.1:8080
```

No existe todavía un cliente conectado.

Ahora ejecutamos:

```bash
curl http://localhost:8080
```

Entonces aparece una conexión TCP.

Conceptualmente:

```text
Cliente
127.0.0.1:xxxxx
      │
      │ TCP
      ▼
Servidor
127.0.0.1:8080
```

Ahora existe una conexión TCP entre ambos endpoints.

El estado de esa conexión será:

```text
ESTABLISHED
```

---

# 11. ¿Por qué el cliente tiene otro puerto?

Supongamos que el servidor está escuchando en:

```text
127.0.0.1:8080
```

El cliente ejecuta:

```bash
curl localhost:8080
```

Podemos encontrar algo como:

```text
127.0.0.1:54321 → 127.0.0.1:8080
```

¿Por qué existe `54321`?

Porque el cliente también necesita un puerto local.

Una conexión TCP necesita identificar ambos extremos.

Por tanto, no debemos pensar únicamente:

```text
IP destino + puerto destino
```

Tenemos:

```text
IP origen
puerto origen
IP destino
puerto destino
```

Por ejemplo:

```text
127.0.0.1:54321
        │
        │ TCP
        ▼
127.0.0.1:8080
```

---

# 12. La 4-tupla de una conexión TCP

Una conexión TCP puede identificarse mediante una 4-tupla:

```text
(source IP,
 source port,
 destination IP,
 destination port)
```

En español:

```text
IP origen
puerto origen
IP destino
puerto destino
```

Ejemplo:

```text
(192.168.1.50, 51000,
 192.168.1.20, 8080)
```

Esto identifica una conexión concreta.

Esta idea permite que una máquina tenga múltiples clientes conectados simultáneamente al mismo puerto servidor.

Por ejemplo:

```text
Cliente A
192.168.1.50:51000
       │
       ▼
192.168.1.20:8080
```

```text
Cliente B
192.168.1.51:51001
       │
       ▼
192.168.1.20:8080
```

```text
Cliente C
192.168.1.52:51002
       │
       ▼
192.168.1.20:8080
```

Todos utilizan:

```text
192.168.1.20:8080
```

como destino.

Sin embargo, las conexiones son diferentes porque los extremos de origen son diferentes.

---

# 13. ¿Cómo distingue el servidor a los clientes?

Mediante la información que identifica cada conexión.

Tenemos:

```text
A:
192.168.1.50:51000 → 192.168.1.20:8080
```

y:

```text
B:
192.168.1.51:51001 → 192.168.1.20:8080
```

Ambas conexiones tienen:

```text
destino:
192.168.1.20:8080
```

pero tienen diferentes orígenes.

Por eso TCP puede mantener separadas las conexiones.

Esto es fundamental para comprender cómo un servidor puede atender múltiples clientes concurrentemente utilizando el mismo puerto.

---

# 14. Puerto efímero

El puerto del cliente normalmente es elegido automáticamente por el sistema operativo.

Este tipo de puerto se denomina **puerto efímero**.

Por ejemplo:

```text
Cliente:
127.0.0.1:53214
```

El cliente normalmente no especifica manualmente:

> Quiero utilizar exactamente el puerto 53214.

El kernel puede seleccionar un puerto disponible.

Entonces:

```bash
curl localhost:8080
```

podría producir:

```text
127.0.0.1:53214 → 127.0.0.1:8080
```

Y otra ejecución podría utilizar:

```text
127.0.0.1:53215 → 127.0.0.1:8080
```

No debemos asumir números concretos.

Lo importante es entender que:

```text
servidor → puerto conocido
cliente  → puerto efímero
```

es un patrón habitual.

---

# 15. "Puerto abierto": qué significa realmente

La expresión:

> "El puerto 8080 está abierto."

es ambigua.

Puede significar diferentes cosas.

Por ejemplo:

```text
LISTEN
127.0.0.1:8080
```

indica que existe un socket TCP escuchando en ese endpoint.

Pero debemos tener cuidado.

Si tenemos:

```text
127.0.0.1:8080
```

el servicio **no está accesible desde otras máquinas**.

Esto se debe a que `127.0.0.1` es la dirección de loopback.

Significa:

> Este propio host.

Por tanto:

```text
127.0.0.1:8080
```

permite acceder al servicio desde el propio host, pero no implica que otro host pueda conectarse directamente a él.

---

# 16. `127.0.0.1:8080` vs. `0.0.0.0:8080`

Esta diferencia es fundamental para DevOps.

## `127.0.0.1:8080`

El servicio está ligado a loopback.

Conceptualmente:

```text
misma máquina
     │
     └── puede acceder
```

Pero otra máquina:

```text
192.168.1.50
```

no puede conectarse directamente a:

```text
127.0.0.1:8080
```

porque `127.0.0.1` significa el propio host.

---

## `0.0.0.0:8080`

En el contexto de `bind`, `0.0.0.0` significa escuchar en las direcciones IPv4 locales disponibles del host.

Supongamos que la máquina tiene:

```text
127.0.0.1
192.168.1.20
172.18.143.87
```

Un servicio ligado a:

```text
0.0.0.0:8080
```

puede aceptar conexiones dirigidas a esas direcciones locales, sujeto a otras condiciones como:

* routing
* firewall
* namespaces
* Docker
* configuración de red

Es importante:

```text
0.0.0.0
```

no es una IP a la que un cliente deba conectarse.

Es una dirección comodín utilizada al especificar el `bind`.

---

# 17. `ss`: Socket Statistics

`ss` significa:

```text
socket statistics
```

Es una herramienta de Linux para inspeccionar sockets de red.

Permite observar:

* qué sockets existen;
* qué estado tienen;
* qué dirección utilizan;
* qué puerto utilizan;
* qué conexiones están establecidas;
* qué proceso está asociado, cuando tenemos permisos suficientes.

Por eso `ss` es una herramienta especialmente importante para SRE/DevOps.

Ante un incidente como:

> "La aplicación no responde."

una de las primeras preguntas puede ser:

> ¿Existe siquiera un socket escuchando donde debería?

Podemos empezar con:

```bash
ss -lnt
```

o:

```bash
ss -lntp
```

---

# 18. Descomponiendo `ss -lntp`

El comando:

```bash
ss -lntp
```

se puede descomponer así:

```text
-l → listening
-n → numeric
-t → TCP
-p → process
```

---

## `-l` — listening

Muestra sockets que están escuchando.

Principalmente veremos:

```text
LISTEN
```

Es útil cuando queremos responder:

> ¿Qué servicios TCP están escuchando?

---

## `-n` — numeric

Evita convertir números a nombres.

Por ejemplo, puede mostrar:

```text
80
```

en lugar de:

```text
http
```

También evita que la herramienta intente resolver nombres DNS.

Esto es útil durante troubleshooting porque evita introducir ruido o retrasos derivados de resolución de nombres.

---

## `-t` — TCP

Limita la salida a sockets TCP.

---

## `-p` — process

Muestra información del proceso asociado, cuando tenemos permisos suficientes.

Podemos encontrar algo como:

```text
users:(("python",pid=1234,fd=3))
```

Ahora podemos relacionar:

```text
python
PID 1234
fd 3
socket
127.0.0.1:8080
```

---

# 19. Interpretación completa de `ss -lntp`

Por tanto:

```bash
ss -lntp
```

significa:

> Muéstrame los sockets TCP que están escuchando, utilizando números y tratando de mostrar qué procesos los tienen.

Modelo mental:

```text
ss
│
├── -l → listening
├── -n → numeric
├── -t → TCP
└── -p → process
```

---

# 20. ¿Qué muestra `ss -lntp`?

Podemos encontrar una línea similar a:

```text
State Recv-Q Send-Q Local Address:Port Peer Address:Port
LISTEN 0 128 127.0.0.1:8080 0.0.0.0:*
```

Con `-p` puede aparecer:

```text
users:(("python",pid=1234,fd=3))
```

La línea contiene varias piezas diferentes.

---

# 21. `State`

Ejemplo:

```text
LISTEN
```

Es el estado TCP asociado al socket.

Durante este día nos interesan principalmente:

```text
LISTEN
ESTABLISHED
```

Más adelante veremos muchos otros estados.

---

# 22. `Recv-Q`

Por ahora podemos pensar en:

> Datos recibidos que están pendientes de ser procesados o consumidos por la aplicación.

No debemos interpretarlo simplemente como:

> Cantidad total de datos recibidos.

Es una cola asociada al socket.

Por ejemplo:

```text
Recv-Q = 0
```

significa que actualmente no hay datos pendientes en esa cola.

Más adelante será necesario estudiar con mayor profundidad las colas TCP y su utilidad para diagnosticar problemas.

---

# 23. `Send-Q`

De forma análoga:

> Información que está pendiente en la cola de envío.

Como modelo inicial:

```text
Recv-Q
→ pendiente de recibir/entregar

Send-Q
→ pendiente de enviar
```

Hay matices adicionales dependiendo del estado y del tipo exacto de socket observado, por lo que este modelo se debe considerar introductorio.

---

# 24. `Local Address:Port`

Esta columna es extremadamente importante.

Ejemplo:

```text
127.0.0.1:8080
```

significa:

```text
dirección local:
127.0.0.1

puerto local:
8080
```

Si vemos:

```text
0.0.0.0:8080
```

se trata de un bind comodín IPv4.

---

# 25. `Peer Address:Port`

`Peer` significa:

> contraparte.

En una conexión establecida podríamos encontrar:

```text
Local:
127.0.0.1:8080

Peer:
127.0.0.1:53214
```

Esto significa:

```text
Servidor
127.0.0.1:8080
      ↕
Cliente
127.0.0.1:53214
```

Pero en un socket `LISTEN` todavía no existe una conexión concreta con un cliente.

Por eso podemos encontrar:

```text
0.0.0.0:*
```

El `*` representa conceptualmente que todavía no existe un peer específico asociado a esa escucha.

---

# 26. `ss -antp`

Ahora utilizamos:

```bash
ss -antp
```

Descomposición:

```text
-a → all
-n → numeric
-t → TCP
-p → process
```

La diferencia importante respecto a:

```bash
ss -lntp
```

es:

```text
-l
```

frente a:

```text
-a
```

`-l` muestra únicamente sockets en escucha.

`-a` muestra todos los sockets TCP relevantes, incluyendo:

* sockets de escucha;
* conexiones establecidas;
* otros estados TCP.

Por eso:

```bash
ss -antp
```

es especialmente útil cuando queremos observar qué ocurre antes y después de establecer una conexión.

---

# 27. Primera observación: antes de levantar el servidor

Primero ejecutamos:

```bash
ss -lntp
```

y:

```bash
ss -antp
```

No queremos memorizar la salida.

Queremos observar.

Debemos buscar específicamente:

```text
LISTEN
```

y anotar:

* IP local;
* puerto;
* proceso;
* PID;
* cualquier información adicional relevante.

Si no existe ningún servicio escuchando en `8080`, eso es perfectamente válido.

De hecho, es útil porque tenemos un punto de referencia antes de crear nuestro propio servidor.

---

# 28. Levantar nuestro primer servidor

Utilizamos Python porque queremos estudiar redes sin introducir infraestructura innecesaria.

Ejecutamos:

```bash
python3 -m http.server 8080 --bind 127.0.0.1
```

Esto inicia un servidor HTTP sencillo.

Conceptualmente tendremos:

```text
Python
   │
   └── servidor HTTP
          │
          └── 127.0.0.1:8080
```

El proceso debe quedar ejecutándose.

---

# 29. ¿Qué ocurrió internamente?

Conceptualmente, Python realizó operaciones equivalentes a:

```text
crear socket
     ↓
asociarlo a 127.0.0.1:8080
     ↓
ponerlo en modo escucha
```

Por eso, después de iniciar el servidor:

```bash
ss -lntp
```

debería mostrar una entrada relacionada con:

```text
127.0.0.1:8080
```

y:

```text
LISTEN
```

Además, mediante `-p`, deberíamos poder relacionarlo con Python.

---

# 30. Leer el socket creado por Python

Ejecutamos:

```bash
ss -lntp
```

Buscamos:

```text
127.0.0.1:8080
```

Debemos poder identificar:

1. estado;
2. IP local;
3. puerto;
4. proceso;
5. PID;
6. file descriptor, si se muestra.

El objetivo no es memorizar una línea.

El objetivo es aprender a leerla.

Por ejemplo, conceptualmente:

```text
LISTEN
127.0.0.1:8080
python
PID 1234
fd 3
```

significa:

```text
Existe un socket TCP
        │
        ├── estado: LISTEN
        ├── dirección: 127.0.0.1
        ├── puerto: 8080
        ├── proceso: python
        ├── PID: 1234
        └── FD: 3
```

---

# 31. Hacer una conexión con `curl`

En otra terminal:

```bash
curl http://127.0.0.1:8080
```

Python debería responder con un listado HTML del directorio actual.

Pero la página no es lo importante.

Lo importante es que acabamos de provocar una conexión TCP.

Tenemos ahora:

```text
curl
   │
   │ TCP
   ▼
Python HTTP server
```

---

# 32. Observar la conexión con `ss -antp`

Inmediatamente después de ejecutar `curl`:

```bash
ss -antp
```

podemos llegar a observar algo parecido a:

```text
LISTEN
127.0.0.1:8080
```

y además una conexión:

```text
ESTAB
127.0.0.1:8080
127.0.0.1:xxxxx
```

La forma exacta depende del momento en el que ejecutemos `ss`.

Esto es importante porque `curl` puede cerrar la conexión extremadamente rápido.

---

# 33. ¿Por qué puede desaparecer `ESTABLISHED` tan rápido?

El flujo de `curl` puede ser aproximadamente:

```text
conectar
   ↓
enviar HTTP request
   ↓
recibir respuesta
   ↓
cerrar conexión
   ↓
terminar
```

Todo puede ocurrir en milisegundos.

Por eso podemos ejecutar:

```bash
ss -antp
```

y no encontrar:

```text
ESTABLISHED
```

Eso **no significa que la conexión nunca haya existido**.

Significa que la conexión ya había terminado cuando tomamos la fotografía del estado del sistema.

Esta es una lección importante para el trabajo de sistemas:

> Las herramientas como `ss` muestran un estado instantáneo del sistema. No son automáticamente una grabación de todo lo que ocurrió.

Por tanto:

```text
no veo ESTABLISHED
```

no implica necesariamente:

```text
nunca hubo una conexión
```

---

# 34. Hacer más fácil la observación

Podemos utilizar una conexión que permanezca abierta para poder observarla.

Una opción es utilizar `nc` si está instalado.

Sin embargo, antes de introducir otra herramienta, debemos tener perfectamente clara la diferencia conceptual:

```text
LISTEN
```

significa:

> El servidor está preparado para aceptar conexiones.

Mientras:

```text
ESTABLISHED
```

significa:

> Existe una conexión TCP establecida entre dos endpoints.

No son sinónimos.

---

# 35. TCP: qué problema resuelve

Hasta ahora hemos hablado de:

```text
IP
```

y:

```text
puerto
```

Pero un puerto por sí mismo no crea una conexión.

TCP es un protocolo de transporte.

Entre otras responsabilidades, TCP se ocupa de:

* establecer conexiones;
* mantener estado;
* entregar datos de manera ordenada;
* detectar pérdidas;
* retransmitir;
* utilizar acknowledgements;
* controlar el flujo;
* gestionar el cierre de conexiones.

No vamos a estudiar todos estos mecanismos en profundidad todavía.

Lo importante para este día es entender por qué TCP tiene estados.

---

# 36. TCP es orientado a conexión

Cuando utilizamos TCP existe normalmente una relación mantenida entre dos endpoints.

Por ejemplo:

```text
Cliente
192.168.1.50:51000
      │
      │ TCP
      ▼
Servidor
192.168.1.20:8080
```

TCP mantiene información sobre esa conexión.

Por eso puede existir un estado como:

```text
ESTABLISHED
```

No significa simplemente:

> Vi un paquete.

Significa:

> Existe una conexión TCP cuyo estado actual es `ESTABLISHED`.

---

# 37. ¿Cómo llega TCP de `LISTEN` a `ESTABLISHED`?

Aquí aparece el famoso:

> Three-way handshake.

Supongamos:

```text
Cliente → Servidor
```

y el servidor está:

```text
LISTEN
```

El cliente quiere conectarse.

TCP utiliza:

```text
SYN
SYN-ACK
ACK
```

---

# 38. Primer paso: `SYN`

El cliente envía:

```text
SYN
```

Conceptualmente:

```text
Cliente
   │
   │ SYN
   ▼
Servidor
```

Significa aproximadamente:

> Quiero iniciar una conexión TCP.

---

# 39. Segundo paso: `SYN-ACK`

El servidor responde:

```text
SYN + ACK
```

Conceptualmente:

```text
Cliente
   ▲
   │ SYN-ACK
   │
Servidor
```

Significa aproximadamente:

> He recibido tu solicitud y estoy preparado para establecer la conexión.

---

# 40. Tercer paso: `ACK`

El cliente responde:

```text
ACK
```

Conceptualmente:

```text
Cliente
   │
   │ ACK
   ▼
Servidor
```

Después de completar este intercambio:

```text
ESTABLISHED
```

---

# 41. ¿Por qué no basta con `SYN`?

TCP necesita que ambos lados sincronicen información relacionada con la conexión, particularmente los números de secuencia iniciales.

Todavía no necesitamos dominar esos números.

Por ahora debemos entender:

```text
SYN
→ quiero iniciar

SYN-ACK
→ recibí tu SYN y acepto/inicio mi lado

ACK
→ recibí tu respuesta
```

Después:

```text
ESTABLISHED
```

---

# 42. ¿Qué significa `ESTABLISHED`?

Cuando `ss` muestra:

```text
ESTAB
```

está mostrando el estado:

```text
ESTABLISHED
```

Esto significa que la conexión TCP está establecida.

Por ejemplo:

```text
ESTAB
127.0.0.1:53214
127.0.0.1:8080
```

Podemos interpretarlo como:

```text
cliente:
127.0.0.1:53214

servidor:
127.0.0.1:8080
```

Existe una conexión TCP entre ambos.

---

# 43. `LISTEN` y `ESTABLISHED` pueden existir simultáneamente

Esto suele ser una fuente importante de confusión.

Podemos tener:

```text
LISTEN
0.0.0.0:8080
```

y simultáneamente:

```text
ESTABLISHED
192.168.1.20:8080
192.168.1.50:53214
```

¿Cómo puede ser?

Porque el servidor mantiene un socket de escucha:

```text
LISTEN socket
```

y también tiene sockets asociados a conexiones concretas:

```text
connection socket A
connection socket B
connection socket C
```

Conceptualmente:

```text
servidor
   │
   │
   ▼
LISTEN :8080
   │
   ├──────────────┬──────────────┐
   ▼              ▼              ▼
conn A           conn B         conn C
   │              │              │
cliente A       cliente B      cliente C
```

Esto es fundamental para entender servidores concurrentes.

---

# 44. `accept()` y el socket de conexión

Cuando llega una conexión:

```text
LISTEN socket
       │
       │ accept()
       ▼
nuevo socket
       │
       ▼
conexión concreta
```

El servidor puede continuar escuchando:

```text
LISTEN
```

mientras maneja conexiones:

```text
ESTABLISHED
```

Por tanto:

```text
un puerto servidor
       ↓
muchísimas conexiones
```

El listener no se convierte simplemente en "la conexión del cliente".

Existe un mecanismo mediante el cual las conexiones concretas se representan separadamente.

---

# 45. ¿El puerto 8080 pertenece al socket o al proceso?

La respuesta requiere precisión.

El proceso puede tener un socket.

Ese socket está asociado a un endpoint:

```text
IP:puerto
```

Pero un proceso puede tener múltiples sockets.

Por ejemplo:

```text
Python PID 1234

fd 3 → listening socket :8080
fd 4 → connection A
fd 5 → connection B
fd 6 → connection C
```

Por tanto, debemos evitar el modelo simplista:

```text
puerto → proceso
```

Es mejor pensar:

```text
proceso
   ↓
file descriptor
   ↓
socket
   ↓
endpoint / conexión
```

`ss -p` ayuda a observar la relación entre el socket y el proceso.

---

# 46. ¿Qué significa "puerto abierto para un cliente"?

Supongamos:

```text
Servidor:
192.168.1.20:8080
```

Para que un cliente pueda conectarse deben cumplirse varias condiciones.

## 1. Debe existir un servicio escuchando

Por ejemplo:

```text
LISTEN
192.168.1.20:8080
```

## 2. El cliente debe poder alcanzar la IP

Debe existir una ruta:

```text
cliente
   ↓
red
   ↓
192.168.1.20
```

## 3. No debe existir un firewall bloqueando la conexión

Por ejemplo:

```text
cliente
   ↓
firewall
   X
servidor
```

## 4. El servicio debe estar escuchando en una dirección accesible

No es lo mismo:

```text
127.0.0.1:8080
```

que:

```text
0.0.0.0:8080
```

## 5. El protocolo debe coincidir

Un cliente TCP necesita un servidor TCP.

Por tanto:

> "Puerto abierto" no significa simplemente que el número `8080` exista.

En términos prácticos significa que existe un endpoint TCP accesible en esa dirección/puerto y que las condiciones de red y filtrado permiten establecer la conexión.

---

# 47. Conexión con el Día 8

Aquí empezamos a unir todo.

El Día 8 nos había dejado aproximadamente:

```text
IP destino
   ↓
routing
   ↓
interfaz
   ↓
gateway
```

El Día 9 extiende el modelo:

```text
IP destino
   ↓
routing
   ↓
host destino
   ↓
puerto
   ↓
socket
   ↓
proceso / servicio
```

Por ejemplo:

```text
curl http://example.com:8080
```

conceptualmente implica:

```text
example.com
    │
    │ DNS
    ▼
93.x.x.x
    │
    │ routing
    ▼
host remoto
    │
    │ TCP
    ▼
puerto 8080
    │
    ▼
socket
    │
    ▼
proceso servidor
```

Ahora tenemos un camino mucho más completo.

---

# 48. TCP y HTTP no son lo mismo

Cuando ejecutamos:

```bash
curl http://127.0.0.1:8080
```

estamos trabajando con varias capas.

Conceptualmente:

```text
HTTP
 │
 ▼
TCP
 │
 ▼
IP
 │
 ▼
Ethernet / interfaz
```

HTTP se ocupa de cosas como:

```text
GET /
HTTP/1.1
Host: ...
```

TCP se ocupa del transporte de esos bytes de forma fiable y ordenada.

IP se ocupa del direccionamiento entre hosts.

Ethernet/Wi-Fi se ocupa de la entrega en la red local.

Por eso `ss` está observando principalmente:

```text
sockets
TCP
estado
endpoints
```

No está mostrando directamente el contenido del protocolo HTTP.

---

# 49. ¿Qué muestra `ss` y qué no muestra?

`ss` puede indicarnos:

* que existe un socket;
* que está en `LISTEN`;
* que está en `ESTABLISHED`;
* qué IP utiliza;
* qué puerto utiliza;
* quién es el peer;
* qué proceso está asociado, si tenemos permisos.

Pero `ss` no nos dice directamente:

> ¿Qué HTTP request envió el cliente?

Para observar otras partes del sistema necesitamos herramientas diferentes.

Por ejemplo:

```text
curl
tcpdump
Wireshark
logs
tracing
```

Cada herramienta observa una parte o capa diferente.

Esto es una idea importante para troubleshooting:

> No existe una única herramienta que muestre todo el sistema.

---

# 50. Práctica completa

La práctica del día consiste en observar el comportamiento real de un socket.

## Paso 1 — Antes del servidor

Ejecutar:

```bash
ss -lntp
```

y:

```bash
ss -antp
```

Guardar las salidas.

La idea es establecer una línea base.

---

## Paso 2 — Levantar el servidor

Ejecutar:

```bash
python3 -m http.server 8080 --bind 127.0.0.1
```

Mantenerlo ejecutándose.

---

## Paso 3 — Observar el listener

En otra terminal:

```bash
ss -lntp
```

Buscar:

```text
127.0.0.1:8080
```

Identificar:

* estado;
* IP;
* puerto;
* proceso;
* PID;
* FD, si aparece.

---

## Paso 4 — Provocar una conexión

En otra terminal:

```bash
curl http://127.0.0.1:8080
```

Esto provoca una conexión TCP contra el servidor.

---

## Paso 5 — Observar inmediatamente

Después de ejecutar `curl`:

```bash
ss -antp
```

Si conseguimos capturar la conexión a tiempo, deberíamos poder identificar:

```text
ESTAB
```

y observar:

```text
Local Address:Port
Peer Address:Port
PID / proceso
```

Si no aparece `ESTAB`, no significa que la conexión no haya existido.

Es posible que `curl` ya haya terminado la conexión.

---

# 51. Observar continuamente con `watch`

Para hacer más visible el carácter dinámico de TCP:

```bash
watch -n 0.1 'ss -antp'
```

Esto ejecutará `ss -antp` cada:

```text
0.1 segundos
```

Desde otra terminal ejecutamos:

```bash
curl http://127.0.0.1:8080
```

Podemos llegar a observar estados aparecer y desaparecer.

Esto enseña algo importante:

> El estado TCP es dinámico.

No es una propiedad eterna del puerto.

Una conexión puede atravesar diferentes estados durante su vida.

---

# 52. Estados TCP que debemos conocer por ahora

No necesitamos memorizar todos los estados TCP de golpe.

Los dos estados principales para este día son:

```text
LISTEN
ESTABLISHED
```

Sin embargo, debemos saber que existen otros:

```text
SYN-SENT
SYN-RECV
FIN-WAIT-1
FIN-WAIT-2
TIME-WAIT
CLOSE-WAIT
LAST-ACK
```

Los estudiaremos posteriormente con mayor profundidad.

Por ahora lo importante es comenzar a entender:

> TCP es una máquina de estados.

No debemos reducirlo a:

```text
conectado / desconectado
```

---

# 53. ¿Por qué SRE necesita entender esto?

En producción podemos encontrarnos con:

```text
service unavailable
```

Una primera pregunta puede ser:

```bash
ss -lntp
```

Si encontramos:

```text
LISTEN
0.0.0.0:8080
```

ya sabemos algo:

> Existe un proceso con un socket TCP escuchando en el puerto 8080.

Después podemos ejecutar:

```bash
ss -antp
```

y encontrar:

```text
cientos de ESTABLISHED
```

Ahora sabemos que no solamente existe el listener:

> Hay conexiones TCP activas.

Pero podemos encontrar situaciones diferentes.

Por ejemplo:

```text
LISTEN
0.0.0.0:8080
```

y desde otro host:

```text
connection refused
```

Esto nos lleva a investigar:

* firewall;
* namespace;
* container;
* routing;
* bind incorrecto;
* proxy;
* configuración de red.

Esto ya es troubleshooting real.

---

# 54. Caso clásico con Docker

Supongamos que una aplicación Java está dentro de un contenedor:

```text
Java app
   │
   └── escucha 127.0.0.1:8080
```

Dentro del contenedor puede funcionar perfectamente:

```bash
curl localhost:8080
```

Pero desde fuera:

```text
cliente
   ↓
container:8080
```

puede fallar.

¿Por qué?

Porque:

```text
127.0.0.1
```

significa el propio namespace de red donde está escuchando el proceso.

En Docker y Kubernetes esto será especialmente importante.

Por eso necesitamos entender perfectamente el significado de:

```text
127.0.0.1
```

antes de profundizar en containers.

---

# 55. Caso clásico con Spring Boot

Supongamos una aplicación Java/Spring Boot escuchando en:

```text
127.0.0.1:8080
```

El desarrollador puede decir:

> "¡Pero funciona!"

Y efectivamente puede funcionar:

```bash
curl localhost:8080
```

desde la misma máquina.

Pero un load balancer puede intentar:

```text
Load Balancer
      │
      ▼
servidor:8080
```

y no poder conectarse.

¿Por qué?

Porque el servicio está escuchando solamente en loopback.

Este es un problema clásico de despliegue.

La aplicación puede estar perfectamente sana desde su propia perspectiva y aun así ser inaccesible desde fuera del host.

---

# 56. `ss` como herramienta de troubleshooting

A partir de este día debemos comenzar a formular preguntas concretas.

## Pregunta 1

> ¿Existe algún socket escuchando?

```bash
ss -lnt
```

---

## Pregunta 2

> ¿Qué proceso lo tiene?

```bash
ss -lntp
```

---

## Pregunta 3

> ¿En qué dirección está escuchando?

Observar si aparece:

```text
127.0.0.1
```

o:

```text
0.0.0.0
```

o una IP específica como:

```text
192.168.x.x
```

---

## Pregunta 4

> ¿Hay conexiones activas?

```bash
ss -antp
```

Buscar:

```text
ESTAB
```

---

## Pregunta 5

> ¿Quién es el peer?

Observar:

```text
Peer Address:Port
```

---

## Pregunta 6

> ¿Hay muchas conexiones acumuladas?

Observar estados como:

```text
ESTAB
TIME-WAIT
CLOSE-WAIT
```

Estos estados serán estudiados posteriormente con mayor profundidad.

---

# 57. Diferencia entre `ss` y `ps`

Ya utilizamos:

```bash
ps
```

para observar procesos.

`ps` responde principalmente preguntas como:

> ¿Qué procesos existen y cuál es su estado?

Por ejemplo:

```text
PID
CPU
MEM
COMMAND
```

Mientras que:

```bash
ss
```

responde preguntas como:

> ¿Qué sockets de red existen y cuál es su estado?

Y con:

```bash
ss -p
```

podemos relacionar ambos mundos:

```text
socket
   ↓
proceso
   ↓
PID
```

Por eso necesitamos ambas perspectivas en SRE.

Una herramienta observa principalmente los procesos.

La otra observa principalmente los sockets y las conexiones.

---

# 58. Diferencia entre `lsof` y `ss`

También hemos trabajado con:

```bash
lsof
```

`lsof` significa:

```text
list open files
```

Como en Unix/Linux los sockets pueden representarse mediante file descriptors, `lsof` puede mostrar:

* proceso;
* PID;
* FD;
* tipo;
* IP;
* puerto;
* estado.

Por otro lado, `ss` está diseñado específicamente para inspeccionar sockets.

Podemos pensar en las herramientas así:

```text
lsof
→ perspectiva:
  "¿Qué recursos abiertos tiene este proceso?"
```

y:

```text
ss
→ perspectiva:
  "¿Qué sockets/conexiones existen?"
```

No compiten exactamente.

Se complementan.

---

# 59. Mapa mental completo

Después del Día 8 teníamos:

```text
NOMBRE
   │
   │ DNS
   ▼
IP
   │
   │ routing
   ▼
interfaz
   │
   ▼
gateway / LAN
   │
   ▼
HOST
```

Ahora agregamos:

```text
HOST
   │
   ▼
PUERTO
   │
   ▼
SOCKET
   │
   ▼
PROCESO
```

Para una conexión TCP:

```text
CLIENTE
IP:puerto
   │
   │ TCP
   ▼
SERVIDOR
IP:puerto
   │
   ▼
SOCKET
   │
   ▼
PROCESO
```

Este es uno de los mapas mentales más importantes que debemos conservar.

---

# 60. La idea más importante del día

La cadena fundamental es:

```text
IP
```

responde:

> ¿Qué host?

```text
puerto
```

responde:

> ¿Qué endpoint de transporte?

```text
socket
```

responde conceptualmente:

> ¿Qué objeto de comunicación administra el kernel para el proceso?

```text
proceso
```

responde:

> ¿Qué programa está utilizando ese socket?

Y:

```text
TCP state
```

responde:

> ¿En qué estado se encuentra esa relación TCP?

Por ejemplo:

```text
127.0.0.1:8080
       │
       ▼
socket TCP
       │
       ▼
LISTEN
       │
       ▼
python PID 1234
```

Después aparece un cliente:

```text
127.0.0.1:53214
       │
       │ TCP
       ▼
127.0.0.1:8080
       │
       ▼
ESTABLISHED
```

---

# 61. Conexión con el direccionamiento estudiado anteriormente

Durante el Día 8 vimos, por ejemplo:

```text
172.18.143.87/20
```

y:

```text
172.18.128.1
```

como conceptos de direccionamiento y routing.

El Día 9 no reemplaza ese conocimiento.

Lo extiende.

Ahora podemos pensar:

```text
172.18.143.87
       │
       │ routing
       ▼
destino
       │
       ▼
IP + puerto
       │
       ▼
TCP
       │
       ▼
socket
       │
       ▼
proceso
```

Por ejemplo:

```bash
curl 172.18.143.100:8080
```

ya no deberíamos interpretarlo simplemente como:

> curl va a esa IP.

Debemos comenzar a pensar en toda la cadena.

---

# 62. Modelo mental de troubleshooting

Ante:

```bash
curl 172.18.143.100:8080
```

podemos preguntarnos:

```text
¿cómo resuelvo la IP?
        ↓
¿qué ruta tiene el kernel?
        ↓
¿por qué interfaz sale?
        ↓
¿llega al host?
        ↓
¿hay TCP/8080?
        ↓
¿hay un LISTEN?
        ↓
¿en qué dirección está ligado?
        ↓
¿qué proceso tiene el socket?
        ↓
¿se establece la conexión?
        ↓
¿qué pasa después?
```

Este es exactamente el tipo de pensamiento que queremos construir durante la Semana 2.

No se trata únicamente de memorizar comandos.

Se trata de poder seguir el camino de una comunicación y localizar en qué punto puede estar fallando.

---

# 63. Relación entre las herramientas

Podemos construir una visión conjunta de las herramientas estudiadas hasta ahora.

## `ps`

Pregunta:

```text
¿Qué procesos existen?
```

Observa principalmente:

```text
PID
CPU
MEM
estado
COMMAND
```

---

## `lsof`

Pregunta:

```text
¿Qué recursos abiertos tiene este proceso?
```

Puede relacionar:

```text
proceso
PID
FD
socket
IP
puerto
```

---

## `ss`

Pregunta:

```text
¿Qué sockets/conexiones de red existen?
```

Puede mostrarnos:

```text
estado
Recv-Q
Send-Q
Local Address:Port
Peer Address:Port
proceso
PID
FD
```

Por eso podemos ir cambiando la perspectiva:

```text
ps
 ↓
proceso

lsof
 ↓
recursos abiertos por proceso

ss
 ↓
sockets y conexiones de red
```

Las tres perspectivas se complementan.

---

# 64. Cadena completa de un servidor TCP

Un servidor TCP puede visualizarse así:

```text
Proceso
   │
   │ socket()
   ▼
Socket
   │
   │ bind()
   ▼
IP:puerto
   │
   │ listen()
   ▼
LISTEN
   │
   │ llega una conexión
   ▼
accept()
   │
   ▼
socket de conexión
   │
   ▼
ESTABLISHED
```

Mientras tanto, el cliente tiene algo parecido a:

```text
Proceso cliente
       │
       ▼
socket
       │
       ▼
puerto efímero
       │
       ▼
connect()
       │
       ▼
TCP handshake
       │
       ▼
ESTABLISHED
```

Esto explica por qué podemos tener simultáneamente:

```text
LISTEN
```

y:

```text
ESTABLISHED
```

en el mismo puerto servidor.

---

# 65. Errores conceptuales que debemos evitar

## Error 1 — "El puerto es el proceso"

Incorrecto:

```text
8080 = proceso
```

Correcto:

```text
proceso
   ↓
file descriptor
   ↓
socket
   ↓
endpoint
```

---

## Error 2 — "`LISTEN` significa que hay un cliente conectado"

Incorrecto.

`LISTEN` significa:

> El socket servidor está preparado para recibir conexiones.

Una conexión concreta se representa mediante otro estado, como:

```text
ESTABLISHED
```

---

## Error 3 — "Si no veo `ESTABLISHED`, nunca hubo conexión"

Incorrecto.

`ss` proporciona una fotografía del estado actual.

Una conexión muy rápida, como la de `curl`, puede haber terminado antes de que ejecutemos:

```bash
ss -antp
```

---

## Error 4 — "`127.0.0.1` significa que cualquier máquina puede acceder"

Incorrecto.

```text
127.0.0.1
```

significa:

> este propio host.

Un servicio ligado únicamente a loopback no está escuchando directamente en la interfaz de red para otros hosts.

---

## Error 5 — "`0.0.0.0` es una IP a la que debo conectarme"

Incorrecto.

En un `bind`, `0.0.0.0` es una dirección comodín para las direcciones IPv4 locales disponibles.

No es una dirección que el cliente deba utilizar como destino.

---

## Error 6 — "IP + puerto explica toda la conexión"

No completamente.

Para identificar una conexión TCP concreta necesitamos considerar ambos extremos:

```text
IP origen
puerto origen
IP destino
puerto destino
```

Es decir, la 4-tupla.

---

## Error 7 — "TCP y HTTP son lo mismo"

No.

Son capas diferentes:

```text
HTTP
 ↓
TCP
 ↓
IP
 ↓
Ethernet / Wi-Fi
```

`ss` está principalmente observando la parte de sockets/TCP.

---

# 66. Checklist conceptual del Día 9

Al terminar este día deberíamos poder explicar, sin depender de memorizar una definición aislada:

* qué problema resuelve el puerto;
* por qué IP y puerto son cosas diferentes;
* qué es un socket;
* por qué el socket aparece como file descriptor;
* cómo se relacionan proceso, FD y socket;
* qué significa que un servidor esté escuchando;
* qué significa `LISTEN`;
* qué significa `ESTABLISHED`;
* por qué un servidor puede tener `LISTEN` y `ESTABLISHED` simultáneamente;
* qué es un puerto efímero;
* qué es una 4-tupla TCP;
* cómo se distinguen varias conexiones hacia el mismo puerto servidor;
* qué significa `127.0.0.1`;
* qué significa `0.0.0.0` al hacer `bind`;
* por qué `127.0.0.1:8080` puede funcionar localmente pero no desde otro host;
* qué hace `ss`;
* qué significa cada opción de `ss -lntp`;
* qué diferencia existe entre `ss -lntp` y `ss -antp`;
* qué representan `Recv-Q` y `Send-Q` a nivel introductorio;
* qué significa `Local Address:Port`;
* qué significa `Peer Address:Port`;
* qué diferencia existe entre `ps`, `lsof` y `ss`;
* qué es el three-way handshake;
* qué significan `SYN`, `SYN-ACK` y `ACK`;
* por qué una conexión puede desaparecer rápidamente de `ss`;
* por qué `ss` es una herramienta de troubleshooting;
* cómo conectar el modelo del Día 8 con el modelo del Día 9.

---

# 67. Comandos utilizados

## Inspeccionar sockets TCP escuchando

```bash
ss -lntp
```

Significado:

```text
-l → listening
-n → numeric
-t → TCP
-p → process
```

---

## Inspeccionar todos los sockets TCP relevantes

```bash
ss -antp
```

Significado:

```text
-a → all
-n → numeric
-t → TCP
-p → process
```

---

## Levantar servidor HTTP de prueba

```bash
python3 -m http.server 8080 --bind 127.0.0.1
```

Crea un servidor HTTP sencillo escuchando en:

```text
127.0.0.1:8080
```

---

## Hacer una petición al servidor

```bash
curl http://127.0.0.1:8080
```

Provoca una conexión TCP y una petición HTTP.

---

## Observar continuamente

```bash
watch -n 0.1 'ss -antp'
```

Ejecuta `ss -antp` cada 0.1 segundos para intentar capturar los cambios rápidos de estado.

---

## Relacionar recursos abiertos con un proceso

```bash
lsof -p <PID>
```

Puede mostrar el socket como un file descriptor del proceso.

---

# 68. Secuencia completa del laboratorio

La secuencia completa propuesta para el laboratorio es:

```bash
ss -lntp
```

```bash
ss -antp
```

Después:

```bash
python3 -m http.server 8080 --bind 127.0.0.1
```

En otra terminal:

```bash
ss -lntp
```

Después:

```bash
curl http://127.0.0.1:8080
```

Inmediatamente:

```bash
ss -antp
```

Y finalmente:

```bash
watch -n 0.1 'ss -antp'
```

Mientras se ejecuta `curl`.

La finalidad no es únicamente comprobar que Python responde.

La finalidad es poder seguir conceptualmente:

```text
proceso Python
      ↓
file descriptor
      ↓
socket
      ↓
127.0.0.1:8080
      ↓
LISTEN
      ↓
cliente curl
      ↓
puerto efímero
      ↓
TCP handshake
      ↓
ESTABLISHED
      ↓
HTTP request/response
      ↓
cierre
      ↓
otro estado TCP
```

---

# 69. Conclusión del Día 9

El Día 8 nos permitió comprender cómo el kernel determina **hacia qué host** debe dirigirse una comunicación:

```text
nombre
 ↓
DNS
 ↓
IP
 ↓
routing
 ↓
interfaz
 ↓
gateway / red local
 ↓
host destino
```

El Día 9 añade la parte que faltaba:

```text
host destino
 ↓
puerto
 ↓
socket
 ↓
proceso
```

Y cuando hablamos específicamente de TCP, añadimos:

```text
cliente IP:puerto
        │
        │ TCP
        ▼
servidor IP:puerto
        │
        ▼
estado TCP
```

La cadena mental completa queda:

```text
NOMBRE
   │
   │ DNS
   ▼
IP
   │
   │ routing
   ▼
HOST
   │
   ▼
PUERTO
   │
   ▼
SOCKET
   │
   ▼
PROCESO
```

Y para una conexión concreta:

```text
IP origen:puerto origen
          │
          │ TCP
          ▼
IP destino:puerto destino
          │
          ▼
     4-tupla TCP
          │
          ▼
     estado TCP
```

La idea fundamental que debemos conservar es:

> **Llegar a una IP no significa que la aplicación sea accesible. Primero debemos llegar al host correcto; después debe existir un endpoint TCP en el puerto correcto; el servicio debe estar escuchando en una dirección accesible; la red y el firewall deben permitir la comunicación; y finalmente TCP debe poder establecer la conexión.**

Por eso `ss` se convierte en una herramienta de troubleshooting muy importante.

Ante un problema de conectividad hacia una aplicación, podemos comenzar a preguntar:

```text
¿Existe un LISTEN?
        ↓
¿En qué IP está escuchando?
        ↓
¿En qué puerto?
        ↓
¿Qué proceso tiene el socket?
        ↓
¿Hay conexiones ESTABLISHED?
        ↓
¿Quiénes son los peers?
        ↓
¿Qué estados TCP aparecen?
        ↓
¿Dónde está fallando la comunicación?
```

Esto conecta directamente los conocimientos de procesos, file descriptors, Linux, routing y TCP que hemos construido durante los días anteriores y prepara el terreno para estudiar posteriormente los demás estados TCP, colas, conexiones persistentes, `TIME-WAIT`, `CLOSE-WAIT`, diagnóstico de conexiones y, posteriormente, cómo estos conceptos aparecen dentro de Docker y Kubernetes.

---

# 70. Referencia rápida

```text
IP
→ identifica el host

Puerto
→ identifica un endpoint de transporte

Socket
→ abstracción de comunicación proporcionada por el kernel

File descriptor
→ descriptor mediante el cual el proceso accede al socket

Proceso
→ programa que utiliza el socket

LISTEN
→ socket TCP preparado para aceptar conexiones

ESTABLISHED
→ conexión TCP establecida

Puerto efímero
→ puerto normalmente seleccionado automáticamente para el extremo cliente

4-tupla
→ IP origen + puerto origen + IP destino + puerto destino

127.0.0.1
→ loopback / propio host

0.0.0.0
→ bind comodín IPv4; no es una dirección de destino para clientes

ss
→ inspección de sockets y conexiones

ss -lntp
→ sockets TCP escuchando + números + procesos

ss -antp
→ sockets TCP relevantes + números + procesos

ps
→ perspectiva de procesos

lsof
→ perspectiva de recursos abiertos por procesos

TCP
→ transporte orientado a conexión y con estado

SYN
→ inicio de conexión

SYN-ACK
→ respuesta del servidor al inicio

ACK
→ confirmación

HTTP
→ protocolo de aplicación que utiliza TCP para transportar sus datos
```
