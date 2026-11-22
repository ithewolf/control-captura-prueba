# CONTROL Lab 6 — Pruebas de visión (etapa 1)

Fecha: 2026-11-22

## Resultado observado (sin exagerar)

El usuario verificó que el capturador recibe y procesa cuadros de Android al abrir Wild Rift y recorrer menús donde aparecen campeones. Los contadores suben fuera de CONTROL, y la captura corre en segundo plano. **Esto NO demuestra detección automática de campeones, objetos, mapa ni eventos.**

## Cambio experimental preparado

Se agregó al diagnóstico una recolección voluntaria de un máximo de 3 imágenes **horizontales** de hasta 1024 px por lado mayor. Las imágenes se toman espaciadas al menos 4,5 segundos y solo mientras la captura está activa. Se conservan temporalmente en memoria; no se guardan ni se envían automáticamente.

1. Iniciar **Diagnóstico: capturar solo pantalla (sin micrófono)**.
2. En CONTROL elegir **Preparar 3 fotogramas reales del juego** y confirmar.
3. Entrar a Wild Rift y navegar por menú/selección de campeón durante unos 15–20 segundos, luego volver.
4. Comprobar en el diagnóstico **Muestras horizontales: 3/3**.
5. Pulsar **Exportar muestras de juego a ZIP (manual)**. Elegir dónde guardar mediante el selector de Android.
6. Revisar las imágenes del ZIP antes de compartirlo. Pueden aparecer nombres de usuarios y mensajes privados.

Los archivos tienen nombre `frame_01.png`, `frame_02.png`, `frame_03.png` más `fechas.txt` y `LEEME.txt`. La prueba no descarga modelos ni utiliza APIs de pago. El ZIP se genera **solo si el usuario lo solicita**.

**Estado de entrega:** cambio preparado en GitHub, pendiente de compilación y prueba en el Samsung. No asumir que está en la APK previamente instalada.

## Próxima etapa de interpretación

Primero recopilar muestras autorizadas y etiquetadas según evidencia visible (menú, selección, campeón visible, partida). Comparar cuadros horizontales nativos para diseñar detectores y registrar incertidumbre. No confundir cambios de color o HUD con una identificación táctica. Luego, conectar observaciones verificadas al cerebro local, que sigue en laboratorio separado.

CONTROL y Wild Rift son exclusivos de este proyecto. No reutilizar contenido de Honor of Kings.

## Revisión offline del ZIP (nueva herramienta)

Una vez exportadas las imágenes manualmente desde CONTROL:

```bash
python3 tools/review_game_frames.py control-frames-wildrift.zip --out vision-review.json
```

El programa verifica firmas, dimensiones y checksums PNG, limita la cantidad y tamaño de archivos y **no extrae imágenes ni las envía a internet**. Devuelve un manifiesto JSON con hashes, orientación y campos de identificación inicialmente `unverified`. Se rechazan nombres de ZIP inseguros.

**Importante:** el programa **no reconoce campeones, menú o acciones**; esos campos requieren inspeccionar y validar ejemplos visuales de Wild Rift. Tampoco demuestra por sí solo que los PNG provengan del juego.

La compilación de GitHub Actions ejecuta las pruebas Java de selección y las pruebas Python del revisor, sin usar imágenes privadas. La prueba en un teléfono sigue pendiente hasta disponer de la nueva APK. No publicar ZIPs con chats, nombres o datos privados en un repositorio.
