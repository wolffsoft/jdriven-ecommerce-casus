#!/bin/sh
set -eu

echo "kibana-token-init: starting"

# If we already have a token stored, reuse it (idempotent)
if [ -s /bootstrap/kibana_service_token.txt ]; then
  echo "kibana-token-init: token file already exists, reusing"
  exit 0
fi

name="kibana-token-$(date +%s)"

echo "kibana-token-init: creating token name=$name"
resp="$(curl -fsS -X POST -u "elastic:${ELASTIC_PASSWORD}" \
  "http://elasticsearch:9200/_security/service/elastic/kibana/credential/token/${name}")"

token="$(echo "$resp" | sed -n 's/.*"value":"\([^"]*\)".*/\1/p')"

if [ -z "$token" ]; then
  echo "kibana-token-init: failed to parse token from response:"
  echo "$resp"
  exit 1
fi

# Write token
umask 077
printf "%s" "$token" > /bootstrap/kibana_service_token.txt

echo "kibana-token-init: token written"
