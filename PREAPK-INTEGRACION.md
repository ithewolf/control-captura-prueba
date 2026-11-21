# CONTROL Coach — pre-APK audio/visión (rama experimental)

**Estado:** cambios de fuente integrados, **NO compilados ni probados en Android**. No declarar APK lista.

## Cambios reales en esta rama
- Se conserva `CaptureService` con MediaProjection e ImageReader.
- `AvContext` asocia cada frase al último frame mediante `SystemClock.elapsedRealtime()` y la sesión activa; rechaza imágenes de más de 2500 ms y sesiones antiguas.
- `VoskVoiceBridge` reemplaza el STT predeterminado por Vosk local en español; no usa la nube.
- Gradle incorpora `com.alphacephei:vosk-android:0.3.47` y descarga el modelo pequeño español `vosk-model-small-es-0.42` **al compilar**, lo incluye como asset `model-es` con `.uuid`; en ejecución no hay descarga. El modelo oficial es Apache-2.0 y ocupa unos 39 MB comprimido.
- Mientras CONTROL habla se detiene Vosk y se reactiva después de terminar la voz. Ciclo de recuperación cada 15 minutos, sin detener la captura.
- `AvContextSmokeTest` cubre recencia, sesión, orden, activación y prohibición de inventar visión. **Tests todavía no ejecutados**.

## No confundir
- Vosk **transcribe**; no produce una voz argentina. La salida sigue siendo TTS local de Android, según las voces instaladas.
- El cerebro aún **no reconoce** campeones, HUD, habilidades ni tácticas. El puente sólo permite asociar la frase a una imagen reciente. No se deben dar consejos de pelea basados en `frames` o `changes` únicamente.
- Si Discord o Wild Rift ocupan el micrófono, Android puede impedir la grabación simultánea. Se requiere prueba real con y sin auriculares.
- El código todavía no implementa cancelación de ruido garantizada; medir la tasa de reconocimiento antes de activar DSP adicional.

## Barreras antes de crear/entregar APK
1. Compilación Gradle de esta rama con modelo local incluido y sin errores.
2. Ejecutar tests Java: `javac -d build/smoke app/src/main/java/com/iawolf/captureprobe/AvContext.java tests/AvContextSmokeTest.java && java -cp build/smoke com.iawolf.captureprobe.AvContextSmokeTest`.
3. Confirmar `RECORD_AUDIO`, `FOREGROUND_SERVICE_MICROPHONE`, permisos en ejecución y que arranque la sesión desde actividad visible en Android 14+.
4. Prueba Samsung S24 Ultra: fotogramas y frases crecen mientras Wild Rift está abierto; Vosk escucha en el mismo servicio; respuesta TTS no se re-transcribe.
5. Prueba con auriculares, juego/Discord en voz y bloqueo/retorno, sin prometer funcionamiento concurrente si Android deniega el micrófono.
6. Medir memoria, CPU, latencias, fallos de extracción del modelo y cierre limpio.
7. Desarrollar el reconocimiento visual semántico por separado y evaluar con fotogramas reales del HUD.

Repositorio: ithewolf/control-captura-prueba
Base: control-vosk-experimental-v25
Rama de integración: control-integracion-av-preapk
