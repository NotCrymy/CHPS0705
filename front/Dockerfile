# Étape 1 : build Vue/Vite (console, overlay, jeu de démo).
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

# Étape 2 : fichiers statiques servis par nginx.
FROM nginx:alpine
# Les fichiers de /etc/nginx/templates passent par envsubst au démarrage → /etc/nginx/conf.d/default.conf
COPY nginx.conf /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html
