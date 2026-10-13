#!/usr/bin/env python3
from pathlib import Path
import xml.etree.ElementTree as ET
import re
root=Path(__file__).resolve().parent.parent
controller=(root/'tracking-rutas-api/api/src/main/java/com/udea/demo/rutas/infrastructure/controller/PanelConductorController.java').read_text()
expect=['@GetMapping("/ruta")','@GetMapping("/catalogo-novedades")','@PostMapping("/entregas/{pedidoId}/resultado")','@PostMapping("/entregas/sincronizacion")']
assert all(x in controller for x in expect),"Falta alguna ruta del conductor"
frontend=(root/'tracking-logistico-frontend/src/lib/api.ts').read_text()
for path in ['/conductor/panel/ruta','/conductor/panel/catalogo-novedades','/conductor/panel/entregas/${pedidoId}/resultado','/conductor/panel/entregas/sincronizacion']:
    assert path in frontend,path
for module in ('identidad','envios','rutas'):
    assert (root/f'tracking-{module}-api/core/src/main/resources/db/migration/V21__eventos_resultado_entrega.sql').is_file()
for pom in root.glob('tracking-*/**/pom.xml'):
    ET.parse(pom)
assert (root/'render.yaml').is_file()
assert (root/'tracking-orquestador-api/.env').is_file()
print('OK: 4 rutas, frontend, SQL V21, XML Maven, render.yaml y .env')
