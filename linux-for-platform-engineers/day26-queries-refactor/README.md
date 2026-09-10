# W4D6 — Búsqueda eficiente y anti-patrones

## Objetivo

Aprender a investigar logs de forma eficiente: no se trata de ejecutar muchos comandos, sino de elegirlos de acuerdo con la pregunta que queremos responder.

La idea central es:

**Problema → pregunta → evidencia → acotar → ordenar → correlacionar → hipótesis → comprobar → conclusión**

---

## 1. Anti-patrones de búsqueda

### Leer miles de líneas sin acotar tiempo

Comando original:

```bash
journalctl -u nginx
```

Muestra el historial disponible de `nginx.service`, pero puede producir demasiado ruido cuando solo queremos investigar un incidente concreto.

El problema no es que el comando sea incorrecto. El problema es utilizarlo sin una pregunta ni un intervalo temporal definido.

---

### Buscar solamente `error`

Ejemplo:

```bash
grep -i "error"
```

Buscar únicamente `error` puede hacer que perdamos información importante.

En el incidente anterior, la señal más específica fue:

```text
unknown directive
```

Por eso conviene buscar diferentes señales relevantes:

```bash
grep -iE "emerg|error|failed|unknown directive"
```

`-i` ignora mayúsculas/minúsculas.

`-E` permite utilizar expresiones regulares extendidas.

El carácter `|` significa **OR**: buscar cualquiera de las alternativas.

---

### Ignorar timestamps

Los timestamps permiten ordenar los acontecimientos y compararlos entre diferentes fuentes.

No basta con encontrar:

```text
error
```

También necesitamos saber:

```text
¿cuándo ocurrió?
¿qué ocurrió antes?
¿qué ocurrió después?
```

Esto permite construir una línea temporal y distinguir señales iniciales de consecuencias posteriores.

---

### Confundir consecuencia con causa

Que un servicio termine en `failed` no significa necesariamente que `failed` sea la causa raíz.

En el incidente de nginx:

```text
configuración inválida
        ↓
nginx -t falla
        ↓
nginx no puede iniciar
        ↓
systemd registra failure
```

Por lo tanto:

```text
nginx.service: Failed
```

es una consecuencia del problema de configuración, no necesariamente su causa.

La señal más específica fue:

```text
unknown directive "THIS_IS_AN_INTENTIONAL_ERROR"
```

---

### Copiar logs completos sin interpretarlos

Copiar grandes cantidades de logs no equivale a diagnosticar.

Los logs son **evidencia**.

La investigación consiste en interpretar esa evidencia, relacionarla temporalmente y utilizarla para comprobar hipótesis.

---

### Ejecutar comandos enormes sin saber qué se busca

Un comando complejo no es automáticamente mejor.

Primero debe existir una pregunta concreta.

Ejemplo:

> "Quiero saber si nginx registró recientemente una falla relacionada con la configuración."

Entonces tiene sentido reducir:

- servicio;
- intervalo temporal;
- señales relevantes;
- cantidad de resultados.

---

## 2. Refactorización de consultas

### Original

```bash
journalctl -u nginx
```

Problema:

- Puede mostrar demasiado historial.
- Incluye información que no necesariamente pertenece al incidente.
- Obliga a revisar manualmente mucho ruido.

---

### Optimizado 1

```bash
journalctl -u nginx --since "15 min ago" --no-pager | grep -iE "emerg|error|failed|unknown directive"
```

Se reduce la búsqueda mediante dos mecanismos:

1. **Tiempo**

```bash
--since "15 min ago"
```

Limita la consulta a los últimos 15 minutos.

2. **Señales relevantes**

```bash
grep -iE "emerg|error|failed|unknown directive"
```

Conserva únicamente líneas que contienen señales relacionadas con errores o fallos.

3. **Sin paginador**

```bash
--no-pager
```

Evita que `journalctl` abra la salida en un paginador interactivo.

---

### Optimizado 2

```bash
sudo grep -iE "emerg|error|failed|unknown directive" /var/log/nginx/error.log | tail -n 50
```

