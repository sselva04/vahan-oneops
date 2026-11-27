#!/usr/bin/env bash
# Runs in GitHub Actions (AWS credentials from OIDC). Ships k8s/ + scripts/server/ to the dev server
# through SSM Run Command and runs apply-dev.sh there. No SSH, no open ports, no stored keys.
set -euo pipefail
SHA="${1:?usage: deploy-dev.sh <full-commit-sha>}"
REGION="${AWS_REGION:-ap-south-1}"

IID=$(aws ec2 describe-instances --region "$REGION" \
  --filters "Name=tag:Name,Values=vahan-dev-k3s" "Name=instance-state-name,Values=running" \
  --query 'Reservations[].Instances[].InstanceId' --output text)
[ -n "$IID" ] && [ "$IID" != "None" ] || { echo "No running dev server found (is it switched off?)"; exit 1; }
echo "server: $IID"

# the SSM agent must be online
for i in $(seq 1 20); do
  P=$(aws ssm describe-instance-information --region "$REGION" \
      --filters "Key=InstanceIds,Values=$IID" --query 'InstanceInformationList[0].PingStatus' --output text 2>/dev/null || true)
  [ "$P" = "Online" ] && break
  echo "waiting for SSM agent ($P)"; sleep 15
done
[ "$P" = "Online" ] || { echo "SSM agent offline"; exit 1; }

B64=$(tar czf - k8s scripts/server | base64 -w0)
echo "payload: ${#B64} chars"

run() {  # run <shell command>; waits and prints output; fails if the command failed
  local ID
  ID=$(aws ssm send-command --region "$REGION" --instance-ids "$IID" \
    --document-name AWS-RunShellScript --timeout-seconds 900 \
    --parameters "$(python3 -c 'import json,sys; print(json.dumps({"commands":[sys.argv[1]],"executionTimeout":["900"]}))' "$1")" \
    --query Command.CommandId --output text)
  for i in $(seq 1 200); do
    local S
    S=$(aws ssm get-command-invocation --region "$REGION" --command-id "$ID" --instance-id "$IID" \
        --query Status --output text 2>/dev/null || echo Pending)
    case "$S" in
      Success|Failed|Cancelled|TimedOut) break;;
    esac
    sleep 5
  done
  aws ssm get-command-invocation --region "$REGION" --command-id "$ID" --instance-id "$IID" \
    --query 'StandardOutputContent' --output text
  if [ "$S" != "Success" ]; then
    echo "---- errors ----"
    aws ssm get-command-invocation --region "$REGION" --command-id "$ID" --instance-id "$IID" \
      --query 'StandardErrorContent' --output text
    echo "SSM command status: $S"; return 1
  fi
}

run "rm -rf /tmp/deploy.b64 /tmp/deploy && mkdir -p /tmp/deploy"
for ((o=0; o<${#B64}; o+=3000)); do
  run "printf %s '${B64:o:3000}' >> /tmp/deploy.b64" >/dev/null
done
run "base64 -d /tmp/deploy.b64 | tar xzf - -C /tmp/deploy && rm -f /tmp/deploy.b64 && cd /tmp/deploy && bash scripts/server/apply-dev.sh $SHA"
