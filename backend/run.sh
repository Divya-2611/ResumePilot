#!/bin/bash
# Loads backend/.env and starts the Spring Boot server (Aiven cloud DB).
set -a
source "$(dirname "$0")/.env"
set +a
mvn spring-boot:run
