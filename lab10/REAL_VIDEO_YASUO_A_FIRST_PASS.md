# Primer muestreo de video real — Yasuo A (22/11/2026)

**Fuente:** archivo de Biblioteca del usuario, `TOP 1 YASUO SERVER “JarePley”🐲 WILD RIFT.mp4`. No se agrega el video ni los fotogramas a GitHub.

**Verificado mediante ffprobe/FFmpeg:** video 1280x720, 60 FPS, duracion 179.948 s. Se extrajeron ocho muestras a 20, 40, 60, 80, 100, 120, 140 y 160 s; extraction exitosa en el entorno de analisis.

**Observacion humana preliminar:** aparecen escenas de combate, movimientos cerca de torres, oleadas de subditos, peleas con varios campeones y elementos de interfaz/minimapa. A 140-160 s se observan enfrentamientos grupales; estas escenas son candidatas para evaluar alertas de pelea y posicionamiento. **No se han certificado campeones/teams ni estrategias exactas a nivel de fotograma**, ni que CONTROL haya detectado las escenas. El video debe validarse como perteneciente a Wild Rift y no reutilizarse si fuera otro juego o version incompatible.

**Proximo paso:** anotar manualmente eventos concretos (inicio, fin, campeones observados, ataque/retirada/objetivo, evidencia, incertidumbre) y compararlos contra un detector REAL. Separar ejemplos de desarrollo de ejemplos retenidos. Hasta obtener predicciones y ground truth independientes, precision, recall y exactitud tactica = NOT_MEASURED.

**Privacidad y distribucion:** solo metadatos y hallazgos descriptivos en repositorio; las imagenes se conservan fuera de GitHub. No redistribuir video ajeno.
