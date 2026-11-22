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
