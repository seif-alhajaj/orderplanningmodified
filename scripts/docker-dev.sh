# ========== SCRIPT 9: docker-dev.sh ==========
#!/bin/bash
echo "🚀 DÉVELOPPEMENT DOCKER"
echo "======================="

# Mode développement avec live reload
docker-compose -f docker-compose.yml -f docker-compose.dev.yml up --build

# ========== INSTRUCTIONS D'UTILISATION ==========
echo ""
echo "📝 UTILISATION DES SCRIPTS:"
echo "============================"
echo "chmod +x docker-*.sh"
echo ""
echo "./docker-build.sh     # Construire et démarrer"
echo "./docker-logs.sh      # Voir les logs"
echo "./docker-status.sh    # Vérifier le statut"
echo "./docker-restart.sh   # Redémarrer"
echo "./docker-stop.sh      # Arrêter"
echo "./docker-clean.sh     # Nettoyer complètement"
echo "./docker-db-connect.sh # Se connecter à MySQL"