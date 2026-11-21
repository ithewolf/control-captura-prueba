# Auditoría de integración incremental HP — 2026-11-21

## Aislamiento y restauración
- Rama intacta de desarrollo Android: `control-integracion-av-preapk`.
- Respaldo previo al laboratorio: `backup-control-pre-lab-2026-11-21`.
- Integración solo en `lab-yolo-cvat-opencv-hp`.
- No se han entrenado modelos, descargado pesos de terceros ni modificado Android.

## Componentes existentes
- CVAT: previsto para anotar; no instalado ni invocado.
- YOLO: puente `predict_with_yolo(image,custom_weights)` opcional, aún sin pesos especializados entrenados.
- OpenCV: `hp_pipeline.py` mide candidatos suministrados por detector, no establece HP real.
- `dataset_gate.py`: valida clases, cajas, rutas, fuentes de video y casi duplicados entre conjuntos.
- `dataset.yaml`: clases `health_bar_track`, `level_badge`; fondo y negativos como imágenes con etiquetas vacías.
- GitHub Actions `hp-lab-tests.yml`: instalado para ejecutar pruebas en la rama aislada.

## Qué está comprobado y qué NO
- Se ejecutaron localmente 10/10 pruebas del validador en una versión inicial equivalente. Tras integrar una versión simplificada al repositorio, **falta confirmar el estado de GitHub Actions**; no se presenta el CI como aprobado.
- Las 9 pruebas sintéticas anteriores de puente simulado YOLO->OpenCV no prueban un modelo YOLO real.
- El test de datos debe evitar fuga entre videos enteros; un pHash de 64 bits es sólo cribado barato y puede ignorar duplicados severamente transformados. Revisar similitud con un segundo método y auditar videos de origen.
- Una puntuación de prueba alta no basta si sólo se usan imágenes sintéticas o de una misma partida.

## Riesgos encontrados / próximos experimentos
1. **Etiquetado**: CVAT aún no produjo dataset real de barras completas y círculo de nivel. Riesgo de falsos positivos e inconsistencias entre anotadores.
2. **Separación**: con solo dos videos de Yasuo no puede demostrarse generalización a otros campeones, HUDs, ajustes de brillo, dispositivos ni escenas.
3. **Métricas**: precisión, recall, error del porcentaje, seguimiento de identidad, latencia CPU y FPS aún desconocidos.
4. **CVAT vs YOLO**: CVAT es fase humana de preparación y validación, no inferencia simultánea en Android.
5. **Licencia**: Ultralytics AGPL/comercial requiere revisión antes de distribuir producto propietario.

## Prueba de aceptación previa a Android
- Anotaciones auditadas de varios videos independientes, incluyen casos negativos, barras llenas/vacías, rojas/verdes y recortes difíciles.
- Evaluación ciega por fuente de video; métricas de precisión, recall, error de relleno y seguimiento.
- Repetir entrenamientos con varias semillas; reportar promedio y dispersión, peores escenas y falsos positivos, no solo el mejor valor.
- Medir inferencia local y perfiles de memoria; comprobar que el costo de cómputo es viable.
- Sin dataset real ni pesos verificados, el estado seguirá **NO APROBADO**.

## Comprobación de CI
Verificar manualmente ejecución de `HP laboratory tests (isolated)` en GitHub Actions. Hasta ver resultado, el estado debe declararse pendiente.
