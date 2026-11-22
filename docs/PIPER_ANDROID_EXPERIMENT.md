# CONTROL Coach: plan experimental de voces propias (Piper / sherpa-onnx)
Fecha 2026-11-22 — diseño investigado, NO integración funcional aún.

## Restricciones verificadas
- Android 11+? Revisar minSdk actual antes de agregar bibliotecas.
- La captura MediaProjection, el reconocimiento Vosk y la respuesta TTS Android local ya se probaron en teléfono.
- No instalar otra app de TTS, no requerir PC, Internet, ni API paga durante el juego.
- Mantener fallback a Android TextToSpeech hasta completar pruebas de motor y voz integrada.
- Personalidad de CONTROL y modo Coach/Compañero separados del modelo acústico de voz.
- No prometer que un fichero de voz Windows ejecuta directo en Android.

## Motor candidato
**sherpa-onnx OfflineTts para Android**, usando una voz Piper ONNX exportada/compatible con el runtime. El proyecto upstream incluye ejemplo Android. No vendorear la APK de otra aplicación ni copiar bibliotecas de terceros sin revisar licencia y redistribución.

Referencias oficiales:
https://github.com/k2-fsa/sherpa-onnx
https://k2-fsa.github.io/sherpa/onnx/tts/piper.html
https://k2-fsa.github.io/sherpa/onnx/tts/apk.html

## Ensayo incremental
1. Rama experimental independiente; elegir UNA voz local en español con licencia de redistribución comprobada. Si no hay voz masculina rioplatense con licencia compatible, usar una voz temporal solo para prueba del motor.
2. Agregar librerías nativas Android arm64-v8a primero; verificar APK alineación 16KB y minSdk/ABI, dependencias y aumento de tamaño.
3. Preparar voz y archivos (.onnx + config + fonemizador/espeak-ng-data según motor) dentro de assets de la APK o en almacenamiento privado de la aplicación. Verificar checksums.
4. Implementar contrato VoiceOutput: speak(text), stop(), isReady(), release(), onDone(); backend AndroidLocalTTS actual y backend Piper/Sherpa. No iniciar dos motores al mismo tiempo.
5. Reutilizar pipeline actual: al hablar, pausar Vosk; al terminar, reanudar en < 2 s. Proteger contra audio de auto-eco.
6. Comparar CPU / RAM PSS / calor / latencia / FPS real con Wild Rift y captura. Si excede presupuesto, usar TTS Android local automáticamente.
7. Añadir selector de voz dentro de CONTROL; no confundir seleccionar una voz local del sistema con tenerla incluida en APK.
8. Solo después permitir segundo paquete de voz. Revisar tamaño total: Vosk ~39 MiB y librerías nativas ~18 MiB ya ocupan espacio en la APK Lite.

## Pruebas que deben pasar antes de publicar
- Sin datos móviles ni Wi-Fi: cinco respuestas habladas; voz argentina si se cuenta con modelo apto.
- Sin instalar ningún paquete TTS externo.
- 10 ciclos de start/stop; evitar falla de audio y fugas de memoria.
- Pantalla + micrófono + TTS durante juego; medir CPU, PSS, FPS, temperatura.
- Cuando falla Piper, vuelve al motor Android sin silenciar CONTROL.
- Revisar licencia del código, de modelos y las restricciones de distribución.

## Estado
Solo estudio/documentación. NO existe una APK que incluya Piper a la fecha de este documento.
