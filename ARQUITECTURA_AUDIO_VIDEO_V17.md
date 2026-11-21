# CONTROL Coach — hallazgos Android y ruta técnica v17

## Objetivo inmediato

Demostrar que CONTROL **recibe una frase del usuario y fotogramas durante el mismo intervalo**
mientras Wild Rift está en primer plano, y que puede devolver una respuesta hablada provisional.
Esto **no** demuestra comprensión del juego ni un LLM.

## Restricciones verificadas en documentación oficial

- Android 14+ exige declarar `foregroundServiceType="mediaProjection|microphone"`,
  `FOREGROUND_SERVICE_MEDIA_PROJECTION`, `FOREGROUND_SERVICE_MICROPHONE` y
  `RECORD_AUDIO`, más consentimiento explícito de MediaProjection.
  https://developer.android.com/about/versions/14/changes/fgs-types-required
- El servicio de micrófono debe iniciarse mientras la Activity esté visible (con
  excepciones limitadas). No puede iniciarse libremente desde el fondo.
  https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start
- `SpeechRecognizer.createOnDeviceSpeechRecognizer` requiere motor local disponible;
  el API no está diseñado para escucha continua de sesiones largas.
  https://developer.android.com/reference/android/speech/SpeechRecognizer
- Para Android 13+ `checkRecognitionSupport()` permite consultar idiomas
  instalados; no asumir que `es-AR` está disponible solo porque existe STT local.
  https://developer.android.com/reference/android/speech/RecognitionSupport
- Dos aplicaciones ordinarias que intentan grabar micrófono a la vez pueden
  provocar que una de ellas reciba silencio; comprobar el caso con chat de voz
  de Wild Rift y con Discord encendido/apagado.
  https://developer.android.com/media/platform/sharing-audio-input

## Criterio de validación antes de hablar de 'cerebro'

1. Sesión activada desde Activity visible, con consentimiento de ambas entradas.
2. Fotogramas recibidos con la pantalla de Wild Rift en primer plano.
3. Frase reconocida localmente. Ver texto y estado por separado.
4. Fotogramas nuevos **durante el mismo intervalo** de reconocimiento.
5. Respuesta TTS audible, luego escuchar una segunda frase.
6. Repetir con chat de voz de juego/Discord activo para detectar conflicto de micrófono.

## Separación de capas

```
MediaProjection -> Captura/FrameSampler -> muestras (en memoria) ----|
                                                                    |-> Coordinador audiovisual -> motor de razonamiento (PENDIENTE)
Micrófono -> STT local -> texto reconocido --------------------------|                         -> TTS provisional -> usuario
```

En v17 el 'cerebro' es **un puente de diagnóstico con respuestas programadas**.
No está interpretando escenas ni generando tácticas.

## Opciones para voz prolongada (a evaluar después del diagnóstico)

- **sherpa-onnx**: librería offline con reconocimiento *streaming* para Android.
  https://github.com/k2-fsa/sherpa-onnx
- **Vosk**: reconocimiento offline para Android con API de streaming, modelos
  compactos; comparar precisión en español, latencia, consumo y tamaño.
  https://github.com/alphacep/vosk-space/blob/master/android.md
- **whisper.cpp**: ejemplo de Android para transcripción local; más apropiado
  para transcripciones por segmentos que para activación de baja latencia sin
  evaluar rendimiento del Samsung.
  https://github.com/ggml-org/whisper.cpp/tree/master/examples/whisper.android.java

No hay elección definitiva ni librería de terceros integrada en la APK todavía.
Seleccionar solo después de medir resultados reales de v17; mantener cero
servicios de pago y no enviar voz/video del usuario a servidores externos.

## Datos que guardar sin almacenar audio

Registrar solo contadores, errores, tiempos, señales de concurrencia y,
en la prueba local, último texto reconocido en RAM. No registrar audio
crudo ni capturas persistentes sin consentimiento explícito.
