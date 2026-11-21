================================================================================
INSTALLMENT SCHEDULE GENERATOR - BACKEND ARCHITECTURE & SPECIFICATION
Technology Stack: Java 17, Spring Boot 3.2.5, Spring Security 6, Spring Data JPA,
                  Hibernate 6, MySQL 8.4, JSON Web Token (JWT), BCrypt, Maven
================================================================================

TABLE OF CONTENTS
--------------------------------------------------------------------------------
1. Executive Summary & Purpose
2. High-Level System Architecture
3. Core Backend Modules
4. Algorithms & Mathematical Models
5. Comprehensive List of Files & Internal Workings
6. Database Schema, Relationships & Cascades
7. REST API Specifications
8. Security, Session & Rate-Limiting Mechanics
9. How to Build & Run


================================================================================
1. EXECUTIVE SUMMARY & PURPOSE
================================================================================
The Installment Schedule Generator is an automated financial loan amortization
and schedule generation system. It allows customers and administrators to calculate
loan repayment structures, generate granular period-by-period installment
schedules, download CSV reports, print formal statements, and manage users.

The backend is built in enterprise-grade Java using Spring Boot 3, providing a
stateless, secure, multi-tier REST API that interfaces with a MySQL database.


================================================================================
2. HIGH-LEVEL SYSTEM ARCHITECTURE
================================================================================
The backend follows a strict 3-Tier Layered Architecture with Separation of Concerns:

  [Client: Browser (HTML5 / Vanilla JS)]
                 │
                 ▼  (HTTP / JSON with HttpOnly JWT Cookie)
  ┌────────────────────────────────────────────────────────┐
  │ 1. PRESENTATION & SECURITY LAYER                       │
  │    - JwtAuthFilter (Token extraction & authentication) │
  │    - SecurityFilterChain (CORS, Role-based RBAC)       │
  │    - RestControllers (Request validation & routing)    │
  └──────────────────────────┬─────────────────────────────┘
                             │
                             ▼
  ┌────────────────────────────────────────────────────────┐
  │ 2. BUSINESS LOGIC & SERVICE LAYER                      │
  │    - InstallmentCalculator (EMI & Amortization math)   │
  │    - LoanService (Loan creation, queries, validation)  │
  │    - AuthService (Registration, BCrypt auth, tokens)   │
  │    - AdminService (System stats, user/loan management) │
  └──────────────────────────┬─────────────────────────────┘
                             │
                             ▼
  ┌────────────────────────────────────────────────────────┐
  │ 3. DATA ACCESS LAYER (Persistence / ORM)               │
  │    - Spring Data JPA Repositories                      │
  │    - Hibernate ORM Entities (User, Loan, Schedule)     │
  │    - MySQL 8.4 Database Server (Port 3306)             │
  └────────────────────────────────────────────────────────┘


================================================================================
3. CORE BACKEND MODULES
================================================================================
1. Security & Authentication Module:
   - Manages user identity, registration, password hashing, and role checks.
   - Generates and verifies HMAC-SHA256 JWT tokens delivered via HttpOnly cookies.
   - Enforces Role-Based Access Control (RBAC): 'ROLE_USER' and 'ROLE_ADMIN'.
   - Mitigates brute-force attacks via an in-memory sliding-window rate limiter.

2. Loan & Schedule Engine Module:
   - Validates user input bounds (Principal, Rate, Tenure, Frequency, Dates).
   - Computes standard EMI using reducing-balance formulas.
   - Builds complete schedule rows showing due date, EMI, principal, interest,
     and remaining balance.
   - Guarantees exact 0.00 final balance absorption.

3. Admin Management Module:
   - Aggregates system metrics (registered user counts, total schedules generated).
   - Provides administrative control: elevate/demote user roles, delete accounts,
     and view/delete all customer loan records.
   - Protects against admin self-demotion and self-account deletion.

4. Persistence & ORM Module:
   - Object-Relational Mapping using Hibernate / Jakarta Persistence (JPA).
   - Manages entity lifecycle, foreign keys (`ON DELETE CASCADE`), and custom JPQL queries.

5. Global Error Handling & Validation Module:
   - Intercepts bad inputs, authentication failures, missing resources, and exceptions.
   - Normalizes error outputs into clean, unified JSON responses: `{"error": "..."}`.


================================================================================
4. ALGORITHMS & MATHEMATICAL MODELS
================================================================================

