#!/bin/sh
set -eu

echo "kibana-init: create keystore if missing"
if [ ! -f /usr/share/kibana/config/kibana.keystore ]; then
  /usr/share/kibana/bin/kibana-keystore create
fi

gen_key () {
  if command -v openssl >/dev/null 2>&1; then
    openssl rand -hex 32
    return 0
  fi
  head -c 32 /dev/urandom | od -An -tx1 | tr -d ' \n'
}

echo "kibana-init: ensure encryption keys exist"
/usr/share/kibana/bin/kibana-keystore list | grep -q '^xpack.security.encryptionKey$' \
  || gen_key | /usr/share/kibana/bin/kibana-keystore add xpack.security.encryptionKey --stdin

/usr/share/kibana/bin/kibana-keystore list | grep -q '^xpack.encryptedSavedObjects.encryptionKey$' \
  || gen_key | /usr/share/kibana/bin/kibana-keystore add xpack.encryptedSavedObjects.encryptionKey --stdin

/usr/share/kibana/bin/kibana-keystore list | grep -q '^xpack.reporting.encryptionKey$' \
  || gen_key | /usr/share/kibana/bin/kibana-keystore add xpack.reporting.encryptionKey --stdin

echo "kibana-init: read service token"
if [ ! -s /bootstrap/kibana_service_token.txt ]; then
  echo "kibana-init: missing /bootstrap/kibana_service_token.txt"
  exit 1
fi

svc_token="$(cat /bootstrap/kibana_service_token.txt)"

/usr/share/kibana/bin/kibana-keystore remove elasticsearch.serviceAccountToken >/dev/null 2>&1 || true
printf "%s" "$svc_token" | /usr/share/kibana/bin/kibana-keystore add elasticsearch.serviceAccountToken --stdin

echo "kibana-init: done"
