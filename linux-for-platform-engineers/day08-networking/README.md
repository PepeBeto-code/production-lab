# Día 8 — Network Basics: interfaces, IP, routing y DNS

## Objetivo

Construir una primera comprensión práctica de cómo una máquina Linux sabe:

- qué interfaces de red tiene;
- qué direcciones IP tiene asignadas;
- qué redes están directamente conectadas;
- qué gateway utilizar para destinos fuera de sus redes locales;
- qué servidor DNS utiliza;
- y cómo distinguir un problema de resolución DNS de un problema de conectividad/routing.

La intención de este laboratorio no es memorizar `ip a`, `ip r` o `resolvectl`, sino aprender a leer la información que proporcionan y relacionarla con lo que el kernel está haciendo.

---

# 1. Modelo mental: de una aplicación hacia la red

Cuando una aplicación necesita comunicarse con un servicio identificado por un nombre, por ejemplo:

```bash
curl https://example.com
```

hay varias decisiones diferentes.

Primero necesitamos resolver:

```text
example.com
    ↓
dirección IP
```

Eso corresponde a DNS.

Una vez obtenida la IP, la máquina necesita decidir cómo alcanzar ese destino:

```text
IP destino
    ↓
tabla de routing
    ↓
interfaz + siguiente salto/gateway
```

Por tanto, para este día debemos separar dos problemas:

```text
DNS
nombre → IP

Routing
IP destino → interfaz / siguiente salto
```

Esto es importante durante troubleshooting. Que un nombre no pueda resolverse no significa necesariamente que la red completa esté caída; y que DNS funcione no garantiza que una conexión TCP o HTTP vaya a funcionar.

El flujo conceptual que estamos construyendo es:

```text
Aplicación
    ↓
DNS
    ↓
IP destino
    ↓
Routing
    ↓
Interfaz
    ↓
Gateway / red local
    ↓
red remota
```

TCP, puertos, HTTP, TLS y el funcionamiento profundo de DNS se estudiarán posteriormente.

---

# 2. Interfaces de red

Una interfaz de red es el punto mediante el cual el kernel puede enviar y recibir tráfico de red.

No debe entenderse simplemente como "una tarjeta de red". Puede representar hardware físico o una interfaz virtual. En entornos como máquinas virtuales, WSL, Docker y Kubernetes aparecerán distintas clases de interfaces.

Por ejemplo:

```text
lo
eth0
ens33
enp0s3
wlan0
```

El nombre depende del sistema.

Por eso, en diagnóstico no conviene asumir que la interfaz siempre será `eth0`. Primero se observa el sistema.

---

# 3. Loopback: `lo`

Una interfaz que normalmente aparece en Linux es:

```text
lo
```

Es la interfaz de loopback.

Su dirección IPv4 habitual es:

```text
127.0.0.1/8
```

Una comunicación hacia:

```text
127.0.0.1
```

permanece dentro de la propia máquina.

Conceptualmente:

```text
Proceso A
    ↓
kernel
    ↓
lo
    ↓
Proceso B
```

No representa una comunicación que tenga que salir hacia el router o Internet.

Esto será importante posteriormente al estudiar servidores y sockets. Por ejemplo, un servicio que escucha únicamente en `127.0.0.1:8080` no está expuesto de la misma manera que uno que escucha en una dirección accesible desde otras máquinas.

---

# 4. `ip a`

Para observar las interfaces y sus direcciones:

```bash
ip a
```

o:

```bash
ip address
```

Una salida típica puede contener algo parecido a:

```text
1: lo: <LOOPBACK,UP,LOWER_UP> ...
    inet 127.0.0.1/8 scope host lo

2: eth0: <BROADCAST,MULTICAST,UP,LOWER_UP> ...
    inet 192.168.1.25/24 brd 192.168.1.255 scope global eth0
```

No todas las máquinas tendrán exactamente esa salida.

## 4.1 Índice de interfaz

Por ejemplo:

```text
1: lo:
2: eth0:
```

El número es el índice de la interfaz dentro del sistema. No es un PID ni está relacionado con procesos.

## 4.2 Nombre

```text
lo
eth0
```

Identifica la interfaz.

## 4.3 `UP`

Si una interfaz muestra:

```text
UP
```

está administrativamente habilitada.

Esto no significa por sí solo que Internet funcione.

Una interfaz puede estar `UP` y aun así tener problemas de:

- dirección IP;
- routing;
- gateway;
- DNS;
- conectividad externa;
- firewall;
- etc.

