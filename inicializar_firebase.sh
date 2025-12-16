#!/bin/bash

# Script para inicializar Firebase Functions en el proyecto

echo "🚀 Inicializando Firebase Functions..."
echo ""

# Verificar si Firebase CLI está instalado
if ! command -v firebase &> /dev/null; then
    echo "❌ Firebase CLI no está instalado"
    echo "📦 Instalando Firebase CLI..."
    npm install -g firebase-tools
    if [ $? -ne 0 ]; then
        echo "❌ Error al instalar Firebase CLI"
        echo "💡 Intenta manualmente: npm install -g firebase-tools"
        exit 1
    fi
    echo "✅ Firebase CLI instalado"
else
    echo "✅ Firebase CLI ya está instalado"
    firebase --version
fi

echo ""
echo "🔐 Iniciando sesión en Firebase..."
firebase login

echo ""
echo "📁 Verificando proyectos..."
firebase projects:list

echo ""
echo "⚠️  IMPORTANTE: Selecciona tu proyecto con:"
echo "   firebase use <TU_PROJECT_ID>"
echo ""
echo "📦 Instalando dependencias de functions..."
cd functions
npm install
cd ..

echo ""
echo "✅ Inicialización completada!"
echo ""
echo "📝 Próximos pasos:"
echo "   1. firebase use <TU_PROJECT_ID>"
echo "   2. firebase functions:config:set google.client_id=\"...\""
echo "   3. firebase functions:config:set google.client_secret=\"...\""
echo "   4. firebase deploy --only functions"
echo ""
echo "📖 Lee DESPLIEGUE_PASO_A_PASO.md para más detalles"

