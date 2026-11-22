# Laboratorio 1 — Integración progresiva / prioridad al juego

Etapa A (implementada y probada sintéticamente): estimación de desplazamiento global de cámara con OpenCV (Lucas-Kanade y RANSAC). Se abstiene si no hay suficientes puntos o consistencia. **No es un detector de campeones**.

Etapa B (política implementada; integración Android pendiente): perfiles full/balanced/lite/pause. Valores de partida para experimentar, NO mediciones reales de velocidad: 5/2/1 capturas por segundo, ancho 480/320/224; pausa si estado térmico severo o pérdida estimada >=15% de fotogramas del juego. Reducir actividad ya desde 5%. Requiere lectura auténtica de telemetría y ensayos por dispositivo. Nunca sustituir FPS real por estimaciones inventadas.

Etapa C: detector específico de Wild Rift, anotaciones de CVAT y validación ciega por partida (NO entrenado).
Etapa D: comparar BoT-SORT con tracking liviano con identidades reales (NO conectado).
Etapa E: reconocimiento de habilidades por clips y reglas tácticas calificadas por revisores (NO entrenado).

Se preserva rama Android principal. Cualquier falta de información impide consejo autónomo de combate. Evitar ciclos de visión continua sin límites, procesos secundarios pesados, créditos, nube o descargas silenciosas.

Fuentes:
- https://docs.opencv.org/4.x/d9/d0c/group__calib3d.html
- https://docs.ultralytics.com/modes/track/
- https://docs.cvat.ai/docs/manual/advanced/formats/format-yolo-ultralytics/
- https://developer.android.com/games/optimize/adpf/thermal
- https://onnxruntime.ai/docs/tutorials/mobile/

Riesgo de licencias: Ultralytics AGPL-3.0; antes de distribuir revisar obligaciones y alternativas con licencias adecuadas.
