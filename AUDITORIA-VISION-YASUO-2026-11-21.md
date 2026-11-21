# Auditoría visual preliminar — CONTROL Coach — 21 noviembre 2026

## Material y alcance
- Archivo corto `TOP 1 YASUO SERVER “JarePley”🐲 WILD RIFT.mp4`: 1280×720, 60 fps, 179.88 s. Presenta grabación sin los controles táctiles completos de jugador.
- Archivo largo `TOP 1 YASUO LEGENDARY🐲 WILD RIFT(1).mp4`: 1280×720, 60 fps, 963.7 s. Entre los segundos 180–190 muestra minimapa y botones de habilidades/ataque, con perspectiva real del jugador.
- Los videos son privados del usuario, no se incorporaron al repositorio.

## Extracción verificada
Mismo video corto, tramo de 12 s desde t=35 s; FFmpeg CPU, imágenes de 512 px:
- 1 fps: 1.34 s
- 2 fps: 1.36 s
- 5 fps: 1.35 s
- 10 fps: 1.84 s

Los tiempos corresponden al equipo de pruebas; no predicen consumo de Android.
Se recomienda 5 fps como punto de partida offline, con muestreo adaptativo en combate. Aumentar FPS no mejora automáticamente la precisión.

## Primera detección visual ejecutada
Se ejecutó OpenCV HoughCircles en fotogramas de 512 px, buscando círculos en el 35% derecho y 45% inferior de la imagen.
- 10 fotogramas del video largo t=180–190 s, etiquetados preliminarmente como con controles de jugador: **10/10 candidatos detectados**.
- 40 fotogramas del video corto, etiquetados preliminarmente como sin controles completos de jugador: **1/40 falsos candidatos**.
- Precisión muestral: 10/11 = 90.9%; sensibilidad muestral: 10/10 = 100%; especificidad muestral: 39/40 = 97.5%.
- Tiempo de detector por frame, CPU del entorno: 15.88 ms de media en video largo y 12.42 ms en video corto. No es una medición de celular ni de pipeline completo.
- Código reproducible: `tools/hud_circle_probe.py`; **prototipo no integrado a Android**.

### Advertencias
1. Solo se probaron 50 imágenes próximas temporalmente de dos clips, por lo que no son observaciones independientes. Métricas de *esta muestra*, no garantía ni estimación poblacional.
2. Etiquetas visuales preliminares revisadas por inspección de contactos. Falta auditoría por fotograma y prueba con clips verdaderamente reservados.
3. Un círculo puede ser escenario, efecto u otra UI; la heurística no identifica nivel, campeón, vida, daño, enemigos, cooldown ni decisiones tácticas.
4. El video corto mezcla pantalla de perfil y jugadas; debe etiquetarse por segmento.
5. Los ensayos previos de WolfLocalVision dieron `unknown` sobre seis fotogramas. La prueba Hough es un detector diferente, no demuestra que el código antiguo haya mejorado.
6. No existe verificación de compilación actual de Vosk + visión + bienvenida ni prueba conjunta en Samsung.

## Diagnóstico y decisión de APK
- **Resultado de esta auditoría: NO generar aún la APK definitiva de coach inteligente.**
- Una APK **diagnóstica** puede ser conveniente antes de que la inteligencia esté terminada, si necesitamos comprobar permisos, MediaProjection, escucha y carga del detector en el Samsung; no llamarla definitiva ni presentarla como entrenador funcional.
- Antes de avanzar, ampliar ejemplos con HUD real de jugador, confirmar anclajes de minimapa y acciones; medir falsa alarma en escenas reservadas; validar vida, habilidades y eventos y documentar cuándo abstenerse.
- Posteriormente integrar pipeline local de baja latencia con muestreo adaptativo y probar rendimiento/batería en Android.

## Reproducibilidad local
```bash
ffmpeg -ss 180 -i "TOP 1 YASUO LEGENDARY🐲 WILD RIFT(1).mp4" -t 10 -vf "fps=1,scale=512:-2" player/frame_%03d.jpg
python tools/hud_circle_probe.py player/frame_*.jpg
```
Para la revisión general del video corto, extraer a 512 px con FFmpeg y usar herramienta equivalente. La evaluación de precisión necesita etiquetas humanas verificadas; no se adjudican resultados sintéticos como inteligencia.
