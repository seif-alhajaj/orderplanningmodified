# ========== SCRIPT 8: docker-restart.sh ==========
#!/bin/bash
echo "🔄 RESTART DOCKER"
echo "================="

# Service à redémarrer (backend, frontend, database, ou all)
SERVICE=${1:-all}

if [ "$SERVICE" = "all" ]; then
    echo "Redémarrage de tous les services..."
    docker-compose restart
else
    echo "Redémarrage du service: $SERVICE"
    docker-compose restart $SERVICE
fi

echo "✅ Redémarrage terminé"
