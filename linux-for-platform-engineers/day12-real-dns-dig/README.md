# Día 12 — DNS “de verdad”: `dig`, TTL y registros A/AAAA

## 1. Objetivo del día

El objetivo de este día fue pasar de una idea simplificada de DNS:

```text
hostname
   ↓
DNS
   ↓
IP
```

a entender qué ocurre realmente cuando se realiza una consulta DNS.

Se estudiaron:

* `query` y `response`
* DNS resolver
* registros `A`
* registros `AAAA`
* IPv4 e IPv6
* TTL (`Time To Live`)
* caché DNS
* uso de `dig`
* interpretación de la salida de `dig`
* tiempo de consulta DNS
* comparación conceptual entre diferentes resolvers

La idea es conectar DNS con el modelo aprendido anteriormente:

```text
hostname
   ↓
DNS
   ↓
IP
   ↓
TCP connect
   ↓
HTTP request
   ↓
HTTP response
```

DNS ocurre antes de que el cliente pueda utilizar la dirección IP para establecer la conexión TCP.

---

# 2. ¿Qué es DNS?

DNS significa:

**Domain Name System**

Es el sistema que permite asociar nombres de dominio con diferentes tipos de información, entre ellos direcciones IP.

Por ejemplo:

```text
example.com
     ↓
DNS
     ↓
104.20.23.154
```

Pero esta representación es una simplificación.

DNS no es simplemente una tabla:

```text
nombre → IP
```

DNS utiliza diferentes tipos de registros.

Algunos ejemplos son:

```text
A
AAAA
MX
NS
TXT
CNAME
```

En este laboratorio nos concentramos principalmente en:

```text
A
AAAA
```

---

# 3. DNS query y DNS response

Una **query** es una consulta DNS.

Es decir, una pregunta que un cliente realiza al sistema DNS.

Ejemplo conceptual:

```text
"¿Cuál es el registro A de example.com?"
```

Una **response** es la respuesta recibida.

Conceptualmente:

```text
CLIENTE
   │
   │ DNS query
   │ "¿A de example.com?"
   ▼
DNS resolver
   │
   │ DNS response
   │ "104.20.23.154"
   ▼
CLIENTE
```

Esto tiene cierta similitud con HTTP:

```text
HTTP:
request → response
```

y DNS:

```text
query → response
```

Sin embargo, son protocolos diferentes y sus mensajes tienen estructuras diferentes.

---

# 4. ¿Qué es un DNS resolver?

Un **resolver** es el componente encargado de obtener una respuesta DNS para una consulta.

El modelo simplificado es:

```text
tu computadora
      │
      │ DNS query
      ▼
DNS resolver
      │
      │ obtiene/busca la respuesta
      ▼
DNS response
      │
      ▼
tu computadora
```

El equipo puede estar configurado para utilizar un resolver proporcionado por:

* el router;
* el proveedor de Internet;
* una red empresarial;
* una VPN;
* un servicio DNS público;
* u otra infraestructura DNS.

En nuestro laboratorio, `dig` mostró que la máquina estaba consultando:

```text
10.255.255.254
```

por el puerto:

```text
53
```

La salida fue:

```text
SERVER: 10.255.255.254#53(10.255.255.254) (UDP)
```

Por lo tanto, en este entorno:

```text
PC
 │
 │ DNS query
 ▼
10.255.255.254:53
 │
 │ DNS response
 ▼
PC
```

---

# 5. Registro A

Un registro:

```text
A
```

se utiliza para asociar un nombre DNS con una dirección **IPv4**.

IPv4 utiliza direcciones como:

```text
192.168.1.10
8.8.8.8
104.20.23.154
```

Una dirección IPv4 tiene cuatro partes separadas por puntos.

Cada parte tiene un valor entre:

```text
0 y 255
```

Por ejemplo:

```text
104.20.23.154
```

es una dirección IPv4.

Por lo tanto:

