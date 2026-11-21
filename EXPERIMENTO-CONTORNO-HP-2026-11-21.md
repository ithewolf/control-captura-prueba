# CONTROL Coach — intento de localizar contorno de vida (21/11/2026)

## Implementación
Herramienta experimental `tools/hp_track_locator.py` para buscar líneas verdes y prolongarlas hacia zonas oscuras. Es un **localizador de candidatos**, no reconocimiento de vida; sus salidas declaran `health_percent=null` y `game_state=UNKNOWN`.

## Resultados
- Prototipo local ensayado con siete casos sintéticos: 7/7 aprobaron. En el archivo versionado se incluyen seis pruebas análogas; **la batería versionada todavía no se ejecutó en CI**.
- Cuatro imágenes reales proporcionadas por Wolf: 0, 1, 1 y 0 candidatos respectivamente (en el orden adjuntado).
- Uno de los candidatos extendió artificialmente la barra hasta 120 píxeles al confundir fondo oscuro con hueco del contorno; **no es una medida de porcentaje de vida**.
- No hay pares cronológicos de una misma barra en estas cuatro capturas, por lo cual es imposible verificar aquí una reducción de HP.

## Problemas surgidos
1. **Barras enemigas rojas** y, según estado/campeón, barras amarillas u otras: buscar solamente verde falla por diseño.
2. **Contorno vacío vs escenario oscuro**: no hay garantía geométrica de que la prolongación oscura sea parte de la barra; falla con fondos complejos.
3. HUD, barras de personajes y monstruos pueden parecerse. Falta rastrear identidad/nivel/círculo y ubicación.
4. Transformar posiciones al variar resolución, compresión y orientación requiere pruebas independientes.
5. Una barra llena puede no presentar extensión oscura, así que el prototipo la omite deliberadamente.

## Próxima solución
Dejar de inferir el tamaño total a partir del primer píxel oscuro. Detectar **bordes opuestos del marco**, estimar anchura total con una plantilla y relación nivel+barra secundaria, luego rastrear el mismo rectángulo entre fotogramas. Clasificar verde/rojo/amarillo sin convertir automáticamente el color en equipo; evaluar anotaciones fuera de muestra.

**Estado**: sin integración en Android, sin validación de precisión, sin APK nueva.
