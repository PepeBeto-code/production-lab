# Aclaración — Diagnóstico vs. Runbook de Storage

La confusión surge al mezclar **tres conceptos distintos** que responden a preguntas diferentes:

1. **El proceso de diagnóstico** (modelo mental de cómo pensamos).
2. **El árbol general de "no puedo escribir"** (modelo conceptual del Día 6 sobre las posibles capas de fallo).
3. **El runbook de Storage Pain** (guía práctica del Día 7 con comandos específicos).

No son la misma cosa. A continuación se desglosa y aclara cada punto.

---

## 1. ¿Qué significa "Diagnóstico"?

Supongamos que recibimos este reporte:

```text
La aplicación no puede escribir su log.
```

Eso es únicamente el **síntoma**. Todavía no conocemos la causa. Diagnosticar significa investigar de forma sistemática para encontrar la causa raíz.

### El flujo del proceso mental:

```text
  SÍNTOMA
     │
     ▼
 HIPÓTESIS
     │
     ▼
COMPROBACIÓN
     │
     ▼
 EVIDENCIA
     │
     ▼
CONCLUSIÓN
```

#### Ejemplo práctico:
* **Síntoma:** "No puedo escribir el log"
* **Hipótesis:** "Quizá el filesystem está lleno"
* **Comprobación (Comando):** `df -h .`
* **Evidencia:** `Use% = 45%`
* **Conclusión:** No es falta de espacio en bytes. Se descarta la hipótesis y se prueba la siguiente.

> **Regla de oro:** *Síntoma → Hipótesis → Comprobación → Evidencia → Conclusión* describe **cómo pensamos**, no qué comando ejecutamos primero.

---

## 2. Los comandos son herramientas, no etapas del proceso

No debemos confundir los pasos del razonamiento con los comandos de Linux:

* **Etapas del diagnóstico:** Síntoma, hipótesis, comprobación, evidencia, conclusión.
* **Herramientas de investigación:** `df`, `du`, `ls`, `findmnt`, `systemctl`, `journalctl`.

```text
               DIAGNÓSTICO
                    │
                 SÍNTOMA
                    │
                    ▼
               HIPÓTESIS
                    │
                    ▼
              COMPROBACIÓN
                    │
               ┌────┴────┐
               │ comando │
               └────┬────┘
                    ▼
                 EVIDENCIA
                    │
                    ▼
              ¿SE CONFIRMA?
               /                      NO           SÍ
             │             │
             ▼             ▼
       nueva hipótesis  causa raíz
```

Un comando como `df -h` no es "una hipótesis"; es el **medio para obtener evidencia** que confirme o descarte una hipótesis.

---

## 3. Árbol general de "No puedo escribir" (Modelo del Día 6)

Este modelo responde a la pregunta: **¿En qué capa puede estar fallando una escritura?**

```text
NO PUEDO ESCRIBIR
       │
       ▼
¿Existe la ruta? ───────────► (ls -ld /ruta)
       │
       ▼
¿Tengo permisos? ───────────► (ls -l /ruta)
       │
       ▼
¿Filesystem rw/ro? ─────────► (findmnt / mount)
       │
       ▼
¿Hay espacio?
       │
       ├── Bytes ───────────► (df -h)
       │
       └── Inodos ──────────► (df -ih)
```

### Capas conceptuales de evaluación:
1. **Ruta:** ¿Existe el directorio/archivo destino?
2. **Permisos:** ¿El usuario/proceso tiene permisos de escritura (`w`) en la carpeta y el archivo?
3. **Filesystem Mount:** ¿Está el punto de montaje en `rw` (read-write) o cayó en `ro` (read-only)?
4. **Espacio en Bytes:** ¿Hay bloques disponibles en disco? (`df -h`)
5. **Inodos:** ¿Hay inodos disponibles para crear/modificar la entrada de directorio? (`df -ih`)

Este modelo es general y sigue siendo totalmente válido.

---

## 4. Runbook de Storage Pain (Día 7)

El Día 7 no invalida el árbol del Día 6. Un **runbook** es una secuencia recomendada de comandos para investigar un tipo de incidente particular (en este caso, presión de almacenamiento).

