#!/bin/bash

# Script de verificação de saúde completa
set -e

NAMESPACE=${1:-techbra-ecommerce}

echo "🏥 Verificando saúde do Techbra E-commerce no namespace: $NAMESPACE"

# Verificar se o namespace existe
if ! kubectl get namespace $NAMESPACE &> /dev/null; then
    echo "❌ Namespace $NAMESPACE não existe"
    exit 1
fi

echo "📦 Verificando pods..."
kubectl get pods -n $NAMESPACE

echo "🔗 Verificando services..."
kubectl get services -n $NAMESPACE

echo "📊 Verificando HPA..."
kubectl get hpa -n $NAMESPACE 2>/dev/null || echo "ℹ️ Nenhum HPA configurado"

echo "💾 Verificando PVCs..."
kubectl get pvc -n $NAMESPACE 2>/dev/null || echo "ℹ️ Nenhum PVC encontrado"

echo "🔐 Verificando Network Policies..."
kubectl get networkpolicy -n $NAMESPACE 2>/dev/null || echo "ℹ️ Nenhuma Network Policy encontrada"

# Teste de conectividade dos health checks
echo "🩺 Testando health checks..."

services=("order-service:8081" "billing-service:8082" "inventory-service:8083" "bff:8084")

for service in "${services[@]}"; do
    service_name=$(echo $service | cut -d':' -f1)
    port=$(echo $service | cut -d':' -f2)

    echo "  Testando $service_name..."

    if kubectl get service $service_name -n $NAMESPACE &> /dev/null; then
        # Port-forward em background e testar
        kubectl port-forward service/$service_name $port:$port -n $NAMESPACE &
        PF_PID=$!

        sleep 3

        if curl -s "http://localhost:$port/actuator/health" > /dev/null; then
            echo "  ✅ $service_name está saudável"
        else
            echo "  ❌ $service_name não está respondendo"
        fi

        kill $PF_PID 2>/dev/null || true
    else
        echo "  ⚠️ $service_name não encontrado"
    fi
done

echo "✅ Verificação de saúde concluída!"