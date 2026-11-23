# Auditoria tecnica CONTROL Coach — 23/11/2026

## Estado real
- Lab10: bridge detector claim -> confirmacion temporal -> planificador A/B/C; CI aprobado por captura del usuario el 23/11.
- Lab11: score para eventos, **solo pruebas de codigo**, no existe detector independiente de estrategias acreditado.
- Lab12: video real Yasuo A inspeccionado localmente, 90 muestras de movimiento; GitHub CI sintetico aprobado por captura del usuario.
- Lab13: formato de informe tactico auditado, sin reconocimiento automatico de objetos; el estado de su CI no fue acreditado en esta auditoria.
- Lab14: siete ventanas con alto movimiento registradas, pero el segundo 2 incluye pantalla/perfil; cambios visuales NO equivalen a combates.
- Lab15: puertas adversariales de procedencia y control negativo; codigo y workflow ampliados a **20 comprobaciones**. Se requiere verificar corrida de GitHub. Ninguna cifra real de precision/recall certificada.
- **Android captura en vivo:** no demostrada con telefono en la etapa nueva; **APK suspendida**.

## Hallazgos prioritarios
**P0 — no hay detector de eventos real**. Los laboratorios pasan sobre datos suministrados/manuales o videos sinteticos, no clasifican combates, emboscadas, campeones ni retiradas de video por su cuenta. La parte de inferencia es una dependencia real que debe implementarse y evaluar.

**P0 — procedencia de video**. Una vista panoramica o montaje puede diferir del HUD del usuario en Wild Rift; validar juego, version y perspectiva antes de usar datos de entrenamiento/test. No inferir datos por el nombre del archivo.

**P1 — controles negativos**. Una pantalla de perfil o transicion con mucho cambio visual debe ser NO_GAMEPLAY; un falso positivo debe constar expresamente. Agregadas seis pruebas en `tools/test_lab15_extra_safety.py`.

**P1 — reporte de errores**. Debe contar TP/FP/FN de ventanas revisadas, FP en NO_GAMEPLAY por separado, intervalos no revisados, duplicados y fuente/modelo. No dividir por cero ni convertir falta de predicciones en 100% de precision.

**P1 — arquitectura**. El planificador offline no automatiza acciones ni usa pixeles; enlazarlo SOLO con datos de un detector real validado. Los planes A/B/C no deben inducir ataque sobre valores desconocidos.

**P1 — aprendizaje**. Videos tutoriales proporcionan conocimiento candidato, no reentrenamiento automatico sin etiquetas, revision humana, particiones desarrollo/prueba y control de parches.

## Puertas de aceptacion antes de APK
1. Confirmar captura Android continua con frame timestamps, seguridad de permisos y errores visibles en dispositivo.
2. Obtener evidencia de detector real y procedencia/version; etiquetado humano independiente.
3. Medir precision, recall y FP con video retenido; no maquillar muestras o resultados negativos.
4. Encadenar detector -> eventos -> plan A/B/C con cambios de escenario y latencia medidos.
5. Validar voz/microfono por separado; solo entonces decidir si compilar APK.

**Importante:** GitHub CI verde prueba que los tests asociados pasaron, no necesariamente estas puertas funcionales.

## Cambios esta ronda
`tools/test_lab15_extra_safety.py` agrega seis verificaciones adversariales y el workflow `.github/workflows/lab15-adversarial-no-apk.yml` ahora ejecuta 20 tests. Sin Android build.
