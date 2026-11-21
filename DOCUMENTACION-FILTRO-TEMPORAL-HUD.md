# Filtro temporal experimental de HUD

## Qué agrega
- Filtro con 3 observaciones consecutivas, validación de contador y confirmación espacial del detector previo.
- Resetea después de brecha superior a 1800 ms o de timestamp repetido/desordenado.
- Los tres estados son: `unknown`, `player_hud_candidate` y `no_player_hud_evidence`.
- **No** presume modo espectador cuando no detecta botones; puede haber menús o HUD oculto.
- **No** analiza campeones, enemigos, vida, habilidades ni decisiones tácticas.
- **No** está integrado en la APK; es una utilidad offline de investigación.

## Uso
`python -m unittest discover -s tests -p 'test_hud_temporal_gate.py' -v`

`hud_temporal_gate.py` consume JSONL con `timestamp_ms` entero y los tres campos de `hud_circle_probe.py`: `lower_right_circles`, `bottom_right_core_circles` y `player_controls_candidate`. Ejemplo:

```json
{"timestamp_ms":1000,"lower_right_circles":13,"bottom_right_core_circles":6,"player_controls_candidate":true}
```

`python tools/hud_temporal_gate.py observations.jsonl`

## Estado verificado
8 pruebas unitarias ejecutadas en entorno Python: todas OK. Son pruebas sintéticas y lógicas, **no** pruebas contra nuevos videos ni Android. La integración futura deberá adjuntar timestamps coherentes a fotogramas y ejecutar el detector de imagen.

## Próxima etapa
Evaluación con anotaciones humanas por fotograma y escenas fuera de muestra; detector de minimapa/HUD regional; después lectura de barras y estado estructurado con abstención por baja confianza.
