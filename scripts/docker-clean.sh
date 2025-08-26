# ========== SCRIPT 4: docker-clean.sh ==========
#!/bin/bash
echo "🧹 NETTOYAGE DOCKER"
echo "==================="

# Arrêter et supprimer tout
docker-compose down -v --remove-orphans

# Supprimer les images
docker-compose down --rmi all

# Nettoyer le système Docker
docker system prune -f

echo "✅ Nettoyage terminé"