```text
A
↓
IPv4
```

Ejemplo:

```text
example.com.    IN    A    104.20.23.154
```

significa que `example.com` tiene asociada esa dirección IPv4 mediante un registro `A`.

---

# 6. Registro AAAA

Un registro:

```text
AAAA
```

se utiliza para asociar un nombre DNS con una dirección **IPv6**.

IPv6 utiliza direcciones con hexadecimal y `:`.

Ejemplo:

```text
2606:4700:10::6814:179a
```

Por lo tanto:

```text
A
↓
IPv4
```

mientras:

```text
AAAA
↓
IPv6
```

Esta relación es fundamental:

| Registro | Dirección |
| -------- | --------- |
| `A`      | IPv4      |
| `AAAA`   | IPv6      |

---

# 7. Un dominio puede tener múltiples direcciones

Una de las cosas que comprobamos en el laboratorio es que un dominio no necesariamente tiene una sola IP.

En nuestro caso, `example.com` devolvió dos registros `A`:

```text
104.20.23.154
172.66.147.243
```

Por lo tanto:

```text
example.com
   │
   ├── A → 104.20.23.154
   │
   └── A → 172.66.147.243
```

También devolvió dos registros `AAAA`:

```text
2606:4700:10::6814:179a
2606:4700:10::ac42:93f3
```

Por lo tanto:

```text
example.com
   │
   ├── A
   │   ├── 104.20.23.154
   │   └── 172.66.147.243
   │
   └── AAAA
       ├── 2606:4700:10::6814:179a
       └── 2606:4700:10::ac42:93f3
```

Esto demuestra que la relación real puede ser:

```text
hostname
   ↓
múltiples registros
   ↓
múltiples direcciones
```

y no simplemente:

```text
hostname → una única IP
```

---

# 8. ¿Qué es `dig`?

`dig` es una herramienta de línea de comandos utilizada para realizar consultas DNS y mostrar las respuestas obtenidas.

Una forma de utilizarla es:

```bash
dig example.com
```

Conceptualmente significa:

```text
"Realiza una consulta DNS para example.com"
```

`dig` permite especificar el tipo de registro que queremos consultar.

Por ejemplo:

```bash
dig example.com A
```

consulta registros `A`.

Y:

```bash
dig example.com AAAA
```

consulta registros `AAAA`.

---

# 9. Comando: `dig example.com`

Comando:

```bash
dig example.com
```

Realiza una consulta DNS para `example.com`.

La salida contiene diferentes secciones de información.

Entre ellas pueden aparecer:

* información de la consulta;
* información de la respuesta;
* registros obtenidos;
* tiempo de consulta;
* servidor DNS utilizado;
* tamaño de la respuesta.

No toda la salida es necesaria para cada análisis, por eso `dig` tiene opciones para reducirla.

---

# 10. Comando: `dig example.com A`

Comando:

```bash
dig example.com A
```

Significa:

```text
dominio = example.com
tipo = A
```

Por lo tanto:

```text
"Quiero los registros A de example.com"
```

El registro `A` representa IPv4.

---

# 11. Comando: `dig example.com AAAA`

Comando:

```bash
dig example.com AAAA
```

Significa:

```text
dominio = example.com
tipo = AAAA
```

Por lo tanto:

```text
"Quiero los registros AAAA de example.com"
```

El registro `AAAA` representa IPv6.

---

# 12. Comando: `dig example.com +noall +answer`

Comando:

```bash
dig example.com +noall +answer
```

Se utiliza para mostrar una salida mucho más limpia.

## `+noall`

```text
+noall
```

reduce la salida de `dig` y evita mostrar todas las secciones normales.

## `+answer`

```text
+answer
```

indica que queremos mostrar la sección de respuesta.

Por lo tanto:

```bash
dig example.com +noall +answer
```

puede producir algo como:

```text
example.com.    255    IN    A    104.20.23.154
example.com.    255    IN    A    172.66.147.243
```

