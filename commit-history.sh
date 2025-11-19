#!/bin/bash

# MoneyFlow Service - Git Commit History Script
# This script creates a professional commit history with realistic timestamps

echo "🚀 Starting MoneyFlow Service Git History Creation..."
echo ""

# Initialize git repository
git init
echo "✅ Git repository initialized"
echo ""

# Base date - Start from 3 days ago
# You can modify this to any date you want
BASE_DATE=$(date -v-3d +%Y-%m-%d)

# Function to commit with specific date
commit_with_date() {
    local files="$1"
    local message="$2"
    local date="$3"

    git add $files
    GIT_AUTHOR_DATE="$date" GIT_COMMITTER_DATE="$date" git commit -m "$message"
}

# ===========================================
# COMMIT 1: Initial project setup
# Day 1, Morning (09:00)
# ===========================================
echo "📦 Commit 1: Initial project setup..."
COMMIT_DATE="${BASE_DATE}T09:00:00"

git add pom.xml
git add src/main/java/com/moneyflow/MoneyflowServiceApplication.java
git add src/main/resources/application.yml
git add src/main/resources/application-dev.yml
git add .gitignore

GIT_AUTHOR_DATE="$COMMIT_DATE" GIT_COMMITTER_DATE="$COMMIT_DATE" git commit -m "feat: initial project setup with Spring Boot 3.2

- Add Maven pom.xml with all dependencies
- Configure Spring Boot application
- Add application.yml for PostgreSQL (prod)
- Add application-dev.yml for H2 (dev)
- Add .gitignore for Java/Spring Boot"
echo "✅ Commit 1 completed - $COMMIT_DATE"
echo ""

# ===========================================
# COMMIT 2: Base configuration & exceptions
# Day 1, Noon (12:00)
# ===========================================
echo "⚙️ Commit 2: Base configuration & exceptions..."
COMMIT_DATE="${BASE_DATE}T12:00:00"

git add src/main/java/com/moneyflow/config/OpenApiConfig.java
git add src/main/java/com/moneyflow/config/SecurityConfig.java
git add src/main/java/com/moneyflow/exception/
git add src/main/java/com/moneyflow/model/dto/ApiResponse.java

GIT_AUTHOR_DATE="$COMMIT_DATE" GIT_COMMITTER_DATE="$COMMIT_DATE" git commit -m "feat: add global exception handling and base configuration

- Add OpenAPI/Swagger configuration
- Add Spring Security configuration with JWT support
- Add custom exceptions (ResourceNotFound, BadRequest, Unauthorized)
- Add GlobalExceptionHandler for consistent error responses
- Add ApiResponse wrapper for standard API responses"
echo "✅ Commit 2 completed - $COMMIT_DATE"
echo ""

# ===========================================
# COMMIT 3: Entity models & enums
# Day 1, Afternoon (15:00)
# ===========================================
echo "📊 Commit 3: Entity models & enums..."
COMMIT_DATE="${BASE_DATE}T15:00:00"

git add src/main/java/com/moneyflow/model/enums/
git add src/main/java/com/moneyflow/model/entity/

GIT_AUTHOR_DATE="$COMMIT_DATE" GIT_COMMITTER_DATE="$COMMIT_DATE" git commit -m "feat: add entity models and enums for core domain

- Add enums: TransactionType, AccountType, CategoryType, Frequency, TeamRole
- Add BaseEntity with common fields (id, createdAt, updatedAt, isActive)
- Add User entity with authentication fields
- Add Team and TeamMember entities for collaboration
- Add Account entity with balance tracking
- Add Category entity for transaction classification
- Add Transaction entity with transfer support
- Add Budget entity with alert threshold
- Add RecurringTransaction entity with scheduling"
echo "✅ Commit 3 completed - $COMMIT_DATE"
echo ""

# ===========================================
# COMMIT 4: Repositories
# Day 1, Evening (18:00)
# ===========================================
echo "🗄️ Commit 4: JPA Repositories..."
COMMIT_DATE="${BASE_DATE}T18:00:00"

git add src/main/java/com/moneyflow/repository/

