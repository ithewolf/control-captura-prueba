# CONTROL Coach — experimento espacial HUD, 21-11-2026

## Objetivo
Reducir falsas detecciones de controles de jugador en escenas de espectador.
Este no es un clasificador táctico ni un modelo con memoria.

## Datos
66 fotogramas guardados de dos videos del usuario:
- Player HUD: 30 imágenes espaciadas del video largo
- Espectador/repetición: 36 imágenes espaciadas del video corto

## Cambio
Antes: HoughCircles en región (x>65%, y>55%), candidato si >=10 círculos.
Ahora: mantener >=10, pero exigir además >=4 círculos en subregión
(x>72%, y>68%), buscando agrupación característica de controles.
Código: tools/hud_circle_probe.py.

## Resultado reproducido
| Tipo de muestra | Antes | Después |
| --- | --- | --- |
| HUD del jugador (30 imágenes) | 29/30 | 29/30 |
| Espectador/repetición (36 imágenes) | 5/36 posibles falsos positivos | 3/36 posibles falsos positivos |

**Advertencia crítica:** El criterio fue ajustado sobre estas mismas imágenes.
Por ello las cifras son de desarrollo, **NO** un benchmark independiente.
La muestra puede incluir pantallas superpuestas y escenas con distinta
visibilidad. La clasificación de referencia debe verificarse fotograma a
fotograma, sin etiquetar automáticamente todo un video como una sola clase.

## Próximos pasos
- Reservar fragmentos nuevos del video largo y de otros partidos, sin calibrar con ellos.
- Etiquetar separadamente HUD, menús, perspectiva y escena.
- Evaluar precisión y sensibilidad en reservas sin duplicados perceptuales.
- Diseñar detector de regiones (minimapa, barras HP y habilidades) con
  incertidumbre explícita antes de integrar a la app.
- No llamar a estos resultados 'comprensión táctica' ni emitir recomendaciones
  de combate basadas solo en un conjunto de círculos.

**Estado:** herramienta offline investigativa modificada y repetida; Android
y cerebro semántico aún pendientes. No se produjo APK.
