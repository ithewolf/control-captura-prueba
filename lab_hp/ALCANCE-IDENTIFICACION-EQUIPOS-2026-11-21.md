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


## Regla funcional CONFIRMADA por Wolf (23:18 UTC, 21/11/2026)
CONTROL es **únicamente un coach observador y preventivo**, no una IA que juegue o ejecute acciones por el usuario. El usuario decide qué atacar y cuándo.

**Identificación mínima deseada:**
- Campeones aliados.
- Campeones enemigos.
- Unidades y objetivos no clasificados como campeones aliados/enemigos: súbditos (minions/creeps), monstruos de jungla y estructuras como torres. Una torre puede pertenecer a un equipo; NO etiquetar toda torre como neutral sin distinguir el bando cuando sea posible. Igual con súbditos: hay súbditos aliados y enemigos. Los monstruos de jungla sí son objetivos neutrales en general. Mantener categoría y afiliación como atributos distintos para evitar errores.

**Comportamiento permitido:**
- Informar presencia y proximidad: «Hay un enemigo cerca», «Cuidado, puede venir un gank», «Hay una torre enemiga adelante», «Hay monstruos neutrales en la zona».
- Explicar riesgos y oportunidades estratégicas con contexto y margen de incertidumbre, sin jugar por el usuario.
- Si no ve con certeza qué objeto/equipo es, responder **desconocido** o callar antes que inventarlo.

**Comportamiento fuera de alcance:**
- Calcular, leer o estimar porcentajes de HP como requisito de identificación.
- Comentarios tipo «Matalo ahora que tiene poca vida», «Rematalo» basados en HP.
- Pulsar botones, mover el personaje, atacar automáticamente ni controlar la partida.
- Ordenar un ataque solo por ver a un enemigo; el coach advierte, no sustituye al jugador.

**Diseño de etiquetas recomendado para futura investigación, NO IMPLEMENTADO:**
- Separar `entity_type` (champion, minion, jungle_monster, turret, other) de `affiliation` (ally, enemy, neutral, unknown).
- Un monstruo de jungla usualmente es neutral; torres y súbditos requieren afiliación aliada/enemiga.
- El prototipo `health_bar_track` y la medición `measure_fill()` son secundarios y no son requisitos funcionales.
- Evaluar precisión y recall para presencia, tipo y afiliación; medir errores graves enemigo/aliado por separado, y la tasa de abstención `unknown`.
- Mantener revisión por videos independientes, prioridad de eficiencia térmica y la prohibición de integración a Android antes de validación real.

**Estado de implementación:** requisito registrado documentalmente. Clasificador de equipos/tipos NO ENTRENADO ni probado. Ningún archivo Android modificado por esta decisión.
