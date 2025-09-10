# ========== SCRIPT 5: docker-db-connect.sh ==========
#!/bin/bash
echo " DATABASE CONNECTION"
echo "============================"

# Connect to MySQL in the container
docker-compose exec database mysql -u ia -pfoufafou dev