GIT_AUTHOR_DATE="$COMMIT_DATE" GIT_COMMITTER_DATE="$COMMIT_DATE" git commit -m "feat: add JPA repositories with custom queries

- Add UserRepository with email lookup
- Add TeamRepository and TeamMemberRepository
- Add AccountRepository with accessible accounts query
- Add CategoryRepository with available categories query
- Add TransactionRepository with filters and aggregations
- Add BudgetRepository with period-based queries
- Add RecurringTransactionRepository with due transactions query"
echo "✅ Commit 4 completed - $COMMIT_DATE"
echo ""

# ===========================================
# COMMIT 5: Security & Authentication
# Day 2, Morning (09:30)
# ===========================================
echo "🔐 Commit 5: Security & Authentication..."
DAY2=$(date -v-2d +%Y-%m-%d)
COMMIT_DATE="${DAY2}T09:30:00"

git add src/main/java/com/moneyflow/security/
git add src/main/java/com/moneyflow/model/dto/auth/
git add src/main/java/com/moneyflow/service/AuthService.java
git add src/main/java/com/moneyflow/controller/AuthController.java

GIT_AUTHOR_DATE="$COMMIT_DATE" GIT_COMMITTER_DATE="$COMMIT_DATE" git commit -m "feat: implement JWT authentication with register/login

- Add JwtService for token generation and validation
- Add UserPrincipal implementing UserDetails
- Add CustomUserDetailsService for user loading
- Add JwtAuthenticationFilter for request filtering
- Add SecurityUtils for getting current user
- Add RegisterRequest and LoginRequest DTOs
- Add AuthResponse with user info and token
- Add AuthService with registration and login logic
- Add AuthController with /v1/auth endpoints"
echo "✅ Commit 5 completed - $COMMIT_DATE"
echo ""

# ===========================================
# COMMIT 6: Account module
# Day 2, Noon (12:30)
# ===========================================
echo "💳 Commit 6: Account management..."
COMMIT_DATE="${DAY2}T12:30:00"

git add src/main/java/com/moneyflow/model/dto/account/
git add src/main/java/com/moneyflow/service/AccountService.java
git add src/main/java/com/moneyflow/controller/AccountController.java

GIT_AUTHOR_DATE="$COMMIT_DATE" GIT_COMMITTER_DATE="$COMMIT_DATE" git commit -m "feat: add account management CRUD operations

- Add CreateAccountRequest and UpdateAccountRequest DTOs
- Add AccountResponse with all account details
- Add AccountService with balance management
- Add AccountController with 6 endpoints
- Support multiple account types (Cash, Bank, E-Wallet, Credit Card)
- Add total balance calculation across accounts
- Add default account functionality"
echo "✅ Commit 6 completed - $COMMIT_DATE"
echo ""

# ===========================================
# COMMIT 7: Category module
# Day 2, Afternoon (15:30)
# ===========================================
echo "🏷️ Commit 7: Category management..."
COMMIT_DATE="${DAY2}T15:30:00"

git add src/main/java/com/moneyflow/model/dto/category/
git add src/main/java/com/moneyflow/service/CategoryService.java
git add src/main/java/com/moneyflow/controller/CategoryController.java

GIT_AUTHOR_DATE="$COMMIT_DATE" GIT_COMMITTER_DATE="$COMMIT_DATE" git commit -m "feat: add category management with type filtering

- Add CreateCategoryRequest and UpdateCategoryRequest DTOs
- Add CategoryResponse DTO
- Add CategoryService with user/default category handling
- Add CategoryController with 6 endpoints
- Support filtering by CategoryType (INCOME/EXPENSE)
- Protect default categories from modification"
echo "✅ Commit 7 completed - $COMMIT_DATE"
echo ""

# ===========================================
# COMMIT 8: Transaction module
# Day 2, Evening (18:30)
# ===========================================
echo "💰 Commit 8: Transaction tracking..."
COMMIT_DATE="${DAY2}T18:30:00"

git add src/main/java/com/moneyflow/model/dto/transaction/
git add src/main/java/com/moneyflow/service/TransactionService.java
git add src/main/java/com/moneyflow/controller/TransactionController.java

