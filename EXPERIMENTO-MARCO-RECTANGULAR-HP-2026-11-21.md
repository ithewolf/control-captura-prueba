# CONTROL Coach — bordes rectangulares, 21 noviembre 2026

Nuevo ensayo `tools/hp_frame_edges.py`: busca contornos rectangulares cerrados con traza de verde o rojo. Nunca prolonga una barra por el mero hecho de encontrar un fondo oscuro y nunca entrega porcentajes de salud. El resultado es `rectangular_bar_candidate`, con `hp_percent = None`.

**Pruebas:** 8/8 unitarias sintéticas aprobadas localmente, incluyendo barra verde/roja, vida aparentemente llena, relleno pequeño, terreno con franja sin marco, sin relleno y geometría inválida.

**Cuatro imágenes aportadas por el usuario:** el algoritmo devuelve 3, 2, 0 y 0 candidatos (según fecha ascendente de archivo las cantidades son 3, 2, 0, 0 para 1795300650851, 1795300661683, 1795300696256, 1795300707543, respectivamente). Algunos candidatos son iconos de HUD, no barras de personajes. No existen anotaciones suficientes para calcular precisión o recall. No es una evaluación independiente: el método se implementó viendo las referencias.

**Problemas nuevos:** colores rojos también son UI; los marcos no siempre son bordes blancos definidos; efectos y compresión borran contornos; nivel circular, barra azul secundaria y personaje debajo deben actuar como señales, no certezas. Algunos positivos en la zona superior son elementos de interfaz, por lo que hay que clasificarlos por contexto.

**Siguiente hito:** anotar manualmente cajas reales y falsos positivos; condicionar detección a círculo de nivel/posición y continuidad del mismo objeto en secuencia; solo entonces medir el llenado interior de un marco confirmado. Código offline, no integrado en Android ni generador de APK.
