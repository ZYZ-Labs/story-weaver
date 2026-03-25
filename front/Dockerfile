FROM node:20-alpine AS build

WORKDIR /app/front

COPY front/package.json front/package-lock.json ./
RUN npm ci

COPY front/ ./

ARG VITE_API_BASE_URL=/api
ARG VITE_API_PROXY_TARGET=http://localhost:8080
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
ENV VITE_API_PROXY_TARGET=${VITE_API_PROXY_TARGET}

RUN npm run build

FROM nginx:1.27-alpine AS runtime

COPY nginx/nginx.conf /etc/nginx/nginx.conf
COPY --from=build /app/front/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
