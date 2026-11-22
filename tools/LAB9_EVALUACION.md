# CONTROL Lab9: reconocimiento de enemigos — antes de APK

**Estado:** código de evaluación y pruebas unitarias subidos. El detector visual de campeones NO está entrenado ni validado. No generar APK nueva hasta tener evidencia con videos reales.

## Referencias
- Riot Wild Rift 5.0: https://wildrift.leagueoflegends.com/en-us/news/game-updates/wild-rift-patch-notes-5-0/
- Riot bots LoL PC: https://www.leagueoflegends.com/en-gb/news/dev/dev-leveling-up-bots/
- OpenCV Android ONNX: https://docs.opencv.org/5.0/tutorials/introduction/android_binary_package/android_dnn_intro.html
- No se usa el código privado de Riot, se estudia el patrón de separación entre percepción y decisiones.

## Regla de producto
- Prioridad: confirmar *presencia, identidad y ubicación aproximada de un campeón enemigo*.
- NO exigir porcentaje de HP o MP enemigo. No inventar datos faltantes.
- No automatizar movimientos ni controles de Wild Rift. Solo evaluación, explicación y conversación.
- Evitar ventajas injustas; verificar políticas aplicables de Riot antes de usar consejos en partidas competitivas.

## Piezas de código
1. `EnemyObservationGate.java` valida que el supuesto enemigo tenga etiqueta ENEMY, confianza >= 0.80, ubicación normalizada válida y confirmación temporal en dos fotogramas.
2. `EnemyCoachPipeline.java` transforma esas observaciones ya verificadas en `TacticalDecisionEngine`. NO analiza píxeles.
3. `TacticalDecisionEngine.java` deja informar el enemigo incluso si desconocemos HP propio y aliados; prioriza retirada si la vida propia conocida es baja.
4. `tools/lab9_video_benchmark.py` inventaría cinco videos, exporta fotogramas con FFmpeg y calcula métricas únicamente cuando existen anotaciones humanas *y* predicciones del detector.
5. `tools/lab9_manifest.json` define cinco grabaciones y separación desarrollo/evaluación por fuente.

## Ejecutar antes de compilar APK
- Prueba Java: GitHub Actions "CONTROL Lab9 - vision and tactical checks ONLY (NO APK)".
- Banco de videos: `python3 tools/lab9_video_benchmark.py --video-root "/ruta/a/los/cinco/videos" --output lab9_results`
- Se necesita FFmpeg/ffprobe instalado donde se ejecuten los archivos.
- Para cuantificar precisión: `--labels ground_truth.csv --predictions detector_predictions.csv`.

CSV mínimos:
```csv
video_id,time_s,champion_id,team
yasuo_a,60,yasuo,ENEMY
```
Predicciones requieren una columna adicional `confidence`. Las etiquetas deben surgir de revisión humana del fotograma, NUNCA del título del MP4. No usar ejemplos de este documento como verdad de terreno.

## Criterios para declarar avance real
- Identidad correcta y equipo enemigo corroborados en fotogramas no vistos durante ajustes.
- Medir precisión, recall y falsos positivos por fuente (desarrollo y held_out) y latencia.
- Reducir falsas identificaciones; un "UNKNOWN" honesto es preferible a inventar enemigos.
- Validar el funcionamiento sin bloquear captura, Vosk ni TTS.
- Evaluación manual en dispositivo solo cuando las pruebas offline muestren mejoras verificables.

## Línea base recuperada
- Yasuo 21/11: 6/6 muestras dieron UNKNOWN; no había reconocimiento semántico.
- Fiora 22/11: 23 fotogramas candidatos, 0 revisados por humano; accuracy NOT_MEASURED.
- Warwick: fuente encontrada, sin métricas de identidad verificadas.
- Los cinco videos están en Biblioteca del usuario, NO en GitHub. El workflow en runner no tiene acceso a los MP4 privados, no se les sube sin autorización. Los artefactos de CI comprueban infraestructura y casos simulados; no acreditan comprensión visual.

## Nota de ejecución
En la sesión de 22/11 el entorno de archivos/video no estuvo disponible para correr FFmpeg sobre los MP4 recuperados. No hay reporte nuevo de precisión real. Las pruebas unitarias de GitHub deben verificarse por Actions. **NO APK** en esta rama.