## 4.4 `LOWER_UP`

Cuando aparece:

```text
UP,LOWER_UP
```

`LOWER_UP` indica que la capa inferior de enlace está reportando el enlace activo.

En una interfaz Ethernet física se relaciona con la existencia de link.

Pero tampoco significa:

> "Internet funciona."

Es solamente otra pieza de evidencia.

---

# 5. IPv4: `inet` y el prefijo

En:

```text
inet 192.168.1.25/24
```

tenemos dos componentes:

```text
IP:
192.168.1.25

prefijo:
 /24
```

El `/24` indica cuántos bits forman parte del prefijo de red.

En este ejemplo, la red es:

```text
192.168.1.0/24
```

Por eso no basta con mirar solamente la IP. Para entender routing necesitamos conocer también el prefijo.

Una máquina con:

```text
192.168.1.25/24
```

puede determinar que:

```text
192.168.1.80
```

pertenece a la misma red:

```text
192.168.1.0/24
```

mientras que:

```text
8.8.8.8
```

no pertenece a ella.

Esa diferencia será fundamental para decidir si un destino es directamente alcanzable o debe utilizarse un gateway.

---

# 6. `brd`

Una interfaz IPv4 puede mostrar algo como:

```text
brd 192.168.1.255
```

Para:

```text
192.168.1.0/24
```

esa es la dirección de broadcast tradicional.

No necesitamos profundizar en broadcast en este día. Lo importante es reconocer que `ip a` proporciona más información que solamente la IP.

---

# 7. `scope`

También pueden aparecer valores como:

```text
scope host
```

o:

```text
scope global
```

Por ejemplo:

```text
127.0.0.1/8 scope host
```

corresponde a una dirección del propio host.

Una dirección de red normal puede aparecer con:

```text
scope global
```

El `scope` describe el ámbito en el que la dirección es válida/alcanzable.

---

# 8. IPv6

`ip a` también puede mostrar:

```text
inet6
```

por ejemplo:

```text
inet6 ::1/128
```

La distinción básica es:

```text
inet
    → IPv4

inet6
    → IPv6
```

IPv6 no se estudia en profundidad en este laboratorio, pero es importante no interpretar una salida con `inet6` como si fuera IPv4.

---

# 9. Routing: por qué una IP sale por una interfaz u otra

Tener una IP no significa que la máquina sepa llegar a cualquier destino.

Supongamos:

```text
IP local:
192.168.1.25/24
```

Entonces la máquina conoce que:

```text
192.168.1.0/24
```

es una red directamente conectada.

Si el destino es:

```text
192.168.1.80
```

está dentro de esa red.

Pero si el destino es:

```text
8.8.8.8
```

está fuera de ella.

La máquina necesita entonces una ruta que indique qué hacer con ese tráfico.

---

# 10. `ip r`

La tabla de rutas se puede observar con:

```bash
ip r
```

o:

```bash
ip route
```

Una salida típica:

```text
default via 192.168.1.1 dev eth0
192.168.1.0/24 dev eth0 proto kernel scope link src 192.168.1.25
```

Hay dos rutas importantes.

## 10.1 Red directamente conectada

```text
192.168.1.0/24 dev eth0
```

Significa que esa red está directamente conectada mediante `eth0`.

## 10.2 Ruta por defecto

```text
default via 192.168.1.1 dev eth0
```

`default` representa conceptualmente:

```text
0.0.0.0/0
```

Es la ruta utilizada cuando no existe una ruta más específica que coincida con el destino.

`via` significa:

> a través de.

Por tanto:

```text
default via 192.168.1.1 dev eth0
```

puede leerse como:

> Para destinos que no tengan una ruta más específica, utiliza `192.168.1.1` como siguiente salto, mediante `eth0`.

`192.168.1.1` es el gateway/next hop.

---

# 11. La diferencia fundamental: destino directo vs `via`

Esta fue una de las partes centrales del laboratorio.

En nuestro entorno WSL obtuvimos:

```text
default via 172.18.128.1 dev eth0 proto kernel
172.18.128.0/20 dev eth0 proto kernel scope link src 172.18.143.87
```

La máquina tiene:

```text
IP:
172.18.143.87/20

red:
172.18.128.0/20

gateway:
172.18.128.1

interfaz:
eth0
```

## 11.1 Gateway directamente alcanzable

Al ejecutar:

```bash
ip route get 172.18.128.1
```

obtuvimos:

```text
172.18.128.1 dev eth0 src 172.18.143.87
```

