# Order Execution Engine
Market Order DEX Router with WebSocket Updates
Node.js → Java Spring Boot Implementation

[![Java](https://img.shields.io/badge/Java-17-blue![Spring Boot](https://img.shields.io/badge/Spring%20Boot![MySQL](https://img.shields.io/badge/MySQL-8.0-orange![Redis](https://img.shields.io/badge/Redis-7-red.svg## 🎯 Why Market Orders?

Market orders chosen for immediate execution focus, demonstrating full DEX routing (Raydium vs Meteora) and WebSocket lifecycle without price monitoring complexity. Engine extends to limit orders via price watchers on Redis streams and sniper orders through token launch detection via Solana logs.

# 🚀 Quick Start
bash
# 1. Clone & start infra
git clone <repo>
cd order-execution-engine
docker-compose up -d

# 2. Run application
mvn spring-boot:run

# 3. Test endpoint
curl -X POST http://localhost:8080/api/orders/execute \
-H "Content-Type: application/json" \
-d '{
"tokenIn": "So11111111111111111111111111111111111111112",
"tokenOut": "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v",
"amountIn": 0.1,
"slippage": 0.01
}'
Live Demo: https://order-engine.onrender.com
YouTube Demo: 2-min walkthrough
Postman Collection: src/main/resources/postman/order-execution.postman_collection.json

# 🏗️ Architecture
text
POST /api/orders/execute → OrderService → Redis Queue (10 concurrent) → OrderQueueProcessor
↓
WebSocket Status: pending → routing → building → submitted → confirmed
↓
DEX Router: Raydium(2-3s, 0.3% fee) vs Meteora(2-5% better, 0.2% fee)

# Key Features
DEX Routing: Compares Raydium/Meteora quotes (2-5% variance simulation)

WebSocket Updates: Real-time status streaming per orderId

Concurrent Processing: 10 workers, 100 orders/min via Redis + @Async

Retry Logic: Exponential backoff (max 3 attempts)

Persistence: MySQL order history + Redis active queue

Mock Implementation: Realistic 2-3s delays, price variance

# 📋 API

Endpoint	Method	Description
/api/orders/execute	POST	Submit market order + WS upgrade
/ws/orders	WebSocket	Subscribe: STOMP /app/order/{orderId}

# Sample Request:

json
{
"tokenIn": "SOL",
"tokenOut": "USDC",
"amountIn": 0.1,
"slippage": 0.01
}

# WebSocket Status Flow:

text
pending → routing → building → submitted → confirmed(txHash)
↓ (on error)
failed(error)

# 🧪 Testing

≥10 Tests + Postman Collection:

bash
# Unit/Integration
mvn test

# Load test (3-5 concurrent)
mvn spring-boot:run & npm run load-test
Test Suite	Coverage
Controller (HTTP→WS)	Order submission
DEX Router	Price comparison (edge cases)
Queue Processor	Concurrency + retry
Repository	CRUD + indexing

# 🛠️ Tech Stack

Layer	Technology
Framework	Spring Boot 3.2 + WebFlux
Queue	Redis List (LPOP/RPUSH)
DB	MySQL 8.0 + JPA
WebSocket	STOMP + SockJS
Testing	JUnit 5 + Testcontainers
Deploy	Docker + Render/Railway

# 📁 Project Structure

text
order-execution-engine/
├── pom.xml                    # Maven + Spring Boot
├── docker-compose.yml         # MySQL + Redis
├── src/main/java/...          # 28+ classes (controller→service→repo)
├── src/main/resources/        # application.yml + schema.sql
├── src/test/java/...          # 10+ tests
└── README.md


# 🚀 Deployment

Render/Railway (Free tier):
bash
# Dockerfile
FROM openjdk:17-jre-slim
COPY target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]

# Deploy
git push heroku main  # or Railway
Local Docker:

Local Docker:

bash
docker-compose up -d
docker build -t order-engine .
docker run -p 8080:8080 --link mysql:mysql --link redis:redis order-engine

# 📈 Performance

Metric	Target	Achieved
Concurrency	10 orders	✅ 10 workers
Throughput	100/min	✅ Redis queue
Latency	<5s E2E	✅ 2-3s DEX + 1s TX
Retry	≤3 attempts	✅ Exponential backoff

# 🔧 Troubleshooting

Issue	Solution
MySQL connection	docker-compose up mysql first
Redis queue empty	Check order:queue key
WS not connecting	Enable SockJS: ?transports=websocket
Tests failing	docker-compose up -d before mvn test

# 📄 License
MIT © 2025 - Production-ready order execution engine demo.

Submit 3-5 concurrent orders → Watch WebSocket + logs for DEX routing decisions! 🎉
