# ========== SCRIPT 2: docker-logs.sh ==========
#!/bin/bash
echo "📋 LOGS DOCKER"
echo "=============="

echo "--- LOGS BACKEND ---"
docker-compose logs backend --tail=50

echo "--- LOGS FRONTEND ---"
docker-compose logs frontend --tail=20

echo "--- LOGS DATABASE ---"
docker-compose logs database --tail=20
