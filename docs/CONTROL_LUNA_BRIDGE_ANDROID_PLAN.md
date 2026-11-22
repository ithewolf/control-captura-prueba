# CONTROL Coach — reutilizar aprendizajes de Proyecto Luna sin depender de su PC

Estado: diseño de laboratorio, NO implementado. Fecha: 2026-11-22.

## Evidencia de partida
- CONTROL Lite Android: MediaProjection, Vosk local, Android TTS y reanudacion de escucha se observaron en el telefono. Respuestas de intenciones simples en LocalIntent.java; un LLM no esta integrado.
- Proyecto Luna Windows: Ollama con modelo `luna:latest` derivado de Qwen2.5 3B respondio en Discord por texto. Voz y captura del juego para Luna NO estan integradas de extremo a extremo; memoria persistente pendiente.
- No trasladar la personalidad de Luna, credenciales Discord, memorias privadas, ni datos especificos de Lineage 2 a CONTROL.

## Arquitectura objetivo (100% offline en Android)
MediaProjection -> vision barata/eventos respaldados -> evidencia visual
Mic Android -> Vosk (texto parcial/final) -> activacion/turnos -> NLU o LLM local
Contexto: modo coach/companero + historial corto + evidencia visual validada
Generacion -> filtro de afirmaciones no respaldadas -> TTS local -> retomar escucha

## Dos fases separadas
1. Validar con el codigo actual: permisos, captura, transcripcion, wake word, respuesta, TTS y modo. Reglas locales predecibles, medir latencia, calor, RAM y caidas al abrir Wild Rift. No afirmar reconocimiento de campeones.
2. LAB opcional de LLM Android: comparar LiteRT-LM y llama.cpp Android con modelo GGUF pequeno Qwen3-0.6B (4-bit si resulta compatible). El archivo puede ser varios cientos de MB y la RAM de ejecucion mayor. Ejecutar inferencia SOLO cuando el jugador pregunta o hay evento respaldado, no por fotograma. Con fallback a LocalIntent y limite de memoria/timeout.
3. El modelo Qwen2.5:3b de Luna sirve para probar calidad de respuestas en una PC, NO para incorporarlo sin mediciones al APK: es una clase de carga mucho mayor.

## Politica de contexto y seguridad
- Dos perfiles de prompt distintos: CONTROL coach = breve, tecnico, argentino, reconoce incertidumbre; CONTROL companero = conversacional, humor argentino, sin spam. Ninguno adopta identidad de Luna.
- Un evento visual es una observacion parcial, nunca inventar habilidades, enemigos, HP ni builds sin evidencia.
- Memoria temporal en RAM por sesion; no replicar chats del Discord de Luna; persistencia opt-in en futuro.
- Si LLM se demora o amenaza FPS/bateria/temperatura, desactivarlo y volver a respuestas locales; opcionalmente modo Lite.
- No requerir Ollama/PC encendida, Internet, suscripciones ni API de pago en producto final.

## Pruebas de aceptacion
A. Audio: al decir "CONTROL me escuchas", produce respuesta pertinente y vuelve a escuchar, >= 5 turnos, no bucle eco.
B. Modos: Coach y Companero responden distinto a la misma pregunta; persiste seleccion por sesion.
C. Captura + voz durante Wild Rift: no se corta el servicio; medir FPS del juego (no inferir de callbacks), RAM PSS, temperatura, latencia.
D. Vision: afirmaciones tacticas solo con detector validado; de lo contrario responde "no tengo evidencia visual suficiente".
E. LLM experimental: benchmark offline, inferencia por demanda, sin requerir red, comparar exactitud vs LocalIntent.

## Referencias de implementacion
- https://developers.google.com/edge/litert-lm/android
- https://github.com/ggml-org/llama.cpp/blob/master/docs/android.md
- https://huggingface.co/Qwen/Qwen3-0.6B-GGUF
- https://github.com/ollama/ollama/blob/main/docs/api.md
