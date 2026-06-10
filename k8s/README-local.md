# Kubernetes — manifests demonstrativos (Lote 10)

Manifests **demonstrativos** dos dois microserviços do Pix Transaction Simulator.
Não representam um deploy real em cloud: o objetivo é mostrar como os serviços
seriam implantados em um ambiente orquestrado (health checks, ConfigMap, Secret,
Deployment, Service) e preparar a integração com o Argo CD (GitOps).

> Para o fluxo local de desenvolvimento/demonstração, use o **Docker Compose** e
> o **frontend-demo** (mais simples). Veja `../frontend-demo/` e o
> `../docker-compose.yml`.

## Conteúdo

```text
k8s/base/
├── namespace.yaml                         # namespace pix-simulator
├── configmap.yaml                         # variáveis NÃO sensíveis
├── secret-example.yaml                    # EXEMPLO com placeholders (sem segredo real)
├── pix-payment-api-deployment.yaml        # Deployment da API
├── pix-payment-api-service.yaml           # Service ClusterIP da API
├── pix-notification-worker-deployment.yaml# Deployment do worker
└── pix-notification-worker-service.yaml   # Service ClusterIP do worker (Actuator)
```

## Premissas

- **Infraestrutura externa/gerenciada**: SQL Server, Redis, Kafka e MongoDB são
  assumidos como serviços já existentes. Não há manifests para eles (fora do
  escopo do Lote 10). Os hostnames no `ConfigMap` são placeholders.
- **Imagens placeholder**: `pedroqueiroz/pix-payment-api:latest` e
  `pedroqueiroz/pix-notification-worker:latest`. Publique as suas e ajuste a tag.
- **Secrets**: `secret-example.yaml` contém apenas placeholders base64 fictícios.
  **Nunca** versione segredos reais. Em produção, use Sealed Secrets, External
  Secrets, Vault ou o gerenciador de secrets da cloud.

## Health checks

`readinessProbe` e `livenessProbe` usam o Actuator (Lote 8):

```text
GET /actuator/health   (API em :8080, worker em :8081)
```

## Validação (sem aplicar nada de verdade)

```bash
# Valida os YAMLs contra o servidor, sem criar recursos:
kubectl apply --dry-run=client -f k8s/base/
```

## Aplicar em um cluster de teste (opcional)

> Requer um cluster (kind / minikube / Docker Desktop) e as imagens publicadas.

```bash
# 1. Crie o Secret REAL fora do Git (NÃO use o secret-example com valores reais):
kubectl create namespace pix-simulator
kubectl -n pix-simulator create secret generic pix-simulator-secret \
  --from-literal=SQLSERVER_DATABASE='...' \
  --from-literal=SQLSERVER_USERNAME='...' \
  --from-literal=SQLSERVER_PASSWORD='...' \
  --from-literal=MONGO_URI='mongodb://user:pass@host:27017/db?authSource=admin'

# 2. Aplique o restante (o secret-example pode ser ignorado):
kubectl apply -f k8s/base/namespace.yaml
kubectl apply -f k8s/base/configmap.yaml
kubectl apply -f k8s/base/pix-payment-api-deployment.yaml
kubectl apply -f k8s/base/pix-payment-api-service.yaml
kubectl apply -f k8s/base/pix-notification-worker-deployment.yaml
kubectl apply -f k8s/base/pix-notification-worker-service.yaml

# 3. Acompanhe:
kubectl -n pix-simulator get pods,svc
```

## GitOps (Argo CD)

O `../argocd/pix-simulator-application.yaml` aponta para `k8s/base` e sincroniza
estes manifests de forma declarativa. Veja o arquivo para detalhes.
