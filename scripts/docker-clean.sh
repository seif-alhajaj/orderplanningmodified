# ========== SCRIPT 4: docker-clean.sh ==========
#!/bin/bash
echo " DOCKER CLEANING"
echo "==================="

# Stop and delete everything
docker-compose down -v --remove-orphans

# Delete images
docker-compose down --rmi all

# Clean up the Docker system
docker system prune -f

echo " Cleaning completed"
