# ========== SCRIPT 5: docker-db-connect.sh ==========
#!/bin/bash
echo "💾 CONNEXION BASE DE DONNÉES"
echo "============================"

# Se connecter à MySQL dans le container
docker-compose exec database mysql -u ia -pfoufafou dev
