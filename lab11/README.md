# CONTROL Lab11 — evaluacion de eventos de video real (SIN APK)

## Material disponible
Video de Yasuo de Biblioteca del usuario: `TOP 1 YASUO SERVER “JarePley”🐲 WILD RIFT.mp4`.
Se extrajeron ocho fotogramas (20,40,60,80,100,120,140,160 segundos) para inspeccion humana preliminar. El video no se incluye en GitHub.

**Precaucion de dominio**: la interfaz es una vista panoramica/espectador sin controles tactiles visibles en esos fotogramas. Antes de usarla para validar HUD del jugador en Wild Rift, revisar procedencia y version del juego. No asumir que controles, barras o niveles equivalen a la vista del telefono.

## Candidatos visuales NO etiquetados como verdad-terreno
- 20 s: combate cerca de estructura/base
- 40 s: enfrentamiento cerca del borde del mapa
- 60 s: oleada y campeones cerca de torre
- 80 s: combate junto a torre/area de peligro
- 100 s: limpieza de oleada en carril
- 120 s: encuentro de campeones y posible hostigamiento
- 140 s: posible pelea grupal cerca de zona central
- 160 s: enfrentamiento grupal con daño visible

Son pistas para revision. Se requiere revisar fotogramas cercanos y los segundos anteriores y posteriores para concluir eventos temporales. Por ejemplo una captura aislada NO certifica retirada, gank ni una estrategia.

## Evaluacion honesta
`tools/lab11_strategy_eval.py` evalua segmentos anotados por un humano contra predicciones independientes del detector. Las columnas CSV son `video_id,start_s,end_s,event`. Eventos: TEAM_FIGHT, RETREAT, GANK, OBJECTIVE, LANE_PUSH, UNKNOWN.
Si no existe deteccion real, el reporte sera **NOT_MEASURED**. Los tests sinteticos solo prueban el calculador de evaluacion; NO representan aprendizaje visual.

## Orden de ejecucion
1. Revisar contenido y version, no contaminar con otros juegos.
2. Anotar ventanas de tiempo e identidad de eventos con criterios observables.
3. Ejecutar detector de eventos (por implementar), generar predicciones separadas sin consultar anotaciones.
4. Evaluar error/timing/precision/recall y registrar desaciertos.
5. Conectar eventos confirmados a estado tactico solo con datos suficientes; probar alternativas A/B/C.
6. Validar captura Android por separado. APK nueva continua prohibida.

No atribuir 'aprendio' ni 'reconoce estrategias' hasta medir resultados reales.