No aparece `via`.

¿Por qué?

Porque:

```text
172.18.143.87/20
```

y:

```text
172.18.128.1
```

están dentro de:

```text
172.18.128.0/20
```

Por lo tanto el gateway está directamente conectado a la red local.

Conceptualmente:

```text
172.18.143.87
       │
       │ eth0
       ▼
172.18.128.1
```

No necesitamos otro router para alcanzar ese destino.

A nivel de red local, la entrega se realiza mediante la capa de enlace; en IPv4, el host puede utilizar ARP para obtener la dirección MAC correspondiente.

## 11.2 Destino remoto

Al ejecutar:

```bash
ip route get 8.8.8.8
```

obtuvimos:

```text
8.8.8.8 via 172.18.128.1 dev eth0 src 172.18.143.87
```

Esto significa:

```text
Destino:
8.8.8.8

Next hop / gateway:
172.18.128.1

Interfaz:
eth0

IP de origen:
172.18.143.87
```

La palabra `via` significa literalmente:

> Para llegar al destino, pasa primero por este siguiente salto.

Conceptualmente:

```text
TU MÁQUINA
172.18.143.87
       │
       │ eth0
       ▼
172.18.128.1
    gateway
       │
       ▼
   otros routers
       │
       ▼
    8.8.8.8
```

Importante: `via 172.18.128.1` **no significa que el destino del paquete cambie a `172.18.128.1`**. El destino IP continúa siendo `8.8.8.8`; `172.18.128.1` es el siguiente salto al que se entrega el paquete para que continúe su recorrido.

---

# 12. `dev` y `src`

En:

```text
8.8.8.8 via 172.18.128.1 dev eth0 src 172.18.143.87
```

tenemos:

### Destino

```text
8.8.8.8
```

Es la dirección a la que queremos llegar.

### `via`

```text
via 172.18.128.1
```

Es el siguiente salto/gateway.

### `dev`

```text
dev eth0
```

Es la interfaz de nuestra propia máquina que se utilizará.

No significa que el gateway "esté dentro" de `eth0`. Significa que nuestra máquina alcanza ese gateway a través de esa interfaz.

### `src`

```text
src 172.18.143.87
```

Es la dirección IP de origen que el kernel elegiría para ese tráfico.

---

# 13. `ip route get`: preguntar directamente al kernel

Una herramienta especialmente útil para troubleshooting es:

```bash
ip route get <destino>
```

Por ejemplo:

```bash
ip route get 8.8.8.8
```

En lugar de mostrar solamente toda la tabla, podemos preguntar:

> ¿Qué ruta elegirías para llegar exactamente a este destino?

En nuestro caso:

```text
8.8.8.8 via 172.18.128.1 dev eth0 src 172.18.143.87
```

Esto es muy útil durante una investigación porque permite comprobar la decisión concreta del kernel.

---

# 14. Comparación de nuestras tres pruebas

## Destino local de la propia máquina

```bash
ip route get 127.0.0.1
```

Resultado:

```text
local 127.0.0.1 dev lo src 127.0.0.1
```

Interpretación:

> `127.0.0.1` es una dirección local del propio host y se utiliza `lo`.

## Gateway en la red local

```bash
ip route get 172.18.128.1
```

Resultado:

```text
172.18.128.1 dev eth0 src 172.18.143.87
```

Interpretación:

> El gateway está directamente conectado a nuestra red y se alcanza por `eth0`.

## Destino remoto

```bash
ip route get 8.8.8.8
```

Resultado:

```text
8.8.8.8 via 172.18.128.1 dev eth0 src 172.18.143.87
```

Interpretación:

> `8.8.8.8` está fuera de nuestra red local, por lo que utilizamos `172.18.128.1` como siguiente salto mediante `eth0`.

---

# 15. DNS: resolución no es conexión

Una confusión que debemos evitar desde el principio:

```text
DNS ≠ conexión al servicio
```

DNS resuelve nombres.

Por ejemplo:

```text
example.com
      ↓
DNS
      ↓
dirección IP
```

Una vez obtenida la IP, todavía tenemos que establecer la comunicación con el servicio.

Conceptualmente:

```text
example.com
     │
     │ DNS
     ▼
IP del servidor
     │
     │ routing
     ▼
interfaz/gateway
     │
     │ posteriormente
     ▼
TCP
     │
     ▼
puerto
     │
     ▼
HTTP
```

Por eso un fallo DNS y un fallo de conexión son problemas diferentes.

