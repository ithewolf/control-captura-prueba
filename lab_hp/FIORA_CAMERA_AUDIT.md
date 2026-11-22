# Auditoría de cámara — Fiora (22/11/2026)

Ensayo local de OpenCV Lucas-Kanade + RANSAC en 6 pares de fotogramas de dos archivos originales; separación de 0.35 s. Estimación aproximada de transformación global de escena en píxeles del vídeo; NO reconocimiento ni precisión validada de cámara.

| Video | t (s) | estado | dx (px) | dy (px) | fracción inliers |
|---|---:|---|---:|---:|---:|
| Fiora vs Sett | 60 | MEASURED | -15.98 | 36.15 | .674 |
| Fiora vs Sett | 204 | MEASURED | -3.80 | 4.92 | .745 |
| Fiora vs Sett | 388 | UNKNOWN | — | — | .399 |
| Fiora vs Syndra | 60 | UNKNOWN | — | — | .527 |
| Fiora vs Syndra | 204 | MEASURED | -12.23 | 3.31 | .839 |
| Fiora vs Syndra | 388 | UNKNOWN | — | — | .307 |

3/6 resultados publicados como estimaciones, 3/6 se abstuvieron. No hay verdad terreno para juzgar exactitud; movimiento de unidades y efectos puede contaminar la estimación. Esto no representa una tasa de acierto de campeones o habilidades.

Pruebas unitarias locales adicionales: 11/11 del prototipo Python; los archivos guardados en GitHub contienen pruebas equivalentes de cámara/perfiles (10 casos). GitHub Actions remoto no comprobado. Aplicación Android NO modificada.
