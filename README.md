# Vahan OneOps

Phase 0 of Vahan's project and work-allocation system.

**What it does today**
- Admin logs in, adds users (admin or employee), activates/deactivates them, resets passwords.
- Admin creates a project by its **drawing number**, adds **child parts**, and for each part chooses the **stages** it goes through, in order. Every part has its own list. Each stage gets one **owner** (a Vahan employee) and an optional due date.
- Admin sees all projects (progress, how many stages are not yet allocated) and can open any user's allocated work.
- Employee logs in and sees **only their own** allocated stages, including which earlier stages they are waiting for. Works on a phone.

**Not built yet (on purpose):** pricing, suppliers, start/hold/done buttons, raw material, billing, machine hours, notifications.

---

## Run it

You need Java 21, Maven (or IntelliJ), Docker (for PostgreSQL) and Node 20+.

```bash
# 1. database
docker compose up -d

# 2. backend  (http://localhost:8080)
cd backend
mvn test                                          # runs the end-to-end tests first
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 3. frontend (http://localhost:5173)
cd frontend
npm install
npm run dev
```

First login (dev profile only): username `admin`, password `ChangeMe#123`. Change it from the top-right name.

**Production settings** (environment variables, no profile):

| Variable | Meaning |
|---|---|
| `VAHAN_JWT_SECRET` | required, at least 32 random characters |
| `VAHAN_DB_URL`, `VAHAN_DB_USER`, `VAHAN_DB_PASSWORD` | PostgreSQL connection |
| `VAHAN_ADMIN_USERNAME`, `VAHAN_ADMIN_PASSWORD` | first admin, created only when the user table is empty. If no password is set, a random one is printed once in the log |
| `VAHAN_CORS_ORIGINS` | the website's address, if the frontend is on a different host |

---

## How the code is organised

```
backend/src/main/java/com/vahan/oneops/
  common/    shared bits: error format, the logged-in user type
  auth/      login, JWT, and ALL access rules (SecurityConfig)
  user/      users and roles
  project/   projects, child parts, stages, "my work"
  <next feature goes in its own folder here>
backend/src/main/resources/db/migration/   V1, V2, ... one file per change to the database
frontend/src/pages/                        one file per screen
```

Rules that keep it easy to extend:

1. **One folder per feature**, each with its own entity, repository, service and controller.
2. **Features talk through services, never each other's tables.** Projects keep a plain user id and ask `UserDirectory` for names.
3. **The database changes only through new migration files** (`V4__...sql`; V3 is the start/complete timestamps). Never edit an old one once it has been used. Hibernate only checks the schema, it never changes it.
4. **Every response and error has one shape**, so the frontend needs no special cases.
5. **Access rules live in one place** (`SecurityConfig`): `/api/admin/**` is admin only, everything else needs login.

### Adding a feature, e.g. start / hold / done
1. New migration, e.g. `V4__stage_events.sql` (a `stage_event` table: stage, who, what, when, reason).
2. Add the entity, repository and service in `project/` (or a new `execution/` folder).
3. A controller, for example `POST /api/my-work/{stageId}/start`. Check inside the service that the stage belongs to the caller. `PartStage.status` already exists.
4. Buttons in `frontend/src/pages/MyWork.tsx`.

### Adding suppliers and outsourced stages (planned)
- `V4__suppliers.sql`: `supplier` table, plus nullable `supplier_id`, `sent_on`, `received_on` columns on `part_stage`.
- Stage owner stays a Vahan employee, as agreed. The supplier is only a record of where the work was done.

### Adding pricing later
New `V5__pricing.sql` and a `pricing/` folder that reads stages by id. Nothing in the current code needs to change.

---

## Checked so far
- Frontend: type-checks and builds; all screens exercised in a real browser (admin and phone-size employee flow) against a stand-in API.
- Database migrations: applied cleanly to PostgreSQL 16.
- Backend: written to the same API contract; **the Maven build and `mvn test` have not been run yet** because the build workspace could not download Java libraries. Run `mvn test` first and send me any error.
