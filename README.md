# POS Payment Service

This project implements a gRPC-based backend service to handle multiple payment methods for a POS-integrated e-commerce system using Java Spring Boot, PostgreSQL, Redis, and MyBatis-Plus.

---

## 📦 Prerequisites

- Java 21
- Maven 3.8+
- PostgreSQL (port: `5432`)
- Redis (port: `6379`)
- Git

---

## ⚙️ Configuration

Set up your `application-local-test.yml` with the following content:

```properties
# Write Datasource
spring.datasource.write.jdbc-url=jdbc:postgresql://<host>:<port>/<database>?useUnicode=true&characterEncoding=utf8
spring.datasource.write.username=<user name>
spring.datasource.write.password=<password>
spring.datasource.write.hikari.maximum-pool-size=50

# Read Datasource
spring.datasource.read.jdbc-url=jdbc:postgresql://<host>:<port>/<database>?useUnicode=true&characterEncoding=utf8
spring.datasource.read.username=<user name>
spring.datasource.read.password=<password>
spring.datasource.read.hikari.maximum-pool-size=50

# Redis Configuration
spring.data.redis.host=<host>
spring.data.redis.port=<port>
spring.data.redis.jedis.pool.max-active=0
redis.key.pre=<key>
```

---

## 🚀 Run the App


### Build and run with custom command

First, build the JAR file:

```bash
mvn clean install
```

Then, prepare postgreSQL:

    schema   src/main/resources/db/schema/1.0.0.sql

    data     src/main/resources/db/data/1.0.0.sql


Next run the service with:

```bash
nohup /usr/local/jdk-21/bin/java \
  -Dlogback_path=/data/logs/anymind/pos \
  -Xms1024M -Xmx2048M \
  -Dspring.profiles.active=local-test \
  -jar service-0.0.1.jar \
   > /dev/null 2>&1 &
```

---

## 🧪 Run Tests

Execute all unit and integration tests using:

```bash
mvn clean test
```

---

## 📦 Build the Project

To package the application into a JAR:

```bash
mvn clean install
```

The JAR will be available at:

```
target/service-0.0.1.jar
```

---
