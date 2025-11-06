# Dockerfile (React frontend)
# --- Build Stage ---
FROM node:16 AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

# --- Runtime Stage ---
FROM nginx:alpine
COPY --from=build /app/build /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf

# Expose port
EXPOSE 80

# Entry point
CMD ["nginx", "-g", "daemon off;"]