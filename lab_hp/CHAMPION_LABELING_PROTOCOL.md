# Campeones Wild Rift — protocolo de etiquetado (22/11/2026)

Se agregó un segundo esquema de CVAT YOLO totalmente independiente de `dataset.yaml` (barras de HP). Las clases iniciales son Fiora, Sett, Syndra, Yasuo, Warwick; **no significa que hayan sido reconocidos por un modelo**.

## Reglas
1. Revisar visualmente cada caja de campeón; nunca tomar nombre del archivo o título del vídeo como etiqueta de detección.
2. Indicar `reviewed:true` solo tras revisión humana y `negative:true` exclusivamente para imagen sin campeones de estas clases, con TXT vacío.
3. Manifest: `image`, `labels`, `source_video`, `scene_id`, `split`, `reviewed`, `negative`.
4. Separar por vídeos enteros (`source_video`), no por fotogramas. Cuando haya menos de tres partidas independientes, no reportar accuracy de generalización.
5. Incluir recortes difíciles: barra de vida superpuesta, efectos, cámara en movimiento, miniatura de campeón, oclusión, aliados y enemigos similares, HUD móvil.
6. No afirmar funcionamiento real hasta contar con etiquetas verificadas, modelo entrenado, test ciego por partida y métricas precision/recall más latencia Android.

## Estado
- Esquema separado y validador agregados a Lab 1.
- Doce casos de validación preparados; ejecución GitHub Actions aún por verificar.
- No hay export CVAT etiquetado ni pesos entrenados.
- Cero evidencias de reconocimiento automático de Fiora/Sett/Syndra.
- La APK 27 no se construye a partir de estos cambios hasta terminar controles de captura y rendimiento.
