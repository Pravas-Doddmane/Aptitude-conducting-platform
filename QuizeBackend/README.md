# QuizeBackend

Spring Boot backend for the Quiz Platform. This service handles authentication, quiz management, attempts, certificates, file uploads, analytics, and the AI proctoring/identity-verification flow.

## What is inside

- Spring Boot 3.5.x with Java 17
- PostgreSQL + Spring Data JPA
- JWT-based authentication and role-based access
- Email support for notifications
- File upload support
- AI/proctoring APIs
- Identity verification APIs

## Main modules

- `controller/` - REST API endpoints
- `service/` - business logic
- `repository/` - database access
- `entity/` - JPA entities
- `dto/` - request/response models
- `security/` - JWT and security configuration
- `common/` - enums, exceptions, utilities

## Prerequisites

- Java 17
- Maven 3.6+ or the included Maven wrapper
- PostgreSQL 12+
- IntelliJ IDEA

## Open in IntelliJ IDEA

1. Open **IntelliJ IDEA**
2. Choose **File > Open**
3. Select the `QuizeBackend` folder
4. Wait for Maven to import the project
5. Make sure the project SDK is set to **Java 17**

## Environment Variables

Before running the application, configure the following environment variables:

### Database

```bash
DB_URL=jdbc:postgresql://localhost:5432/quizdb
DB_USERNAME=postgres
DB_PASSWORD=your_password
```

### JWT

```bash
JWT_SECRET=your_jwt_secret
```

### Bootstrap Admin

```bash
BOOTSTRAP_ADMIN_EMAIL=admin@example.com
BOOTSTRAP_ADMIN_PASSWORD=secure_password
```

### Email

For Gmail, you must use an **App Password** instead of your normal Gmail password.

#### How to generate a Gmail App Password

1. Sign in to your Google Account.
2. Go to **Manage your Google Account**.
3. Open the **Security** tab.
4. Enable **2-Step Verification** (required).
5. In the search bar, search for **App Passwords**.
6. Open **App Passwords**.
7. Enter an app name (e.g., `QuizBackend`).
8. Click **Create**.
9. Copy the generated 16-character password.
10. Paste it into `MAIL_PASSWORD`.

```bash
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password
```

> ⚠️ Never use your actual Gmail account password here. Always use a Gmail App Password.


### AI Provider (Optional)

```bash
AI_BASE_URL=http://127.0.0.1:1234
AI_MODEL=gemma-4-e4b-it
```

### CORS

```bash
CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:3000
```

### Upload Directory

```bash
UPLOAD_DIR=uploads
```

## Database setup

The application connects to PostgreSQL by default.

Default values in `src/main/resources/application.properties`:

- Database URL: `jdbc:postgresql://localhost:5432/quizdb`
- Username: `postgres`
- Password: `1234`
- Server port: `8081`

If you need to change the setup, update these values in `src/main/resources/application.properties` or override them with environment variables.

## Before running

1. Start PostgreSQL
2. Create the database:

```sql
CREATE DATABASE quizdb;
```

3. If your schema is out of sync, run the SQL fix/migration scripts included in the backend folder:
   - `FIX_DATABASE.sql`
   - `IDENTITY_VERIFICATION_MIGRATION.sql`
   - `src/main/resources/db/proctoring_migration.sql`

## Run the project

### Option 1: From IntelliJ IDEA

1. Open `QuizeBackendApplication.java`
2. Click **Run**
3. The backend starts on `http://localhost:8081`

### Option 2: From terminal

```bash
cd "C:\Users\prava\Java Developer Projects\QuizApp Complex Project\QuizeBackend"
mvn clean install
mvn spring-boot:run
```

You can also use the wrapper:

```bash
.\mvnw.cmd clean install
.\mvnw.cmd spring-boot:run
```

## Build and test

```bash
mvn test
mvn clean package
```

## Project features

- User login and registration
- Quiz creation and management
- Quiz attempts and result handling
- Certificate generation
- Feedback collection
- Admin dashboard APIs
- AI proctoring session management
- Identity verification during quiz attempts

## AI Proctoring Features

The platform supports competition-mode examination monitoring.

### Current supported features

- Face Detection
- Multiple Face Detection
- Reference Face Capture
- Identity Verification
- Life System
- Warning System
- Proctoring Sessions
- Proctoring Violations
- Risk Assessment
- Auto Submission
- Evidence URL Tracking
- Snapshot URL Tracking

### Competition Quiz Flow

1. User starts competition quiz.
2. Camera permission is requested.
3. User captures a reference image.
4. Reference image is used for identity verification.
5. Python Proctoring Service continuously monitors:
   - Face Presence
   - Multiple Faces
   - Identity Match
6. Violations are recorded through the Spring Boot backend.
7. Lives are deducted according to proctoring rules.
8. Quiz is automatically submitted when configured limits are reached.

### Python Proctoring Service

The backend is designed to integrate with the standalone Python Proctoring Service.

#### Current supported monitoring

- Face Detection
- Multiple Face Detection
- Identity Verification

#### Future supported monitoring

- Phone Detection
- Audio Monitoring
- Looking Away Detection
- Eye Tracking

## Troubleshooting

- **App does not start**: check PostgreSQL is running and `quizdb` exists
- **Database connection error**: verify `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`
- **Port already in use**: change `server.port` in `application.properties`
- **Mail or AI features fail**: verify the related environment variables

## Notes

- The backend uses `ddl-auto=update` for JPA during development.
- Uploaded files are stored in the `uploads/` folder by default.
- If you want to change any runtime setting, `src/main/resources/application.properties` is the first place to check.
- The AI proctoring features are intended to work with the separate Python proctoring service.

