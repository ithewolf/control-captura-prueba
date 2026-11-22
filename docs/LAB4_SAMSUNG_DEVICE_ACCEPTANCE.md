# CONTROL — protocolo de aceptación de APK experimental (Samsung)

Esta versión de laboratorio contiene captura MediaProjection + Vosk local + Piper Ald provisional. **No** contiene reconocimiento visual entrenado, conversación libre ni clonación de CONTROL.

## Revisión previa
1. Guardar la APK estable antes de instalar la de Lab4. Si Android impide instalar ambas, verificar ID del paquete y preservar datos.
2. Confirmar permiso del micrófono en Ajustes > Aplicaciones > CONTROL > Permisos. La notificación de servicio de primer plano no significa que Vosk reciba voz.
3. En CONTROL, elegir «Piper Ald» únicamente para el ensayo; **NO** es la voz definitiva.
4. Iniciar «Pantalla + micrófono» y conceder la captura completa al sistema Android.

## Prueba aislada con el juego CERRADO
1. Verificar que aumenten `Imágenes recibidas` y `Imágenes analizadas`.
2. Decir «CONTROL, hola», esperar el texto Vosk en «Último texto escuchado».
3. Presionar «Diagnóstico: hacer responder al CONTROL de la sesión».
4. Revisar `Piper (prueba)`: exigir «Piper OK»; si dice «Piper ERROR», registrar el error exacto. **Escuchar** la frase: un «Piper OK» no garantiza que haya sido audible por parlante.
5. Esperar al terminar: verificar «Vuelta a escuchar después de hablar (ms)» y pronunciar una nueva frase. Debe volver a reconocer voz.
6. Detener desde la notificación y volver a abrir: comprobar `Último registro guardado`.

## Prueba real con Wild Rift abierto
1. Solo después del ensayo aislado. Comprobar FPS y temperatura observables en el móvil, sin inferirlos de contadores de CI.
2. Mantener CONTROL capturando y cambiar a Wild Rift sin reiniciar permiso de pantalla.
3. Hablar una frase corta; al regresar, comprobar «Audio + imagen simultáneos: observados». **No usar solo ese contador como prueba de comprensión del juego**.
4. Probar 5-10 minutos, con y sin voz de juego; registrar interrupciones. Si Wild Rift/Discord pierde micrófono, detener ensayo y analizar conflicto de AudioRecord.

## Condiciones de fallo
- `Piper ERROR` o fallback a TTS de dispositivo => Piper NO aprobado.
- No aumenta `Imágenes recibidas` => captura NO aprobada.
- Hay permiso pero no cambia «Último texto escuchado» => Vosk/micrófono NO aprobado.
- El texto reconoce pero no reanuda escucha tras TTS => flujo de audio NO aprobado.
- La app responde con frases básicas => no indica conversación libre o IA táctica.
- Temperatura excesiva, bloqueo del juego o desconexión del micrófono de terceros => detener; APK no apta para stream/juego continuo.

## Evidencia mínima
Registrar fecha, Android versión, modelo de teléfono, versión de APK, `Piper OK/ERROR` y su tiempo, lecturas captura/voz, temperatura aproximada y comportamiento de FPS; **sin** publicar capturas privadas ni audio del usuario.

### Fuentes Android
- https://developer.android.com/about/versions/14/changes/fgs-types-required
- https://developer.android.com/reference/android/media/AudioTrack

**Estado:** lista de verificación preparada; sin resultados reales de dispositivo todavía.
