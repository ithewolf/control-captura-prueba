# CONTROL Coach — Ensayo de reconocimiento visual (21/11/2026)

## Decisión de producto
La interfaz de juego seguirá siendo **solo micrófono / respuestas habladas**. No se incorpora chat escrito al modo en vivo. La entrada textual sigue existiendo internamente después de STT y en pruebas unitarias.

## Videos aportados por Wolf (no publicar los videos)
1. `TOP 1 YASUO SERVER “JarePley”🐲 WILD RIFT.mp4`: 1280×720, 60 fps, ~180 s; espectador/repetición con escenas de equipo, campeones y torres.
2. `10+ DEATHS on EVERY LANE how TO play WARWICK & CARRY!!.mp4`: 854×480, 30 fps, ~2186 s; League of Legends PC, control negativo de transferencia de interfaz.
3. `231949.mp4`: 1080×2340, ~49 s; grabación de celular con tramos negros; **no es evidencia de reconocimiento de Wild Rift**.
4. `TOP 1 YASUO LEGENDARY🐲 WILD RIFT(1).mp4`: grabación larga con HUD de jugador, preparada para contraste con pruebas previas del laboratorio `lab-yolo-cvat-opencv-hp`.

## Prueba A: OpenCV HSV + geometría de franjas horizontales
Script de laboratorio: lectura por OpenCV, HSV para rojo/azul/verde, apertura morfológica horizontal 13×1, rectángulos de 16–100 píxeles, altura <=9, exclusión aproximada de regiones de minimapa / HUD.

- Wild Rift Yasuo, 16 muestras cada 10 s desde t=20: 210 franjas rojas, 211 azules, 586 verdes = **1007 candidatos**.
- League PC Warwick, 25 muestras cada 90 s desde t=20: 147 franjas rojas, 216 azules, 198 verdes = **561 candidatos**.
- No son unidades: pueden ser efectos, terreno, iconos, barras y decoración. Se vieron numerosas cajas fuera de barras de personaje por inspección visual de fotogramas anotados en t=95 s (Yasuo) y t=420 s (Warwick).
- En el fotograma t=95 s de Yasuo se observan varios campeones y barras de ambos equipos; también muchos resultados espurios sobre brillos y escenario. En t=420 s de Warwick el jugador está en un campamento de jungla, y color rojo **no equivale a campeón enemigo**.
- **No reportar precision/recall de enemigos** porque todavía no hay etiquetado exhaustivo ni correspondencia de identidad.
- **Capacidad táctica automática validada: ninguna**. El algoritmo no debe producir `enemyNear` o `allyNear` para el cerebro.

## Prueba B: revisar el detector HUD ya existente
La rama `lab-yolo-cvat-opencv-hp` tiene `tools/hud_circle_probe.py`, un prototipo que mide la geometría de botones circulares en la esquina inferior derecha. Sus auditorías previas ya distinguen vista de jugador y espectador.
- Esto puede servir para identificar el tipo de escena, **no** para reconocer personajes, aliados o enemigos.
- No trasladar el HUD de PC a Wild Rift: minimapa y controles cambian de posición.

## Referencias de fotogramas revisados
Yasuo t=35,95,165 s; Warwick t=420,780,1140 s. Inspección visual manual, sin etiquetado exhaustivo.
Interpretaciones humanas de prueba (NO predicciones del software):
- Yasuo t=95: enfrentamiento con varias unidades y barras visibles; el consejo no debe afirmar una ventaja sin visión/conteo confiable.
- Yasuo t=165: combate cercano junto a una torre y súbditos. No inferir porcentaje HP.
- Warwick t=420: personaje en campamento de jungla; **no anunciar un gank solo por barra roja**.

## Resultado y ruta de validación
**NO APROBADO para advertencias en vivo sobre unidades**. La detección por color tiene demasiados falsos candidatos; el cerebro actual sigue sin recibir `allyNear`/`enemyNear` desde observaciones verificadas.

Siguiente hito:
1. Armar un lote etiquetado pequeño, estratificado por `wild_rift_player`, `wild_rift_spectator`, `lol_pc_negative`, `menu_or_black`. Etiquetar `ally_unit`, `enemy_unit`, `neutral_unit` y `unknown` (no porcentajes HP). Reservar al menos un clip distinto para test.
2. Entrenar un detector **específico** de etiquetas sobre cuadros Wild Rift (o un detector de HUD+unidad) y evaluar falsos avisos en Warwick.
3. Usar evidencia temporal (>=2 cuadros), confianza calibrada y caducidad para entregar datos estructurados al motor táctico.
4. Ejecutar benchmark de CPU, RAM y temperatura en Samsung; solo entonces habilitar avisos hablados reales.

Documentación oficial: CVAT Ultralytics YOLO Detection <https://docs.cvat.ai/docs/manual/advanced/formats/format-yolo-ultralytics/>; MediaPipe ObjectDetector Android <https://developers.google.com/edge/mediapipe/solutions/vision/object_detector>; ML Kit modelos personalizados <https://developers.google.com/ml-kit/vision/object-detection/custom-models/android>.