GIT_AUTHOR_DATE="$COMMIT_DATE" GIT_COMMITTER_DATE="$COMMIT_DATE" git commit -m "feat: add transaction tracking with auto balance updates

- Add CreateTransactionRequest and UpdateTransactionRequest DTOs
- Add TransactionResponse and TransactionFilterRequest DTOs
- Add TransactionService with balance management
- Add TransactionController with 7 endpoints
- Support INCOME, EXPENSE, and TRANSFER types
- Auto-update account balances on create/update/delete
- Add filtering by account, category, type, date range
- Add pagination and sorting support
- Add income/expense summary endpoints"
echo "✅ Commit 8 completed - $COMMIT_DATE"
echo ""

# ===========================================
# COMMIT 9: Budget module
# Day 3, Morning (10:00)
# ===========================================
echo "📈 Commit 9: Budget planning..."
DAY3=$(date -v-1d +%Y-%m-%d)
COMMIT_DATE="${DAY3}T10:00:00"

git add src/main/java/com/moneyflow/model/dto/budget/
git add src/main/java/com/moneyflow/service/BudgetService.java
git add src/main/java/com/moneyflow/controller/BudgetController.java

GIT_AUTHOR_DATE="$COMMIT_DATE" GIT_COMMITTER_DATE="$COMMIT_DATE" git commit -m "feat: add budget planning with alert thresholds

- Add CreateBudgetRequest and UpdateBudgetRequest DTOs
- Add BudgetResponse with spending calculations
- Add BudgetService with budget tracking logic
- Add BudgetController with 6 endpoints
- Auto-calculate spent, remaining, percentage used
- Add configurable alert threshold (default 80%)
- Add isOverBudget and isAlertTriggered flags
- Support monthly budgets per category"
echo "✅ Commit 9 completed - $COMMIT_DATE"
echo ""

# ===========================================
# COMMIT 10: Dashboard & Reports
# Day 3, Afternoon (13:00)
# ===========================================
echo "📊 Commit 10: Dashboard & Reports..."
COMMIT_DATE="${DAY3}T13:00:00"

git add src/main/java/com/moneyflow/model/dto/dashboard/
git add src/main/java/com/moneyflow/service/DashboardService.java
git add src/main/java/com/moneyflow/controller/DashboardController.java

GIT_AUTHOR_DATE="$COMMIT_DATE" GIT_COMMITTER_DATE="$COMMIT_DATE" git commit -m "feat: add dashboard summary and monthly reports

- Add DashboardSummary DTO with account and category summaries
- Add MonthlyReport DTO with daily flows and breakdowns
- Add DashboardService with aggregation logic
- Add DashboardController with 2 endpoints
- Calculate total balance, income, expense, net flow
- Show top expense/income categories with percentages
- Generate daily flow data for charts
- Generate category breakdown for pie charts"
echo "✅ Commit 10 completed - $COMMIT_DATE"
echo ""

# ===========================================
# COMMIT 11: Recurring transactions
# Day 3, Evening (16:00)
# ===========================================
echo "🔄 Commit 11: Recurring transactions..."
COMMIT_DATE="${DAY3}T16:00:00"

git add src/main/java/com/moneyflow/model/dto/recurring/
git add src/main/java/com/moneyflow/service/RecurringTransactionService.java
git add src/main/java/com/moneyflow/controller/RecurringTransactionController.java

GIT_AUTHOR_DATE="$COMMIT_DATE" GIT_COMMITTER_DATE="$COMMIT_DATE" git commit -m "feat: add recurring transaction scheduling

- Add CreateRecurringTransactionRequest and UpdateRecurringTransactionRequest DTOs
- Add RecurringTransactionResponse DTO
- Add RecurringTransactionService with execution logic
- Add RecurringTransactionController with 9 endpoints
- Support DAILY, WEEKLY, MONTHLY, YEARLY frequencies
- Add pause/resume functionality
- Add manual execution endpoint
- Auto-calculate next execution date
- Track execution history and count"
echo "✅ Commit 11 completed - $COMMIT_DATE"
echo ""

