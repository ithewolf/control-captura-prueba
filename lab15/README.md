# CONTROL Lab15 — challenge adversarial; aceptar un resultado VERDADERO, no solo verde

Wolf pidio 'presionarlo mas': se agregan catorce pruebas adversariales de evaluacion, incluyendo un **control negativo** basado en una observacion del video de Yasuo: la ventana 0–6 s contiene una pantalla/perfil o transicion, no una pelea verificada. Los fotogramas originales NO se incluyen en GitHub.

## Evidencia y limite
- La observacion de ventana sin gameplay es una revision visual preliminar humana, **no** un dataset exhaustivo.
- El control positivo de TEAM_FIGHT en 38–44 segundos dentro de los tests es **artificial**. Sirve solo para probar la evaluacion. NO afirmar que el usuario o un modelo haya etiquetado esa escena real.
- Lab12 midio movimiento visual; no genero predicciones de campeones/estrategias.
- **Aceptacion real = BLOCKED / NOT_MEASURED** hasta disponer de procedencia verificada de Wild Rift, anotaciones humanas independientes y predicciones de un detector real.

## Reglas
1. Un falso combate sobre una pantalla no-jugable se informa como `FALSE_GAMEPLAY_TRIGGER_ON_NON_GAMEPLAY`.
2. Ninguna exactitud sin etiquetas revisadas y predicciones separadas; exigir `model_id` y confianza valida.
3. Los fragmentos sin revision se reportan, no se convierten automaticamente en FP o TP.
4. Rechazar duplicados, intervalos incoherentes y la mezcla de resultados de detectores.
5. Evaluar solo intervalos revisados, incluyendo controles negativos y positivos.
6. No autorizar APK ni afirmacion de aprendizaje de Wild Rift con estas pruebas.

## Meta siguiente
Conseguir una prediccion real del detector sobre clips etiquetados, medir precision/recall y falsos positivos de pantallas de perfil. Despues evaluar si las recomendaciones A/B/C se justifican con estados verificados. Los tests de Lab15 solo comprueban el contrato de auditoria.