### Flujo práctico del Runbook de Storage:

```text
STORAGE PAIN DETECTADO
          │
          ▼
       1. df -h       ───► Estado general de bytes
          │
          ▼
       2. df -ih      ───► Estado de inodos
          │
          ▼
       3. du -sh      ───► Localizar directorios/archivos consumidores
          │
          ▼
       4. ls -l       ───► Validar permisos/propiedades de los archivos hallados
          │
          ▼
 5. Correlación       ───► Comparar timeline con logs del sistema/app
          │
          ▼
     CONCLUSIÓN
```

---

## 5. Distinciones clave entre herramientas

### `df` vs. `du`
* **`df` (Disk Free):** Consulta la metadata del sistema de archivos a nivel global.
  * *Pregunta:* ¿Cómo está la capacidad total del filesystem?
  * *Ejemplo:* 100 GB total, 98 GB usados, 2 GB libres.
* **`du` (Disk Usage):** Recorre el árbol de directorios contando el espacio que ocupa cada carpeta/archivo.
  * *Pregunta:* ¿Quién o qué carpeta se está consumiendo esos 98 GB?
  * *Ejemplo:* `/var/log` está consumiendo 60 GB.

### ¿Dónde entran los permisos (`ls -l`)?
Depende del **síntoma exacto**:
* Si el log dice **`Permission denied`**: Tu primera hipótesis debe ser permisos (`ls -ld /ruta`), por lo que miras los permisos **antes** que el espacio en disco.
* Si el log dice **`No space left on device`**: Tu primera hipótesis es almacenamiento, por lo que usas `df -h` y `df -ih` **antes** que revisar permisos detallados.

---

## 6. Correlación con Logs

La investigación de almacenamiento no termina en el comando `df` o `du`; se debe correlacionar la evidencia física con los registros de la aplicación:

```text
TIMELINE DEL INCIDENTE:

14:00 ─── Aplicación operando normalmente.
15:00 ─── Incremento inusual de tráfico / error en bucle.
15:30 ─── Crecimiento acelerado del log (`du`).
15:45 ─── Filesystem alcanza el 100% de uso (`df -h`).
15:46 ─── Aplicación arroja fallo de escritura (SÍNTOMA).
```

### Construcción de la causa raíz:
1. Los logs crecieron desproporcionadamente.
2. Agotaron el espacio en bytes del filesystem.
3. El filesystem bloqueó escrituras posteriores.
4. La aplicación falló al intentar registrar nuevos eventos.

---

## 7. Mapa Conceptual Consolidado

```text
                         DIAGNÓSTICO
                              │
                              ▼
                           SÍNTOMA
                              │
                              ▼
                          HIPÓTESIS
                              │
                              ▼
                       COMPROBACIONES
                              │
             ┌────────────────┼────────────────┐
             │                │                │
            RUTA           PERMISOS       FILESYSTEM
             │                │                │
          ls -ld           ls -l            findmnt
                                               │
                                      ┌────────┼────────┐
                                      │        │        │
                                     rw/ro   bytes    inodos
                                               │        │
                                             df -h    df -ih
                                               │
                                               ▼
                                              du
                                               │
                                               ▼
                                           EVIDENCIA
                                               │
                                               ▼
                                          CORRELACIÓN
                                          CON LOS LOGS
                                               │
                                               ▼
                                           CONCLUSIÓN
                                               │
                                               ▼
                                          CAUSA RAÍZ
```

---

## Resumen de Integración

| Concepto | Propósito | Ejemplo |
| :--- | :--- | :--- |
| **Proceso de Diagnóstico** | Estructura el pensamiento lógico. | *Síntoma → Hipótesis → Comprobación → Evidencia → Conclusión* |
| **Árbol de "No puedo escribir"** | Clasifica todas las posibles razones de fallo de escritura. | *Ruta → Permisos → Filesystem (rw/ro) → Bytes (`df -h`) → Inodos (`df -ih`)* |
| **Runbook de Storage Pain** | Secuencia rápida de comandos para investigar problemas de espacio. | *`df -h` → `df -ih` → `du` → `ls -l` → Correlación* |
