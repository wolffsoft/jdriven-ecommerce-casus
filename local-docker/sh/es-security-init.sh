#!/bin/sh
set -eu

echo "es-security-init: upserting role + user for index=$APP_ES_INDEX_PATTERN"

# Upsert role (idempotent)
curl -fsS -u "elastic:${ELASTIC_PASSWORD}" \
  -X PUT "http://elasticsearch:9200/_security/role/${APP_ES_USERNAME}-role" \
  -H "Content-Type: application/json" \
  -d "{\"cluster\":[\"monitor\"],\"indices\":[{\"names\":[\"${APP_ES_INDEX_PATTERN}\"],\"privileges\":[\"auto_configure\",\"create_index\",\"read\",\"write\",\"view_index_metadata\",\"manage\"]}]}" \
  >/dev/null

# Upsert user (idempotent)
curl -fsS -u "elastic:${ELASTIC_PASSWORD}" \
  -X PUT "http://elasticsearch:9200/_security/user/${APP_ES_USERNAME}" \
  -H "Content-Type: application/json" \
  -d "{\"password\":\"${APP_ES_PASSWORD}\",\"roles\":[\"${APP_ES_USERNAME}-role\"]}" \
  >/dev/null

echo "es-security-init: done"