Por ejemplo, es posible tener:

```text
DNS funcionando
```

pero que la conexión TCP al puerto del servicio falle.

También puede ocurrir:

```text
conectividad IP funcionando
```

pero que DNS no pueda resolver el nombre.

Esta separación será fundamental cuando estudiemos troubleshooting de red.

---

# 16. DNS en Linux: `resolvectl`

Para inspeccionar el estado de resolución cuando el sistema utiliza `systemd-resolved` podemos utilizar:

```bash
resolvectl status
```

`resolvectl` es la herramienta para consultar/controlar el servicio de resolución de nombres de `systemd-resolved`.

Es importante distinguir tres conceptos:

```text
Nombre
    example.com

Resolver
    componente que gestiona/resuelve nombres

DNS server
    servidor al que se realizan las consultas DNS
```

Resolver y DNS server no son la misma cosa.

Conceptualmente:

```text
Aplicación
    │
    │ "¿Qué IP tiene example.com?"
    ▼
resolver
    │
    │ consulta
    ▼
DNS server
    │
    │ respuesta
    ▼
IP
```

---

# 17. `Global` en `resolvectl`

En nuestro entorno vimos:

```text
Global
    Current DNS Server: 10.255.255.254
    DNS Servers: 10.255.255.254
```

`Global` **no es una interfaz de red**.

Representa una configuración DNS de ámbito global, no asociada específicamente a `eth0`.

Después aparece:

```text
Link 2 (eth0)
    Current Scopes: none
    Default Route: no
```

Esto significa que `systemd-resolved` no tiene una configuración DNS específica activa para ese enlace.

No significa:

- que `eth0` esté apagada;
- que `eth0` no tenga IP;
- que `eth0` no tenga conectividad;
- ni que Linux carezca de una ruta por defecto.

En particular, nuestro `ip r` sí muestra:

```text
default via 172.18.128.1 dev eth0
```

El `Default Route: no` mostrado dentro de `resolvectl` se refiere a la selección/configuración de rutas para **DNS dentro de `systemd-resolved`**, no a la tabla de routing IP del kernel.

Esta distinción evita confundir:

```text
ip r
```

con:

```text
resolvectl
```

---

# 18. `/etc/resolv.conf`

También inspeccionamos:

```bash
cat /etc/resolv.conf
```

En nuestro WSL apareció:

```text
# This file was automatically generated by WSL.
...
nameserver 10.255.255.254
```

La línea:

```text
nameserver 10.255.255.254
```

indica el servidor DNS que aparece configurado para la resolución de nombres a través de esa configuración.

En nuestro caso, la dirección coincidía con la mostrada por `resolvectl`:

```text
resolvectl:
10.255.255.254

/etc/resolv.conf:
10.255.255.254
```

No debe asumirse como regla universal que ambas salidas siempre serán idénticas. Representan puntos de observación/configuración relacionados, pero Linux puede estar configurado de distintas maneras y distintos componentes pueden intervenir.

En nuestro caso concreto de WSL, sí coincidieron.

Además, `/etc/resolv.conf` indicaba explícitamente:

```text
This file was automatically generated by WSL.
```

Por lo tanto, en este entorno WSL debemos tener presente que WSL participa en la generación de la configuración de resolución.

---

# 19. Particularidades observadas en nuestro WSL

Nuestra salida de `ip a` fue:

```text
1: lo: <LOOPBACK,UP,LOWER_UP> ...
    inet 127.0.0.1/8 scope host lo
    inet 10.255.255.254/32 ... scope global lo

2: eth0: <BROADCAST,MULTICAST,UP,LOWER_UP> ...
    inet 172.18.143.87/20 brd 172.18.143.255 scope global eth0
```

Esto muestra dos detalles importantes:

### Loopback

```text
127.0.0.1/8
```

para comunicaciones locales.

También aparece:

```text
10.255.255.254/32
```

en `lo`, como parte de la configuración particular del entorno WSL.

### Interfaz de red

```text
172.18.143.87/20
```

en:

```text
eth0
```

La red correspondiente es:

```text
172.18.128.0/20
```

y el gateway:

```text
172.18.128.1
```

---

# 20. Nuestro mapa de red

A partir de las salidas reales podemos construir:

```text
                         WSL
                          │
             ┌────────────┴────────────┐
             │                         │
            lo                        eth0
             │                         │
      127.0.0.1/8             172.18.143.87/20
      10.255.255.254/32                │
             │                         │
             │                  172.18.128.0/20
             │                         │
             │                  172.18.128.1
             │                     gateway
             │                         │
             └───────────────┬─────────┘
                             │
                         red externa
```

