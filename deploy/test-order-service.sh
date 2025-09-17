#!/bin/bash

# Teste apenas do Order Service
set -e

echo "🧪 Testando apenas Order Service..."

# Verificar se o cluster está ativo
if ! kubectl cluster-info &> /dev/null; then
    echo "❌ Cluster Kubernetes não está acessível"
    echo "💡 Inicie o minikube: minikube start"
    exit 1
fi

# Criar namespace
echo "📦 Criando namespace..."
kubectl create namespace techbra-test --dry-run=client -o yaml | kubectl apply -f -

# Deploy apenas infraestrutura mínima para Order Service
echo "🗄️ Deploy PostgreSQL..."
helm upgrade --install postgres charts/infrastructure/postgres \
    -n techbra-test \
    -f charts/infrastructure/postgres/values-dev.yaml

echo "🔄 Deploy Kafka..."
helm upgrade --install kafka charts/infrastructure/kafka \
    -n techbra-test \
    -f charts/infrastructure/kafka/values-dev.yaml

# Aguardar infraestrutura
echo "⏳ Aguardando infraestrutura..."
kubectl wait --for=condition=ready pod -l app=postgres -n techbra-test --timeout=180s
kubectl wait --for=condition=ready pod -l app=kafka -n techbra-test --timeout=180s

# Deploy Order Service
echo "📦 Deploy Order Service..."
helm upgrade --install order-service charts/order-service \
    -n techbra-test \
    -f charts/order-service/values-dev.yaml

echo "⏳ Aguardando Order Service..."
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=order-service -n techbra-test --timeout=120s

echo "✅ Order Service testado com sucesso!"
echo ""
echo "🔗 Para testar:"
echo "kubectl port-forward service/order-service 8081:8081 -n techbra-test"
echo "curl http://localhost:8081/actuator/health"