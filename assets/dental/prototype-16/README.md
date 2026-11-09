# DentIA · Primera pieza dental

Prototipo 0.1.1 del primer molar superior derecho permanente, identificado como **FDI 16**.

## Archivos

- `molar_16.blend`: original editable con corona y raíces separadas, luces y tres cámaras de presentación.
- `molar_16.glb`: exportación detallada, sin cámaras, luces ni fondo.
- `molar_16_web.glb`: exportación reducida para probar en Three.js.
- `molar_16_preview.png`, `molar_16_occlusal.png`, `molar_16_lateral.png`: vistas renderizadas con Blender.
- `build_molar.py`: generador original reproducible de la geometría y materiales.
- `validation.json`: controles de volumen y cierre de las mallas fuente.
- `verify_exports.py` y `export_validation.json`: verificación de la estructura GLB y reimportación.

## Organización del modelo

El objeto padre lleva `fdi=16`, `dentition=permanent`, `arch=maxillary`, `tooth_type=first_molar` y `approximate=true` en sus propiedades personalizadas, exportadas como `extras` de glTF.

Sus hijos son `FDI_16_Corona` (`part=crown`) y `FDI_16_Raices` (`part=roots`). Las tres raíces forman una sola malla. El origen está en el centro cervical, adecuado para posicionar y rotar el diente completo; corona y conjunto radicular se pueden ocultar independientemente.

Las coordenadas del archivo están en **metros**, y Blender muestra milímetros. El GLB utiliza el eje vertical Y conforme a glTF; Blender trabaja con Z vertical. La pieza tiene unos 21 mm de altura de diseño. Estas medidas son elegidas para el prototipo, no medidas de una radiografía.

## Procedencia y alcance

Toda la geometría y los materiales se generan localmente mediante el script de este directorio. No se descargaron ni incorporaron modelos, texturas o fotografías de terceros. No contiene información de pacientes.

Referencia descriptiva de anatomía para cuatro cúspides principales y tres raíces: [Tooth Morphology – Permanent Posterior Teeth, Oral Facial Anatomy Online](https://ecampusontario.pressbooks.pub/oralfacialonline/chapter/tooth-morphology-part-a/). La referencia se consultó como información anatómica; sus ilustraciones no forman parte del modelo.

Es una pieza base ilustrativa para el odontograma aproximado propuesto. Falta la revisión del odontólogo y crear las demás piezas permanentes y temporales. Este prototipo no incluye detección de dientes ni reconstrucción desde radiografías.

## Regenerar

En PowerShell, desde este directorio:

```powershell
& 'C:\Program Files\Blender Foundation\Blender 5.1\blender.exe' --background --factory-startup --python .\build_molar.py
& 'C:\Program Files\Blender Foundation\Blender 5.1\blender.exe' --background --factory-startup --python .\build_molar.py -- --render
& 'C:\Program Files\Blender Foundation\Blender 5.1\blender.exe' --background --factory-startup --python .\verify_exports.py
```

Regenerar actualiza los archivos de salida del prototipo. Si editas manualmente el `.blend`, guarda esa versión con otro nombre antes de regenerar.
