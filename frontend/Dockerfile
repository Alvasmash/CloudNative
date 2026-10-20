# =========================================================================
# DOCKERFILE - FRONTEND ANGULAR (PEDIDOS360)
# Multi-stage build con Node 20 y Nginx Alpine
# =========================================================================

# Etapa 1: Compilacion de la aplicacion Angular
FROM node:20-alpine AS builder
WORKDIR /app

# Instalar dependencias
COPY package*.json ./
RUN npm ci

# Copiar codigo fuente
COPY . .

# Compilar aplicacion Angular en modo produccion
RUN npm run build -- --configuration production

# Etapa 2: Servidor Nginx ligero para distribuir la SPA
FROM nginx:1.27-alpine
WORKDIR /usr/share/nginx/html

# Eliminar pagina por defecto de Nginx
RUN rm -rf ./*

# Copiar artefactos compilados desde el builder
COPY --from=builder /app/dist/frontend/browser .

# Copiar configuracion personalizada de Nginx
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
