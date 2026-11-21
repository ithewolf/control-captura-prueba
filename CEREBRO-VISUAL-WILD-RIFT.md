# CONTROL Coach — cerebro visual local: arquitectura verificable

## Objetivo
Convertir fotogramas del HUD real de Wild Rift en un estado de juego con evidencias y grado de confianza, y responder por voz solamente usando datos fiables. **No es lo mismo detectar cambios de píxeles que entender el juego**.

## Módulos propuestos
1. Captura MediaProjection / ImageReader (existente): un frame con reloj monotónico y sesión.
2. Preprocesamiento local: detección de orientación, escalado conservando aspecto y recortes de HUD.
3. Visión específica de Wild Rift (PENDIENTE): comenzar por indicadores del HUD (vida, minimapa, enfriamientos, muerte/reaparición). Evaluar OCR y/o detectores TFLite/LiteRT personalizados, no suponer que un detector COCO entiende el juego.
4. Estado temporal: combinar varias observaciones, rechazar contradicciones, timestamp y confianza; `unknown` si no hay evidencia reciente.
5. Audio local Vosk: activar por palabra CONTROL, usar marca de tiempo de la frase y emparejar último estado reciente.
6. Motor de decisión táctico: reglas auditables según rol, posición y riesgos *solo tras validar cada campo*. Si no hay evidencia: pedir contexto, nunca inventar enemigos u objetivos.
7. Síntesis local TTS: pausar STT durante reproducción, reanudar después, evitar frases repetidas y avisos excesivos.

## Próximas pruebas antes de una APK que se denomine Coach
- Dataset de frames del **HUD del jugador** con situaciones etiquetadas, no solo vídeos de espectador (el video Yasuo sigue siendo referencia de diagnóstico).
- Métricas: precisión/recall por indicador, latencia por frame, RAM/CPU y consumo de batería en el Samsung S24 Ultra.
- Test adversarial: pantalla antigua, muerte, menú, mapa diferente, partida pausada, orientación, chat/Discord, auriculares.
- Test de extremo a extremo: «CONTROL, ¿puedo entrar?» con un HUD no reconocido => decir que no dispone de contexto táctico; con HUD validado => usar solamente indicadores comprobados.

## Investigación
- MediaPipe Tasks Object Detector admite modelos personalizados en el dispositivo: https://developers.google.com/edge/mediapipe/solutions/vision/object_detector
- TensorFlow Lite Android custom object detector: https://developers.google.com/codelabs/tflite-object-detection-android
- ML Kit custom object detector with bundled model: https://developers.google.com/ml-kit/vision/object-detection/custom-models/android

## Estado real al 21/11/2026
Captura: código disponible, pruebas históricas en Samsung; integración v25 en curso.
Vosk: integración de código preparada, sin compilación ni prueba en dispositivo.
Contexto temporal audio-visión: lógica de código incorporada, pruebas nuevas todavía no ejecutadas.
Inteligencia táctica: propuesta, **NO implementada ni validada**.
No generar APK final hasta pasar compilar + pruebas de A/V + reconocer HUD.
