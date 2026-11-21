# Auditoría incremental — negativos verificados (2026-11-21)

## Problema detectado
`dataset_gate.py` aceptaba una etiqueta TXT vacía incluso cuando la fila del manifiesto **no** estaba marcada como negativa. Esto podía permitir que una imagen con barras reales sin anotar se tomase por un negativo válido para el detector.

## Cambio integrado
- El validador solo acepta TXT vacío o ausente cuando `negative: true` está explícito en el manifiesto.
- Una imagen marcada negativa con cajas positivas provoca rechazo.
- Se actualizó el fixture del dataset en la prueba para que la imagen vacía esté efectivamente marcada como negativa.
- Se agregaron 3 comprobaciones de regresión: vacío sin flag se rechaza; vacío con flag se acepta; flags de tipo incorrecto se rechazan.

## Estado de pruebas
Las nuevas pruebas **están escritas y versionadas**. El resultado de GitHub Actions **no ha sido comprobado** en esta auditoría; no declararlas superadas sin obtener la ejecución. El entrenamiento de YOLO sigue pendiente; CVAT aún no dispone de un dataset real etiquetado y revisado.

## Riesgo residual
Un flag `negative: true` no prueba que el fotograma esté libre de barras; alguien debe revisar la imagen. Requerimos etiquetado humano, revisión de muestras negativas y evaluación independiente entre videos.

## Respaldo
Las ramas `backup-control-pre-lab-2026-11-21` y `control-integracion-av-preapk` se mantienen sin cambios en este trabajo. No se ha editado Android.