1. EQUAL MONTHLY INSTALLMENT (EMI) FORMULA (Reducing Balance Amortization)
   Formula:
                 P × R × (1 + R)^N
          EMI = ───────────────────
                   (1 + R)^N - 1

   Where:
   - P = Principal Loan Amount (in INR)
   - R = Periodic Interest Rate = (Annual Rate % / 100) / PeriodsPerYear
         * Weekly:    PeriodsPerYear = 52
         * Monthly:   PeriodsPerYear = 12
         * Quarterly: PeriodsPerYear = 4
         * Yearly:    PeriodsPerYear = 1
   - N = Total Number of Installments
         * Monthly:   N = Tenure in Months
         * Weekly:    N = round(Tenure × 52 / 12)
         * Quarterly: N = Tenure / 3 (requires Tenure % 3 == 0)
         * Yearly:    N = Tenure / 12 (requires Tenure % 12 == 0)

   Precision implementation:
   - Computed using Java `BigDecimal` with 20-digit precision (`MathContext(20, HALF_UP)`).
   - Final EMI is rounded to 2 decimal places using standard banking rounding (HALF_UP).

2. ZERO-INTEREST SPECIAL CASE ALGORITHM
   If R == 0 (Interest-free loan):
          EMI = Principal / N
   No compound multiplier is applied; principal is distributed equally across periods.

3. AMORTIZATION SCHEDULE BREAKDOWN ALGORITHM
   For period i = 1 to N:
   a. Interest Paid for period:
          Interest_i = Balance_(i-1) × R   (rounded to 2 decimal places)
   b. Principal Paid for period:
          If i < N:
              Principal_i = min(max(EMI - Interest_i, 0), Balance_(i-1))
              This_EMI    = EMI
          If i == N (Final Installment):
              Principal_i = Balance_(i-1)   [Absorbs any remaining fractional paise]
              This_EMI    = Principal_i + Interest_i
   c. New Balance:
          Balance_i = Balance_(i-1) - Principal_i
   d. Totals Aggregation:
          Total_Payment  = sum(This_EMI)
          Total_Interest = Total_Payment - Principal

4. DATE PROJECTION & MONTH-END CLAMPING ALGORITHM
   - Uses `java.time.LocalDate` which respects calendar days and leap years:
     * Weekly:    DueDate = StartDate + (7 × i) days
     * Monthly:   DueDate = StartDate.plusMonths(i)
     * Quarterly: DueDate = StartDate.plusMonths(3 × i)
     * Yearly:    DueDate = StartDate.plusYears(i)
   - Automatically clamps month ends (e.g. 31 Jan + 1 month = 28 Feb in non-leap year).

5. BCRYPT PASSWORD HASHING ALGORITHM
   - Uses OpenBSD BCrypt with a cost factor (log rounds) of 10.
   - Formula:
          hash = BCrypt(password, salt)
   - Includes a cryptographically random 128-bit salt embedded inside the 60-character
     modular crypt format string ($2a$10$...).
   - Protects against rainbow tables and GPU brute-force attacks.

6. HMAC-SHA256 JWT TOKEN ALGORITHM
   - Token structure: `Header.Payload.Signature`
   - Algorithm: HS256 (HMAC with SHA-256 hash function).
   - Payload claims:
     * `userId`: Database Primary Key
     * `sub`: User's email address
     * `role`: User role ('user' or 'admin')
     * `iat`: Issued At timestamp
     * `exp`: Expiration timestamp (Current Time + 8 Hours)
   - Validated on every incoming API request by the stateless security filter.

7. SLIDING-WINDOW BRUTE-FORCE RATE LIMITING ALGORITHM
   - Key: `ClientIP + ":" + Email`
   - Tracks failed authentication attempts in a thread-safe `ConcurrentHashMap`.
   - Window: 15 minutes (900,000 ms).
   - Threshold: Maximum 10 failed login attempts.
   - If `failures >= 10` within window, returns HTTP 429 Too Many Requests.
   - Cleared immediately upon successful authentication.


================================================================================
5. COMPREHENSIVE LIST OF FILES & INTERNAL WORKINGS
================================================================================

