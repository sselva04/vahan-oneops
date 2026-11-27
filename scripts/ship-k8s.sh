#!/usr/bin/env bash
# Run on your Mac. Packs k8s/ and scripts/server/ and delivers them to the dev server through
# AWS Systems Manager (no SSH, no git clone, no tokens). They land in /tmp/deploy on the server.
#   AWS_PROFILE=vahan-dev bash scripts/ship-k8s.sh <instance-id>
set -euo pipefail
INSTANCE="${1:?usage: $0 <instance-id>}"
REGION="${AWS_REGION:-ap-south-1}"
cd "$(dirname "$0")/.."

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
tar czf - k8s scripts/server | base64 | tr -d '\n' > "$TMP/pkg.b64"

python3 - "$TMP" "$INSTANCE" <<'PY'
import json, sys
tmp, instance = sys.argv[1], sys.argv[2]
b64 = open(f"{tmp}/pkg.b64").read()
chunks = [b64[i:i+900] for i in range(0, len(b64), 900)]
cmds = ["rm -rf /tmp/deploy && mkdir -p /tmp/deploy"]
cmds += [f"printf %s '{c}' >> /tmp/deploy/pkg.b64" for c in chunks]
cmds += ["cd /tmp/deploy && base64 -d pkg.b64 | tar xzf - && rm pkg.b64 && chown -R ssm-user:ssm-user /tmp/deploy && ls -R /tmp/deploy | head -40"]
json.dump({"InstanceIds": [instance], "DocumentName": "AWS-RunShellScript",
           "Comment": "ship k8s manifests", "Parameters": {"commands": cmds}},
          open(f"{tmp}/cmd.json", "w"))
PY

CMD_ID=$(aws ssm send-command --region "$REGION" --cli-input-json "file://$TMP/cmd.json" \
  --query 'Command.CommandId' --output text)
echo "Sent (command $CMD_ID). Waiting..."
for _ in $(seq 1 30); do
  STATUS=$(aws ssm get-command-invocation --region "$REGION" --command-id "$CMD_ID" \
    --instance-id "$INSTANCE" --query 'Status' --output text 2>/dev/null || echo Pending)
  case "$STATUS" in Success|Failed|Cancelled|TimedOut) break;; esac
  sleep 2
done
echo "Status: $STATUS"
aws ssm get-command-invocation --region "$REGION" --command-id "$CMD_ID" --instance-id "$INSTANCE" \
  --query 'StandardOutputContent' --output text
[ "$STATUS" = "Success" ]
