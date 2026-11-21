# CONTROL Coach v17 — diagnóstico audiovisual local

Este prototipo NO interpreta Wild Rift ni incluye un LLM. Su objetivo exclusivo es comprobar
que la captura de pantalla de `MediaProjection` y el reconocimiento de voz
**local de Android** pueden ejecutarse en un mismo servicio en primer plano,
y responder con una frase TTS provisional.

## Prueba en Samsung

1. Instalá la APK de la ejecución exitosa **más reciente** de la rama
   `control-cerebro-av-v17` en GitHub Actions. No mezcles APK de compilaciones anteriores.
2. Abrí CONTROL y tocá **Autorizar captura + micrófono**. Permití el micrófono
   y luego la captura de toda la pantalla. No cierres la sesión desde la notificación.
3. Abrí Wild Rift y, durante los primeros **3 minutos**, decí:
   - «CONTROL, hola, ¿me escuchás?»
   - «CONTROL, ¿cuál es el estado de captura?»
4. Volvé a CONTROL para inspeccionar:
   - «Imágenes recibidas», «Imágenes analizadas».
   - «Intentos de escucha», «Frases reconocidas» y «Último texto escuchado».
   - «Imágenes recibidas durante última escucha» y «Audio + imagen simultáneos».
   - «Micrófono/voz» y «Respuesta local».
5. Repetí una prueba con voz y otra sin hablar para observar los resultados.

## Criterio de éxito

- La pantalla sigue aportando cuadros mientras Wild Rift está en primer plano.
- Se reconoce al menos una frase real del jugador.
- Se reciben cuadros nuevos *durante el mismo intervalo de reconocimiento*.
- CONTROL responde por TTS sin abandonar el servicio de captura.

**Limitación importante:** el indicador de simultaneidad solo se activa si se ha
reconocido una frase y se han recibido imágenes durante esa escucha. No garantiza
que el audio funcione durante toda una partida ni implica comprensión visual.

## Diagnóstico

Si la app informa «sin reconocimiento local instalado», «no tiene español compatible»
o códigos de error, el motor local del teléfono puede estar ausente/no compatible.
La app **no usa un servicio de reconocimiento en la nube como sustituto**.

La prueba de escucha finaliza automáticamente tras 3 minutos; la captura
continúa con su propio límite de 10 minutos. El botón «Probar voz de Android
(independiente)» es solo TTS y por sí mismo no prueba el micrófono.

La voz masculina definitiva, la interpretación de campeones y el entrenador
inteligente son fases posteriores. Los últimos textos reconocidos permanecen
en memoria durante la sesión; esta prueba no guarda ni envía grabaciones de audio.
