# W4D5 — Incident Postmortem

## Incidente

Fallo controlado de `nginx` provocado mediante una modificación inválida en su archivo de configuración.

## Síntoma

`nginx.service` no pudo reiniciarse y quedó en estado:

```text
Active: failed (Result: exit-code)
```

El intento de `restart` terminó con:

```text
Job for nginx.service failed because the control process exited with error code.
```

## Unidad afectada

```text
nginx.service
```

## Timeline

```text
22:14:07 → nginx inicia correctamente y queda active (running).

22:38:01 → Se crea backup de /etc/nginx/nginx.conf:
           nginx.conf.bak.2026-09-09_223801

22:46:08 → sudo nginx -t detecta una configuración inválida:
           unknown directive "THIS_IS_AN_INTENTIONAL_ERROR"
           en /etc/nginx/nginx.conf:7

22:51:07 → Se ejecuta systemctl restart nginx.

22:51:07 → Durante ExecStartPre, nginx -t devuelve:
           status=1/FAILURE

22:51:07 → journald registra:
           unknown directive "THIS_IS_AN_INTENTIONAL_ERROR"

22:51:07 → systemd registra que el control process terminó
           con status=1/FAILURE.

22:51:07 → nginx.service queda en estado failed.

23:13:09 → Se restaura el backup de nginx.conf.

23:13:09 → sudo nginx -t:
           syntax is ok
           test is successful

23:13:09 → Se reinicia nginx correctamente.

23:13:09 → nginx.service vuelve a:
           Active: active (running)
```

## Causa

La causa fue una configuración inválida en `/etc/nginx/nginx.conf`.

Se agregó deliberadamente la siguiente directiva:

```nginx
THIS_IS_AN_INTENTIONAL_ERROR;
```

nginx no reconoció dicha directiva.

## Evidencia

La evidencia principal fue:

```text
2026/09/09 22:46:08 [emerg] unknown directive
"THIS_IS_AN_INTENTIONAL_ERROR"
in /etc/nginx/nginx.conf:7
```

Durante el intento de reinicio, `systemctl status` mostró:

```text
ExecStartPre=/usr/sbin/nginx -t ...
(code=exited, status=1/FAILURE)
```

`journalctl -u nginx` confirmó la misma secuencia:

```text
nginx[7535]: unknown directive "THIS_IS_AN_INTENTIONAL_ERROR"
nginx: configuration file /etc/nginx/nginx.conf test failed
nginx.service: Control process exited, code=exited, status=1/FAILURE
nginx.service: Failed with result 'exit-code'
```

El archivo `/var/log/nginx/error.log` no contenía el error correspondiente al intento de arranque de las 22:51:07; únicamente mostró un evento anterior de las 22:14:08.

## Hipótesis

La configuración inválida provocó que nginx no pudiera arrancar porque `nginx -t` detectó la directiva desconocida antes del inicio del servicio. El fallo de `nginx -t` provocó que `ExecStartPre` terminara con `status=1/FAILURE`, y posteriormente systemd marcó `nginx.service` como `failed`.

## Acción correctiva

Se restauró el archivo de configuración original utilizando el backup:

```text
/etc/nginx/nginx.conf.bak.2026-09-09_223801
```

mediante:

```bash
sudo mv /etc/nginx/nginx.conf.bak.2026-09-09_223801 \
/etc/nginx/nginx.conf
```

## Verificación

Primero se comprobó la configuración:

```bash
sudo nginx -t
```

Resultado:

```text
syntax is ok
test is successful
```

Después se reinició nginx:

```bash
sudo systemctl restart nginx
```

Finalmente se comprobó el estado:

```bash
systemctl status nginx --no-pager
```

Resultado:

```text
Active: active (running)
```

Además, `ExecStartPre` volvió a terminar correctamente:

```text
(code=exited, status=0/SUCCESS)
```

## Conclusión

El incidente fue diagnosticado mediante evidencia proveniente de `nginx -t`, `systemctl status` y `journalctl`.

La secuencia permitió distinguir entre la causa y sus consecuencias:

```text
configuración inválida
        ↓
nginx -t falla
        ↓
inicio de nginx falla
        ↓
systemd registra el fallo
        ↓
nginx.service = failed
```

La restauración de la configuración y la posterior validación demostraron que la hipótesis era correcta y que el servicio había sido recuperado.

Principio aplicado:

```text
EVENTO
↓
TIMESTAMP
↓
ORDEN TEMPORAL
↓
CORRELACIÓN
↓
HIPÓTESIS
↓
COMPROBACIÓN
↓
CORRECCIÓN
↓
VERIFICACIÓN
```
