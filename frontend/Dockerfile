FROM node:20-bullseye AS build
WORKDIR /app
COPY package*.json ./
RUN npm install --legacy-peer-deps --no-audit --no-fund
COPY . .
ENV NODE_OPTIONS=--max-old-space-size=1024
RUN npm run build

FROM nginx:1.27-alpine
COPY --from=build /app/dist/ia-frontend /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
