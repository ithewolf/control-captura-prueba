# CONTROL Lab17 — examen táctico 100 preguntas

El usuario requiere: 100 preguntas de selección múltiple; umbral 80/100; solo si aprueba, examen sin opciones.

## Qué mide esta primera fase
El código Java `Exam100.java` hace 100 consultas reales al `TacticalDecisionEngine.evaluate` del proyecto, con cinco opciones fijas: RETREAT, RECOVER, FARM_SAFELY, IDENTIFY_ENEMY, INSUFFICIENT_EVIDENCE. Las respuestas correctas fueron definidas por casos de decisión explícitos en 10 categorías y nunca se suministran al motor. Cada caso registra entrada, respuesta y resultado en JSON Lines. Se ejecuta contra el código Java original por `javac`, fuera de los videos.

**Resultado preliminar local:** 100/100 sobre las 100 variaciones de condiciones definidas por el motor, usando una copia idéntica del archivo Java auditado. Esto valida coherencia con estas reglas, NO inteligencia estratégica general, conocimiento de campeones, timing, macro o improvisación. Repetir en GitHub Actions contra el archivo del repositorio y revisar logs.

**Evitar fuga de respuestas**: son escenarios derivados de las reglas que se auditan; no se presentan como examen ciego independiente. El evaluador comparte el dominio de la política.

## Examen 2 (SIN opciones) — condición gate
Como se cumple 80%, la siguiente prueba debe administrar 20 escenarios tácticos originales a un **motor de generación de lenguaje/razonamiento real**, sin enumerar opciones, recopilar sus explicaciones, anonimizar respuestas y puntuar con rúbrica independiente: decisión, riesgo, evidencia, plan alternativo, límites y cambios de condiciones. No generar respuestas ficticias ni usar cadenas predefinidas del motor como si fueran una respuesta razonada.

**Estado actual de fase 2:** BLOCKED_NO_FREE_FORM_RESPONDER. `TacticalDecisionEngine` solo devuelve enum Action y rationale prefijada. La fase 2 está autorizada, pero no puede administrarse honestamente al 'cerebro' actual hasta conectar su interfaz de texto real. La ausencia de respuestas debe quedar registrada, no contarse como aprobada.

## Repetir examen 100
```bash
javac -d /tmp/control_exam_build \
  app/src/main/java/com/iawolf/captureprobe/TacticalDecisionEngine.java \
  app/src/test/java/com/iawolf/captureprobe/Exam100.java
java -cp /tmp/control_exam_build com.iawolf.captureprobe.Exam100 \
  > exam_100_responses.jsonl
```
La salida estándar incluye 100 respuestas y clave auditable; salida de error publica puntuación agregada; exit 1 si no pasa 80%. NO APK.
