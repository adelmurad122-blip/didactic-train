# EGX‑AI Platform — Institutional Market Intelligence Engine

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)](#)
[![Java](https://img.shields.io/badge/Java-17-orange)](#)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green)](#)
[![License](https://img.shields.io/badge/license-Proprietary-red)](#)

EGX‑AI هو نظام مؤسسي متكامل لتحليل سوق المال المصري (EGX)، مبني على معمارية
**Microservices** حديثة باستخدام Spring Boot 3، Spring Cloud، Apache Kafka، وKeycloak.

المنصة توفر بيانات لحظية، تحليل موجي (NeoWave)، توصيات ذكاء اصطناعي،
وتنبيهات تلقائية — مع قابلية توسّع أفقي عالية وصفر-وقت-توقف أثناء النشر.

---

## جدول المحتويات

- [المميزات الأساسية](#-المميزات-الأساسية)
- [مكونات النظام](#-مكونات-النظام)
- [المعمارية](#-المعمارية)
- [متطلبات التشغيل](#-متطلبات-التشغيل)
- [التثبيت السريع](#-التثبيت-السريع)
- [المصادقة والأمان](#-المصادقة-والأمان)
- [الـ API Reference](#-api-reference)
- [الاختبارات](#-الاختبارات)
- [المراقبة والـ Observability](#-المراقبة-والـ-observability)
- [متغيرات البيئة](#-متغيرات-البيئة)
- [هيكل المشروع](#-هيكل-المشروع)

---

## ✨ المميزات الأساسية

- **أسعار لحظية** — بيانات أسهم EGX المباشرة + OHLCV تاريخية عبر TimescaleDB
- **تحليل موجي NeoWave** — محرك تحليل تقني مبني بـ Python (FastAPI)
- **توصيات بالذكاء الاصطناعي** — نماذج HuggingFace عبر DJL داخل Java
- **أخبار السوق** — جمع + تحليل محتوى + ترتيب بحسب الصلة
- **تنبيهات تلقائية** — بريد إلكتروني عند تحرك الأسعار أو صدور أخبار مهمة
- **مصادقة OAuth2/OIDC** — Keycloak بصلاحيات متدرجة (ROLE_USER، ROLE_ADMIN)
- **Event‑Driven** — Apache Kafka لنقل الأحداث بين الخدمات
- **API Gateway موحّد** — نقطة دخول واحدة مع JWT Validation + Rate Limiting
- **Service Discovery** — Eureka للتسجيل والاكتشاف التلقائي
- **Observability كاملة** — Prometheus + Grafana + Zipkin Tracing
- **CI‑Ready** — Docker + Docker Compose + تغطية اختبارات 85%

---

## 🧱 مكونات النظام

| الخدمة | المنفذ | التقنية | الوظيفة |
|--------|--------|---------|---------|
| **API Gateway** | 8080 | Spring Cloud Gateway | نقطة الدخول + JWT + Routing |
| **Eureka Server** | 8761 | Spring Cloud Netflix | اكتشاف الخدمات |
| **Stock Service** | داخلي | Spring Boot + TimescaleDB | أسعار + OHLCV |
| **News Service** | داخلي | Spring Boot + PostgreSQL | أخبار + سجل القراءة |
| **Scraping Service** | داخلي | Spring Boot + Jsoup | جمع البيانات |
| **Recommendation Svc** | داخلي | Spring Boot + DJL + HuggingFace | توصيات المقالات |
| **Notification Service** | داخلي | Spring Boot + JavaMail | بريد + تنبيهات |
| **NeoWave Engine** | 8000 | Python FastAPI | تحليل موجي + Backtest |
| **Keycloak** | 8081 | Keycloak 23 | OAuth2 / OIDC |
| **Apache Kafka** | 9092 | Kafka + Zookeeper | Message Broker |
| **TimescaleDB** | 5432 | PostgreSQL + TimescaleDB | بيانات الأسعار |
| **Redis** | 6379 | Redis 7 | Cache + Rate Limiting |

---

## 🏗️ المعمارية

```
                        ╔══════════════════════╗
                        ║       Keycloak       ║
                        ║  OAuth2/OIDC Server  ║
                        ║  Port: 8081          ║
                        ╚══════════╤═══════════╝
                                   │  JWT issuance & JWKS
                                   │
                        ╔══════════▼═══════════╗
                        ║     API Gateway      ║
                        ║  Spring Cloud Gwy    ║
                        ║  Port: 8080          ║
                        ║  • JWT Validation    ║
                        ║  • Rate Limiting     ║
                        ║  • CORS              ║
                        ║  • Correlation-ID    ║
                        ╚══════════╤═══════════╝
                                   │  lb:// (Eureka)
           ┌───────────────────────┼──────────────────────────┐
           │                       │                          │
           ▼                       ▼                          ▼
╔══════════════════╗  ╔════════════════════╗  ╔════════════════════╗
║  Stock Service   ║  ║   News Service     ║  ║  Scraping Service  ║
║  /api/v1/stocks  ║  ║  /api/v1/news      ║  ║  /api/v1/scraping  ║
║  TimescaleDB     ║  ║  PostgreSQL        ║  ║  External Sources  ║
╚════════╤═════════╝  ╚══════════╤═════════╝  ╚════════╤═══════════╝
         │                       │                      │
         └───────────────────────┴──────────────────────┘
                                 │
                    ╔════════════▼═════════════╗
                    ║      Apache Kafka        ║
                    ║  Topics:                 ║
                    ║  • stock.price.updated   ║
                    ║  • news.published        ║
                    ║  • alert.triggered       ║
                    ╚════════════╤═════════════╝
                                 │
           ┌─────────────────────┼───────────────────────┐
           │                     │                       │
           ▼                     ▼                       ▼
╔══════════════════╗  ╔══════════════════╗  ╔════════════════════╗
║  Recommendation  ║  ║  Notification    ║  ║  NeoWave Engine    ║
║  Service         ║  ║  Service         ║  ║  (Python/FastAPI)  ║
║  HuggingFace+DJL ║  ║  Email Alerts    ║  ║  Port: 8000        ║
╚══════════════════╝  ╚══════════════════╝  ╚════════════════════╝
           │
           ▼
╔══════════════════════════════════════════════════════╗
║                  Shared Infrastructure               ║
║  ┌────────────┐  ┌──────────────┐  ┌─────────────┐  ║
║  │  Eureka    │  │    Redis 7   │  │  Prometheus │  ║
║  │  Port 8761 │  │  Port 6379   │  │  + Grafana  │  ║
║  └────────────┘  └──────────────┘  └─────────────┘  ║
╚══════════════════════════════════════════════════════╝
```

### تدفق الطلب (Request Flow)

```
Client
  │
  │  HTTPS + Bearer <JWT>
  ▼
API Gateway (8080)
  ├─ [1] JWT Signature Verified (local JWKS cache from Keycloak)
  ├─ [2] Rate Limit Check (Redis token bucket)
  ├─ [3] X-Correlation-ID injected
  ├─ [4] Route resolved via Eureka (lb://)
  │
  ▼
Microservice
  ├─ [5] JWT re-validated as Resource Server (claims/roles)
  ├─ [6] Business logic executed
  └─ [7] Kafka event published (if state changes)
```

---

## ⚙️ متطلبات التشغيل

| الأداة | الإصدار الأدنى |
|--------|----------------|
| Docker | 24.x |
| Docker Compose | 2.x (V2 plugin) |
| Java JDK | 17 (للبناء المحلي) |
| Maven | 3.8+ (للبناء المحلي) |

---

## 🚀 التثبيت السريع

### 1. استنساخ المستودع

```bash
git clone https://github.com/your-org/egx-ai.git
cd egx-ai
```

### 2. إعداد متغيرات البيئة

```bash
cp .env.example .env
# افتح .env وأضف القيم الحقيقية
nano .env
```

### 3. تشغيل الخدمات

```bash
# تشغيل البنية التحتية أولاً (Kafka + DB + Keycloak)
docker compose up -d keycloak timescaledb redis kafka zookeeper

# انتظر حتى يصبح Keycloak جاهزاً (~30 ثانية)
docker compose logs -f keycloak | grep "started in"

# تشغيل باقي الخدمات
docker compose up -d
```

### 4. التحقق من التشغيل

```bash
# Gateway health check
curl http://localhost:8080/actuator/health

# Eureka dashboard
open http://localhost:8761

# Keycloak Admin Console
open http://localhost:8081
# Username: admin | Password: admin (قيم .env)
```

### 5. Smoke Test

```bash
# الحصول على JWT Token من Keycloak
TOKEN=$(curl -s -X POST \
  http://localhost:8081/realms/EGX_ALPHA/protocol/openid-connect/token \
  -d "grant_type=password&client_id=egx-client&username=testuser&password=test123" \
  | jq -r .access_token)

# استدعاء API عبر Gateway
curl -H "Authorization: Bearer $TOKEN" \
     http://localhost:8080/api/v1/stocks/EFG
```

---

## 🔐 المصادقة والأمان

### مخطط OAuth2

```
Client ──► Keycloak (8081)
             │  POST /token (client_credentials or password)
             │  ← { access_token: "eyJ..." }
             │
Client ──► Gateway (8080)
             │  Authorization: Bearer eyJ...
             │  JWT verified locally (JWKS cached)
             │
Gateway ──► Microservice
             │  X-Forwarded-* headers + original JWT
             │  Service validates token again (defense-in-depth)
```

### Realm & Client إعداد Keycloak

| الإعداد | القيمة |
|---------|--------|
| Realm | `EGX_ALPHA` |
| Client ID | `egx-gateway-client` |
| Grant Types | `client_credentials`, `password`, `authorization_code` |
| Roles | `ROLE_USER`, `ROLE_ANALYST`, `ROLE_ADMIN` |

### Issuer URI

```
http://keycloak:8080/realms/EGX_ALPHA
```

> **تنبيه:** يجب أن تتطابق هذه القيمة مع `iss` داخل كل JWT صادر.

---

## 📡 API Reference

كل الطلبات تمر عبر Gateway على المنفذ `8080`.

### Stocks

| Method | Endpoint | الوصف |
|--------|----------|-------|
| GET | `/api/v1/stocks` | قائمة الأسهم مع آخر سعر |
| GET | `/api/v1/stocks/{symbol}` | تفاصيل سهم محدد |
| GET | `/api/v1/stocks/{symbol}/ohlcv` | بيانات شمعدانية تاريخية |
| GET | `/api/v1/stocks/{symbol}/analysis` | تحليل NeoWave |

### News

| Method | Endpoint | الوصف |
|--------|----------|-------|
| GET | `/api/v1/news` | آخر الأخبار |
| GET | `/api/v1/news/{id}` | خبر محدد |
| POST | `/api/v1/news/{id}/read` | تسجيل القراءة |

### Recommendations

| Method | Endpoint | الوصف |
|--------|----------|-------|
| GET | `/api/v1/recommendations` | توصيات للمستخدم الحالي |

### Swagger UI

```
http://localhost:<service-port>/swagger-ui.html
```

---

## 🧪 الاختبارات

```bash
# تشغيل جميع الاختبارات
mvn test

# تشغيل اختبارات خدمة محددة
cd gateway && mvn test

# تقرير تغطية الكود (JaCoCo)
mvn verify
open target/site/jacoco/index.html

# Smoke Test كامل للمنظومة
python3 scripts/smoke_test_gateway.py \
  --base-url http://localhost:8080 \
  --token-url http://localhost:8081/realms/EGX_ALPHA/protocol/openid-connect/token
```

**هدف التغطية:** 85% على مستوى السطر.

---

## 📊 المراقبة والـ Observability

| الأداة | الرابط | الوظيفة |
|--------|--------|---------|
| Prometheus | http://localhost:9090 | جمع المقاييس |
| Grafana | http://localhost:3001 | لوحات المراقبة |
| Zipkin | http://localhost:9411 | Distributed Tracing |
| Eureka | http://localhost:8761 | Service Registry |

### المقاييس الرئيسية (Prometheus)

```promql
# معدل الطلبات على Gateway
rate(gateway_requests_seconds_count[1m])

# نسبة الأخطاء
rate(gateway_requests_seconds_count{status="5xx"}[5m]) /
rate(gateway_requests_seconds_count[5m])

# متوسط وقت الاستجابة
histogram_quantile(0.95, gateway_requests_seconds_bucket)
```

---

## 🌍 متغيرات البيئة

| المتغير | القيمة الافتراضية | الوصف |
|---------|-------------------|-------|
| `KEYCLOAK_ISSUER_URI` | `http://keycloak:8080/realms/EGX_ALPHA` | Keycloak Realm URI |
| `EUREKA_URI` | `http://eureka:8761/eureka/` | Eureka Service URL |
| `REDIS_URL` | `redis://redis:6379` | Redis Connection |
| `KAFKA_BOOTSTRAP` | `kafka:9092` | Kafka Brokers |
| `DB_URL` | `jdbc:postgresql://timescaledb:5432/egxai` | Database URL |
| `DB_USERNAME` | `egxai` | Database User |
| `DB_PASSWORD` | — | **يجب تعيينه** |
| `KEYCLOAK_ADMIN_PASSWORD` | — | **يجب تعيينه** |

---

## 📂 هيكل المشروع

```
egx-ai/
├── docker-compose.yml          # تعريف كل الخدمات
├── .env.example                # قالب متغيرات البيئة
├── README.md
│
├── gateway/                    # API Gateway (Spring Cloud Gateway)
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/
│       └── main/
│           ├── java/com/egxai/gateway/
│           │   ├── GatewayApplication.java
│           │   └── config/
│           │       ├── SecurityConfig.java
│           │       └── CorrelationIdFilter.java
│           └── resources/
│               └── application.yml
│
├── eureka-server/              # Service Discovery
├── stock-service/              # Stocks + OHLCV
├── news-service/               # News + Reading History
├── scraping-service/           # Data Collection
├── recommendation-service/     # AI Recommendations
├── notification-service/       # Alerts + Email
│
├── neowave-engine/             # Python FastAPI — NeoWave Analysis
│   ├── main.py
│   ├── worker.py
│   └── backend/
│
└── scripts/
    ├── smoke_test_gateway.py
    └── seed_keycloak.sh
```

---

## 🤝 المساهمة

1. Fork → Branch باسم وصفي (`feat/add-rsi-indicator`)
2. اكتب اختباراتك أولاً (TDD)
3. تأكد من تغطية 85%: `mvn verify`
4. افتح Pull Request مع وصف واضح

---

## 📬 التواصل

هذا المشروع جزء من منظومة **EGX‑AI** للتحليل المؤسسي لسوق المال المصري.

> يُحظر إعادة توزيع هذا الكود دون إذن كتابي صريح.
