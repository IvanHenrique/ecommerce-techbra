#!/bin/bash

# Configurar Minikube para o projeto
set -e

echo "🚀 Configurando Minikube para Techbra E-commerce..."

# Verificar se minikube está instalado
if ! command -v minikube &> /dev/null; then
    echo "❌ Minikube não está instalado"
    echo "💡 Instale: https://minikube.sigs.k8s.io/docs/start/"
    exit 1
fi

# Parar minikube se estiver rodando
echo "🛑 Parando Minikube existente..."
minikube stop 2>/dev/null || true

# Configurar e iniciar minikube com recursos adequados
echo "📦 Iniciando Minikube com configurações otimizadas..."
minikube start \
    --cpus=6 \
    --memory=12288 \
    --disk-size=40g \
    --driver=docker \
    --kubernetes-version=v1.28.0 \
    --extra-config=kubelet.housekeeping-interval=10s

# Habilitar addons necessários
echo "🔧 Habilitando addons..."
minikube addons enable metrics-server
minikube addons enable dashboard
minikube addons enable storage-provisioner
minikube addons enable default-storageclass

# Configurar Docker registry para usar o do Minikube
echo "🐳 Configurando Docker registry..."
eval $(minikube docker-env)

# Verificar se tudo está funcionando
echo "✅ Verificando configuração..."
kubectl cluster-info
kubectl get nodes
kubectl get storageclass

echo "✅ Minikube configurado com sucesso!"
echo ""
echo "📊 Para acessar o dashboard:"
echo "minikube dashboard"
echo ""
echo "🚀 Para fazer deploy completo:"
echo "./deploy/deploy-complete.sh dev"
echo ""
echo "🔗 URLs importantes após o deploy:"
echo "- BFF: kubectl port-forward service/bff 8084:8084 -n techbra-ecommerce"
echo "- Kafka UI: kubectl port-forward service/kafka-ui 8080:8080 -n techbra-ecommerce"
echo "- Order Service: kubectl port-forward service/order-service 8081:8081 -n techbra-ecommerce"