ROOT & CONFIGURATION:
---------------------
1. pom.xml
   - Maven build configuration file.
   - Declares dependencies: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`,
     `spring-boot-starter-security`, `spring-boot-starter-validation`, `mysql-connector-j`,
     and `jjwt-api` / `jjwt-impl` / `jjwt-jackson` (0.12.5).

2. src/main/resources/application.properties
   - Defines application configuration properties:
     * `server.port=9090` (Port of the Spring Boot REST API)
     * `spring.datasource.url=jdbc:mysql://localhost:3306/installment_schedule_db`
     * `spring.datasource.username=root`, `password=`
     * `spring.jpa.hibernate.ddl-auto=none` (Relies on schema.sql)
     * `spring.jpa.open-in-view=false` (Closes DB session after service layer)
     * `app.jwt.secret` (Base64 encryption key for JWT)
     * `app.admin.*` (Default admin seeding credentials)

3. src/main/resources/schema.sql
   - DDL database initialization script.
   - Creates the 3 relational tables: `users`, `loans`, and `installment_schedule`.

MAIN APPLICATION:
-----------------
4. InstallmentApplication.java (Package: com.installment)
   - Contains `main(String[] args)` method to launch Spring Boot.
   - Implements an `ApplicationRunner` bean (`seedAdmin`) that checks if the admin
     account (`admin@example.com`) exists in the database on boot; if not, seeds it.

CONFIGURATION CLASSES:
----------------------
5. AppConfig.java (Package: com.installment.config)
   - Defines system beans:
     * `passwordEncoder()`: Creates `BCryptPasswordEncoder(10)`.
     * `corsConfigurationSource()`: Configures Cross-Origin Resource Sharing (CORS)
       allowing requests from `http://localhost:3000` with `allowCredentials=true`.

6. SecurityConfig.java (Package: com.installment.config)
   - Configures the Spring Security Filter Chain:
     * Disables CSRF (since stateless JWT tokens are used).
     * Configures `SessionCreationPolicy.STATELESS`.
     * Whitelists public endpoints: `/api/auth/**`.
     * Restricts `/api/admin/**` to `hasRole('ADMIN')`.
     * Requires authentication for all other `/api/**` endpoints.
     * Injects `JwtAuthFilter` before `UsernamePasswordAuthenticationFilter`.
     * Implements custom 401 Unauthorized and 403 Forbidden JSON handlers.

CONTROLLERS (REST API Endpoints):
---------------------------------
7. AuthController.java (Package: com.installment.controller)
   - Handles authentication routes:
     * `POST /api/auth/register` (Registers a new user)
     * `POST /api/auth/login` (Authenticates user, sets HttpOnly JWT cookie)
     * `POST /api/auth/logout` (Clears JWT cookie with maxAge=0)
     * `GET  /api/auth/session` (Returns current session user or `{user: null}`)
     * `GET  /api/auth/me` (Returns logged-in user profile)

8. LoanController.java (Package: com.installment.controller)
   - Handles loan schedule routes:
     * `POST   /api/loans` (Calculates and saves loan + schedule)
     * `GET    /api/loans` (Lists all loans belonging to current user)
     * `GET    /api/loans/{id}` (Retrieves specific loan details and schedule)
     * `DELETE /api/loans/{id}` (Deletes loan and cascades deletion of schedule)

9. AdminController.java (Package: com.installment.controller)
   - Handles administrative routes (requires `ROLE_ADMIN`):
     * `GET    /api/admin/stats` (Returns counts of users and total loans)
     * `GET    /api/admin/users` (Lists all users with their loan counts)
     * `PATCH  /api/admin/users/{id}/role` (Promotes/demotes user role)
     * `DELETE /api/admin/users/{id}` (Deletes a user and their loans)
     * `GET    /api/admin/loans` (Lists all loans created across the system)

SERVICES (Business Logic):
--------------------------
10. InstallmentCalculator.java (Package: com.installment.service)
    - Pure calculation engine.
    - Implements EMI formula, frequency calculations, dueDate projection,
      residual balance balancing, and entity conversions.

11. AuthService.java (Package: com.installment.service)
    - Validates uniqueness of user emails.
    - Hashes passwords using BCrypt.
    - Validates credentials against MySQL and generates JWTs.
    - Sets and unsets HttpOnly `isg_token` cookies with SameSite=Lax.
    - Implements rate limiting.

12. LoanService.java (Package: com.installment.service)
    - Manages transactional operations for loans.
    - Verifies user ownership before returning or deleting records.
    - Grants administrators override access to view/delete any schedule.

13. AdminService.java (Package: com.installment.service)
    - Aggregates user and loan metrics.
    - Uses grouped SQL queries to compute loan counts per user efficiently.
    - Enforces rules preventing admin from modifying or deleting their own account.

