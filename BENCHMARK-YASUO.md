# Banco de pruebas YASUO — CONTROL Coach (pre-APK)

## Material recuperado
- TOP 1 YASUO SERVER “JarePley”🐲 WILD RIFT.mp4 (aprox. 180 s; 1280×720; 60 fps)
- TOP 1 YASUO LEGENDARY🐲 WILD RIFT(1).mp4 (archivo original largo)

Los videos están en la Biblioteca del usuario; no se subieron al repositorio ni se distribuyen.

## Línea de base verificada en la documentación anterior
Se probaron 6 fotogramas de la grabación de Yasuo en t=15,30,60,90,120,150 s con WolfLocalVision: 6/6 'unknown', sin detección fiable de HUD/vida/minimapa. **No se interpreta como comprensión del juego.**

## Investigación para el motor visual
- Google ML Kit OCR local: https://developers.google.com/ml-kit/vision/text-recognition/v2/android
- Google ML Kit detectores personalizados: https://developers.google.com/ml-kit/vision/object-detection/custom-models/android
- TensorFlow Lite detector propio: https://developers.google.com/codelabs/tflite-object-detection-android
- Procesamiento de HUD en video: https://pmc.ncbi.nlm.nih.gov/articles/PMC7302570/

## Orden de evaluación
1. **Muestreo:** extraer al menos 50 fotogramas por video cubriendo selección, mapa, combate, muerte y pantalla final. Registrar segundo exacto.
2. **Etiquetado humano:** `hud_visible`, `player_health_visible`, `minimap_visible`, `death_screen`, además de oclusión, perspectiva espectador/POV y layout. Sin inferir valores ocultos.
3. **Separar entrenamiento/evaluación por tiempo y video:** nunca medir en los mismos fotogramas usados para ajustar parámetros. Evaluar explícitamente en el segundo video.
4. **Modelo/heurísticas:** optimizar primero localización del HUD; seguir con OCR de niveles/temporizadores; después detectar barras de vida, cooldowns y eventos. Un modelo genérico COCO no identifica campeones ni las habilidades.
5. **Métricas:** ejecutar `tools/evaluate_yasuo.py labels.json predictions.json`. Mide precisión, sensibilidad, cobertura (porcentaje donde predice) y abstenciones por clase.
6. **Temporalidad:** rechazar observaciones de más de 2.5 s, cambiar de escena, barras tapadas y configuraciones incompatibles.
7. **Decisiones:** casos etiquetados por experto; medir si el consejo táctico se justifica con indicadores presentes. El motor siempre debe indicar 'no sé' ante incertidumbre.
8. **Generalización:** un video de Yasuo no basta. Probar HUD real en perspectiva del jugador, otros campeones, roles, resolución y orientación.
9. **Desempeño:** latencia por frame y consumo de CPU, RAM y batería en el Samsung antes de emitir consejos automáticos.

## Condición mínima de avance
Provisionalmente, para tareas de HUD se busca >=90% de precisión y >=85% de recall **en clips reservados no usados para ajuste** y cobertura >=80% para las señales críticas, con >=50 ejemplos positivos y negativos por etiqueta. Son umbrales de ingeniería propuestos, no resultados medidos ni garantía de comprensión táctica. Para consejos que afecten decisiones de combate, exigir validación específica y prioridad a abstenerse.

**Estado actual:** los dos archivos están identificados y materializados para análisis, pero la sesión de ejecución de video no estuvo disponible; no hay nuevos resultados de fotogramas, entrenamiento ni inferencia realizados en esta etapa. No generar APK hasta que el cerebro supere benchmarks reproducibles.
