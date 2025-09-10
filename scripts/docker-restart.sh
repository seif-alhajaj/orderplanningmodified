# ========== SCRIPT 8: docker-restart.sh ==========
#!/bin/bash
echo " RESTART DOCKER"
echo "================="

# Service to restart(backend, frontend, database, ou all)
SERVICE=${1:-all}

if [ "$SERVICE" = "all" ]; then
    echo "Restarting all services..."
    docker-compose restart
else
    echo "Restarting the service: $SERVICE"
    docker-compose restart $SERVICE
fi

echo " Reboot complete"