Aquí se consulta directamente:

```text
/var/log/nginx/error.log
```

y después:

```bash
tail -n 50
```

limita la salida a los últimos 50 resultados obtenidos.

---

## 3. Qué ruido se elimina

Al optimizar las consultas eliminamos principalmente:

- eventos fuera del intervalo que estamos investigando;
- líneas que no contienen señales relevantes para la pregunta actual;
- salida excesivamente extensa;
- interacción innecesaria con el paginador;
- información que todavía no aporta evidencia útil.

La optimización no consiste en eliminar información arbitrariamente, sino en **reducir el espacio de búsqueda sin perder las señales necesarias para responder la pregunta**.

---

## 4. Qué información importante se conserva

Una búsqueda eficiente debe conservar:

- **Timestamp:** cuándo ocurrió el evento.
- **Servicio:** qué componente produjo la evidencia.
- **Evento:** qué ocurrió.
- **Señal relevante:** error, fallo, directiva desconocida, etc.
- **Contexto suficiente:** información necesaria para interpretar el evento.
- **Orden temporal:** qué ocurrió antes y después.

La información debe ser suficiente para continuar la investigación, no necesariamente para mostrar todo el historial.

---

## 5. Limitación importante del ejercicio

El incidente práctico de W4D5 ocurrió horas antes de realizar W4D6.

Por eso, una consulta como:

```bash
--since "15 min ago"
```

probablemente no encontraría aquel incidente.

Esto es una lección importante:

> **Una consulta correctamente construida puede producir evidencia insuficiente si el intervalo temporal elegido no corresponde al incidente que estamos investigando.**

El filtro debe responder al contexto real del incidente.

---

## 6. Por qué se omitió el laboratorio

El laboratorio práctico se omitió deliberadamente.

La razón no es que el concepto no sea importante, sino que el entorno actual todavía no proporciona suficiente complejidad para que el ejercicio aporte mucho valor.

nginx acaba de instalarse y todavía no existe un historial significativo de:

- múltiples incidentes;
- diferentes tipos de errores;
- mucho ruido;
- múltiples servicios interactuando;
- señales contradictorias;
- grandes cantidades de logs que deban correlacionarse.

Repetir artificialmente una consulta sobre un entorno prácticamente vacío sería principalmente practicar sintaxis, cuando el objetivo de W4D6 es desarrollar **criterio de investigación**.

El laboratorio se retomará con mayor valor cuando existan sistemas más complejos y más fuentes de evidencia.

---

## 7. Aprendizaje principal

El objetivo de troubleshooting no es memorizar comandos.

El objetivo es saber:

> **qué quiero averiguar → qué evidencia necesito → dónde encontrarla → cómo reducir el ruido → cómo relacionarla con el resto de la evidencia.**

Por eso, conocer:

```bash
journalctl
grep
tail
```

es solo la parte mecánica.

La habilidad importante es decidir **cuándo, por qué y cómo utilizarlos**.

---

## 8. Modelo mental

La investigación puede verse como un proceso similar al método científico:

```text
Problema
   ↓
Pregunta concreta
   ↓
Evidencia
   ↓
Acotar búsqueda
   ↓
Ordenar eventos
   ↓
Correlacionar señales
   ↓
Formular hipótesis
   ↓
Comprobar hipótesis
   ↓
Conclusión
```

Una investigación de producción no consiste en encontrar "la línea roja".

Consiste en construir una explicación respaldada por evidencia.

---

## Conclusión

W4D6 reforzó que una buena investigación de logs depende más del **criterio** que de la cantidad de comandos utilizados.

La meta es reducir progresivamente el espacio de búsqueda hasta encontrar evidencia suficiente para explicar el comportamiento observado.

Por ahora, el laboratorio se considera **omitido de forma consciente**, porque el entorno actual no tiene suficiente complejidad para justificar una práctica artificial. El concepto quedó comprendido y podrá aplicarse con mayor profundidad conforme se incorporen sistemas, servicios y fallos más complejos.
