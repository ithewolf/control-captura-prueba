# LAB 5 — CONTROL · Identidad vocal (aislado)

## Objetivo
Generar palabras y frases nuevas en español conservando, hasta donde lo permita un modelo autorizado, el timbre masculino seleccionado para CONTROL. **No basta con repetir un MP3** y no es correcto afirmar que un modelo Piper tradicional aprenderá automáticamente un nuevo timbre de un MP3.

## Audio original
- La referencia elegida por Wolf es `control(1).mp3`, de unos 17,63 segundos.
- El original NO se sube a este repositorio ni a artefactos públicos de GitHub Actions; permanece local.
- **Antes de usar una voz original de ElevenLabs para adaptación o clonación local**, revisar sus derechos y términos de uso y asegurar que se cuenta con autorización para usar esa identidad vocal.
- Se requiere escuchar una síntesis de frases **que no aparezcan en el MP3** y compararla con el original. El timbre no puede garantizarse idéntico con 17 segundos.

## Motores candidatos
1. OpenVoice V2: conversión de color vocal sobre TTS español, bajo MIT según su documentación. Dos etapas, mayor complejidad Android.
2. XTTS-v2: clonación de voz zero-shot con español; licencia Coqui Public Model License que se debe revisar antes de distribuir.
3. F5-TTS español: candidato condicionado a pesos/permiso específicos y calidad rioplatense.

## Separación de laboratorios
- LAB 4 Piper-Ald: funciona como **motor de voz genérico** y se mantiene intacto.
- LAB 5: estudio de identidad vocal y síntesis con voces autorizadas. No insertar las muestras privadas en APK sin permiso/verificación.
- Ningún motor de clonación está integrado ni se ha demostrado aún en Android.

## Ensayo local inmediato
```bash
python tools/voice_identity_reference.py --reference "/ruta/privada/control(1).mp3" --output build/control-voice-private
```
Requiere `ffmpeg` instalado. El comando crea una versión WAV mono 24 kHz y un perfil técnico, pero **no sintetiza voz**.

## Criterios de aceptación (antes de integrarlo)
- Al menos 5 frases nuevas en español, incluido vocabulario del juego: "Wolf, retrocedé", "Está bien, ve por él", "Prestá atención al minimapa".
- Revisar pronunciación argentina, inteligibilidad, consistencia del timbre, entonación y tiempos de respuesta.
- Sin nube, sin cargos por uso, sin transferir audios originales a repositorios públicos.
- Medición de CPU, RAM, latencia y estabilidad real en el celular. Si no funciona, conservar TTS Android/Piper provisional.

## Implementación candidata 1: OpenVoice V2, totalmente aislada

Basada en el cuaderno oficial `myshell-ai/OpenVoice/demo_part3.ipynb`:
- `MeloTTS` produce **texto nuevo** en español.
- `ToneColorConverter` de OpenVoice V2 intenta convertir el timbre para acercarse a la **referencia autorizada**.
- Los audios generados y el informe se guardan **solo en una carpeta privada** y requieren escucha y aprobación humana antes de cualquier integración.
- No se añade el modelo ni el MP3 a ninguna APK ni al repositorio.

Requisitos de entorno privado: Python 3.9 compatible con OpenVoice, PyTorch, ffmpeg, dependencias oficiales de OpenVoice y MeloTTS, y `checkpoints_v2` descargados del repositorio de MyShell (fuentes oficiales: https://github.com/myshell-ai/OpenVoice y https://huggingface.co/myshell-ai/OpenVoiceV2). **No hay inferencia en el CI público**.

Ejemplo de ejecución después de instalar las dependencias y los pesos oficiales:

```bash
python tools/lab5_openvoice_v2.py --reference "/ruta/privada/control(1).mp3" --checkpoints "/ruta/privada/checkpoints_v2" --out "/ruta/privada/lab5-samples"
```

En Linux o Windows, si `--checkpoints` no contiene los archivos correctos, la prueba aborta antes de cargar la IA. La evaluación real es la comparación de **cinco frases nuevas**, no el MP3 original.

### Puerta de salida hacia la APK

No se propondrá añadir un motor de clonación a CONTROL Coach hasta cumplir:
1. Autorización de uso de la identidad vocal y de la muestra original.
2. Cinco frases nuevas, inteligibles, estables, sin cambio perceptible importante de timbre respecto a la voz elegida, aprobadas por Wolf.
3. Funcionamiento local sin solicitudes de pago ni nube.
4. Motor realmente viable en Android y medido sin bloquear Vosk, captura o juego.
5. APK Lite estable preservada como respaldo.

No se promete identidad vocal perfecta ni que el modelo de escritorio pueda ejecutarse dentro de la APK sin adaptar u optimizar el runtime.