Y para DNS:

```text
/etc/resolv.conf
        │
        ▼
10.255.255.254
```

`resolvectl` también mostró:

```text
Global
    DNS Servers: 10.255.255.254
```

---

# 21. Flujo completo en nuestro entorno

Si una aplicación ejecuta:

```bash
curl https://example.com
```

el modelo conceptual que debemos tener hasta este punto es:

```text
                    curl
                      │
                      │ necesita resolver
                      ▼
                example.com
                      │
                      │ DNS
                      ▼
              10.255.255.254
                      │
                      ▼
                  IP destino
                      │
                      │ routing
                      ▼
              tabla de rutas
                      │
                      ▼
              172.18.128.1
                  gateway
                      │
                      ▼
                 red externa
```

Todavía faltan las capas que estudiaremos después:

```text
TCP
↓
puerto
↓
TLS
↓
HTTP
```

Por eso no debemos saltar directamente de:

```text
example.com
```

a:

```text
"Internet"
```

Hay varias decisiones intermedias.

---

# 22. Troubleshooting: cómo pensar en capas

Una de las metas de este día es comenzar a investigar un síntoma de forma ordenada.

Supongamos:

```text
"El servicio no responde."
```

No sabemos todavía qué está fallando.

Podemos formular hipótesis:

### Hipótesis 1 — interfaz

```bash
ip a
```

Preguntar:

- ¿existe la interfaz?
- ¿está `UP`?
- ¿tiene una IP?
- ¿qué prefijo tiene?

### Hipótesis 2 — routing

```bash
ip r
```

Preguntar:

- ¿existe una ruta por defecto?
- ¿qué gateway utiliza?
- ¿qué interfaz utiliza?

### Hipótesis 3 — ruta concreta

```bash
ip route get <IP>
```

Preguntar:

> ¿Qué haría realmente el kernel con este destino?

### Hipótesis 4 — DNS

```bash
resolvectl status
cat /etc/resolv.conf
```

Preguntar:

- ¿hay un DNS configurado?
- ¿es global o específico de un enlace?
- ¿qué servidor DNS aparece?

### Hipótesis posteriores

Si DNS y routing están bien, todavía pueden existir problemas de:

```text
TCP
puerto
firewall
TLS
HTTP
aplicación
```

Esas capas se estudiarán después.

---

# 23. Errores de interpretación que debemos evitar

## Error 1

> "La interfaz está `UP`, entonces tengo Internet."

Incorrecto.

`UP` es solamente una pieza de información sobre el estado de la interfaz.

## Error 2

> "`via` es la IP del destino."

Incorrecto.

En:

```text
8.8.8.8 via 172.18.128.1
```

el destino es:

```text
8.8.8.8
```

y:

```text
172.18.128.1
```

es el siguiente salto.

## Error 3

> "`dev eth0` significa que el gateway está dentro de eth0."

Incorrecto.

Significa que nuestra máquina utiliza `eth0` para alcanzar ese siguiente salto.

## Error 4

> "`Default Route: no` en `resolvectl` significa que no tengo default route."

Incorrecto.

Ese `Default Route` pertenece al contexto de resolución DNS de `systemd-resolved`.

La ruta IP por defecto se observa con:

```bash
ip r
```

## Error 5

> "DNS y conexión son lo mismo."

Incorrecto.

DNS:

```text
nombre → IP
```

Routing:

```text
IP destino → cómo salir
```

TCP/puerto/HTTP son etapas posteriores.

---

# 24. Comandos utilizados

Durante el laboratorio utilizamos:

```bash
ip a
```

Para inspeccionar interfaces y direcciones.

```bash
ip r
```

Para consultar la tabla de rutas.

```bash
ip route get 8.8.8.8
```

Para consultar la decisión de routing para un destino concreto.

```bash
ip route get 127.0.0.1
```

Para observar el tratamiento de una dirección local.

```bash
ip route get 172.18.128.1
```

Para observar el tratamiento de un destino directamente conectado.

```bash
resolvectl status
```

Para consultar el estado de resolución DNS cuando `systemd-resolved` está presente.

```bash
cat /etc/resolv.conf
```

Para observar la configuración de servidores DNS expuesta mediante ese archivo.

---

# 25. Evidencia real del laboratorio

## Tabla de rutas

