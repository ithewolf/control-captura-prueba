# Lab13 — informe tactico detallado con evidencias (NO APK)

## Objetivo
Wolf exige una devolucion mas detallada: **que vio CONTROL, con que confianza,
que descarto, que ignora, por que recomienda A, alternativa B y contingencia C**.
El informe incluye observaciones individuales, modelo/procedencia, confirmacion temporal,
eventos candidatos, estado observado, motivos y limites de validez.

## Comportamiento obligatorio
- No transformar movimiento de fotogramas en 'pelea' o 'emboscada'.
- No identificar campeones por el titulo del video ni atribuir detecciones a modelos inexistentes.
- Sin detector y anotaciones humanas independientes, precision, recall y acierto tactico = NOT_MEASURED.
- Sin evidencias fiables, `can_recommend=false`, `plan_a=REASSESS`.
- Los planes son **sugerencias**, nunca entradas automaticas al juego; persistencia limitada a la partida.
- No narrar incertidumbres en voz constantemente: mostrar detalles por escrito; sintetizar voz solo cuando haya urgencia y datos verificados.

## Primer caso de video real
El fotograma de Yasuo ~40 s ya paso por el detector *de cambios de imagen* Lab12,
pero NO por un clasificador de campeones ni de estrategia. El JSON
`lab13/yasuo_a_40s_motion_only.json` conserva esa limitacion.
Lab13 produce un informe honesto, SIN inventar pelea o un plan de ataque.

## Ejecucion
```bash
python3 -m unittest tools.test_lab13_detailed_report -v
python3 tools/lab13_detailed_report.py --input lab13/yasuo_a_40s_motion_only.json --out /tmp/control_lab13_report.json
```

## Siguiente barrera
Se necesita un detector visual independiente que entregue campeones y eventos
con ventanas de tiempo; anotaciones humanas ciegas; comparacion en videos retenidos.
Revisar por separado la procedencia del gameplay. El CI usa un caso de procedencia
real pero SIN afirmaciones de reconocimiento; no monta video, no invoca Android y NO genera APK.
