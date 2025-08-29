#!/bin/bash

set -e

docker build -t order-platform-msa-order .

docker stop order > /dev/null 2>&1 || true
docker rm order > /dev/null 2>&1 || true

docker run --name order \
    --network entity-repository_order-network \
    -p 8081:8081 \
    -e DB_URL=jdbc:postgresql://postgres:5432/order_platform \
    -e DB_USERNAME=bonun \
    -e DB_PASSWORD=password \
    -e OAUTH_JWKS_URI=http://host.docker.internal:8083/oauth/jwks \
    -e AUTH_INTERNAL_AUDIENCE=internal-services \
    -e MONGO_INITDB_ROOT_USERNAME \
    -e MONGO_INITDB_ROOT_PASSWORD \
    -e MONGODB=mongodb \
    -e MONGO_PORT=27017 \
    -e MONGO_NAME=store-db \
    -e REDIS_HOST=redis \
    -e REDIS_PORT=6379 \
    -e REDIS_PASSWORD=password \
    -e REDIS_REFUND_QUEUE=refund-disable-queue \
    -e STORE_SVC_URI=http://localhost:8082 \
    -d order-platform-msa-order


# Check container status
docker ps -f "name=order"