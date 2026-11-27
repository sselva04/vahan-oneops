#!/usr/bin/env bash
# Run on the k3s server, from /tmp/deploy:   bash scripts/server/deploy-dev.sh <full-commit-sha>
# Safe to run again: existing secrets are kept, everything else is re-applied.
set -euo pipefail
export KUBECONFIG="${KUBECONFIG:-/etc/rancher/k3s/k3s.yaml}"
TAG="${1:?usage: deploy-dev.sh <full-commit-sha>}"
OWNER="sselva04"
cd "$(dirname "$0")/../.."

echo "== 1/5 issuer and namespace"
kubectl apply -f k8s/cluster/cluster-issuer.yaml
kubectl apply -f k8s/overlays/dev/namespace.yaml

echo "== 2/5 app secrets"
if kubectl -n dev get secret vahan-secrets >/dev/null 2>&1; then
  echo "vahan-secrets already exists - keeping it"
else
  read -rsp "Choose the admin password for the app (12+ characters), then press Enter: " AP; echo
  [ "${#AP}" -ge 12 ] || { echo "Too short, run again."; exit 1; }
  kubectl -n dev create secret generic vahan-secrets \
    --from-literal=db-password="$(openssl rand -hex 24)" \
    --from-literal=jwt-secret="$(openssl rand -hex 32)" \
    --from-literal=admin-password="$AP"
  unset AP
fi

echo "== 3/5 image pull secret"
if kubectl -n dev get secret ghcr-pull >/dev/null 2>&1; then
  echo "ghcr-pull already exists - keeping it"
else
  read -rsp "Paste the GitHub read:packages token, then press Enter: " PT; echo
  PT="$(printf %s "$PT" | tr -d '\r\n ')"
  echo "token length: ${#PT}"
  kubectl -n dev create secret docker-registry ghcr-pull \
    --docker-server=ghcr.io --docker-username="$OWNER" --docker-password="$PT"
  unset PT
fi

echo "== 4/5 pin images to commit $TAG"
RUN=k8s/overlays/.run-dev
mkdir -p "$RUN"
cat > "$RUN/kustomization.yaml" <<YAML
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization
resources:
  - ../dev
images:
  - name: ghcr.io/$OWNER/vahan-oneops-backend
    newTag: $TAG
  - name: ghcr.io/$OWNER/vahan-oneops-frontend
    newTag: $TAG
YAML
kubectl kustomize "$RUN" | grep 'image:' | sort -u

echo "== 5/5 deploy"
kubectl apply -k "$RUN"
rm -rf "$RUN"
kubectl -n dev rollout status deploy/backend --timeout=300s || true
kubectl -n dev get pods
