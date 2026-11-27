#!/usr/bin/env bash
# Runs ON the k3s server (started by the pipeline through SSM). Never asks questions.
#   bash scripts/server/apply-dev.sh <full-commit-sha>
# Order: check prerequisites -> backup the database -> deploy pinned images -> wait -> roll back if unhealthy.
set -euo pipefail
export KUBECONFIG="${KUBECONFIG:-/etc/rancher/k3s/k3s.yaml}"
TAG="${1:?usage: apply-dev.sh <full-commit-sha>}"
OWNER="sselva04"
NS=dev
cd "$(dirname "$0")/../.."

echo "== 1/6 prerequisites"
kubectl apply -f k8s/cluster/cluster-issuer.yaml
kubectl apply -f k8s/overlays/dev/namespace.yaml
for s in vahan-secrets ghcr-pull; do
  kubectl -n "$NS" get secret "$s" >/dev/null 2>&1 || {
    echo "ERROR: secret '$s' is missing in namespace $NS. Create it once (see docs), then re-run the pipeline."; exit 1; }
done

echo "== 2/6 backup before deploy"
if kubectl -n "$NS" get statefulset postgres >/dev/null 2>&1; then
  JOB="pre-deploy-$(date +%Y%m%d%H%M%S)"
  kubectl -n "$NS" create job --from=cronjob/db-backup "$JOB"
  if ! kubectl -n "$NS" wait --for=condition=complete "job/$JOB" --timeout=180s; then
    kubectl -n "$NS" logs "job/$JOB" --tail=30 || true
    echo "ERROR: backup failed - not deploying."; exit 1
  fi
  kubectl -n "$NS" logs "job/$JOB" --tail=5 || true
  kubectl -n "$NS" delete job "$JOB" --wait=false
else
  echo "no database yet - first deploy, nothing to back up"
fi

echo "== 3/6 remember what is running now"
PREV=$(kubectl -n "$NS" get deploy backend -o jsonpath='{.spec.template.spec.containers[0].image}' 2>/dev/null || true)
echo "backend currently: ${PREV:-none}"

echo "== 4/6 pin images to $TAG"
RUN=k8s/overlays/.run-dev
rm -rf "$RUN"; mkdir -p "$RUN"
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

echo "== 5/6 deploy"
kubectl apply -k "$RUN"
rm -rf "$RUN"

echo "== 6/6 wait for health"
if kubectl -n "$NS" rollout status deploy/backend --timeout=420s \
   && kubectl -n "$NS" rollout status deploy/frontend --timeout=180s; then
  kubectl -n "$NS" get pods
  echo "DEPLOY OK: $TAG"
else
  echo "ERROR: new version did not become healthy - rolling back"
  kubectl -n "$NS" logs deploy/backend --tail=40 || true
  kubectl -n "$NS" rollout undo deploy/backend || true
  kubectl -n "$NS" rollout undo deploy/frontend || true
  kubectl -n "$NS" rollout status deploy/backend --timeout=300s || true
  exit 1
fi
