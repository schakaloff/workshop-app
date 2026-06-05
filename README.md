# Work Order Manager

A desktop CRM application for managing a repair workshop — built with Java 21 and JavaFX.

## Features

- **Work Orders** — create, view, and track repair orders with status and due dates
- **Customers** — manage customer records including contact details and history
- **Technicians** — assign technicians to jobs and track their work
- **Vendors** — manage vendor/supplier records
- **Invoices** — generate and print invoices (PDF output)
- **Payments** — record and track payments per order
- **Print Support** — configurable print options with PDF export via PDFBox and iTextPDF
- **Auto-update** — built-in update mechanism via update4j (bootstrap + app JAR split)

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| UI | JavaFX 21, MaterialFX, JFoenix |
| Database | MySQL / MariaDB |
| Connection pool | HikariCP |
| PDF | Apache PDFBox, iTextPDF |
| Build | Maven (maven-shade-plugin) |
| Updates | update4j |

## Requirements

- Java 21+
- MySQL or MariaDB server
- Maven 3.8+ (for building from source)

## Getting Started

### 1. Set up the database

Import the schema from `src/DB/workshopdb.sql` into your MySQL/MariaDB instance:

```bash
mysql -u <user> -p < src/DB/workshopdb.sql
```

### 2. Configure the database connection

Update the database connection settings in the app's config (host, port, username, password) on first launch via the Settings screen.

### 3. Build

```bash
mvn package
```

This produces two JARs in `target/`:

| JAR | Purpose |
|---|---|
| `workordermanager-app.jar` | Main application fat JAR |
| `workordermanager-bootstrap.jar` | Launcher with auto-update support |

### 4. Run

```bash
java -jar target/workordermanager-bootstrap.jar
```

Or, to skip the updater and launch directly:

```bash
java -jar target/workordermanager-app.jar
```

## Project Structure

```
src/main/java/
├── Controllers/       # JavaFX controllers for each screen
│   └── DbRepo/        # Database query classes per feature
├── DB/                # DB config, connection provider, entity classes
├── Skeletons/         # Model/data classes (Customer, WorkOrder, etc.)
├── main/              # Entry points, launcher, update screen
├── print/             # Print logic
└── utils/             # Shared utilities, document output, spell check
src/DB/
└── workshopdb.sql     # Database schema
```

## Version

Current version: **0.8.2**
