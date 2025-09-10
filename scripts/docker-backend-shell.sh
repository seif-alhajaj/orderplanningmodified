# ========== SCRIPT 6: docker-backend-shell.sh ==========
#!/bin/bash
echo " SHELL BACKEND"
echo "================"

# Access the backend container shell
docker-compose exec backend /bin/sh
