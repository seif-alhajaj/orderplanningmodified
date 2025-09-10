# ========== SCRIPT 3: docker-stop.sh ==========
#!/bin/bash
echo " STOP DOCKER"
echo "==============="

# Stop all services
docker-compose down

# Optional: Delete volumes (lost data)
# docker-compose down -v

echo " Services stopped"
