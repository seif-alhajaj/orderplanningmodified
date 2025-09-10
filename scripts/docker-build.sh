#!/bin/bash

# ===============================================
# SCRIPTS DOCKER - POKEMON CARD PLANNING
# ===============================================

# ========== SCRIPT 1: docker-build.sh ==========
echo " BUILD AND START DOCKER"
echo "============================="

# Build and start all services
docker-compose up --build -d

# Wait for services to be ready
echo " Wait for services to start..."
sleep 30

# Check status
docker-compose ps

echo " Application available on:"
echo "   Frontend: http://localhost:3000"
echo "   Backend:  http://localhost:8080"
echo "   MySQL:    localhost:3306"








