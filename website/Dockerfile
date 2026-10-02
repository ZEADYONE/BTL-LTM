# Static landing page. Built from the repository root so it can reuse the desktop
# client's SVG art and fonts; website/Dockerfile.dockerignore limits the context.
# The release ZIP is not baked in: docker-compose.yml mounts ./downloads read-only.
FROM nginx:1.30-alpine

ENV NGINX_ENVSUBST_FILTER=^BOMBERMAN_ \
    BOMBERMAN_PUBLIC_HOST="" \
    BOMBERMAN_PUBLIC_PORT=8081 \
    BOMBERMAN_REPO_URL=""

RUN rm /etc/nginx/conf.d/default.conf \
    && mkdir -p /usr/share/nginx/html/downloads

COPY website/nginx/default.conf.template /etc/nginx/templates/default.conf.template

COPY client-fx/src/main/resources/assets/svg /usr/share/nginx/html/assets/svg
COPY client-fx/src/main/resources/assets/app_icon.png /usr/share/nginx/html/assets/app_icon.png
COPY client-fx/src/main/resources/fonts /usr/share/nginx/html/fonts

COPY website/assets /usr/share/nginx/html/assets
COPY website/css /usr/share/nginx/html/css
COPY website/js /usr/share/nginx/html/js
COPY website/index.html /usr/share/nginx/html/index.html

EXPOSE 80
