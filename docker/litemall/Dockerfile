# Stage 1: Build litemall-admin
FROM node:14 AS admin-builder
WORKDIR /app/litemall-admin
COPY litemall-admin/package*.json ./
RUN npm install --registry=https://registry.npmmirror.com
COPY litemall-admin/ ./
RUN npm run build:dep

# Stage 2: Build litemall-vue
FROM node:14 AS vue-builder
WORKDIR /app/litemall-vue
COPY litemall-vue/package*.json ./
RUN npm install --registry=https://registry.npmmirror.com
COPY litemall-vue/ ./
RUN npm run build:dep

# Stage 3: Build java application
FROM maven:3-jdk-8-slim AS maven-builder
WORKDIR /app
COPY pom.xml ./
COPY litemall-core ./litemall-core
COPY litemall-db ./litemall-db
COPY litemall-wx-api ./litemall-wx-api
COPY litemall-admin-api ./litemall-admin-api
COPY litemall-all ./litemall-all
COPY litemall-all-war ./litemall-all-war

# Copy built admin and vue static files from builders into respective directories
COPY --from=admin-builder /app/litemall-admin/dist ./litemall-admin/dist
COPY --from=vue-builder /app/litemall-vue/dist ./litemall-vue/dist

RUN mvn clean package -Dmaven.test.skip=true

# Stage 4: Final runtime image
FROM eclipse-temurin:8-jre
WORKDIR /app
COPY docker/litemall/application.yml ./application.yml
COPY --from=maven-builder /app/litemall-all/target/litemall-all-*-exec.jar ./litemall.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","litemall.jar"]