#!/bin/bash

# Deploy completo usando o umbrella chart
set -e

ENVIRONMENT=${1:-dev}
NAMESPACE=${2:-techbra-ecommerce}

echo "🚀 Iniciando deploy completo do Techbra E-commerce..."
echo "📦 Ambiente: $ENVIRONMENT"
echo "🏷️ Namespace: $NAMESPACE"

# Verificar se o cluster está ativo
if ! kubectl cluster-info &> /dev/null; then
    echo "❌ Cluster Kubernetes não está acessível"
    echo "💡 Inicie o minikube: minikube start"
    exit 1
fi

# Verificar se Helm está instalado
if ! command -v helm &> /dev/null; then
    echo "❌ Helm não está instalado"
    echo "💡 Instale: https://helm.sh/docs/intro/install/"
    exit 1
fi

# Criar namespace
echo "📦 Criando namespace $NAMESPACE..."
kubectl create namespace $NAMESPACE --dry-run=client -o yaml | kubectl apply -f -

# Build de todas as imagens (se necessário)
if [ "$ENVIRONMENT" = "dev" ]; then
    echo "🐳 Construindo imagens para desenvolvimento..."
    ./deploy/build-images.sh techbra dev
fi

# Deploy usando umbrella chart
echo "🎯 Fazendo deploy do umbrella chart..."
helm dependency update charts/techbra-ecommerce/

helm upgrade --install techbra-ecommerce charts/techbra-ecommerce/ \
    --namespace $NAMESPACE \
    --values charts/techbra-ecommerce/values-$ENVIRONMENT.yaml \
    --timeout 10m \
    --wait

echo "⏳ Aguardando todos os pods ficarem prontos..."
kubectl wait --for=condition=ready pod --all -n $NAMESPACE --timeout=600s

echo "✅ Deploy completo concluído com sucesso!"
echo ""
echo "📊 Para verificar o status:"
echo "kubectl get all -n $NAMESPACE"
echo ""
echo "🔗 Para acessar os serviços:"
echo "kubectl port-forward service/bff 8084:8084 -n $NAMESPACE"
echo "kubectl port-forward service/kafka-ui 8080:8080 -n $NAMESPACE"
echo ""
echo "🏥 Para verificar a saúde:"
echo "./deploy/health-check.sh $NAMESPACE"