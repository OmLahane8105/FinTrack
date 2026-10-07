# FinTrack

A full-stack personal finance management platform built with Spring Boot, PostgreSQL, React, TypeScript, Spring Security, JWT, OAuth2, and Spring AI.

FinTrack helps users manage accounts, transactions, budgets, savings goals, recurring transactions, reports, notifications, and AI-powered financial insights.

## Features

### Authentication & Security

* User registration and login
* Email verification
* JWT access and refresh tokens
* Automatic refresh-token flow
* Secure password hashing with BCrypt
* Role-based authorization
* Google OAuth2 login
* Logout support
* User-specific financial data isolation
* CORS configuration for production frontend/backend separation

### Personal Finance Management

* Bank, savings, cash, credit-card and wallet accounts
* Income and expense transactions
* Transaction categories
* Budgets and spending tracking
* Financial goals
* Recurring transactions
* Dashboard financial summaries
* Search and transaction filters

### Reports & Exports

* Monthly income, expense and savings reports
* Spending-by-category analysis
* Savings-rate calculations
* Monthly financial overview
* CSV report export
* PDF report export

### AI Features

FinTrack AI uses Google Gemini through Spring AI.

#### AI Financial Assistant

The assistant answers finance-related questions using the authenticated user's supplied FinTrack financial context.

The AI is designed to:

* use only available user financial data
* avoid inventing financial values
* explain calculations
* provide concise financial guidance
* keep user data isolated

#### AI Financial Insights

Financial Insights generate:

* an overall financial snapshot
* important spending and savings observations
* practical recommendations

The insights are returned as structured JSON and displayed directly in the frontend.

### Notifications

* Notification management
* Read/unread state
* Scheduled notification-related processing

### Recurring Transactions

Recurring financial entries are processed through scheduled backend jobs.

## Tech Stack

### Backend

* Java 23
* Spring Boot 4.1
* Spring MVC
* Spring Data JPA
* Hibernate
* Spring Security
* JWT
* OAuth2 Client
* Spring AI
* Google Gemini
* Jakarta Mail
* PostgreSQL
* Maven
* JUnit 5
* Mockito
* Apache PDFBox
* Docker

### Frontend

* React
* TypeScript
* Vite
* Axios
* React Router
* Recharts

## Architecture

```text
React + TypeScript Frontend
            |
            | REST / JSON
            v
Spring Boot REST API
            |
    +-------+--------+----------------+
    |                |                |
Spring Security    JPA/Hibernate    Spring AI
JWT + OAuth2          |                |
    |                 v                v
    |             PostgreSQL       Google Gemini
    |
    v
Email Verification
      |
      v
   SMTP Provider
```

## Project Structure

```text
FinTrack
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com.fintrack
│   │   │       ├── config
│   │   │       ├── controller
│   │   │       ├── dto
│   │   │       ├── entity
│   │   │       ├── exception
│   │   │       ├── repository
│   │   │       ├── security
│   │   │       └── service
│   │   └── resources
│   │       └── application.properties
│   └── test
│       └── java
├── Dockerfile
├── docker-compose.yml
├── pom.xml
└── .env.example
```

## Local Development

### Requirements

* Java 23
* Maven
* PostgreSQL
* Node.js and npm
* Docker Desktop (optional)

### Backend Configuration

Create a local environment configuration using the variables shown in `.env.example`.

Do not commit real credentials, API keys, OAuth secrets, JWT secrets, database passwords, or SMTP credentials.

Typical configuration includes:

```text
DB_PASSWORD
JWT_SECRET
GEMINI_API_KEY
GOOGLE_CLIENT_ID
GOOGLE_CLIENT_SECRET
FRONTEND_URL
MAIL_FROM
MAIL_HOST
MAIL_PORT
MAIL_USERNAME
MAIL_PASSWORD
```

The application also supports environment-based database configuration for deployment.

### Run the Backend

```bash
mvn spring-boot:run
```

The backend runs on port `8080` by default.

### Run Tests

```bash
mvn clean test
```

Current automated test suite:

```text
303 tests
0 failures
0 errors
```

### Build

```bash
mvn clean package
```

## Docker

Build and run the complete local environment with Docker Compose:

```bash
docker compose up --build
```

This runs:

* PostgreSQL
* Spring Boot backend
* React frontend

The Docker configuration keeps credentials and secrets outside the image through environment variables.

## Production Deployment

FinTrack is deployed as separate frontend and backend services.

### Backend

* Spring Boot application
* Docker deployment
* PostgreSQL hosted on Neon
* Environment-based secrets
* Production CORS configuration
* Health endpoint
* Automatic deployment from Git

### Frontend

* React/Vite static deployment
* Production API URL configured through `VITE_API_URL`
* React Router rewrite configured for direct route access
* Automatic deployment from Git

## Production Verification

The deployed application has been verified for:

* Registration
* Email verification
* Password login
* Logout
* Google OAuth2 login
* Account create/edit/delete
* Transaction management
* Search and filters
* Budgets
* Goals
* Recurring transactions
* Reports and charts
* CSV export
* PDF export
* AI Assistant
* AI Financial Insights
* Notifications behavior locally
* PostgreSQL persistence
* Production frontend/backend communication

## Security

FinTrack follows several security practices:

* Passwords are hashed with BCrypt
* JWT access and refresh tokens are used for API authentication
* OAuth2 is used for Google authentication
* Secrets are supplied through environment variables
* `.env` files are excluded from Git
* Financial context is scoped to the authenticated user
* AI prompts explicitly treat user financial data as untrusted data
* Production CORS allows the configured frontend origin

## Testing

The backend uses JUnit 5 and Mockito for service-level testing.

Tests cover areas including:

* Authentication
* Token handling
* Account operations
* Transaction operations
* Categories
* Budgets
* Goals
* Recurring transactions
* Reports
* AI service behavior
* Financial Insights JSON parsing and failure handling

Current status:

```text
303 tests passing
```

## Future Improvements

Potential improvements include:

* More granular frontend code splitting
* Additional integration and end-to-end tests
* Broader notification scenarios in production
* Expanded analytics
* More advanced AI financial planning features
* Additional deployment monitoring and observability