Esta forma es especialmente útil para estudiar los registros DNS sin tener demasiado ruido en la terminal.

---

# 13. Cómo leer un registro DNS

Obtuvimos:

```text
example.com.    162    IN    A    104.20.23.154
```

Cada campo tiene un significado:

```text
example.com.    162    IN    A    104.20.23.154
      │           │     │    │          │
      │           │     │    │          └── valor
      │           │     │    └──────────── tipo
      │           │     └───────────────── clase
      │           └─────────────────────── TTL
      └─────────────────────────────────── nombre
```

Por lo tanto:

```text
example.com.
```

es el nombre.

```text
162
```

es el TTL observado.

```text
IN
```

es la clase Internet.

```text
A
```

es el tipo de registro.

```text
104.20.23.154
```

es la dirección IPv4.

---

# 14. ¿Qué significa `IN`?

En las respuestas de DNS aparece:

```text
IN
```

`IN` significa:

**Internet**

Es la clase DNS utilizada normalmente para los registros de Internet.

Por ejemplo:

```text
IN A
```

significa:

```text
clase = Internet
tipo = A
```

Y:

```text
IN AAAA
```

significa:

```text
clase = Internet
tipo = AAAA
```

---

# 15. ¿Qué es TTL?

TTL significa:

**Time To Live**

En DNS representa el tiempo asociado con la permanencia de una respuesta en caché.

No significa:

```text
"el dominio desaparecerá después de X segundos"
```

ni:

```text
"el servidor se apagará después de X segundos"
```

El concepto importante es:

```text
respuesta DNS
      ↓
caché
      ↓
TTL
```

El resolver puede almacenar temporalmente una respuesta para evitar tener que obtenerla nuevamente inmediatamente.

---

# 16. ¿Qué es una caché DNS?

Una caché es almacenamiento temporal.

Supongamos que un resolver obtiene:

```text
example.com
A
104.20.23.154
```

Puede almacenar temporalmente esa información:

```text
CACHE

example.com
A
104.20.23.154
TTL restante
```

Si otro cliente pregunta poco después:

```text
"¿Cuál es la IP de example.com?"
```

el resolver puede utilizar la información almacenada en caché en lugar de tener que repetir todo el proceso de obtención de la respuesta.

Esto permite reducir trabajo y mejorar tiempos de respuesta.

---

# 17. TTL como tiempo restante de caché

Supongamos que una respuesta tiene:

```text
TTL = 300
```

300 segundos son:

```text
5 minutos
```

Si el resolver guarda esa respuesta y transcurre tiempo, el TTL observado puede disminuir:

```text
300
299
298
297
...
```

hasta llegar a:

```text
0
```

Cuando la información deja de ser válida para la caché, el resolver deberá obtener una respuesta nueva cuando corresponda.

Conceptualmente:

```text
consulta
   ↓
TTL = 300
   ↓
caché
   ↓
299
   ↓
298
   ↓
...
   ↓
0
   ↓
nueva obtención
   ↓
nuevo TTL
```

---

# 18. Importante: TTL no es lo mismo que el valor del registro

Hay que distinguir:

```text
IP
```

de:

```text
TTL
```

Por ejemplo:

```text
example.com. 300 IN A 104.20.23.154
```

Si posteriormente vemos:

```text
example.com. 250 IN A 104.20.23.154
```

la IP sigue siendo:

```text
104.20.23.154
```

pero el TTL observado cambió:

```text
300 → 250
```

Eso no significa necesariamente que el registro DNS haya cambiado.

Puede simplemente significar que estamos observando el tiempo restante asociado con la respuesta cacheada.

---

# 19. Evidencia real obtenida en el laboratorio

Se realizaron varias consultas:

```bash
dig example.com +noall +answer
```

Primera salida:

```text
example.com.            255     IN      A       104.20.23.154
example.com.            255     IN      A       172.66.147.243
```

Segunda salida:

