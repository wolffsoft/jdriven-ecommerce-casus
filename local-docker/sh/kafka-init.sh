#!/usr/bin/env bash
set -eu

echo "Waiting for Kafka to be ready..."
for i in {1..60}; do
  if kafka-broker-api-versions --bootstrap-server kafka:29092 >/dev/null 2>&1; then
    echo "Kafka is ready."
    break
  fi
  echo "Kafka not ready yet (${i}/60)..."
  sleep 2
done

# Final check
kafka-broker-api-versions --bootstrap-server kafka:29092 >/dev/null 2>&1 \
  || { echo "Kafka did not become ready in time"; exit 1; }

echo "Creating topics..."
kafka-topics --bootstrap-server kafka:29092 --create --if-not-exists --topic product-events --partitions 3 --replication-factor 1
kafka-topics --bootstrap-server kafka:29092 --create --if-not-exists --topic product-events.DLT --partitions 3 --replication-factor 1

echo "Done."
