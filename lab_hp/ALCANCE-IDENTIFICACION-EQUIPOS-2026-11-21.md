# Decisión de alcance — Identificar aliados y enemigos (21/11/2026)

## Requisito confirmado por Wolf
CONTROL **NO necesita estimar el porcentaje de HP de un enemigo**. El jugador se fija en la barra de vida. Prioridad visual: distinguir con fiabilidad **enemigo** versus **aliado** para sostener el análisis táctico. No asumir que detección de una barra equivale a identidad del personaje.

## Consecuencias de arquitectura
- Reducir prioridad del prototipo `measure_fill()`; conservarlo aislado como experimento previo sin incorporarlo a Android ni convertirlo en requisito.
- En datos y evaluación futuros, diferenciar: detección de personajes, clasificación aliado/enemigo, asociación personaje-indicador si aplica y estado `unknown` cuando haya incertidumbre.
- No inferir equipo únicamente por color de barra sin pruebas: HUD, colores de equipo, efectos, skins, espectador, accesibilidad y oclusión pueden alterar la señal.
- Preferir señales múltiples, siempre verificadas: ubicación visual, marcadores de equipo, indicadores sobre unidad, minimapa cuando es visible y reglas confirmadas en Wild Rift; evitar inferir por color de ropa o campeón (el mismo campeón puede estar en ambos equipos).
- La métrica central será precisión/recall de detección y tasa de confusión aliado-enemigo; **falso positivo de enemigo** y **falso negativo de amenaza** deben medirse separadamente. Reportar cobertura de abstención `unknown` en paralelo; no maquillar la precisión evitando todos los casos difíciles.
- Conservar entrenamiento y validación por partidos/vídeos independientes. Evaluar grandes peleas, campeones solapados, minions, torres, monstruos neutrales, cambios de resolución y UI.
- Requisitos de consumo térmico intactos: inferencia adaptativa, colas sin acumulación, pruebas A/B largas en Galaxy; nunca afirmar consumo menor hasta medirlo.
- Ninguna nueva implementación móvil está aprobada; el respaldo `backup-control-pre-lab-2026-11-21` no cambia.

## Diseño de etiquetas candidato (pendiente de muestras reales)
- `ally_champion`
- `enemy_champion`
- `unknown_champion` o ejemplos difíciles a revisar (no forzar clase si la identidad real es desconocida)
- `neutral_object` si es necesario como categoría para evitar confusiones.
El esquema de clases final dependerá de imágenes verificadas de Wild Rift y del objetivo concreto; no transformar el dataset `health_bar_track` ya existente sin migración y revalidación explícitas.

## Auditoría de este cambio
**Requisito registrado; modelo aún no entrenado ni probado.** Este documento redefine prioridad y criterios futuros, no demuestra identificación en tiempo real.
