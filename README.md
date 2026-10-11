# StoreMate API (Backend)

REST API ฝั่ง Backend ของระบบ **StoreMate** แพลตฟอร์ม E-commerce และระบบจัดการร้านค้า พัฒนาด้วย **Java 21 + Spring Boot 4** ใช้ **PostgreSQL** เป็นฐานข้อมูล ยืนยันตัวตนด้วย **JWT**

> Repository นี้เป็นส่วนหนึ่งของ StoreMate ซึ่งแยกเป็น 3 repo:
> | ส่วน | Repository |
> |---|---|
> | Frontend | [nabnoey/storemate](https://github.com/nabnoey/storemate) |
> | **Backend (repo นี้)** | [JeyZ9/store-mate-api](https://github.com/JeyZ9/store-mate-api) |
> | E2E Test | [BoonruksaBL036/playwright-storemate](https://github.com/BoonruksaBL036/playwright-storemate) |
>
> ภาพรวมของทั้งโปรเจกต์ดูได้ที่ [FamSP/Project_README](https://github.com/FamSP/Project_README)

---

## สารบัญ

1. [เทคโนโลยีที่ใช้](#เทคโนโลยีที่ใช้)
2. [โครงสร้างโปรเจกต์](#โครงสร้างโปรเจกต์)
3. [สิ่งที่ต้องติดตั้งก่อน](#สิ่งที่ต้องติดตั้งก่อน)
4. [ตั้งค่า Environment Variables](#ตั้งค่า-environment-variables)
5. [วิธีรันแบบ Local (Maven)](#วิธีรันแบบ-local-maven)
6. [วิธีรันด้วย Docker Compose](#วิธีรันด้วย-docker-compose)
7. [เอกสาร API (Swagger)](#เอกสาร-api-swagger)
8. [ฐานข้อมูลและ Migration](#ฐานข้อมูลและ-migration)
9. [การทดสอบ](#การทดสอบ)
10. [CI/CD และการ Deploy](#cicd-และการ-deploy)
11. [เชื่อมต่อกับ Frontend](#เชื่อมต่อกับ-frontend)
12. [ปัญหาที่พบบ่อย](#ปัญหาที่พบบ่อย)

---

## เทคโนโลยีที่ใช้

| หมวด | เทคโนโลยี |
|---|---|
| ภาษา / Runtime | Java 21 |
| Framework | Spring Boot 4.0.1 (Web MVC, Validation, WebSocket) |
| Security | Spring Security, JWT (`io.jsonwebtoken:jjwt`), Spring Security Messaging |
| Database | PostgreSQL, Spring Data JPA / Hibernate, Spring JDBC |
| Migration | Flyway (`flyway-core`, `flyway-database-postgresql`) |
| API Docs | springdoc-openapi (Swagger UI) |
| Mapping | ModelMapper, Lombok |
| ชำระเงิน | Stripe (`stripe-java`) |
| ไฟล์/รูปภาพ | Supabase Storage |
| อีเมล | ThaiBulkSMS Email API (ใช้ส่งอีเมลรีเซ็ตรหัสผ่าน) |
| แจ้งเตือน | LINE Messaging API (Webhook) |
| Export | Apache POI (`poi-ooxml`) สำหรับสร้างไฟล์ Excel |
| Build | Maven (มี Maven Wrapper `mvnw`) |
| Test | JUnit / Spring Boot Test, H2 (in-memory DB) |
| DevOps | Docker, Docker Compose, Jenkins, GitHub Actions, SonarCloud, Render |

---

## โครงสร้างโปรเจกต์

```
store-mate-api/
├── .github/workflows/
│   ├── ci.yml                 # รัน mvn test เมื่อเปิด PR เข้า main
│   └── deploy_render.yml      # สั่ง deploy ไป Render เมื่อ push เข้า main
├── .mvn/wrapper/              # Maven Wrapper
├── src/main/java/com/sm/jeyz9/storemateapi/
│   ├── config/                # Security, CORS, WebSocket, Swagger ฯลฯ
│   ├── controllers/           # REST Controllers (ดูรายการด้านล่าง)
│   ├── dto/                   # Request/Response DTO
│   ├── exceptions/            # Custom exception + global handler
│   ├── models/                # JPA Entities
│   ├── repository/            # Spring Data Repositories
│   ├── seeders/               # DatabaseSeeder (ข้อมูลตั้งต้น)
│   ├── services/              # Business logic
│   ├── utils/                 # Helper ต่าง ๆ
│   └── StoreMateApiApplication.java
├── src/main/resources/
│   ├── application.yml        # ค่าหลัก (อ่านจาก env)
│   ├── application-dev.yml    # profile dev
│   ├── application-prod.yml   # profile prod
│   └── db/migration/          # Flyway SQL migration
├── .env.example               # ตัวอย่างไฟล์ env
├── Dockerfile                 # Multi-stage build (Maven → JRE 21)
├── docker-compose.yml         # PostgreSQL + API
├── Jenkinsfile                # Pipeline build/test/sonar/docker/deploy
├── mvnw / mvnw.cmd
└── pom.xml
```

### Controllers หลัก

| Controller | หน้าที่ |
|---|---|
| `AuthController` | สมัครสมาชิก, เข้าสู่ระบบ, ลืม/รีเซ็ตรหัสผ่าน |
| `UserProfileController` | ข้อมูลโปรไฟล์ผู้ใช้, ที่อยู่ |
| `ProductController` | สินค้า ค้นหา/กรอง จัดการสต็อก |
| `CartController` | ตะกร้าสินค้า |
| `OrderController` | คำสั่งซื้อ ติดตามสถานะ |
| `PaymentController` | ชำระเงิน (Stripe / PromptPay / COD) และ Stripe webhook |
| `ReviewController` | รีวิวสินค้า |
| `NotificationController` | การแจ้งเตือน (มี WebSocket/STOMP) |
| `OwnerDashboardController` | Dashboard และรายงานยอดขายของเจ้าของร้าน |
| `UserManagementController` | จัดการผู้ใช้ (ฝั่งแอดมิน) |
| `StoreInfoController` | ข้อมูลร้านค้า |
| `LineWebhookController` | รับ Webhook จาก LINE |

> รายละเอียด endpoint, request/response ทั้งหมดดูได้ที่ Swagger UI (หัวข้อ [เอกสาร API](#เอกสาร-api-swagger))

---

## สิ่งที่ต้องติดตั้งก่อน

| เครื่องมือ | เวอร์ชันที่แนะนำ | หมายเหตุ |
|---|---|---|
| JDK | 21 (เช่น Eclipse Temurin 21) | จำเป็นถ้ารันด้วย Maven |
| Maven | ไม่ต้องติดตั้งเอง | ใช้ `./mvnw` ที่มากับ repo ได้ |
| PostgreSQL | 15+ (compose ใช้ `postgres:18.3-alpine`) | หรือรันผ่าน Docker |
| Docker + Docker Compose | ล่าสุด | ถ้าต้องการรันทั้งหมดด้วย container |
| Git | ล่าสุด | |

บัญชีบริการภายนอก (ใส่เฉพาะที่ต้องการใช้ฟีเจอร์นั้น):
- **Supabase** – สร้าง Storage bucket สำหรับรูปสินค้า / รูปผู้ใช้ / รูปร้าน
- **Stripe** – Secret key และ Webhook secret (ใช้ Test mode ระหว่างพัฒนา)
- **ThaiBulkSMS** – API key/secret และ template สำหรับอีเมลรีเซ็ตรหัสผ่าน
- **LINE Messaging API** – Channel secret และ Channel access token

---

## ตั้งค่า Environment Variables

`application.yml` ตั้งค่า `spring.config.import: optional:file:.env[.properties]` ดังนั้นแอปจะ **อ่านไฟล์ `.env` ที่ root ของโปรเจกต์ให้อัตโนมัติ** เมื่อรันจากโฟลเดอร์นั้น

```bash
cp .env.example .env
```

แล้วแก้ไขค่าใน `.env`:

| ตัวแปร | จำเป็น | คำอธิบาย / ตัวอย่าง |
|---|---|---|
| `JWT_SECRET` | ✅ | คีย์ลับสำหรับเซ็น JWT ควรเป็นสตริงสุ่มยาว ≥ 32 ไบต์ (เช่น สร้างด้วย `openssl rand -base64 64`) |
| `SPRING_PROFILES_ACTIVE` | ✅ | `dev` หรือ `prod` (ค่าเริ่มต้น `dev`) |
| `SPRING_DATASOURCE_URL` | ✅ | `jdbc:postgresql://localhost:5432/store_mate_db` |
| `SPRING_DATASOURCE_USERNAME` | ✅ | ผู้ใช้ PostgreSQL เช่น `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | ✅ | รหัสผ่าน PostgreSQL |
| `FRONTEND_URL` | ✅ | URL ของ Frontend ใช้กับ CORS และลิงก์ในอีเมล เช่น `http://localhost:5173` |
| `THAIBULKEMAIL_KEY` | ⚪ | API key ของ ThaiBulkSMS |
| `THAIBULKEMAIL_SECRET` | ⚪ | API secret ของ ThaiBulkSMS |
| `THAIBULKEMAIL_SEND_URL` | ⚪ | Endpoint ส่งอีเมล |
| `THAIBULKEMAIL_EMAIL_SENDER` | ⚪ | อีเมลผู้ส่ง |
| `THAIBULKEMAIL_RESET_PASSWORD_TEMPLATE_UUID` | ⚪ | UUID ของ template รีเซ็ตรหัสผ่าน |
| `SUPABASE_URL` | ⚪ | `https://<project-ref>.supabase.co` |
| `SUPABASE_KEY` | ⚪ | Service key ของ Supabase |
| `SUPABASE_BUCKET_PRODUCT` | ⚪ | ชื่อ bucket รูปสินค้า |
| `SUPABASE_BUCKET_USER` | ⚪ | ชื่อ bucket รูปผู้ใช้ |
| `SUPABASE_BUCKET_STORE` | ⚪ | ชื่อ bucket รูปร้าน (*มีใน `application.yml` แต่ยังไม่มีใน `.env.example` ให้เพิ่มเอง*) |
| `STRIPE_SECRET_KEY` | ⚪ | `sk_test_...` |
| `STRIPE_WEBHOOK_SECRET` | ⚪ | `whsec_...` |
| `LINE_CHANNEL_SECRET` | ⚪ | LINE channel secret |
| `LINE_CHANNEL_ACCESS_TOKEN` | ⚪ | LINE channel access token |

✅ = ต้องมีจึงจะสตาร์ทได้, ⚪ = จำเป็นเมื่อใช้ฟีเจอร์นั้น

> ⚠️ Spring จะ resolve placeholder `${...}` ตอนสตาร์ท ถ้าตัวแปรใดไม่มีค่าเลยอาจทำให้แอปสตาร์ทไม่ขึ้น หากยังไม่ใช้บริการใด ให้ใส่ค่าว่างหรือค่า dummy ไว้ก่อน
>
> 🔒 ห้าม commit ไฟล์ `.env` ขึ้น Git (มีอยู่ใน `.gitignore` แล้ว)

ค่าคงที่อื่นใน `application.yml` ที่ควรรู้:
- JWT หมดอายุ `604800000` ms (= 7 วัน)
- อัปโหลดไฟล์ได้ไม่เกิน 5 MB ต่อไฟล์ / 25 MB ต่อ request
- Time zone: `Asia/Bangkok`

---

## วิธีรันแบบ Local (Maven)

### 1. Clone

```bash
git clone https://github.com/JeyZ9/store-mate-api.git
cd store-mate-api
```

### 2. เตรียมฐานข้อมูล PostgreSQL

ถ้ามี PostgreSQL ในเครื่องอยู่แล้ว:

```bash
psql -U postgres -c "CREATE DATABASE store_mate_db;"
```

หรือรันเฉพาะ DB ผ่าน Docker Compose:

```bash
docker compose up -d db
```

### 3. ตั้งค่า `.env`

ดูหัวข้อ [ตั้งค่า Environment Variables](#ตั้งค่า-environment-variables)

### 4. รันแอป

macOS / Linux / Git Bash:

```bash
./mvnw spring-boot:run
```

Windows (PowerShell / CMD):

```powershell
.\mvnw.cmd spring-boot:run
```

เมื่อสตาร์ทสำเร็จ API จะอยู่ที่ `http://localhost:8080` และ Flyway จะสร้างตารางให้อัตโนมัติ

### 5. Build เป็นไฟล์ JAR (ทางเลือก)

```bash
./mvnw clean package -DskipTests
java -Duser.timezone=Asia/Bangkok -jar target/*.jar
```

---

## วิธีรันด้วย Docker Compose

`docker-compose.yml` มี 2 service:

| Service | Image | Port | หมายเหตุ |
|---|---|---|---|
| `db` | `postgres:18.3-alpine` | `5432:5432` | DB ชื่อ `store_mate_db`, เก็บข้อมูลใน volume `postgres_data` |
| `store-mate-api` | build จาก `Dockerfile` | `8080:8080` | เชื่อม DB ผ่าน `jdbc:postgresql://db:5432/store_mate_db` |

ขั้นตอน:

1. แก้ `docker-compose.yml` ใส่ค่าที่ว่างอยู่ (อย่างน้อย `JWT_SECRET`) และ **เปลี่ยนรหัสผ่าน DB** ให้ตรงกันทั้ง `db` และ `store-mate-api`
2. รัน

```bash
docker compose up -d --build
```

3. ดู log

```bash
docker compose logs -f store-mate-api
```

4. หยุดระบบ

```bash
docker compose down
```

ลบข้อมูล DB ทั้งหมดด้วย (ระวัง ข้อมูลหายถาวร):

```bash
docker compose down -v
```

> 💡 ใน `docker-compose.yml` ยังไม่มีตัวแปร Stripe, LINE, `SUPABASE_BUCKET_USER` และ `SUPABASE_BUCKET_STORE` ถ้าใช้ฟีเจอร์เหล่านั้นให้เพิ่มเข้าไป หรือเปลี่ยนไปใช้ `env_file: .env` แทนการเขียนค่าไว้ในไฟล์
>
> 🔒 ไฟล์ compose ปัจจุบันมีรหัสผ่าน DB เขียนไว้ตรง ๆ ควรย้ายไปไว้ใน `.env` ก่อนนำไปใช้จริง

### Dockerfile

- **Build stage:** `maven:3.9.9` + Temurin JDK 21 (Alpine) → `mvn clean package -DskipTests`
- **Runtime stage:** Temurin 21 JRE, expose `8080`, รันด้วย `-Duser.timezone=Asia/Bangkok`

Build/Run เอง:

```bash
docker build -t store-mate-api .
docker run -d -p 8080:8080 --env-file .env --name store-mate-api store-mate-api
```

> เมื่อรันใน container แล้วต่อ DB ที่อยู่บนเครื่อง host ให้ใช้ `host.docker.internal` แทน `localhost` ใน `SPRING_DATASOURCE_URL`

---

## เอกสาร API (Swagger)

โปรเจกต์ใช้ springdoc-openapi สแกน package `com.sm.jeyz9.storemateapi.controllers`

| สภาพแวดล้อม | URL |
|---|---|
| Local | `http://localhost:8080/swagger-ui/index.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |
| Production | `https://api.store-mate-api.me/swagger-ui/index.html` |

Base path ของ API คือ **`/api/v1/`** (เช่น `/api/v1/auth`, `/api/v1/products`, `/api/v1/cart`, `/api/v1/reviews`, `/api/v1/moderator`)

### การเรียก endpoint ที่ต้องยืนยันตัวตน

1. เรียก endpoint login ใน `AuthController` เพื่อรับ JWT
2. แนบ header ในทุก request ถัดไป

```
Authorization: Bearer <token>
```

3. ใน Swagger UI กดปุ่ม **Authorize** แล้ววาง token

---

## ฐานข้อมูลและ Migration

- ใช้ **Flyway** อ่านไฟล์จาก `src/main/resources/db/migration`
- `baseline-on-migrate: true`, `baseline-version: 1` จึงใช้กับฐานข้อมูลที่มีอยู่แล้วได้
- Hibernate ตั้ง `ddl-auto: none` → **โครงสร้างตารางมาจาก Flyway เท่านั้น** ห้ามแก้ตารางด้วยมือ
- `DatabaseSeeder` ใน `seeders/` ใส่ข้อมูลตั้งต้นตอนสตาร์ท

### เพิ่ม Migration ใหม่

สร้างไฟล์ใหม่ใน `db/migration` ตามรูปแบบชื่อของ Flyway:

```
V<เลขเวอร์ชันถัดไป>__<คำอธิบาย>.sql
ตัวอย่าง: V12__add_promotion_table.sql
```

> ⚠️ ห้ามแก้ไฟล์ migration ที่รันไปแล้ว ให้สร้างเวอร์ชันใหม่เสมอ ไม่เช่นนั้น Flyway จะ checksum ไม่ตรงและสตาร์ทไม่ได้
>
> ⚠️ profile `dev` ตั้ง `clean-disabled: false` แปลว่าสั่ง `flyway clean` ได้ (ลบทุกตาราง) ห้ามใช้ profile `dev` กับฐานข้อมูล production

Profile `dev` จะแสดง SQL และค่า binding ใน log (`show-sql: true`, `SQL: DEBUG`, `bind: TRACE`) ช่วย debug ได้แต่ log จะเยอะ

---

## การทดสอบ

```bash
./mvnw test
```

- ใช้ Spring Boot Test + H2 in-memory database
- รันทั้ง build + test + รายงานผล:

```bash
./mvnw clean verify
```

---

## CI/CD และการ Deploy

### GitHub Actions

| Workflow | Trigger | ทำอะไร | Secret ที่ต้องตั้ง |
|---|---|---|---|
| `ci.yml` | Pull Request เข้า `main` | ตั้ง Java 21 (Temurin) แล้วรัน `mvn test` | – |
| `deploy_render.yml` | Push เข้า `main` | `curl -X POST` ไปที่ Render Deploy Hook | `RENDER_DEPLOY_HOOK` |

ตั้ง secret ได้ที่ **Settings → Secrets and variables → Actions** ของ repo

### Deploy บน Render

1. สร้าง Web Service บน Render เลือก Runtime เป็น **Docker** (ใช้ `Dockerfile` ของ repo)
2. ใส่ Environment Variables ทั้งหมดตามตาราง และตั้ง `SPRING_PROFILES_ACTIVE=prod`
3. เชื่อม PostgreSQL (Render PostgreSQL หรือ Supabase) แล้วใส่ `SPRING_DATASOURCE_*`
4. คัดลอก **Deploy Hook URL** ไปใส่เป็น secret `RENDER_DEPLOY_HOOK` ใน GitHub
5. ทุกครั้งที่ push เข้า `main` ระบบจะ deploy ใหม่อัตโนมัติ

### Jenkins

`Jenkinsfile` จะทำงานเมื่อมี GitHub push และ **ทำทุก stage เฉพาะ branch `main`**

| Stage | คำสั่งหลัก |
|---|---|
| 1. Build + Test + Sonar | `mvn clean verify sonar:sonar` (SonarCloud org `jeyzdev`, project `jeyzdev_store-mate`) |
| 2. Build & Push Docker | `docker build` / `docker push jeyzdev/store-mate-api:latest` |
| 3. Stop Old Container | `docker stop` / `rm` / `rmi` ของเดิม |
| 4. Deploy | `docker run -d -p 8081:8080 --restart always --env-file $ENV_LOCATION ...` |

Credential ที่ต้องสร้างใน Jenkins:

| Credential ID | ชนิด | ใช้ทำอะไร |
|---|---|---|
| `docker-hub-creds` | Username/Password | Login Docker Hub |
| `sonar-token` | Secret text | SonarCloud token |
| `env-location` | Secret file | ไฟล์ `.env` สำหรับ production |
| `n8n-webhook` | Secret text | URL webhook ของ n8n สำหรับแจ้งผล build |

ผลลัพธ์ (SUCCESS / FAILED / ABORTED) จะถูกส่งไป n8n ทุกครั้ง และ API จะอยู่ที่ port **8081** ของเครื่อง Jenkins

> ถ้า fork ไปใช้เอง อย่าลืมเปลี่ยน `REGISTRY_USER`, SonarCloud project key/organization ให้เป็นของตัวเอง

### Stripe Webhook

ตั้ง endpoint ใน Stripe Dashboard ให้ชี้ไปที่ endpoint webhook ใน `PaymentController` (ดู path ที่ Swagger) แล้วนำ signing secret มาใส่ `STRIPE_WEBHOOK_SECRET`

ทดสอบบนเครื่องด้วย Stripe CLI:

```bash
stripe listen --forward-to localhost:8080/<path-ของ-webhook>
```

### LINE Webhook

ตั้ง Webhook URL ใน LINE Developers Console ให้ชี้ไปที่ endpoint ใน `LineWebhookController` (ต้องเป็น HTTPS ที่เข้าถึงได้จากภายนอก ตอนพัฒนาใช้ ngrok ได้)

---

## เชื่อมต่อกับ Frontend

1. ตั้ง `FRONTEND_URL` ใน API ให้ตรงกับ origin ของ Frontend (เพื่อให้ CORS ผ่าน) เช่น `http://localhost:5173`
2. ตั้ง `VITE_BASE_URL` ใน Frontend ให้ชี้มาที่ API:

```
VITE_BASE_URL=http://localhost:8080/api/v1/
```

ดูวิธีรัน Frontend ได้ที่ [nabnoey/storemate](https://github.com/nabnoey/storemate)

---

## ปัญหาที่พบบ่อย

| อาการ | สาเหตุ / วิธีแก้ |
|---|---|
| `Could not resolve placeholder '...'` | ตัวแปรใน `.env` หายไป ให้เพิ่มให้ครบ (ใส่ค่าว่างได้) และรันคำสั่งจาก root ของโปรเจกต์ |
| `Connection refused` ไปที่ `localhost:5432` | PostgreSQL ยังไม่รัน หรือรัน API ใน Docker แต่ใช้ `localhost` → ใช้ `db` (ใน compose) หรือ `host.docker.internal` |
| `FlywayValidateException: checksum mismatch` | มีการแก้ไฟล์ migration เก่า ให้ revert แล้วสร้างไฟล์ใหม่แทน |
| Frontend เรียก API แล้วติด CORS | `FRONTEND_URL` ไม่ตรงกับ origin จริง (รวม http/https และ port) |
| `401 Unauthorized` | ไม่ได้แนบ `Authorization: Bearer <token>` หรือ token หมดอายุ (7 วัน) |
| `JWT signature does not match` / key สั้นเกิน | `JWT_SECRET` สั้นเกินไปหรือเปลี่ยนค่าแล้ว ให้ login ใหม่ |
| อัปโหลดรูปไม่ได้ | ตรวจ `SUPABASE_URL`, `SUPABASE_KEY`, ชื่อ bucket และขนาดไฟล์ (≤ 5 MB) |
| Port 8080 ถูกใช้ | หยุดโปรแกรมที่ใช้อยู่ หรือรันด้วย `./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081` |

---

## ผู้พัฒนา

- Backend: [@JeyZ9](https://github.com/JeyZ9)
