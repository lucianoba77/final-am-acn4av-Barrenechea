#!/bin/bash

# Script para sincronizar el proyecto Android y resolver errores de classpath

echo "🔄 Sincronizando proyecto Android..."

# Detener daemons de Gradle
echo "1. Deteniendo daemons de Gradle..."
./gradlew --stop

# Limpiar proyecto
echo "2. Limpiando proyecto..."
./gradlew clean

# Sincronizar dependencias
echo "3. Sincronizando dependencias..."
./gradlew --refresh-dependencies

# Compilar proyecto para verificar classpath
echo "4. Compilando proyecto para verificar classpath..."
./gradlew :app:compileDebugJavaWithJavac

echo "✅ Sincronización completada!"
echo ""
echo "📝 Próximos pasos en Android Studio/Cursor:"
echo "   1. File > Sync Project with Gradle Files"
echo "   2. File > Invalidate Caches / Restart (si el problema persiste)"
echo "   3. Build > Rebuild Project"




