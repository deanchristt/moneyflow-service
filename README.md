# MoneyFlow Service

A comprehensive money flow tracking REST API built with Spring Boot for managing personal finances, including income, expenses, budgets, and recurring transactions.

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [API Documentation](#api-documentation)
- [Database Schema](#database-schema)
- [API Endpoints](#api-endpoints)

## Features

### Core Features

#### User Authentication
- User registration with email and password
- JWT-based authentication
- Secure password hashing with BCrypt
- Auto-seeding of default categories and account on registration

#### Account Management
- Multiple account types: Cash, Bank, E-Wallet, Credit Card
- Track balance per account
- Set default account
- Custom icon and color for each account
- Calculate total balance across all accounts

#### Category Management
- Separate categories for Income and Expense
- 24 pre-defined default categories on registration
- Create custom categories
- Custom icon and color support
- Protected default categories (cannot be modified/deleted)

#### Transaction Tracking
- Record income, expense, and transfer transactions
- Automatic balance updates on transaction creation/update/delete
- Transfer between accounts
- Filter by account, category, type, date range
- Pagination and sorting support
- Transaction summaries (total income/expense for period)

#### Budget Planning
- Set monthly budgets per category
- Track spending against budget
- Alert threshold (default 80%)
- Auto-calculate: spent amount, remaining, percentage used
- Over-budget and alert indicators

#### Dashboard & Reports
- Dashboard summary with:
  - Total balance across accounts
  - Total income/expense for period
  - Net cash flow
  - Account summaries
  - Top expense/income categories
- Monthly report with:
  - Daily income/expense flows (for charts)
  - Category breakdown with percentages

#### Recurring Transactions
- Schedule recurring income/expenses
- Frequencies: Daily, Weekly, Monthly, Yearly
- Pause/resume functionality
- Manual execution option
- Auto-calculation of next execution date
- Track execution history

#### Data Export
- Export transactions to CSV format
- Export transactions to PDF with summary
- Export monthly financial report to PDF
- Formatted reports with:
  - Income/expense totals
  - Category breakdown
  - Color-coded transaction types
  - Professional styling

### Additional Features

- Soft delete for all entities (data recovery)
- Audit trail (created_at, updated_at)
- Global exception handling
- Input validation
- OpenAPI/Swagger documentation
- Health check endpoints
- Team/Family sharing support (entities ready)

## Tech Stack

- **Java 17**
- **Spring Boot 3.2.0**
- **Spring Security** - Authentication & Authorization
- **Spring Data JPA** - Database operations
- **PostgreSQL** - Production database
- **H2 Database** - Development/testing
- **JWT (jjwt)** - Token-based authentication
- **Lombok** - Reduce boilerplate
- **MapStruct** - DTO mapping
- **SpringDoc OpenAPI** - API documentation
- **OpenCSV** - CSV export
- **OpenPDF** - PDF generation
- **Maven** - Build tool

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- PostgreSQL (for production) or use H2 (for development)

### Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd moneyflow-service
   ```

2. **Run with H2 (Development)**
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```

3. **Run with PostgreSQL (Production)**

   First, create a PostgreSQL database:
   ```sql
   CREATE DATABASE moneyflow;
   ```

   Then run:
   ```bash
   ./mvnw spring-boot:run
   ```

4. **Build JAR**
   ```bash
   ./mvnw clean package
   java -jar target/moneyflow-service-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
   ```

### Access Points

- **API Base URL**: `http://localhost:8080/api`
- **Swagger UI**: `http://localhost:8080/api/swagger-ui.html`
- **H2 Console** (dev only): `http://localhost:8080/api/h2-console`
- **Health Check**: `http://localhost:8080/api/actuator/health`

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_USERNAME` | Database username | postgres |
| `DB_PASSWORD` | Database password | postgres |
| `JWT_SECRET` | JWT signing key (Base64) | (default dev key) |

### Application Properties

```yaml
# Server
server.port: 8080
server.servlet.context-path: /api

# JWT
jwt.expiration: 86400000  # 24 hours

# Database (Production)
spring.datasource.url: jdbc:postgresql://localhost:5432/moneyflow
```

## API Documentation

### Authentication

All endpoints except `/v1/auth/**` require authentication.

**Request Header:**
```
Authorization: Bearer <token>
```

### Quick Start Examples

**Register:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123",
    "firstName": "John",
    "lastName": "Doe"
  }'
```

**Login:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123"
  }'
```

**Create Transaction:**
```bash
curl -X POST http://localhost:8080/api/v1/transactions \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "accountId": 1,
    "categoryId": 1,
    "type": "EXPENSE",
    "amount": 50.00,
    "description": "Lunch",
    "transactionDate": "2024-01-15"
  }'
```

**Get Dashboard:**
```bash
curl "http://localhost:8080/api/v1/dashboard/summary?startDate=2024-01-01&endDate=2024-01-31" \
  -H "Authorization: Bearer <token>"
```

## Database Schema

### Entity Relationship

```
User
 ├── Account (1:N)
 ├── Category (1:N)
 ├── Transaction (1:N)
 ├── Budget (1:N)
 ├── RecurringTransaction (1:N)
 └── TeamMember (1:N)

Team
 ├── TeamMember (1:N)
 └── Account (1:N) [shared accounts]

Account
 └── Transaction (1:N)

Category
 ├── Transaction (1:N)
 ├── Budget (1:N)
 └── RecurringTransaction (1:N)

RecurringTransaction
 └── Transaction (1:N) [generated]
```

### Enums

| Enum | Values |
|------|--------|
| TransactionType | INCOME, EXPENSE, TRANSFER |
| AccountType | CASH, BANK, E_WALLET, CREDIT_CARD |
| CategoryType | INCOME, EXPENSE |
| Frequency | DAILY, WEEKLY, MONTHLY, YEARLY |
| TeamRole | OWNER, ADMIN, MEMBER, VIEWER |

## API Endpoints

### Authentication (2 endpoints)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/v1/auth/register` | Register new user |
| POST | `/v1/auth/login` | Login user |

### Accounts (6 endpoints)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/v1/accounts` | Create account |
| GET | `/v1/accounts` | Get all accounts |
| GET | `/v1/accounts/{id}` | Get account by ID |
| PUT | `/v1/accounts/{id}` | Update account |
| DELETE | `/v1/accounts/{id}` | Delete account |
| GET | `/v1/accounts/total-balance` | Get total balance |

### Categories (6 endpoints)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/v1/categories` | Create category |
| GET | `/v1/categories` | Get all categories |
| GET | `/v1/categories/type/{type}` | Get by type |
| GET | `/v1/categories/{id}` | Get category by ID |
| PUT | `/v1/categories/{id}` | Update category |
| DELETE | `/v1/categories/{id}` | Delete category |

### Transactions (7 endpoints)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/v1/transactions` | Create transaction |
| GET | `/v1/transactions` | Get transactions (filtered) |
| GET | `/v1/transactions/{id}` | Get transaction by ID |
| PUT | `/v1/transactions/{id}` | Update transaction |
| DELETE | `/v1/transactions/{id}` | Delete transaction |
| GET | `/v1/transactions/summary/income` | Get total income |
| GET | `/v1/transactions/summary/expense` | Get total expense |

**Query Parameters for GET /v1/transactions:**
- `accountId` - Filter by account
- `categoryId` - Filter by category
- `type` - INCOME, EXPENSE, TRANSFER
- `startDate` - Start date (yyyy-MM-dd)
- `endDate` - End date (yyyy-MM-dd)
- `page` - Page number (default: 0)
- `size` - Page size (default: 20)
- `sortBy` - Sort field (default: transactionDate)
- `sortDirection` - asc/desc (default: desc)

### Budgets (6 endpoints)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/v1/budgets` | Create budget |
| GET | `/v1/budgets` | Get budgets by month/year |
| GET | `/v1/budgets/year/{year}` | Get budgets by year |
| GET | `/v1/budgets/{id}` | Get budget by ID |
| PUT | `/v1/budgets/{id}` | Update budget |
| DELETE | `/v1/budgets/{id}` | Delete budget |

### Dashboard (2 endpoints)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/v1/dashboard/summary` | Get dashboard summary |
| GET | `/v1/dashboard/monthly-report` | Get monthly report |

### Recurring Transactions (9 endpoints)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/v1/recurring-transactions` | Create recurring transaction |
| GET | `/v1/recurring-transactions` | Get all recurring transactions |
| GET | `/v1/recurring-transactions/active` | Get active only |
| GET | `/v1/recurring-transactions/{id}` | Get by ID |
| PUT | `/v1/recurring-transactions/{id}` | Update |
| DELETE | `/v1/recurring-transactions/{id}` | Delete |
| POST | `/v1/recurring-transactions/{id}/pause` | Pause |
| POST | `/v1/recurring-transactions/{id}/resume` | Resume |
| POST | `/v1/recurring-transactions/{id}/execute` | Execute manually |

### Export (3 endpoints)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/v1/export/transactions/csv` | Export transactions to CSV |
| GET | `/v1/export/transactions/pdf` | Export transactions to PDF |
| GET | `/v1/export/monthly-report/pdf` | Export monthly report to PDF |

**Query Parameters:**
- `startDate` - Start date (yyyy-MM-dd)
- `endDate` - End date (yyyy-MM-dd)
- `month` - Month number (1-12) for monthly report
- `year` - Year for monthly report

### Health (1 endpoint)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/v1/health` | Health check |

**Total: 41 endpoints**

## Response Format

All API responses follow this format:

```json
{
  "success": true,
  "message": "Operation successful",
  "data": { },
  "timestamp": "2024-01-15T10:30:00"
}
```

Error response:
```json
{
  "success": false,
  "message": "Error description",
  "data": null,
  "timestamp": "2024-01-15T10:30:00"
}
```

Validation error:
```json
{
  "success": false,
  "message": "Validation failed",
  "data": {
    "email": "Invalid email format",
    "password": "Password must be at least 8 characters"
  },
  "timestamp": "2024-01-15T10:30:00"
}
```

## Default Categories

### Expense Categories (14)
- Food & Dining
- Transportation
- Shopping
- Entertainment
- Bills & Utilities
- Healthcare
- Education
- Personal Care
- Home
- Gifts & Donations
- Travel
- Insurance
- Taxes
- Other Expense

### Income Categories (9)
- Salary
- Freelance
- Investments
- Rental Income
- Business
- Bonus
- Gifts Received
- Refunds
- Other Income

### Other
- Transfer

## Export Examples

**Export transactions to CSV:**
```bash
curl "http://localhost:8080/api/v1/export/transactions/csv?startDate=2024-01-01&endDate=2024-01-31" \
  -H "Authorization: Bearer <token>" \
  -o transactions.csv
```

**Export transactions to PDF:**
```bash
curl "http://localhost:8080/api/v1/export/transactions/pdf?startDate=2024-01-01&endDate=2024-01-31" \
  -H "Authorization: Bearer <token>" \
  -o transactions.pdf
```

**Export monthly report to PDF:**
```bash
curl "http://localhost:8080/api/v1/export/monthly-report/pdf?month=1&year=2024" \
  -H "Authorization: Bearer <token>" \
  -o monthly_report.pdf
```

### PDF Report Features

- Professional formatting with headers and summaries
- Color-coded transactions (green for income, red for expense)
- Income/expense totals and net flow calculation
- Category breakdown with amounts
- Transaction count summary

## Future Enhancements

- [x] Data export (CSV, PDF)
- [ ] Email notifications for budget alerts
- [ ] Scheduled job for recurring transactions
- [ ] Team/Family sharing implementation
- [ ] Currency conversion support
- [ ] Receipt image upload
- [ ] Mobile app API optimizations

## License

This project is for educational and portfolio purposes.

## Author

MoneyFlow Service - Personal Finance Tracking API
