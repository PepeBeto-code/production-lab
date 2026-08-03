# Detección de procesos CPU Bound vs I/O Bound

## Objetivo
Explicar cómo identificar si un proceso está limitado por CPU o por operaciones de entrada/salida (I/O).

## Herramientas utilizadas
- ps
- top
- htop (opcional)
- pidstat (opcional)

## Comandos principales
- ps -eo pid,%cpu,%mem,etime,time,comm --sort=-%cpu
- ps -eo pid,%cpu,%mem,etime,time,comm --sort=-%mem
- top
- htop
- pidstat 1

## Indicadores de CPU Bound
- Uso de CPU muy alto.
- El tiempo de CPU (`TIME`) aumenta rápidamente.
- El proceso permanece en estado Running.
- Poco tiempo esperando recursos externos.

## Indicadores de I/O Bound
- Uso de CPU bajo o moderado.
- Gran parte del tiempo esperando disco, red o base de datos.
- El tiempo total (`ETIME`) crece mucho más rápido que `TIME`.
- El proceso suele estar en estado Sleeping.

## Conclusión
Antes de optimizar una aplicación, es indispensable identificar qué recurso limita su rendimiento. Un proceso CPU Bound requiere optimizar cálculos o paralelismo; un proceso I/O Bound requiere investigar discos, red, bases de datos, cachés o servicios externos.
