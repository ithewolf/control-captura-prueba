# CONTROL Coach — Revisión visual independiente (21-11-2026)

## Nuevas pruebas sobre archivos originales
Usando FFmpeg, se tomaron **66 fotogramas** a 512 px:
- Video de Yasuo con HUD real de jugador, intervalo entre t=150 y 450 segundos, 1 muestra cada 10 segundos: 30 imágenes.
- Video corto de Yasuo con predominio de grabación/espectador, una muestra cada 5 segundos: 36 imágenes.

Se ejecutó el detector de círculos actual descrito en `tools/hud_circle_probe.py`: HoughCircles, región inferior derecha (x>65%, y>55%), umbral >=10 círculos.
- Video largo: **29/30** positivos. El fotograma sin detección corresponde a un menú que tapa gran parte del juego, por lo que *no debe considerarse error* sin etiqueta específica.
- Video corto: **5/36** positivos. Las imágenes muestran interfaz de espectador/repetición; algunas contienen iconos y círculos de distinta procedencia. Estos 5 son **candidatos a falsas detecciones**, no una tasa de falsos positivos validada por etiquetado exhaustivo.
- La detección de círculos **no es un modelo entrenado** y **no usa memoria de fotogramas**. No identifica campeones, movimientos, HP, cooldowns, ni decisiones.

## Puntos de revisión del código Android
- `CaptureService.frame()` actualmente limita el muestreo con `now-lastAt<900` (~1.1 fps), sin motor visual semántico conectado.
- `FrameSampler` registra huella visual y oscuridad; `AvContext` solo recibe metadatos de imagen reciente. No existe conexión válida desde pixels a estado de juego específico.
- `AvContext.respond()` es deliberadamente diagnóstico y evita inventar una situación táctica.
- `VoskVoiceBridge` incorpora respuesta a voz, pero todavía falta compilar y validar el conjunto en un Android real.

## Próximas medidas
1. Separar HUD de jugador, HUD de espectador y menús interpuestos; etiquetar explícitamente (no contar un inventario como fracaso de una detección de HUD visible).
2. Reemplazar el conteo genérico de círculos por detección de regiones y relaciones espaciales (minimapa superior izquierdo, controles inferior derecho, estado del campeón), con rechazo cuando falten evidencias.
3. Crear clips reservados y pruebas memoria-cero sin reutilización de imágenes o estados de sesión.
4. Solo después integrar un detector probado al hilo de captura Android a 2–5 fps adaptativos, medir tiempo por frame y energía.
5. Validar vida/enfriamientos/enemigos; evaluar si el consejo es correcto. No generar una APK anunciada como entrenador inteligente hasta superar esa validación.

**Decisión:** conservar la APK final en espera. Las métricas presentes son detección de posibles controles, no comprensión del juego.
