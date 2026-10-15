# Stacks: Online Library Management System (Java)

A web application where librarians manage books, members, loans and notifications, and members search the catalog, borrow and return books, and follow their own history.

**Stack:** Java 17, Spring Boot 3.3, Spring MVC + Thymeleaf, Spring Data JPA (Hibernate), Spring Security, H2 file database, JUnit 5 + MockMvc.

## Features

**Librarian**
- Book management: add, edit and delete books (title, author, ISBN, genre, copies). ISBNs are validated and unique.
- Member management: create, edit and delete member accounts (name, email, membership ID, password).
- Transaction management: record borrows and returns with a date; filter by on loan, overdue or returned.
- Notification management: send messages to all or selected members, and send due-date reminders in one click. New books automatically notify members who opted in to new-arrival alerts.
- Inventory reports: stock totals, genre breakdown, most-borrowed books, borrowing-trend and genre charts, CSV export.

**Member**
- Search by title, author, genre, or everything.
- Borrow and return books. Due dates and overdue loans are shown with a stamp.
- Borrowing history, search history and profile management (name, email, password).
- Notification alerts with a live unread badge (refreshes every 30 seconds) and per-type preferences.

**Rules** (configurable)
- Loan period 14 days, maximum 5 active loans per member.
- A member cannot hold two copies of the same title or borrow a title with no copies left.
- A book that is on loan cannot be deleted. A member with books on loan cannot be deleted.

## Requirements

- **JDK 17 or newer** (Temurin/OpenJDK is fine). Check with `java -version`.
- **Apache Maven 3.9+**. Check with `mvn -version`. (Or open the project in IntelliJ IDEA / Eclipse / VS Code, which bundle Maven.)

### Windows
1. Install a JDK 17+ (for example Temurin from adoptium.net) and tick the option to set `JAVA_HOME` / add to PATH.
2. Download Maven from maven.apache.org, unzip it, and add its `bin` folder to PATH.
3. Open a new Command Prompt and confirm `java -version` and `mvn -version` both work.

## Run

```bash
mvn spring-boot:run
```

Open http://localhost:8080.

| Role      | Email                  | Password      |
|-----------|------------------------|---------------|
| Librarian | `admin@library.local`  | `admin12345`  |
| Member    | `member@library.local` | `member12345` |

On first start the app creates the librarian account and (because `library.seed-demo=true`) ten sample books and the demo member. **Change the admin password and turn off demo data before deploying** (see Configuration).

## Test

```bash
mvn test
```

## Build a runnable jar

```bash
mvn package
java -jar target/library-management-system-1.0.0.jar
```

## Configuration

Edit `src/main/resources/application.properties`, or override with environment variables (Spring maps `library.admin.password` to `LIBRARY_ADMIN_PASSWORD`).

| Property                  | Default               | Purpose                               |
|---------------------------|-----------------------|---------------------------------------|
| `library.loan-days`       | `14`                  | Days before a book is due             |
| `library.max-active-loans`| `5`                   | Books a member can hold at once       |
| `library.admin.email`     | `admin@library.local` | Default librarian login               |
| `library.admin.password`  | `admin12345`          | Default librarian password            |
| `library.admin.name`      | `Head Librarian`      | Default librarian display name        |
| `library.seed-demo`       | `true`                | Add sample books and a demo member    |
| `server.port`             | `8080`                | HTTP port                             |

The database is stored in `./data/library.mv.db` and created on first run. Delete the `data` folder to start fresh. The admin account is only created when no librarian exists, so changing `library.admin.password` later does not alter an existing account.

## Docker

```bash
docker build -t stacks-java .
docker run -p 8080:8080 -v stacks-data:/app/data \
  -e LIBRARY_ADMIN_PASSWORD=change-me -e LIBRARY_SEED_DEMO=false stacks-java
```

## Project layout

```
src/main/java/com/library/
  LibraryApplication.java
  config/        SecurityConfig, user lookup, startup data, shared model attributes
  model/         JPA entities: User, Book, Loan, Notification, SearchHistory
  repository/    Spring Data repositories
  service/       business rules: BookService, MemberService, LoanService,
                 NotificationService, ReportService
  dto/           form and view classes
  controller/    Spring MVC controllers (auth, librarian/*, member)
src/main/resources/
  templates/     Thymeleaf pages (librarian/, member/, fragments.html)
  static/        CSS and JavaScript
src/test/java/   service tests and MockMvc security/page tests
```

## Security notes

- Passwords are hashed with BCrypt. Sessions and form login are handled by Spring Security.
- CSRF protection is on: every POST form carries a token.
- `/librarian/**` requires the LIBRARIAN role and `/member/**` requires MEMBER.
- Queries are parameterised through JPA, and Thymeleaf escapes output.
- CSV export neutralises spreadsheet formula injection.

## Known limitations

- Notifications are in-app only (no email delivery).
- Deleting a book or member also deletes their borrowing history.
- The reports charts load Chart.js from a CDN and need internet access.
- H2 suits a single server. For heavier use, point `spring.datasource.*` at PostgreSQL or MySQL and add its driver to `pom.xml`.

## License

MIT
