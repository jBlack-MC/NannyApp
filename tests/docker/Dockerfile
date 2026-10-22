FROM php:8.3-cli-bookworm
RUN apt-get update && apt-get install -y --no-install-recommends python3 python3-psycopg2 python3-bcrypt nodejs apache2 libcurl4-openssl-dev libpq-dev libonig-dev \
    && docker-php-ext-install pdo_mysql pdo_pgsql curl mbstring \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /workspace
COPY . .
CMD ["python3", "tests/run-checks.py", "--backend"]