```text
example.com.            272     IN      A       172.66.147.243
example.com.            272     IN      A       104.20.23.154
```

Después se realizó:

```bash
dig example.com A
```

y se obtuvo:

```text
example.com.            162     IN      A       104.20.23.154
example.com.            162     IN      A       172.66.147.243
```

Se observa que las direcciones IPv4 permanecieron iguales:

```text
104.20.23.154
172.66.147.243
```

mientras que el TTL observado fue diferente.

---

# 20. Observación importante sobre el TTL del laboratorio

Los valores observados fueron:

```text
255
272
162
```

No se debe concluir simplemente:

```text
"el TTL siempre disminuye exactamente cada segundo"
```

porque los resultados no muestran eso.

De hecho:

```text
255 → 272
```

representa un aumento.

La conclusión correcta a partir de estas mediciones es más cuidadosa:

* el registro A observado mantuvo las mismas direcciones;
* el TTL observado cambió;
* el TTL es un valor asociado al estado de la respuesta/caché que se está observando;
* no debemos asumir que cada consulta necesariamente llega al mismo estado de caché;
* una medición aislada no es suficiente para explicar toda la infraestructura DNS que hay detrás.

Esto es un buen ejemplo de troubleshooting:

> No inventar una explicación cuando la evidencia disponible no alcanza para demostrarla.

---

# 21. Salida completa de `dig example.com A`

La consulta:

```bash
dig example.com A
```

produjo:

```text
; <<>> DiG 9.20.18-1ubuntu2.1-Ubuntu <<>> example.com A
;; global options: +cmd
;; Got answer:
;; ->>HEADER<<- opcode: QUERY, status: NOERROR, id: 11171
;; flags: qr rd ra ad; QUERY: 1, ANSWER: 2, AUTHORITY: 0, ADDITIONAL: 0

;; QUESTION SECTION:
;example.com.                   IN      A

;; ANSWER SECTION:
example.com.            162     IN      A       104.20.23.154
example.com.            162     IN      A       172.66.147.243

;; Query time: 19 msec
;; SERVER: 10.255.255.254#53(10.255.255.254) (UDP)
;; WHEN: Tue Aug 18 13:55:24 CST 2026
;; MSG SIZE  rcvd: 61
```

---

# 22. `status: NOERROR`

En la cabecera apareció:

```text
status: NOERROR
```

Esto significa que la consulta DNS terminó sin un error de protocolo DNS de ese tipo.

No significa:

```text
"HTTP funciona"
```

ni:

```text
"la aplicación funciona"
```

ni:

```text
"el servidor web está sano"
```

Significa específicamente que la consulta DNS obtuvo una respuesta sin un error DNS indicado por ese estado.

---

# 23. `QUERY: 1`

La salida mostró:

```text
QUERY: 1
```

Esto indica que hubo una consulta.

En este caso:

```text
example.com IN A
```

---

# 24. `ANSWER: 2`

La salida mostró:

```text
ANSWER: 2
```

Esto indica que la sección de respuesta contiene dos registros.

Y efectivamente:

```text
example.com. 162 IN A 104.20.23.154
example.com. 162 IN A 172.66.147.243
```

son dos registros `A`.

Por eso podemos representar la respuesta:

```text
example.com
   │
   ├── A → 104.20.23.154
   └── A → 172.66.147.243
```

---

# 25. `Query time`

En la consulta A apareció:

```text
;; Query time: 19 msec
```

Esto indica que `dig` observó aproximadamente:

```text
19 ms
```

para esa consulta DNS.

La consulta AAAA posteriormente mostró:

```text
;; Query time: 51 msec
```

Por lo tanto, en esas mediciones concretas:

```text
A
→ 19 ms

AAAA
→ 51 ms
```

Pero no debemos concluir que:

```text
AAAA siempre es más lento que A
```

porque solamente se realizó una medición de cada una.

El tiempo puede variar por múltiples factores.