SECURITY & AUTHENTICATION UTILITIES:
------------------------------------
14. JwtUtil.java (Package: com.installment.security)
    - Generates, parses, and validates JSON Web Tokens using JJWT.
    - Extracts `userId`, `email`, and `role` claims from token payloads.

15. JwtAuthFilter.java (Package: com.installment.security)
    - Extends `OncePerRequestFilter`.
    - Intercepts every incoming request, extracts `isg_token` from cookies (or
      `Bearer` Authorization header), verifies token validity, and sets the
      Spring Security `Authentication` context with authorities (`ROLE_USER` / `ROLE_ADMIN`).

DATA TRANSFER OBJECTS (DTOs):
-----------------------------
16. RegisterRequest.java (Package: com.installment.dto.request)
    - Input DTO: Validates `name` (max 100), `email` (valid email format), `password` (min 6).

17. LoginRequest.java (Package: com.installment.dto.request)
    - Input DTO: Validates `email` and `password`.

18. LoanRequest.java (Package: com.installment.dto.request)
    - Input DTO: Validates `principal` (100 to 1,000,000,000), `interestRate` (0 to 100%),
      `tenure` (1 to 600 months), `frequency` (regex weekly|monthly|quarterly|yearly),
      and `startDate` (valid date).

19. RoleRequest.java (Package: com.installment.dto.request)
    - Input DTO: Validates `role` (user or admin).

20. UserResponse.java (Package: com.installment.dto.response)
    - Output DTO: Returns `id`, `name`, `email`, `role`, `createdAt`, `loanCount`.

21. LoanResponse.java (Package: com.installment.dto.response)
    - Output DTO: Formats loan entity for JSON, including aliases `id` and `loanId`,
      `ownerName`, `ownerEmail`, principal, interest rate, tenure, installment amount,
      and total interest.

22. ScheduleEntryResponse.java (Package: com.installment.dto.response)
    - Output DTO: Formats schedule entry with aliases `no`, `dueDate`, `amount`,
      `principal`, `interest`, `balance`.

23. LoanWithScheduleResponse.java (Package: com.installment.dto.response)
    - Output DTO: Encapsulates both `LoanResponse` and `List<ScheduleEntryResponse>`.

ENTITIES (Hibernate / Database Mapping):
----------------------------------------
24. User.java (Package: com.installment.entity)
    - JPA Entity mapped to `users` table.
    - Fields: `userId` (PK), `name`, `email` (unique), `password`, `role`, `createdAt`.
    - One-to-many relationship with `Loan` with cascade delete.

25. Loan.java (Package: com.installment.entity)
    - JPA Entity mapped to `loans` table.
    - Fields: `loanId` (PK), `user` (ManyToOne, Eager), `principal`, `interestRate`,
      `tenure`, `frequency`, `startDate`, `numInstallments`, `installmentAmount`,
      `totalPayment`, `totalInterest`, `createdAt`.
    - One-to-many relationship with `InstallmentSchedule` with cascade delete.

26. InstallmentSchedule.java (Package: com.installment.entity)
    - JPA Entity mapped to `installment_schedule` table.
    - Fields: `scheduleId` (PK), `loan` (ManyToOne), `installmentNo`, `dueDate`,
      `installmentAmount`, `principalPaid`, `interestPaid`, `balance`.

REPOSITORIES (Spring Data JPA):
-------------------------------
27. UserRepository.java (Package: com.installment.repository)
    - Interface extending `JpaRepository<User, Long>`.
    - Custom methods: `findByEmail(String email)`.

28. LoanRepository.java (Package: com.installment.repository)
    - Interface extending `JpaRepository<Loan, Long>`.
    - Custom methods:
      * `findByUserOrderByCreatedAtDesc(User user)`
      * `findAllWithUser()` (JPQL fetch join to prevent N+1 issues)
      * `countLoansGroupedByUserId()` (Aggregated counts for admin panel)

29. InstallmentScheduleRepository.java (Package: com.installment.repository)
    - Interface extending `JpaRepository<InstallmentSchedule, Long>`.
    - Custom methods: `findByLoanOrderByInstallmentNoAsc(Loan loan)`.

EXCEPTIONS:
-----------
30. AppException.java (Package: com.installment.exception)
    - Custom runtime exception carrying `HttpStatus`, message, and optional `field` name.

