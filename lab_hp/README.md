# Laboratorio independiente: YOLO + CVAT + OpenCV

**AISLADO** de la aplicación Android CONTROL. Punto de partida: rama `control-integracion-av-preapk`.
Respaldo sin modificaciones: `backup-control-pre-lab-2026-11-21`.

## Función de cada herramienta
1. **CVAT (editor, no motor de inferencia)**: anotar `health_bar_track` con toda la extensión del contorno; `level_badge`; `not_a_health_bar` para análisis de negativos. Exportar a formato Ultralytics YOLO (confirmar soporte según versión de CVAT). Guardar origen, fecha, video, tramo, clase y revisión del anotador.
2. **YOLO**: entrenar un detector específico sobre datos etiquetados de Wild Rift; no utilizar pesos genéricos pretendiendo que ya reconocen barras.
3. **OpenCV**: recortar cajas de la predicción, medir fracción coloreada y rechazar resultados ambiguos; tracking por identidad es una fase futura.
4. **Evaluador**: medir precisión, recall y error de relleno con otro video o escenas *retenidas*. No mezclar escenas casi duplicadas entre train/validación/test.

## Decisión de seguridad técnica
CVAT **no corre al mismo tiempo por cada fotograma**: trabaja en preparación/revisión de datos. YOLO y OpenCV sí pueden ejecutarse en cadena en inferencia. No hay GPU remota ni servicios pagos activados. No se instaló ni entrenó YOLO todavía.

## Pruebas realizadas
9 pruebas unitarias sintéticas de integración YOLO-output simulado -> OpenCV: correctas en entorno Python. No se usó un modelo YOLO entrenado, no se midieron FPS ni tasa real de aciertos. Falta evaluar imágenes reales y empezar a etiquetar.

## Precaución comercial
La integración opcional usa Ultralytics YOLO, cuya licencia AGPL-3.0/comercial debe revisarse antes de distribución cerrada. Para una futura app con suscripción, evaluar alternativas de entrenamiento e inferencia bajo términos compatibles (por ejemplo bibliotecas Apache-2.0) antes de hacerla producto.

## Objetivo de aprobación previo a Android
Test independiente: precision >=98%, recall >=95%, error absoluto medio de relleno <=5 puntos porcentuales en pistas anotadas, medición de eventos de HP decreciente y seguimiento de identidad. Umbrales objetivos, no resultados.


## Revisión web 21/11: formatos y ejemplos negativos
- CVAT permite exportar `Ultralytics YOLO Detection 1.0` con cuadros normalizados; `Detection Track 1.0` es un formato distinto para seguir identidades, no debe mezclarse inadvertidamente con el de detección.
- YOLO permite **TXT vacío o ausente** para fotogramas sin objetos. Para evitar confundir fallos de exportación con negativos, `dataset_gate.py` solo acepta archivo ausente cuando `"negative":true` aparece explícitamente en el manifiesto.
- Etiquetar **todas** las barras visibles de las clases objetivo; omitirlas convierte objetos reales en falsos fondos que perjudican el entrenamiento.
- Fuentes: https://docs.cvat.ai/docs/dataset_management/formats/format-yolo-ultralytics/ ; https://docs.ultralytics.com/datasets/detect ; https://docs.ultralytics.com/tasks/detect
- Auditar datasets como **integridad**, nunca como precisión del modelo.
- Si GitHub Actions no ejecuta, revisar ramas, disparadores, permisos y ejecución en https://docs.github.com/en/actions/how-tos/troubleshoot-workflows ; no atribuir estado de éxito sin ver el run.
