# Auditoría general CONTROL Coach — 21/11/2026

## Fuente inspeccionada y límites
Revisión de árbol real de `control-integracion-av-preapk` (commit `e6fceb72102f58b054c766c7cde78653caaf7760`), rama experimental `lab-yolo-cvat-opencv-hp` (hasta `eaa91ad86f8d93dfd473f33c0c3f58664b240921`), archivos fuente de Android, planes y resultados públicos de GitHub Actions. No se compiló APK en esta auditoría ni se ejecutó la app en teléfono; no hay datos de temperatura o batería.

## Diagnóstico por sistema
1. **Captura Android: prototipo implementado, falta verificación integrada actual.** `CaptureService` usa MediaProjection, ImageReader, `acquireLatestImage()` y cierre de imágenes. Muestreo alrededor de 1,1 fps por `now-lastAt<900`. Previews reducidas a 320px, historial de 10 Bitmap y refresco UI con reconstrucción de miniaturas cada segundo: potencial fuente de asignaciones/reconstrucciones de UI. Falta medición real de CPU/RAM/energía.
2. **Voz: integración de código, no pruebas de hardware concluyentes.** `VoskVoiceBridge` STT español local, pausa al hablar TTS, recupera escucha; dependencia/modelo descargado al compilar en `app/build.gradle`. Modelo puede aumentar APK; no extrapolar tamaño exacto sin compilar. Filtros de ruido/eco intentados condicionalmente. Verificar voz con juego/Discord, permisos y ciclo de vida.
3. **Cerebro táctico: no implementado.** `ControlBrain` y `AvContext` son respuestas de diagnóstico: admiten explícitamente ausencia de reconocimiento de campeones, enemigos, torres y tácticas. Asociar timestamps de audio y frames no implica reconocer contenido.
4. **Percepción visual: experimentos de HUD/HP pero no detector de clases aprobado.** `HudRegionSampler` solo estadísticas de brillo/contraste; `lab_hp` contiene scripts offline de barras HP, no reconocimiento de tipos de unidad y bando. Nueva prioridad confirmada: distinguir campeón aliado/enemigo; súbdito aliado/enemigo; monstruo neutral y torre aliada/enemiga, con salida unknown; sin estimar porcentaje de vida ni jugar.
5. **UX:** `MainActivity` es panel de diagnóstico y controles de captura/voz, no interfaz de entrenador terminada.
6. **Pruebas:** Última ejecución del workflow `HP laboratory tests (isolated)`, GitHub run 44961140250, **success**, commit `eaa91ad...`; valida pruebas unitarias sintéticas del laboratorio, no precisión. En integración, último workflow `CONTROL cerebro - pruebas previas (sin APK)`, run 44958604524, **failure**. Logs: tests Java AvContext y FrameSampler pasaron, la fase Python produjo cuatro ImportError por falta de numpy/cv2. Esto muestra defecto de configuración del CI, no de funcionamiento táctico. No se verificó compilación APK actual ni instalación real v25.
7. **Backup:** `backup-control-pre-lab-2026-11-21` y `control-integracion-av-preapk` apuntan al mismo commit `e6fceb7...`. El laboratorio es otra rama; respaldo conservado, no prueba de restauración.
8. **Térmica/batería:** documento de protocolo A/B, sin mediciones; no hay lógica de thermal headroom ni apagado de inferencia por sobrecalentamiento en Android.
9. **Seguridad/calidad:** abstención `unknown` prevista; falta validar alertas falsas, latencia de voz y advertencias; no hay control autónomo de juego implementado, adecuado para enfoque coach.

## Riesgos concretos y orden de trabajo
P0. **Reparar CI de integración** incluyendo paquetes de pruebas Python (`numpy`/`opencv-python-headless`) y ejecutar de nuevo; comprobar compilación Gradle v25 y APK firmada de prueba.
P0. **Captura + micrófono reales** en Samsung con Wild Rift activo, regreso a app, cierre y permisos; exigir pruebas de sesiones largas.
P0. **Redefinir dataset/modelo según nueva misión**: tipos de entidad + afiliación, anotaciones exhaustivas por partidas independientes, abstención unknown, sin clases HP como prioridad. CVAT es herramienta offline, no parte de APK.
P0. **Detección real** + evaluación ciega de clasificaciones de aliados/enemigos/objetivos; métricas por clase, confusiones graves y tasa de abstención.
P1. **Motor de consejos preventivos** basado solo en hechos confirmados, sin porcentajes de HP, sin control de juego, sin órdenes de remate y sin consejos inventados.
P1. **Optimización** eliminar trabajo duplicado, probar latest-only y bajo FPS, reducir recreación de miniaturas en UI, medir Vosk/TTS y memoria.
P1. **Térmica A/B** 15/30/60 min repetidos en Samsung con registros de FPS, batería y Android thermal status; evitar frecuencias fijas sin medición.
P2. **Interfaz de coach**, tutorial de permisos, robustez recuperación y experiencia del usuario; empaquetado y distribución de beta.

## Estimación de progreso — evaluación de ingeniería, no medición automática
**~30% hacia una beta funcional conforme al nuevo alcance**, con incertidumbre grande (aprox. 20–40%). El porcentaje representa entregables completados/validados y sus riesgos, no líneas de código ni pruebas sintéticas; no debe leerse como que el entrenador ya funciona 30% del tiempo. Una versión comercial estaría sensiblemente más lejos. Prioridad real: cerrar captura+voz en hardware y obtener reconocimiento semántico validado. Hasta entonces no declarar APK coach lista.

## Criterio de avance
Revisión incremental: implementar en laboratorio/ramas protegidas, ejecutar CI verde, añadir evidencia en videos de partidas desconocidas, medir teléfono A/B, mantener respaldo y no prometer rendimiento antes de medir.
