# CONTROL Coach v18 — sesiones largas (prototipo)

Esta versión experimental **no interpreta Wild Rift** ni conversa con un modelo de IA.
Prueba simultaneidad entre captura visual y reconocimiento de voz local de Android.

## Comportamiento solicitado

- Una sesión dura hasta que el usuario la detiene o Android interrumpe la proyección.
- Eliminados los límites propios de v17: **3 minutos de escucha** y **10 minutos
  de captura**. No se afirma que Android garantice horas de servicio continuo.
- El reconocedor de Android procesa una frase por intento y, al devolver
  `onResults` o `onError`, se vuelve a activar con pausas cortas.
- Cada 20 minutos el motor de voz se libera y crea nuevamente, tras una pausa
  de 4 segundos; **la captura de pantalla no se reinicia**, y por tanto no
  necesita repetir el consentimiento en cada reciclado de voz.
- Si hay errores de servicio, se realizan reintentos espaciados, no un bucle
  acelerado. Si no está disponible el motor offline/español, se explica en UI.
- El texto reconocido se mantiene solo en RAM; no se graba ni envía audio.
- El servicio presenta una notificación persistente con acción **Detener**.

## Conflicto del micrófono

Modo inicial recomendado: **CONTROL prioritario**. Al iniciar la app
se muestra una advertencia: el chat de Wild Rift, Discord u otra app
puede dejar de recibir micrófono. No se puede garantizar exclusividad
real ni bloquear por completo el micrófono de las otras apps; Android
elige cuál recibe la entrada, y dos aplicaciones ordinarias normalmente
no pueden capturar al mismo tiempo.

Fuente: https://developer.android.com/media/platform/sharing-audio-input

## Limitaciones que debemos verificar en Samsung

- El STT integrado no está diseñado para reconocimiento continuo
  indefinido. `onResults/onError` son la frontera correcta antes del
  siguiente `startListening`.
  https://developer.android.com/reference/android/speech/SpeechRecognizer
- Aunque el servicio esté en primer plano, el sistema puede interrumpir
  `MediaProjection` (por ejemplo, bloqueo de pantalla o detención por
  parte del usuario) y requerir una autorización nueva.
  https://developer.android.com/media/grow/media-projection
- La política de prioridad de entrada puede entregar silencio a CONTROL
  al hablar en el chat del juego.
- Medir batería, temperatura y latencia al menos durante 50, 100, 200 y
  300 minutos de uso combinado. El código compilado no equivale a esas pruebas.

## Prueba

1. Con CONTROL visible, tocá «Iniciar sesión larga: pantalla + micrófono».
2. Aceptá la advertencia, autorizá micrófono y captura.
3. Entrá a Wild Rift y decí «CONTROL, hola, ¿me escuchás?»;
   después «CONTROL, estado de captura».
4. Volvé a CONTROL sin detener la sesión y revisá:
   «Frases reconocidas», «Imágenes recibidas durante última escucha»,
   «Audio + imagen simultáneos», «Reinicios del motor de voz» y
   «Sesión activa (min)».
5. Continuá más de 20 minutos y confirmá que aparece al menos
   un reinicio del motor sin pérdida de captura de pantalla.
6. Hacé una prueba adicional con chat de voz de Wild Rift y/o Discord;
   anotá cuál app recibe audio.

**No desinstalar v17/v16 antes de guardar diagnósticos útiles.**
La v18 no cambia la voz femenina provisional ni incluye LLM.

## Medición del tiempo de reactivación tras la respuesta

La pausa de CONTROL después de hablar está configurada inicialmente en **900 ms**.
No existe un mínimo universal de segundos impuesto por la API de Android:
la siguiente solicitud debe esperar a `RecognitionListener.onResults` o `onError`.
La implementación registra la diferencia real entre el evento `TTS.onDone` y el
próximo `SpeechRecognizer.onReadyForSpeech`. La muestra incluye los 900 ms
programados y el tiempo adicional que tarde el motor local en reaccionar.
Se muestra como **Vuelta a escuchar después de hablar (ms)**.
Si la API no llega a `onReadyForSpeech`, queda «aún sin medir».

Reiniciar una solicitud **no garantiza** reiniciar cuotas internas de Android,
ni el servicio está diseñado para escuchas indefinidas. Si aparecen errores
`ERROR_TOO_MANY_REQUESTS` o `ERROR_RECOGNIZER_BUSY`, usar pausas mayores
y analizar un motor ASR local de streaming para sesiones de varias horas.

Referencias oficiales:
- https://developer.android.com/reference/android/speech/SpeechRecognizer
- https://developer.android.com/reference/android/speech/tts/UtteranceProgressListener
