# Primer benchmark CPU Fiora — 22/11/2026
30 pares de fotogramas reales de 6 clips, 2 iteraciones = 60 observaciones por modo.
Resultados **medidos localmente** con una versión equivalente del módulo OpenCV (el script publicado se diseñó para reproducir la misma clase de medición, sus resultados concretos pueden variar por equipo o versión).

| ancho de evaluación | media ms | p95 ms | resultados medidos / unknown |
|---|---:|---:|---|
|224|6.90|11.16|30 / 30|
|320|6.27|7.92|30 / 30|
|480|6.78|9.59|30 / 30|

Límites: mide SOLO compensación de cámara y residuo visual, NO redes neuronales, aprendizaje YOLO, reconocimiento de habilidades, ONNX ni captura Android. La compensación de cámara se ejecuta sobre escalado propio, por lo que el cambio de 224 a 480 en el residuo no reduce toda la carga. 30/60 UNKNOWN **no son falsos negativos** sin ground truth. No afirmar FPS del juego ni rendimiento Android a partir de esta tabla.

Próximo paso: analizar las causas de UNKNOWN, medir latencia de componentes adicionales y realizar pruebas de temperatura/FPS con Wild Rift real antes de escoger un modo definitivo.
