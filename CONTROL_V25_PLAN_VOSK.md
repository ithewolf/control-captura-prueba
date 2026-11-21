# CONTROL Coach v25 — Migración experimental a Vosk

Estado: rama experimental creada desde control-audio-focus-v24. **Sin APK ni integración funcional todavía**.

## Decisión técnica (Wolf, 21-11-2026)
- Motor STT candidato: Vosk Android con modelo oficial `vosk-model-small-es-0.42` (aprox. 39 MB comprimido; Apache-2.0).
- Objetivos: sin costes por uso; procesar micrófono localmente; rendimiento bajo durante Wild Rift; sesiones largas.
- Mantener la v24 como respaldo: no modificar sus archivos directamente.
- No asumir escucha continua probada, precisión rioplatense ni compatibilidad simultánea con audio del juego.

## Integración a implementar y verificar
1. Preparar dependencia `com.alphacephei:vosk-android` desde Maven Central, fijando una versión publicada compatible.
2. Proveer modelo de español desde fuente oficial con comprobación de integridad, instalación local y ruta interna privada. Evitar incluir una descarga invisible y no empezar STT hasta que esté listo.
3. Crear un puente `VoskVoiceBridge` independiente, que use `AudioRecord` o el servicio `SpeechService` de Vosk para streaming a 16 kHz mono, aislado de `VoiceBridge` existente.
4. Aplicar estados explícitos: IDLE / LISTENING / PROCESSING / SPEAKING / RECOVERING; nunca levantar dos micrófonos, recognizers o procesos de escucha simultáneos.
5. Pausar/cerrar de forma segura la entrada mientras habla TTS; reiniciar solo cuando la salida de voz termine; gestionar errores con backoff.
6. Probar `NoiseSuppressor.isAvailable()` y el efecto asociado a un AudioRecord compatible; registrar si está realmente activo y dar fallback.
7. Añadir al diagnóstico: texto parcial/final, tiempo de escucha, errores, TTS, consumo de RAM/CPU y tiempo de recuperación. No guardar audio crudo.
8. Mantener activación «CONTROL» pero conservar un modo de conversación acotado para preguntas siguientes sin repetir wake word; evitar respuestas a charlas ajenas.
9. No afirmar que Vosk sea un cerebro: `ControlBrain` sigue siendo respuestas de diagnóstico. Motor táctico es un trabajo separado.
10. Validar 5 preguntas consecutivas, después sesiones de 15, 50 y 100 minutos con Wild Rift y captura simultánea, sin caídas ni pérdida permanente de voz.

## Criterio de aceptación
- APK compilada con modelo listo y fuente de voz elegible.
- Frase reconocida mostrada textualmente y respuesta reproducida.
- Captura visual comprobada durante la misma escucha.
- Cero bucles de reconexión y salida TTS sin realimentación.
- Comparar consumo/precisión con v24 antes de convertir Vosk en predeterminado.

Referencias:
- https://alphacephei.com/vosk/install
- https://alphacephei.com/vosk/models
- https://github.com/alphacep/vosk-android-demo
- https://developer.android.com/media/platform/sharing-audio-input
