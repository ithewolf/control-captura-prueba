# CONTROL Coach — detector experimental de barras verdes (21/11/2026)

## Implementación guardada
- `tools/health_bar_probe.py`: procesa imágenes JPG/PNG localmente con OpenCV, filtra color verde saturado y proporciones horizontales en región central.
- `tests/test_health_bar_probe.py`: seis pruebas unitarias sintéticas superadas (negro, barra verde, vertical, minimapa, rojo, abstención de HP).

## Ejecución sobre videos de Yasuo
- Video largo: 30 fotogramas; 19 con al menos un candidato; 31 candidatos totales.
- Video corto: 36 fotogramas; 15 con al menos un candidato; 27 candidatos totales.
- En `long_012.jpg` el detector marca una barra verde encima de un campeón y otra región cerca del suelo: **evidencia visual de falso positivo**. Por tanto no puede adjudicarse una precisión ni el porcentaje de vida con esta prueba.
- Todos los números son exploratorios; el algoritmo fue construido viendo estas muestras. No son resultados de validación independiente.

## Límites
- No distingue equipo aliado/enemigo, campeón/monstruo, ni cuánto HP tiene.
- No lee la barra total de fondo ni interpreta colores de enemigos.
- No está integrado en Android; no afirma inferencia en tiempo real.
- Este detector se debe mantener aislado del motor de decisiones hasta anotar bounding boxes reales y superar evaluación en escenas reservadas.

## Siguiente experimento
1. Crear etiquetas manuales de barras reales en fotogramas reservados.
2. Añadir contorno de marco, relación vertical con nombre/nivel y seguimiento temporal.
3. Medir precisión, cobertura y latencia por dispositivo.
4. Solo después portar a Java/TFLite e integrarlo con estados `UNKNOWN` cuando no hay evidencia.
