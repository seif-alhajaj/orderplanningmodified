# ========== SCRIPT 3: docker-stop.sh ==========
#!/bin/bash
echo "🛑 ARRÊT DOCKER"
echo "==============="

# Arrêter tous les services
docker-compose down

# Optionnel: Supprimer les volumes (données perdues)
# docker-compose down -v

echo "✅ Services arrêtés"
