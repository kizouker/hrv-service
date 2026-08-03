#!/bin/bash
set -e

echo "Starting Docker..."
open -a Docker
echo "Waiting for Docker to start..."
until docker info > /dev/null 2>&1; do
  sleep 2
done

echo "Stopping and removing old containers..."
docker-compose down --remove-orphans 2>/dev/null || true
docker rm -f hrv-postgres 2>/dev/null || true

echo "Freeing port 8080..."
lsof -ti:8080 | xargs kill -9 2>/dev/null || true

echo "Starting PostgreSQL..."
docker-compose up -d postgres


echo "Waiting for PostgreSQL to be ready..."
until docker exec hrv-postgres pg_isready -U postgres > /dev/null 2>&1; do
  sleep 1
done

echo "Starting Spring Boot..."
mvn spring-boot:run

