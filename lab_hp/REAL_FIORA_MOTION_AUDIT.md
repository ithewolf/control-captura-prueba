# Fiora — auditoría óptica exploratoria

Muestras reales de vídeos cargados por el usuario; fotogramas separados por 0.35 s, procesados con OpenCV Farneback en recorte central 320x180. El algoritmo **no reconoce campeones ni habilidades**; el flujo puede representar cámara, partículas, súbditos o jugadores.

| Vídeo | Instante (s) | Mediana flujo px | P90 px | Fracción >1 px |
|---|---:|---:|---:|---:|
| Fiora vs Sett | 60 | 3.228 | 10.240 | 0.717 |
| Fiora vs Sett | 204 | 1.454 | 2.177 | 0.859 |
| Fiora vs Sett | 636 | 1.626 | 5.405 | 0.586 |
| Fiora vs Sett | 780 | 4.099 | 5.100 | 0.932 |
| Fiora vs Syndra | 60 | 5.124 | 9.734 | 0.945 |
| Fiora vs Syndra | 388 | 4.962 | 7.438 | 0.911 |
| Fiora vs Syndra | 608 | 1.153 | 3.984 | 0.525 |
| Fiora vs Syndra | 827 | 4.820 | 6.027 | 0.989 |

Resultados: 8/8 pares decodificados y medidos; **0 escenas verificadas como detección de habilidad**. No confundir tasa de procesamiento con precisión. 

Pruebas sintéticas del detector de movimiento: 5/5 en laboratorio local. Junto con 10/10 de temporal_tactics.py: 15/15. El workflow remoto GitHub Actions no está confirmado. Próximos pasos: anotación manual y dataset, exclusión/corrección del movimiento de cámara, tracking de campeones, validación ciega y pruebas de rendimiento Android.

Sin permisos para publicar los vídeos, no se suben los archivos originales.
