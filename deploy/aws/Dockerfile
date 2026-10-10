FROM php:8.3-apache-bookworm
RUN apt-get update && apt-get install -y --no-install-recommends libcurl4-openssl-dev \
    && docker-php-ext-install pdo_mysql curl \
    && a2enmod rewrite headers \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /srv/nannyapp
COPY NannyApp.Shared/config/ NannyApp.Shared/config/
COPY NannyApp.Shared/database/ NannyApp.Shared/database/
COPY NannyApp.Shared/bin/ NannyApp.Shared/bin/
COPY NannyApp.Web/ NannyApp.Web/
COPY NannyApp.Mobile/NannyApp/api/ NannyApp.Mobile/NannyApp/api/
COPY NannyApp.Mobile/NannyApp/config/ NannyApp.Mobile/NannyApp/config/
COPY NannyApp.Mobile/NannyApp/includes/ NannyApp.Mobile/NannyApp/includes/
COPY deploy/aws/apache.conf /etc/apache2/sites-available/000-default.conf
COPY deploy/aws/php.ini /usr/local/etc/php/conf.d/production.ini
RUN mkdir -p /srv/nannyapp/private/uploads /srv/nannyapp/private/releases \
    && chown -R www-data:www-data /srv/nannyapp/private \
    && sed -i 's|RewriteBase /nannyapp/|RewriteBase /|; s|/nannyapp/404.php|/404.php|; s|/nannyapp/500.php|/500.php|' NannyApp.Web/.htaccess

