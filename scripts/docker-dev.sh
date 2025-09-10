# ========== SCRIPT 9: docker-dev.sh ==========
#!/bin/bash
echo " DOCKER DEVELOPMENT"
echo "======================="

# Development mode with live reload
docker-compose -f docker-compose.yml -f docker-compose.dev.yml up --build

# ========== INSTRUCTIONS FOR USE ==========
echo ""
echo " USE OF SCRIPTS:"
echo "============================"
echo "chmod +x docker-*.sh"
echo ""
echo "./docker-build.sh     # Build and start"
echo "./docker-logs.sh      # See the logs"
echo "./docker-status.sh    # Check status"
echo "./docker-restart.sh   # Restart"
echo "./docker-stop.sh      # Stop"
echo "./docker-clean.sh     # Clean completely"
echo "./docker-db-connect.sh # Connect to MySQL"