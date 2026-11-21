# CONTROL Coach: investigación térmica y protocolo de resistencia (21-11-2026)

**Estado:** diseño y fuentes verificadas; ninguna medición física del Samsung realizada. NO integrar detector a Android todavía. Rama aislada `lab-yolo-cvat-opencv-hp`; respaldo `backup-control-pre-lab-2026-11-21` intacto.

## Documentación oficial
- ADPF Thermal API: https://developer.android.com/games/optimize/adpf/thermal
  - Android PowerManager.getCurrentThermalStatus() y addThermalStatusListener(...).
  - PowerManager.getThermalHeadroom(forecastSeconds) predice margen térmico. No consultar con intervalo inferior a 10 segundos (puede devolver NaN); algunos dispositivos no lo soportan.
  - Estados: NONE, LIGHT, MODERATE, SEVERE, CRITICAL, EMERGENCY y SHUTDOWN. La interpretación concreta depende del dispositivo. No usar una temperatura Celsius universal.
- Best practices ADPF: https://developer.android.com/games/optimize/adpf/best-practices-adpf
  - Reducir carga preventivamente, monitorizar FPS y tiempos de frame, no esperar calentamiento extremo.
- Android Power Profiler: https://developer.android.com/studio/profile/power-profiler
  - ODPM Power Rails no es universal; dispositivos no compatibles pueden ofrecer datos de batería.
- Macrobenchmark PowerMetric: https://developer.android.com/topic/performance/benchmarking/macrobenchmark-metrics
  - PowerMetric exige Pixel 6+; NO asumir soporte en Galaxy S24 Ultra.
- Android system trace / Perfetto: https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview

## Estrategia inicial propuesta, PENDIENTE de medir
- **Solo en futuras versiones Android**, una cola latest-only sin backlog, procesamiento asíncrono que nunca bloquee hilo principal ni capture game input.
- Escenario tranquilo: objetivo 1-2 inferencias por segundo; acción alta 3-5, condicionado a latencia y temperatura.
- Estado térmico MODERATE: reducir resolución de inferencia/tasa; SEVERE o mayor: suspender inferencia costosa y reactivar tras recuperarse; conservar salida de voz solo si no agrava el estado.
- Si el headroom se acerca al rango severo, degradar antes; nunca depender solo de getCurrentThermalStatus porque el reporte puede ser inconsistente o no estar implementado.
- Cuando unsupported/NaN, modo conservador y controles de duración/latencia; nunca interpretar NaN como temperatura normal.
- No activar sustained-performance o fixed-performance mode para ocultar throttling; no desactivar protecciones del SO.
- Entrenamiento, CVAT y generación de etiquetas se ejecutan fuera del teléfono; solo la inferencia mínima necesaria se ejecutaría en móvil.

## Diseño de validación A/B en teléfono real
Dispositivo: Galaxy S24 Ultra de Wolf (siempre identificar SO/build exacto y modo de juego).
A) Wild Rift solo; B) Wild Rift + CONTROL captura+voz sin inferencia; C) Wild Rift + detector en modo ahorro; D) Wild Rift + detector con tasa adaptativa.
- Pruebas separadas de 15/30/60 minutos, mínimo 3 corridas por condición con temperatura ambiente, brillo, red, volumen, frecuencia gráfica y carga inicial de batería tan similares como sea posible. Aleatorizar/intercalar orden cuando resulte práctico y dejar enfriar entre corridas.
- Registrar por minuto: estado térmico Android, thermal headroom (cada 10-15s si disponible), FPS/tiempo de frame, latencia p50/p95/p99 inferencia, imágenes omitidas, memoria (PSS), uso CPU/GPU si accesible, nivel y corriente/carga de batería cuando estén disponibles, fallos, consumo del reconocimiento de voz, y percepción de incomodidad térmica.
- Comparar especialmente **diferencias B-A, C-B y D-C** para aislar el costo de captura, voz y visión.
- Reprobar en partidas reales largas, menús, combate denso, carga/descarga, brillo alto y ambiente cálido; no extrapolar desde una corrida de 5 minutos ni de emulador.
- Si aparecen degradación sostenida de FPS, temperatura incómoda, SEVERE o peor, fallos de captura/voz o fuerte descarga: **rechazar** configuración, investigar, reducir carga y repetir. Pausar uso si dispositivo muestra alertas térmicas.
- Definir límites numéricos de descarga por hora, pérdida FPS y p95 solo después de baseline real y test A/B, nunca inventarlos con datos de PC.

## Estado de aprobación
Plan de medición investigado; rendimiento y temperatura aún NO MEDIDOS en hardware real. Modelo de barras NO ENTRENADO; validación de precisión INCOMPLETA; ninguna APK nueva autorizada.