31. GlobalExceptionHandler.java (Package: com.installment.exception)
    - Annotated with `@RestControllerAdvice`.
    - Intercepts `AppException`, Spring `MethodArgumentNotValidException`,
      `NoResourceFoundException`, and unexpected server errors, mapping them to
      appropriate HTTP status codes (400, 401, 403, 404, 500) and consistent JSON formats.


================================================================================
6. DATABASE SCHEMA, RELATIONSHIPS & CASCADES
================================================================================
Database Name: `installment_schedule_db`
Engine:        InnoDB (ACID Compliant)
Charset:       utf8mb4

Schema Structure:

  ┌──────────────┐
  │    users     │
  ├──────────────┤
  │ user_id (PK) │◄──┐
  │ name         │   │ 1:N Relationship
  │ email (UQ)   │   │ (FOREIGN KEY user_id REFERENCES users(user_id) ON DELETE CASCADE)
  │ password     │   │
  │ role         │   │
  │ created_at   │   │
  └──────────────┘   │
                     │
  ┌──────────────────┴─┐
  │       loans        │
  ├────────────────────┤
  │ loan_id (PK)       │◄──┐
  │ user_id (FK)       │   │ 1:N Relationship
  │ principal          │   │ (FOREIGN KEY loan_id REFERENCES loans(loan_id) ON DELETE CASCADE)
  │ interest_rate      │   │
  │ tenure             │   │
  │ frequency          │   │
  │ start_date         │   │
  │ num_installments   │   │
  │ installment_amount │   │
  │ total_payment      │   │
  │ total_interest     │   │
  │ created_at         │   │
  └────────────────────┘   │
                           │
  ┌────────────────────────┴─┐
  │   installment_schedule   │
  ├──────────────────────────┤
  │ schedule_id (PK)         │
  │ loan_id (FK)             │
  │ installment_no           │
  │ due_date                 │
  │ installment_amount       │
  │ principal_paid           │
  │ interest_paid            │
  │ balance                  │
  └──────────────────────────┘


================================================================================
7. REST API SPECIFICATIONS
================================================================================

Authentication Endpoints:
  POST   /api/auth/register    Register a new user
  POST   /api/auth/login       Login with email & password (sets cookie)
  POST   /api/auth/logout      Logout (destroys cookie)
  GET    /api/auth/session     Check current session state
  GET    /api/auth/me          Get authenticated user profile

Loan Endpoints (Requires User / Admin Authentication):
  POST   /api/loans            Create loan, calculate EMI, and generate schedule
  GET    /api/loans            Retrieve list of user's saved loans
  GET    /api/loans/{id}       Retrieve a single loan and its full schedule
  DELETE /api/loans/{id}       Delete a loan and its schedule

Admin Endpoints (Requires Admin Role):
  GET    /api/admin/stats      Get total count of users and loans
  GET    /api/admin/users      Get list of all users and their schedule counts
  PATCH  /api/admin/users/{id}/role Update user role ('user' or 'admin')
  DELETE /api/admin/users/{id} Delete a user and all their schedules
  GET    /api/admin/loans      Get all loans across all users


================================================================================
8. SECURITY, SESSION & RATE-LIMITING MECHANICS
================================================================================
- Cookies: The JWT token is saved inside an `HttpOnly`, `SameSite=Lax` cookie named
  `isg_token`. This prevents Cross-Site Scripting (XSS) attacks from stealing tokens.
- CORS: Spring Security permits incoming calls from `http://localhost:3000` with
  credentials enabled (`credentials: 'include'`).
- Role Enforcement: Method-level and path-level security ensures standard users
  cannot call `/api/admin/*` endpoints or access other customers' records.


================================================================================
9. HOW TO BUILD & RUN
================================================================================
Prerequisites:
- Java JDK 17 or higher
- Apache Maven 3.8+
- MySQL Server 8.0 / 8.4 on port 3306

One-Click Start:
  Run `start-all.bat` or `powershell -ExecutionPolicy Bypass -File start-all.ps1`

Manual Terminal Commands:
  1. Compile Backend:
     cd backend
     mvn clean package -DskipTests

  2. Run Backend:
     java -jar target/installment-schedule-generator-1.0.0.jar
     (Listening on port 9090)

  3. Run Frontend:
     npm start
     (Listening on port 3000)

  4. Open in Browser:
     http://localhost:3000

================================================================================
END OF DOCUMENTATION
================================================================================

