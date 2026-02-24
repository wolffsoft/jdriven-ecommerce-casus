#!/bin/sh
set -eu

apk add --no-cache curl jq

echo "Registering Avro schemas using TopicRecordNameStrategy subjects..."

SUBJECT_CREATED="product-events-com.wolffsoft.catalog.events.ProductCreatedEvent"
SUBJECT_UPDATED="product-events-com.wolffsoft.catalog.events.ProductUpdatedEvent"
SUBJECT_PRICE="product-events-com.wolffsoft.catalog.events.ProductPriceUpdatedEvent"
SUBJECT_DELETED="product-events-com.wolffsoft.catalog.events.ProductDeletedEvent"

AVRO_CREATED="$(jq -Rs '{schema: .}' < /schemas/product-created-event.avsc)"
AVRO_UPDATED="$(jq -Rs '{schema: .}' < /schemas/product-updated-event.avsc)"
AVRO_PRICE="$(jq -Rs '{schema: .}' < /schemas/product-price-updated-event.avsc)"
AVRO_DELETED="$(jq -Rs '{schema: .}' < /schemas/product-deleted-event.avsc)"

curl -sS -X POST \
  -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  --data "$AVRO_CREATED" \
  "http://schema-registry:8081/subjects/$SUBJECT_CREATED/versions"

curl -sS -X POST \
  -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  --data "$AVRO_UPDATED" \
  "http://schema-registry:8081/subjects/$SUBJECT_UPDATED/versions"

curl -sS -X POST \
  -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  --data "$AVRO_PRICE" \
  "http://schema-registry:8081/subjects/$SUBJECT_PRICE/versions"

curl -sS -X POST \
  -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  --data "$AVRO_DELETED" \
  "http://schema-registry:8081/subjects/$SUBJECT_DELETED/versions"

echo "Schema registration complete."
