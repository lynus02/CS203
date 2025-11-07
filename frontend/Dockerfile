# Dockerfile (React frontend)
# --- Build Stage ---
FROM node:18-alpine AS build
WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

# --- Runtime Stage ---
FROM nginx:alpine

# Fix: Copy from build directory (not dist)
COPY --from=build /app/build /usr/share/nginx/html

# Copy your custom nginx.conf
COPY nginx.conf /etc/nginx/nginx.conf

# Inspect build output (for debugging)
RUN ls -la /usr/share/nginx/html

# Expose the port that Nginx serves on
EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]