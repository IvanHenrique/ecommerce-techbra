#!/bin/bash

# Build e push das imagens Docker
set -e

echo "🐳 Construindo imagens Docker..."

# Variáveis
REGISTRY=${1:-techbra}
TAG=${2:-latest}

services=("order-service" "billing-service" "inventory-service" "bff")

for service in "${services[@]}"; do
    echo "📦 Construindo $service..."

    cd "$service"
    docker build -t "$REGISTRY/$service:$TAG" .

    if [ "$REGISTRY" != "techbra" ]; then
        echo "📤 Fazendo push de $service..."
        docker push "$REGISTRY/$service:$TAG"
    fi

    cd ..
done

echo "✅ Todas as imagens foram construídas!"

if [ "$REGISTRY" = "techbra" ]; then
    echo "💡 Para fazer push para um registry:"
    echo "./deploy/build-images.sh your-registry.com/techbra latest"
fi