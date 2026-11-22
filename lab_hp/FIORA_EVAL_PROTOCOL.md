# CONTROL Coach — protocolo Fiora (evaluación visual, 2026-11-21)

## Objetivo
Evaluar con clips de Wild Rift si CONTROL observa la escena, interpreta riesgo y genera recomendaciones útiles. **Este documento NO afirma que el modelo pasó las pruebas.** Los vídeos originales no se suben al repositorio.

## Material fuente
- Fiora vs Sett: duración medida 1128.954 segundos.
- Fiora vs Syndra mid: duración medida 887.327 segundos.
- Fotogramas de evaluación extraídos localmente; las etiquetas aún requieren revisión manual.

## Muestras (segundos desde comienzo del vídeo)
- Sett: 60, 204, 348, 492, 636, 780, 924.
- Syndra: 60, 169, 279, 388, 498, 608, 717, 827.

## Evaluación por muestra
1. Identificación de HUD / ubicación / campeones y peligros **visibles**, con confianza y evidencia.
2. Interpretación: situación de combate, oleada, torre, objetivos, posicionamiento sin inferir lo oculto.
3. Recomendación: una orden vocal breve con fundamento y urgencia proporcional al peligro; permitir «no sé / necesito más fotogramas».
4. Prueba temporal adicional: secuencia de al menos 3 fotogramas y comparación para detectar cambios; un solo frame no valida microdecisiones ni predicción.
5. Pruebas de regresión con Yasuo y Warwick, separando material Wild Rift de cualquier otro juego y sin introducir conocimiento exclusivo de PC.

## Escala propuesta
- Observación (0-2): 0 inventa, 1 parcialmente correcto, 2 verificable.
- Interpretación (0-2): 0 contradice, 1 insuficiente, 2 contextual.
- Consejo (0-2): 0 peligroso/irrelevante, 1 genérico, 2 seguro y accionable.
- Calibración (0-2): 0 falsa certeza, 1 mezcla, 2 reconoce límites.
**Criterio de avance sugerido:** >= 6/8 por escena y 0 errores críticos antes de unir el cerebro a la APK.

## Estado
- Fuentes recibidas y decodificadas correctamente, 15 muestras preparadas localmente.
- Falta etiquetado de verdad-terreno, ejecución real de inferencia en CONTROL, pruebas con video continuo, y validación de captura del dispositivo Android.
- No interpretar el nombre del vídeo («vs Sett» / «vs Syndra») como prueba de detección de campeones.