Una sola medición es evidencia de esa ejecución concreta, no una regla general.

---

# 26. Resolver utilizado

La salida mostró:

```text
;; SERVER: 10.255.255.254#53(10.255.255.254) (UDP)
```

Esto nos permite identificar:

```text
Servidor DNS:
10.255.255.254

Puerto:
53

Transporte observado:
UDP
```

El modelo de esta consulta fue:

```text
dig
 │
 │ DNS query
 ▼
10.255.255.254:53
 │
 │ DNS response
 ▼
dig
```

---

# 27. Consulta AAAA real

Se ejecutó:

```bash
dig example.com AAAA
```

Resultado relevante:

```text
example.com.            150     IN      AAAA    2606:4700:10::6814:179a
example.com.            150     IN      AAAA    2606:4700:10::ac42:93f3
```

Esto demuestra que `example.com` devolvió registros `AAAA`.

Por lo tanto:

```text
example.com
   │
   ├── AAAA → 2606:4700:10::6814:179a
   └── AAAA → 2606:4700:10::ac42:93f3
```

Y confirma que en nuestro entorno pudimos consultar registros IPv6 mediante DNS.

---

# 28. Mapa completo obtenido

Después de las consultas, podemos representar el resultado de esta forma:

```text
example.com
│
├── A
│   ├── 104.20.23.154
│   └── 172.66.147.243
│
└── AAAA
    ├── 2606:4700:10::6814:179a
    └── 2606:4700:10::ac42:93f3
```

Por lo tanto, el DNS no solamente nos dio "la IP".

Nos proporcionó diferentes registros según el tipo de consulta.

---

# 29. Relación con los días anteriores

Antes teníamos:

```text
hostname
   ↓
DNS
   ↓
IP
   ↓
TCP connect
   ↓
HTTP request
   ↓
HTTP response
```

Ahora podemos abrir la parte DNS:

```text
hostname
   ↓
DNS query
   ↓
resolver
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

Esto conecta directamente lo aprendido en los días anteriores.

DNS proporciona la información que permite al cliente saber qué dirección IP utilizar.

Después comienza la parte que ya estudiamos:

```text
IP
   ↓
TCP
   ↓
HTTP
```

---

# 30. Laboratorio realizado

## Objetivo

Comprobar mediante `dig`:

* registros A;
* registros AAAA;
* múltiples direcciones;
* TTL;
* tiempo de consulta;
* resolver utilizado.

## Comandos utilizados

### Consulta general

```bash
dig example.com
```

### Consulta IPv4

```bash
dig example.com A
```

### Consulta IPv6

```bash
dig example.com AAAA
```

### Mostrar solamente la sección de respuesta

```bash
dig example.com +noall +answer
```

---

# 31. Resultados del laboratorio

## Registros A

Se encontraron:

```text
104.20.23.154
172.66.147.243
```

## Registros AAAA

Se encontraron:

```text
2606:4700:10::6814:179a
2606:4700:10::ac42:93f3
```

## Resolver

```text
10.255.255.254
```

## Puerto

```text
53
```

## Transporte observado

```text
UDP
```

## Tiempo observado para A

```text
19 ms
```

## Tiempo observado para AAAA

```text
51 ms
```

## TTL observado

Para diferentes consultas se observaron valores como:

```text
255
272
162
```

Para AAAA:

```text
150
```

---

# 32. Ejercicio de razonamiento

A partir de la evidencia obtenida:

### Pregunta 1

¿El dominio tiene registros A?

**Sí.**

Se observaron:

```text
104.20.23.154
172.66.147.243
```

### Pregunta 2

¿El dominio tiene registros AAAA?

**Sí.**

Se observaron:

```text
2606:4700:10::6814:179a
2606:4700:10::ac42:93f3
```

### Pregunta 3

¿El dominio tiene una sola dirección IPv4?

**No.**

La consulta devolvió dos registros A.

### Pregunta 4

¿El TTL fue siempre igual?

**No.**

Se observaron diferentes valores.

### Pregunta 5

¿Que cambie el TTL significa necesariamente que cambió la IP?

**No.**

En nuestras mediciones las direcciones permanecieron iguales mientras el TTL observado cambió.

### Pregunta 6

¿A y AAAA representan lo mismo?

No exactamente.

```text
A
→ IPv4

