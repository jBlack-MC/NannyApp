FROM php:8.3-cli-bookworm
RUN apt-get update && apt-get install -y --no-install-recommends python3 nodejs apache2 libcurl4-openssl-dev \
    && docker-php-ext-install pdo_mysql curl \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /workspace
COPY . .
CMD ["python3", "tests/run-checks.py", "--backend"]
