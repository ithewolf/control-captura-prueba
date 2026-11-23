# LAB20 — Diagnóstico de captura y visión para CONTROL Coach

Fecha: 2026-11-23
Base: lab19-capture-contract-no-apk-20261123
Estado: investigación y planificación. NO compilamos una APK ni verificamos captura en dispositivo.

## Decisión del usuario
Existe una APK de diagnóstico anterior que el usuario decidió no probar. No reemplazarla ni generar APK nueva hasta constatar por pruebas de laboratorio que la siguiente proporciona diagnóstico útil. Conservar la prioridad ya aprobada: **captura real de pantalla Android primero, reconocimiento visual después**.

## Arquitectura y contrato
1. Android MediaProjection con consentimiento del usuario → Surface/VirtualDisplay → ImageReader.
2. Entregar frames con timestamp monotónico, tamaño, orientación, pixel/row stride y estado de recepción; asegurar cierre de Image.
3. Instrumentar: frame_count, last_frame_age_ms, black_frame_ratio, processing_latency_ms, callback/revoke, resize/recreation, dropped_frames.
4. El diagnóstico debe distinguir permiso otorgado, sesión iniciada, frames recibidos, frames interpretables, detecciones realizadas y recomendaciones generadas.
5. Permitir un detector VISUAL de prueba solamente cuando se hayan verificado frames reales; procesar en dispositivo, sin envío de imagen a servidores.

## Ideas reutilizables (solo algoritmos genéricos)
- L2 CV Bot como referencia de lectura visual y regiones estables de HUD. NO incorporar assets, datos, términos ni UI de Lineage 2.
- Regiones proporcionales y calibración por orientación/resolución.
- Estimación de barras HP/recursos con salida numérica más confianza, sin inventar certeza.
- Comprobar frames obsoletos o vacíos; no emitir recomendaciones a partir de ellos.
- En la app CONTROL Coach no integrar ningún bot ni control automático del personaje.

## Pruebas previas a APK
A. Verificar estado del proyecto Android y permisos de MediaProjection, foreground service y callback en Android 14+.
B. Tests unitarios de conversión ImageReader a bitmap: row/pixel stride, recorte, rotación, recursos y excepción.
C. Fixture sintético: regiones HUD, barras parcialmente llenas, cambio de escala y confianza.
D. Test de diagnóstico: sin permiso, sesión detenida, frames vacíos, tamaño cambiado, frames válidos.
E. Registrar resultados del ensayo y revisar riesgos de rendimiento, privacidad y batería.
F. Solo después: valorar una APK de diagnóstico con panel que muestre frames recibidos/segundo y última muestra útil.

## Criterio de salida
No marcar la visión 'lista' solo porque el análisis offline de un vídeo funcione. El ensayo final exige recepción real de capturas Android comprobada por el usuario y comportamiento estable durante una partida.

## Fuentes técnicas
- https://developer.android.com/media/grow/media-projection
- https://developer.android.com/reference/android/media/ImageReader
- https://docs.opencv.org/4.x/de/da9/tutorial_template_matching.html
