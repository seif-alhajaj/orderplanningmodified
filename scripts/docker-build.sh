#!/bin/bash

# ===============================================
# SCRIPTS DOCKER - POKEMON CARD PLANNING
# ===============================================

# ========== SCRIPT 1: docker-build.sh ==========
echo "🐳 BUILD ET DÉMARRAGE DOCKER"
echo "============================="

# Construire et démarrer tous les services
docker-compose up --build -d

# Attendre que les services soient prêts
echo "⏳ Attendre que les services démarrent..."
sleep 30

# Vérifier le statut
docker-compose ps

echo "✅ Application disponible sur:"
echo "   Frontend: http://localhost:3000"
echo "   Backend:  http://localhost:8080"
echo "   MySQL:    localhost:3306"








