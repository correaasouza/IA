FROM node:20-bullseye AS build
WORKDIR /app
COPY package*.json ./
RUN npm install --legacy-peer-deps --no-audit --no-fund
COPY . .
ENV NODE_OPTIONS=--max-old-space-size=2048
ENV NG_BUILD_MAX_WORKERS=1
RUN npm run build -- --configuration production --build-optimizer=false --optimization=false --source-map=false

FROM nginx:1.27-alpine
COPY --from=build /app/dist/ia-frontend /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
