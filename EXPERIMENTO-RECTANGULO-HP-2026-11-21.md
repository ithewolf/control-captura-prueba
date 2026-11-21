# CONTROL Coach: medición exploratoria de rectángulos de vida

Se incorporó `tools/hp_rectangle_probe.py` y su prueba unitaria (10 casos sintéticos OK en ejecución local).

### Aporte del usuario
Cuatro capturas adjuntas de Wild Rift muestran barras horizontales, niveles circulares y barras secundarias. La observación importante es que la fracción verde ocupa una parte de un rectángulo total. El porcentaje exige detectar *ambos extremos del contorno*, no solo el relleno visible.

### Prueba sobre las cuatro capturas
Se tomaron recortes aproximados manuales. La función reconoció rellenos verdes, pero **no debe interpretarse el número como HP real**, porque los recortes pueden excluir espacio vacío del rectángulo. Las capturas son de situaciones diferentes: **no permiten inferir disminución temporal de vida**.

### Algoritmo añadido
- Requiere un rectángulo completo definido externamente y comprueba continuidad horizontal de verde saturado anclado al extremo izquierdo.
- Devuelve `unknown` ante relleno ausente, geometría inválida o manchas verdes aisladas.
- Un comparador temporal solo anuncia `fill_decreasing_candidate` cuando la identidad del mismo personaje está establecida por otro componente y hay dos medidas recientes válidas.
- No detecta automáticamente barras, identidad de personajes, equipos, ni vida verdadera.
- No está integrado a la APK ni al motor táctico.

### Próximos pasos
1. Obtener coordenadas verificadas de los contornos completos de barras y anotarlas en capturas de partidas.
2. Encontrar nivel circular y barra secundaria como señales auxiliares, no obligatorias.
3. Rastrear la misma barra compensando movimientos de cámara y personaje.
4. Comparar frames consecutivos del **mismo combate**, etiquetar y evaluar por separado.
5. Integrar a Android solo tras verificar precisión/latencia. No emitir consejos tácticos basados en colores no validados.
