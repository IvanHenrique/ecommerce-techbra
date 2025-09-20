#!/bin/bash

# Limpeza completa do ambiente
set -e

NAMESPACE=${1:-default}
CLUSTER_NAME=${2:-techbra-ecommerce}
DELETE_CLUSTER=${3:-false}

echo "🧹 Limpando ambiente Kubernetes no namespace: $NAMESPACE"

# Remover todos os releases Helm
echo "📦 Removendo releases Helm..."
helm uninstall bff -n $NAMESPACE 2>/dev/null || true
helm uninstall inventory-service -n $NAMESPACE 2>/dev/null || true
helm uninstall billing-service -n $NAMESPACE 2>/dev/null || true
helm uninstall order-service -n $NAMESPACE 2>/dev/null || true
helm uninstall kafka -n $NAMESPACE 2>/dev/null || true
helm uninstall redis -n $NAMESPACE 2>/dev/null || true
helm uninstall postgres -n $NAMESPACE 2>/dev/null || true
helm uninstall techbra-ecommerce -n $NAMESPACE 2>/dev/null || true

# Aguardar cleanup dos pods
echo "⏳ Aguardando cleanup dos pods..."
kubectl wait --for=delete pods --all -n $NAMESPACE --timeout=60s 2>/dev/null || true

# Remover PVCs se existirem
echo "💾 Removendo PVCs..."
kubectl delete pvc --all -n $NAMESPACE 2>/dev/null || true

# Remover ConfigMaps
echo "🗄️ Removendo ConfigMaps..."
kubectl delete configmap --all -n $NAMESPACE 2>/dev/null || true

# Remover Secrets
echo "🔑 Removendo Secrets..."
kubectl delete secret --all -n $NAMESPACE 2>/dev/null || true

# Remover Services
echo "🔌 Removendo Services..."
kubectl delete svc --all -n $NAMESPACE 2>/dev/null || true

# Remover Deployments
echo "🚀 Removendo Deployments..."
kubectl delete deployment --all -n $NAMESPACE 2>/dev/null || true

# Remover StatefulSets
echo "📊 Removendo StatefulSets..."
kubectl delete statefulset --all -n $NAMESPACE 2>/dev/null || true

# Remover namespace se não for o namespace default
if [ "$NAMESPACE" != "default" ]; then
  echo "🗑️ Removendo namespace $NAMESPACE..."
  kubectl delete namespace $NAMESPACE 2>/dev/null || true
else
  echo "⚠️ Namespace default não será removido, apenas limpo."
fi

# Remover cluster Kind se solicitado
if [ "$DELETE_CLUSTER" = "true" ]; then
  echo "🗑️ Removendo cluster Kind: $CLUSTER_NAME..."
  kind delete cluster --name $CLUSTER_NAME 2>/dev/null || true
  echo "✅ Cluster Kind removido com sucesso!"
else
  echo "ℹ️ Cluster Kind mantido. Para remover, execute com o terceiro parâmetro como 'true'."
fi

echo "✅ Ambiente limpo com sucesso!"
