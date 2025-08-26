# ========== SCRIPT 6: docker-backend-shell.sh ==========
#!/bin/bash
echo "🔧 SHELL BACKEND"
echo "================"

# Accéder au shell du container backend
docker-compose exec backend /bin/sh
