#!/bin/bash

# Deploy local usando Minikube/Kind
set -e

echo "🚀 Iniciando deploy local do Techbra E-commerce..."

# Verificar se o cluster está ativo
if ! kubectl cluster-info &> /dev/null; then
    echo "❌ Cluster Kubernetes não está acessível"
    echo "💡 Inicie o minikube: minikube start"
    exit 1
fi

# Criar namespace
echo "📦 Criando namespace..."
kubectl create namespace techbra-ecommerce --dry-run=client -o yaml | kubectl apply -f -

# Deploy infrastructure first
echo "🏗️ Fazendo deploy da infraestrutura..."
helm upgrade --install postgres charts/infrastructure/postgres \
    -n techbra-ecommerce \
    -f charts/infrastructure/postgres/values-dev.yaml

helm upgrade --install redis charts/infrastructure/redis \
    -n techbra-ecommerce \
    -f charts/infrastructure/redis/values-dev.yaml

helm upgrade --install kafka charts/infrastructure/kafka \
    -n techbra-ecommerce \
    -f charts/infrastructure/kafka/values-dev.yaml

# Aguardar infraestrutura
echo "⏳ Aguardando infraestrutura ficar pronta..."
kubectl wait --for=condition=ready pod -l app=postgres -n techbra-ecommerce --timeout=300s
kubectl wait --for=condition=ready pod -l app=redis -n techbra-ecommerce --timeout=300s
kubectl wait --for=condition=ready pod -l app=kafka -n techbra-ecommerce --timeout=300s

# Deploy microservices
echo "🛍️ Fazendo deploy dos microserviços..."

helm upgrade --install order-service charts/order-service \
    -n techbra-ecommerce \
    -f charts/order-service/values-dev.yaml

helm upgrade --install billing-service charts/billing-service \
    -n techbra-ecommerce \
    -f charts/billing-service/values-dev.yaml

helm upgrade --install inventory-service charts/inventory-service \
    -n techbra-ecommerce \
    -f charts/inventory-service/values-dev.yaml

helm upgrade --install bff charts/bff \
    -n techbra-ecommerce \
    -f charts/bff/values-dev.yaml

echo "✅ Deploy local concluído!"
echo ""
echo "📊 Para verificar o status:"
echo "kubectl get pods -n techbra-ecommerce"
echo ""
echo "🔗 Para acessar os serviços:"
echo "kubectl port-forward service/order-service 8081:8081 -n techbra-ecommerce"
echo "kubectl port-forward service/bff 8084:8084 -n techbra-ecommerce"