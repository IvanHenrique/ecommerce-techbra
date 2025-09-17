#!/bin/bash

# Limpeza completa do ambiente
set -e

NAMESPACE=${1:-techbra-ecommerce}

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

# Remover namespace
echo "🗑️ Removendo namespace..."
kubectl delete namespace $NAMESPACE 2>/dev/null || true

echo "✅ Ambiente limpo com sucesso!"