```text
default via 172.18.128.1 dev eth0 proto kernel
172.18.128.0/20 dev eth0 proto kernel scope link src 172.18.143.87
```

Interpretación:

- la red local es `172.18.128.0/20`;
- se alcanza directamente por `eth0`;
- la IP local utilizada es `172.18.143.87`;
- para destinos sin una ruta más específica se utiliza `172.18.128.1` como gateway.

## Ruta hacia Internet

```text
8.8.8.8 via 172.18.128.1 dev eth0 src 172.18.143.87
```

Interpretación:

- destino: `8.8.8.8`;
- siguiente salto: `172.18.128.1`;
- interfaz: `eth0`;
- IP de origen: `172.18.143.87`.

## Ruta hacia el gateway

```text
172.18.128.1 dev eth0 src 172.18.143.87
```

Interpretación:

El gateway pertenece a la red local y por eso puede alcanzarse directamente, sin otro `via`.

## Ruta hacia loopback

```text
local 127.0.0.1 dev lo src 127.0.0.1
```

Interpretación:

El destino pertenece al propio host y se utiliza `lo`.

## DNS

`resolvectl status` mostró:

```text
Global
    Current DNS Server: 10.255.255.254
    DNS Servers: 10.255.255.254
```

Mientras que `eth0` mostró:

```text
Link 2 (eth0)
    Current Scopes: none
    Default Route: no
```

Y `/etc/resolv.conf` contenía:

```text
nameserver 10.255.255.254
```

Interpretación:

El DNS aparece configurado globalmente en `resolvectl`, no específicamente en `eth0`, y la configuración expuesta por `/etc/resolv.conf` utiliza el mismo servidor DNS `10.255.255.254`.

---

# 26. Conclusiones del Día 8

El objetivo principal del día era dejar de pensar en una conexión de red como una sola cosa y empezar a dividirla en componentes observables.

Una máquina Linux necesita conocer:

```text
1. Interfaces
2. Direcciones IP
3. Redes asociadas
4. Rutas
5. Gateway/next hop
6. Configuración de resolución DNS
```

Aprendimos a obtener esa información con:

```text
ip a
ip r
ip route get
resolvectl
/etc/resolv.conf
```

La distinción más importante de routing fue:

```text
Destino directamente conectado
        ↓
dev eth0
```

frente a:

```text
Destino remoto
        ↓
via gateway
        ↓
dev eth0
```

En nuestro entorno:

```text
172.18.128.1
```

está dentro de:

```text
172.18.128.0/20
```

por lo que:

```text
172.18.128.1 dev eth0
```

no necesita `via`.

En cambio:

```text
8.8.8.8
```

está fuera de nuestra red, por lo que:

```text
8.8.8.8 via 172.18.128.1 dev eth0
```

indica que `172.18.128.1` es el siguiente salto.

También establecimos una separación fundamental para el troubleshooting:

```text
DNS:
nombre → IP

Routing:
IP destino → interfaz / siguiente salto
```

Esto permite empezar a investigar problemas de red por capas en lugar de asumir inmediatamente que "no hay Internet".

---

# 27. Lo que todavía NO hemos estudiado

Este día solamente construyó los fundamentos.

Todavía quedan por estudiar en profundidad:

- ARP;
- Ethernet y MAC;
- TCP;
- UDP;
- puertos;
- sockets;
- handshake TCP;
- estados TCP;
- retransmisiones;
- latencia;
- MTU;
- HTTP;
- TLS;
- DNS recursivo;
- caché DNS;
- servidores autoritativos;
- herramientas de captura y diagnóstico como `ss`, `ping`, `traceroute`/`tracepath` y `tcpdump`.

La intención es estudiar cada una de estas piezas después y conectarlas con el modelo construido aquí.

---

# 28. Modelo mental final del Día 8

Cuando una aplicación necesita llegar a un servicio remoto, debemos comenzar a pensar:

```text
¿Tengo una interfaz?
        ↓
¿Está activa?
        ↓
¿Tengo una IP?
        ↓
¿A qué red pertenece?
        ↓
¿Tengo una ruta hacia el destino?
        ↓
¿Es directamente conectado?
        │
        ├── sí → interfaz
        │
        └── no → siguiente salto/gateway → interfaz
        ↓
Si utilizo un nombre:
¿DNS puede resolverlo?
        ↓
nombre → IP
        ↓
routing
        ↓
TCP/puerto
        ↓
HTTP/TLS
```

Este es el punto de partida para el diagnóstico de red que construiremos durante la Semana 2.
