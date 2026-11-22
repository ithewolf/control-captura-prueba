# LAB 6 — Hito prioritario: modelo conversacional REAL en Android

Decisión del usuario (22-11-2026): la prioridad de CONTROL Coach es **conectar un modelo de lenguaje real dentro de la APK**. No expandir más el catálogo `LocalIntent` de frases predefinidas.

## Estado verificable
- Respaldo intacto: `backup-lab4-2026-11-22` (commit `980bb97942406ebf8a39831b84a211c58fee1aa0`).
- Rama experimental: `lab6-control-cerebro-local`.
- `ConversationCore.java` conserva seis intercambios por modo, genera el prompt de CONTROL y admite un motor inyectado mediante `LocalTextModel`; **no incluye todavía inferencia real**.
- La voz/Piper y captura de Lab4 NO se modifican hasta aprobar una prueba por texto.

## Motor y modelo (candidatos, NO integrados)
1. **llama.cpp**, integración Android nativa JNI de su proyecto oficial: https://github.com/ggml-org/llama.cpp/blob/master/docs/android.md
2. Primer ensayo con un **GGUF instruct pequeño**, como Qwen3 0.6B cuantizado para validar carga, generación y consumo. Su calidad de conversación puede ser insuficiente: comparar con 1.5–1.7B después de medir.
3. Alternativa: **LiteRT-LM** de Google con modelo en formato `.litertlm`, si JNI aumenta demasiado la complejidad. https://developers.google.com/edge/litert-lm/android

No copiar código privado de ChatGPT, Gemini ni Meta AI de WhatsApp. No añadir APIs ni servicios de cobro.

## Integración por etapas (separadas de capturas/voz)
1. Implementar el puente de inferencia nativa que reciba la lista de `ConversationCore.Message` y genere texto real. Configuración inicial conservadora: CPU arm64, contexto 2048 tokens, salida limitada a 120 tokens, un único modelo/corrida a la vez; medir consumo. No ejecutar inferencia en el hilo principal de Android.
2. Añadir en la app una prueba **solo texto**: importar GGUF al almacenamiento privado, mostrar modelo listo/error, enviar pregunta y mostrar tiempo de respuesta. La importación debe comprobar espacio y tamaño, e impedir rutas externas no verificadas. Ninguna descarga de cientos de MB al compilar o abrir la app sin acción explícita.
3. Probar diez preguntas abiertas nuevas y una repregunta de contexto, sin usar `LocalIntent`. Rechazar como fallo cualquier respuesta enlatada, vacía o engañosa.
4. Conectar Vosk → modelo → TTS con estados `LISTENING / THINKING / SPEAKING`, pausa de micrófono durante TTS, límite de espera y recuperación controlada. Solo usar respuesta predefinida de diagnóstico cuando **no** haya modelo real disponible; explicarlo en pantalla.
5. Verificar en el Samsung S24 Ultra: calidad de español, RAM, CPU, temperatura, velocidad y convivencia con Wild Rift y MediaProjection. Esta etapa es una condición de aceptación y no está aprobada todavía.
6. Mantener modo Coach y Compañero separados; no inventar lecturas del juego antes de validar la visión.

## Criterio de éxito
- APK compilada y modelo realmente cargado dentro del teléfono.
- Sin internet durante la inferencia, sin PC activa ni servicio de pago.
- Diez preguntas nuevas respondidas con contenido generado, y recuerdo de la repregunta.
- Grabación/lectura de pantalla no se degrada, se mide uso de recursos y se conserva opción para desactivar el modelo.

## Precauciones de distribución
Los modelos GGUF NO están en este repositorio. No incluir archivos de centenas de MB en Git. Verificar licencia por modelo, cuantización y fuente antes de distribución. Evitar descarga no autorizada o consumo accidental de servicios pagos.

## Trabajo implementado en Lab6 (22/11/2026)
- `LiteRtTextModel.kt`: conexión de la interfaz `ConversationCore.LocalTextModel` con el motor de inferencia **LiteRT-LM 0.18.0**. Ejecuta `Engine.initialize` y `sendMessage` fuera del hilo principal, en CPU. **Sin verificación todavía de compilación Android ni de inferencia en dispositivo**.
- `Lab6TextActivity.java`: interfaz independiente para importar mediante selector de archivos un modelo `.litertlm` al almacenamiento privado del teléfono, cargarlo localmente, escribir preguntas y medir latencia. No descarga modelos por su cuenta.
- `MainActivity.java`: acceso a la pantalla de prueba; no se modificó el flujo Vosk / captura.
- `app/build.gradle` incorpora LiteRT-LM y Kotlin 2.4.21. El paquete debug usa `com.iawolf.captureprobe.lab6`: puede coexistir con la aplicación de Lab4.
- `.github/workflows/lab6-ia-local.yml`: compilación experimental con `ubuntu-24.04` estándar, tests Java y artefacto APK retenido 3 días; se activa por cambios en la rama Lab6. No equivale a compilación exitosa: mirar GitHub Actions.
- Modelo recomendado para ensayo inicial: `litert-community/Qwen3-0.6B`, archivo `Qwen3-0.6B.litertlm` (no incluido), https://huggingface.co/litert-community/Qwen3-0.6B. Revisar licencia y tamaño antes de descargar.
- El test real de calidad exige 10 preguntas no previstas, una repregunta y métricas en el Samsung. El modelo puede rendir insuficientemente; no afirmar calidad sin medición.

**Importante:** Este laboratorio prueba LLM por texto sin micrófono ni juego. El éxito de build, modelo cargado y respuesta en dispositivo son tres pasos distintos. No dar por resuelta la inteligencia de CONTROL hasta cumplirlos.

