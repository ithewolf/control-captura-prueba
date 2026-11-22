# Laboratorio 2 — Cinco componentes y estudio de CONTROL Lite

Esta sala es independiente de Lab 1 y de Android. Contiene un **harness de contratos**, NO cinco modelos entrenados. Sus cinco entradas experimentales son:
1. Cámara: OpenCV (compensación global) — requiere conectar implementación y medir vídeo real.
2. Tracking: BoT-SORT — NO instalado/ejecutado, requiere detector con cajas e identidades verificables.
3. Etiquetas visuales: CVAT + detector de campeones entrenado — CVAT es editor de anotación, NO modelo y NO corre en inferencia.
4. Acciones: clasificador temporal o heurística validada — NO entrenado.
5. Ejecución móvil: ONNX Runtime — NO desplegado ni benchmarkeado en Android.

Las entradas faltantes aparecen como UNAVAILABLE, nunca se sustituyen por predicciones ficticias. Aunque un mock de los cinco devuelva MEASURED, ready_for_live permanece false.

## Comparación Full / Balanced / Lite
Full: objetivo exploratorio 5 FPS de muestras a 480px; Balanced: 2 FPS/320px; Lite: 1 FPS/224px. La latencia real, calor, batería, memoria y FPS de Wild Rift **siguen sin medir**. Ensayo obligatorio en móvil real. Ajustar o apagar análisis al detectarse caída del juego o calor.
Posibles ahorros: recorte del HUD, compartir un backbone pequeño, detección cada N fotogramas y seguimiento barato intermedio, cuantización de pesos con validación de exactitud, ventanas temporales solo ante eventos y tasa de consejos limitada.

## Criterios de una eventual versión Lite
- Juego fluido comparado con Wild Rift solo, sin sobrecalentamiento persistente.
- Sin cierres, permisos perdidos ni OOM.
- Reconocimiento correcto por campeón y versión, precisión/recall sobre partidas retenidas.
- Latencia de consejo que permita actuar (medir percentiles, no solo promedio).
- Suspensión automática con falta de recursos. Nada de promesas «funciona en cualquier celular» sin pruebas.
- Usar exclusivamente programas gratuitos/locales sin costos por uso.

Fuentes:
https://docs.ultralytics.com/modes/track/
https://docs.cvat.ai/docs/manual/advanced/formats/format-yolo-ultralytics/
https://onnxruntime.ai/docs/tutorials/mobile/
https://developer.android.com/games/optimize/adpf/thermal
