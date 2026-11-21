# Auditoría incremental — tolerancia a fallos del puente de visión (21/11/2026)

## Trabajo realizado
- Se leyó la versión actual de `lab_hp/hp_pipeline.py` y su batería de pruebas.
- Se identificó un fallo de robustez: valores `NaN`/infinito en confianza podían pasar el umbral y cajas mal formadas podían provocar excepción.
- Se corrigieron validaciones de tipos, rango de confianza [0,1], coordenadas finitas y cajas presentes dentro del fotograma.
- `analyze()` descarta el objeto defectuoso, no detiene otras predicciones y mantiene `game_state=UNKNOWN` y `ready_for_android=False`.
- Se agregaron **cinco métodos de pruebas de regresión** a `lab_hp/test_hp_pipeline.py`, con múltiples variantes adversariales.

## Estado honesto
**Código escrito y pruebas añadidas en la rama aislada**. Estado de GitHub Actions pendiente de comprobar; NO declarar esos nuevos tests aprobados sin ejecución verificada. Los 9 ensayos antiguos eran sintéticos; no hay modelo YOLO entrenado con imágenes reales ni evaluación de precisión o consumo.
El respaldo `backup-control-pre-lab-2026-11-21` no se cambió. Android tampoco.

## Próxima auditoría
1. Confirmar ejecución de workflow `hp-lab-tests.yml` y resolver fallos reales de CI.
2. Etiquetar imagen/escena/video de barras reales en CVAT, con anotaciones completas.
3. Detectar fugas entre training y test, y medir precisión, recall y errores por clase.
4. Exigir generalización a nuevos campeones, escenas y distintos estados de HUD.
5. Hacer pruebas de temperatura/batería recién al tener inferencia móvil candidata.

Si el flujo CI no se ejecuta, investigarlo primero. No llamar `aprobado` a cambios que sólo se guardaron.
