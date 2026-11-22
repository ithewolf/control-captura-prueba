# Filtro de calidad preliminar Fiora — 22/11/2026
23 imágenes auditadas en dos vídeos. Zona central del fotograma 22%-78% ancho, 22%-80% alto. Fracción de brillo bajo (V<65) sobre recorte. Umbrales heurísticos: >.80 posible obstrucción; >.35 ambiguo; resto candidato visible.

Resultados: 19 CANDIDATE_VISIBLE, 2 REVIEW_AMBIGUOUS, 2 REVIEW_OCCLUDED.
Alertas:
- source bceb4619cdf9 t=825s: .417 ambiguo
- source d02db8d34cdb t=225s: .966 oscurecido
- source d02db8d34cdb t=525s: .386 ambiguo
- source d02db8d34cdb t=750s: .908 oscurecido

Limitaciones: oscuridad de escena, muerte, scoreboard y efectos pueden producir resultados similares; NO es clasificación semántica ni detector de habilidades/campeones. Revisión humana requerida para las 23 imágenes. No se entrenó ni evaluó YOLO. 5/5 tests sintéticos locales superados; CI de GitHub no verificado.
