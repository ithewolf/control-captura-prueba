# CONTROL Coach — Soluciones verificadas en fuentes oficiales (2026-11-21)

## Diagnóstico real del repositorio
- CaptureService usa ImageReader.acquireLatestImage() con maxImages=3, y ya cierra imágenes en finally. Correcto para evitar cola de fotogramas antiguos.
- frame() contiene now-lastAt<900, por lo que el procesamiento real de metadatos está limitado a ~1,1 FPS. Subirlo sin un analizador semántico no produce comprensión del juego.
- FrameSampler solo produce fingerprint, cambios y oscuridad. AvContext registra antigüedad y contadores, no recibe información de HUD/campeones/salud ni de cooldowns. AvContext.respond() reconoce solo frases y responde diagnósticos.
- VoskVoiceBridge conecta escucha local a AvContext y TTS. Faltan compilación de la rama y prueba integrada en Android.

## Decisiones técnicas
1. **Primero anotación de datos con perspectivas separadas**: HUD completo de jugador, espectador/repetición, menús y pantallas de transición; validar con segundos de video y escenas reservadas. HoughCircles solo presenta falsos positivos plausibles.
2. **Detector localizado + estado estructurado**: devolver region/etiqueta/confianza/timestamp para minimapa, botones de habilidad, indicador HP visible y pantallas superpuestas. Nunca convertir la mera presencia de círculos en 'campeón detectado'.
3. **OCR de UI**: ML Kit Text Recognition v2 offline, aplicar solo a zonas recortadas y a resolución que conserve texto; usar para niveles/temporizadores legibles. Leer números sin texto legible debe devolver unknown.
4. **Secuencia temporal**: mantener observaciones recientes de 2-3 frames y exigir consistencia para generar eventos; comparar cooldown, HP y variaciones en el tiempo, no atribuir eventos a una sola imagen.
5. **Pipeline asíncrono, latest-only**: una inferencia a la vez, no acumular cola, descartar cuadros intermedios; marcar timestamps y medir latencia. El código actual ya usa acquireLatestImage; el procesador nuevo deberá respetar esta política.
6. **Frecuencia adaptativa**: empezar experimentos a 2 FPS para menús/estado y 5 FPS durante combate. NO elevar indiscriminadamente FPS ni afirmar que el Samsung mantiene estas tasas hasta medir.
7. **Motor de decisión**: separar percepción, estado, reglas tácticas y personalidad del modo Coach/Compañero. Exigir confianza y observaciones frescas. Si falta HP/cooldown/enemigos, no responder 'entra a pelear' como recomendación fiable.
8. **Benchmark honesto**: holdout por escenas y videos, eliminar duplicados perceptuales; métricas de cobertura, precision/recall y falsos positivos; registrar latencia y memoria en Android.
9. **Vosk**: fuente de SpeechService usa VOICE_RECOGNITION y en código actual admite getAudioSessionId; AEC y NS son opcionales, pueden no filtrar diálogos del juego. Mantener wake word y recomendar auriculares.

## Fuentes
- ImageReader: https://developer.android.com/reference/android/media/ImageReader
- ML Kit OCR: https://developers.google.com/ml-kit/vision/text-recognition/v2/android
- MediaPipe object detector live-stream: https://developers.google.com/edge/mediapipe/solutions/vision/object_detector
- ML Kit on-device: https://developers.google.com/ml-kit/guides
- Vosk SpeechService oficial: https://github.com/alphacep/vosk-api/blob/master/android/lib/src/main/java/org/vosk/android/SpeechService.java

## Recomendación para APK
No declarar cerebro comprendiendo Wild Rift, porque todavía no existe percepción semántica integrada. Próximo hito: detector de HUD jugador / espectador y lectura de al menos una señal táctica etiquetada en clips nuevos, seguido de compilación Android. Una APK diagnóstica sí puede ser útil para probar captura+STT si se distingue de una APK Coach final.