AAAA
→ IPv6
```

### Pregunta 7

¿El tiempo de consulta A fue exactamente igual al de AAAA?

No.

En nuestras mediciones:

```text
A    → 19 ms
AAAA → 51 ms
```

Pero no podemos concluir que AAAA sea siempre más lento porque solamente hicimos una medición de cada una.

---

# 33. Conclusiones

Este laboratorio permitió pasar de una visión simplificada de DNS:

```text
hostname → IP
```

a una visión más real:

```text
hostname
   ↓
DNS query
   ↓
resolver
   ↓
DNS response
   ↓
registros
   ↓
A / AAAA
   ↓
IPv4 / IPv6
```

Se comprobó que:

1. DNS funciona mediante consultas (`query`) y respuestas (`response`).

2. `dig` permite realizar consultas DNS directamente desde la terminal.

3. Los registros `A` representan direcciones IPv4.

4. Los registros `AAAA` representan direcciones IPv6.

5. Un dominio puede tener múltiples registros A y múltiples registros AAAA.

6. El TTL representa información relacionada con el tiempo de permanencia de una respuesta DNS en caché.

7. El TTL observado puede cambiar entre consultas.

8. El cambio del TTL no implica necesariamente que haya cambiado la dirección IP.

9. `dig` permite conocer el tiempo observado de una consulta mediante `Query time`.

10. `dig` también permite identificar el servidor DNS consultado.

11. Una medición individual de latencia no permite establecer una regla general sobre qué tipo de consulta es más rápido.

12. DNS es una etapa previa al establecimiento de la conexión TCP en el modelo simplificado:

```text
DNS
 ↓
IP
 ↓
TCP
 ↓
HTTP
```

---

# 34. Modelo mental final del Día 12

El modelo que debo conservar después de este día es:

```text
CLIENTE
   │
   │ DNS query
   │
   ▼
DNS RESOLVER
   │
   │ DNS response
   │
   ▼
REGISTROS DNS
   │
   ├── A
   │    └── IPv4
   │
   └── AAAA
        └── IPv6
   │
   ▼
IP
   │
   ▼
TCP CONNECT
   │
   ▼
HTTP REQUEST
   │
   ▼
HTTP RESPONSE
```

Y el registro DNS debe poder leerse de izquierda a derecha:

```text
example.com.    162    IN    A    104.20.23.154
     │           │      │    │          │
     │           │      │    │          └── IPv4
     │           │      │    └───────────── tipo
     │           │      └────────────────── clase
     │           └───────────────────────── TTL
     └───────────────────────────────────── nombre
```

La idea fundamental del día es:

> **DNS no es simplemente “convertir un nombre en una IP”. Es un sistema de consultas y respuestas que devuelve registros de diferentes tipos, los cuales pueden estar sujetos a caché y TTL. `dig` permite observar directamente esa evidencia desde la terminal.**

---

# 35. Próximo paso

El siguiente concepto que queda por estudiar es la comparación entre **diferentes DNS resolvers**.

La idea será comparar algo como:

```text
Resolver A
   ↓
respuesta
   ↓
IP
TTL
tiempo
```

contra:

```text
Resolver B
   ↓
respuesta
   ↓
IP
TTL
tiempo
```

Esto permitirá entender mejor:

* caché;
* TTL;
* diferencias entre resolvers;
* por qué dos consultas al mismo dominio pueden mostrar información temporalmente diferente;
* y cómo interpretar esas diferencias sin sacar conclusiones incorrectas.

Ese será el siguiente laboratorio del Día 12.