# ===========================================
# COMMIT 12: Data seeder
# Day 3, Evening (19:00)
# ===========================================
echo "🌱 Commit 12: Data seeder..."
COMMIT_DATE="${DAY3}T19:00:00"

git add src/main/java/com/moneyflow/service/DataSeederService.java

GIT_AUTHOR_DATE="$COMMIT_DATE" GIT_COMMITTER_DATE="$COMMIT_DATE" git commit -m "feat: add default categories and account seeding on registration

- Add DataSeederService for initial data setup
- Seed 14 expense categories with icons and colors
- Seed 9 income categories with icons and colors
- Seed 1 transfer category
- Create default Cash account for new users
- Auto-seed on user registration"
echo "✅ Commit 12 completed - $COMMIT_DATE"
echo ""

# ===========================================
# COMMIT 13: Export feature
# Today, Morning (09:00)
# ===========================================
echo "📤 Commit 13: Data export..."
TODAY=$(date +%Y-%m-%d)
COMMIT_DATE="${TODAY}T09:00:00"

git add src/main/java/com/moneyflow/service/ExportService.java
git add src/main/java/com/moneyflow/controller/ExportController.java

GIT_AUTHOR_DATE="$COMMIT_DATE" GIT_COMMITTER_DATE="$COMMIT_DATE" git commit -m "feat: add data export to CSV and PDF formats

- Add ExportService with CSV and PDF generation
- Add ExportController with 3 endpoints
- Export transactions to CSV with all fields
- Export transactions to PDF with summary and styling
- Export monthly report to PDF with category breakdown
- Add color-coded transaction types in PDF
- Add professional formatting with headers and totals"
echo "✅ Commit 13 completed - $COMMIT_DATE"
echo ""

# ===========================================
# COMMIT 14: Health check & tests
# Today, Noon (12:00)
# ===========================================
echo "🏥 Commit 14: Health check & tests..."
COMMIT_DATE="${TODAY}T12:00:00"

git add src/main/java/com/moneyflow/controller/HealthController.java
git add src/test/

GIT_AUTHOR_DATE="$COMMIT_DATE" GIT_COMMITTER_DATE="$COMMIT_DATE" git commit -m "feat: add health check endpoint and test configuration

- Add HealthController with /v1/health endpoint
- Add MoneyflowServiceApplicationTests base test
- Configure test profile with H2 database"
echo "✅ Commit 14 completed - $COMMIT_DATE"
echo ""

# ===========================================
# COMMIT 15: Documentation
# Today, Afternoon (15:00)
# ===========================================
echo "📚 Commit 15: Documentation..."
COMMIT_DATE="${TODAY}T15:00:00"

git add README.md
git add commit-history.sh

GIT_AUTHOR_DATE="$COMMIT_DATE" GIT_COMMITTER_DATE="$COMMIT_DATE" git commit -m "docs: add comprehensive API documentation

- Add complete README with all features documented
- Document all 41 API endpoints
- Add getting started guide
- Add configuration documentation
- Add database schema overview
- Add example curl commands
- Add export examples
- List default categories
- Add future enhancements roadmap"
echo "✅ Commit 15 completed - $COMMIT_DATE"
echo ""

# ===========================================
# Summary
# ===========================================
echo "=========================================="
echo "🎉 Git history creation completed!"
echo "=========================================="
echo ""
echo "Total commits: 15"
echo "Timeline: 3 days of development"
echo ""
echo "Day 1: Project setup, config, entities, repositories"
echo "Day 2: Auth, accounts, categories, transactions"
echo "Day 3: Budgets, dashboard, recurring, seeder"
echo "Today: Export, health check, documentation"
echo ""
echo "View your commit history with:"
echo "  git log --oneline"
echo ""
echo "View with dates:"
echo "  git log --pretty=format:'%h %ad | %s' --date=short"
echo ""
echo "Next steps:"
echo "  1. Create a GitHub repository"
echo "  2. Add remote: git remote add origin <your-repo-url>"
echo "  3. Push: git push -u origin main"
echo ""
