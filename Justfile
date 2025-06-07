DATABASE_URL := "postgresql://postgres:postgres@localhost:5432/postgres"

down:
    docker compose down --remove-orphans
up:
    DATABASE_URL={{DATABASE_URL}} docker compose up -d

