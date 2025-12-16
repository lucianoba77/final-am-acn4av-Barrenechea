#!/bin/bash
# Script para obtener logs de la app relacionados con Botiquín

echo "Obteniendo logs de BotiquinActivity y FirebaseService..."
echo "Por favor, abre la app y ve a 'Mi Botiquín', luego presiona Ctrl+C para detener"

adb logcat -c  # Limpiar logs anteriores
adb logcat | grep -E "(BotiquinActivity|FirebaseService|obtenerMedicamentos|separarMedicamentos|mapToMedicamento)" > botiquin_logs.txt

echo "Logs guardados en botiquin_logs.txt"
