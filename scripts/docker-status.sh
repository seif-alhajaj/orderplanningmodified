# ========== SCRIPT 7: docker-status.sh ==========
#!/bin/bash
echo " STATUS DOCKER"
echo "================"

echo "--- CONTAINERS STATUS ---"
docker-compose ps

echo ""
echo "--- HEALTH CHECKS ---"
docker-compose exec backend curl -f http://localhost:8080/api/employees/debug || echo " Backend unhealthy"
docker-compose exec frontend curl -f http://localhost:3000/ || echo " Frontend unhealthy"
docker-compose exec database mysqladmin ping -h localhost -u root -prootpassword || echo " Database unhealthy"

echo ""
echo "--- RESOURCES USAGE ---"
docker stats --no-stream
