# Impact Radius

**See what breaks when a server goes down.**

Impact Radius is a service-dependency explorer backed by [CognoDB](https://console.cognodb.com), a managed graph database. Given any server in an infrastructure, it answers a question every ops/SRE team eventually has to answer under pressure: *if this goes down, what's the blast radius — which services, applications, and teams are affected, and who needs to be notified?*

- **Live demo:** https://impact-radius.vercel.app
- **Backend API:** https://impact-radius.onrender.com/api


> Note: the backend runs on Render's free tier, which spins down after inactivity. The first request after a period of idle time may take 30–60 seconds to respond while it wakes up.

---

## Why a graph database?

Infrastructure dependencies are inherently a network, not a table. A server can host multiple services; services call other services; applications are built from services; teams own applications. Answering "what's affected if X goes down" means walking that chain outward — and the chain can be arbitrarily deep and irregularly shaped (some servers have two dependents, others have none, others have a chain five layers deep).

In a relational schema, this means:
- Joining Server → Service → Application → Team, and
- Not knowing in advance how many joins deep the chain goes, which means falling back to a recursive CTE — verbose, easy to get wrong, and hard for most engineers to read at a glance.

In Cypher, the same question is a single, readable pattern:

```cypher
MATCH (srv:Server {name: $serverName})<-[:RUNS_ON]-(directService:Service)
OPTIONAL MATCH (directService)<-[:CALLS*0..3]-(upstreamService:Service)
...
```

The `*0..3` syntax — "zero to three hops through CALLS relationships" — expresses variable-depth traversal directly in the query language, with no recursion, no temp tables, and no guessing the maximum depth up front. This is the kind of query a relational database finds genuinely awkward, and it's exactly the kind of question this application exists to answer.

---

## Data model

**Nodes**
| Label | Properties |
|---|---|
| `Server` | `name`, `region`, `status` |
| `Service` | `name`, `version` |
| `Application` | `name`, `criticality` |
| `Team` | `name`, `slack_channel` |

**Relationships**
| Relationship | Direction | Meaning |
|---|---|---|
| `(Service)-[:RUNS_ON]->(Server)` | Service → Server | The service runs on this server |
| `(Service)-[:CALLS]->(Service)` | Service → Service | One service depends on another at runtime |
| `(Application)-[:USES]->(Service)` | Application → Service | The application is built from this service |
| `(Application)-[:OWNED_BY]->(Team)` | Application → Team | The team accountable for this application |

```
(Application) --USES--> (Service) --RUNS_ON--> (Server)
     |                      ^
   OWNED_BY               CALLS
     |                      |
   (Team)              (Service) ...chain continues
```

To find the blast radius of a server, the app walks these relationships **backward** — starting at the `Server` node and traversing against the arrows to find every `Service`, `Application`, and `Team` that ultimately depends on it.

---

## Architecture

```
┌─────────────┐        HTTPS/JSON        ┌──────────────────┐       Bolt (openCypher)       ┌───────────┐
│   React UI   │  ──────────────────────▶ │  Spring Boot API  │ ─────────────────────────────▶ │  CognoDB  │
│  (Vercel)    │  ◀──────────────────────  │    (Render)       │ ◀───────────────────────────── │  (Cloud)  │
└─────────────┘                           └──────────────────┘                                └───────────┘
```

- **Backend:** Java 21, Spring Boot, official Neo4j Java driver (CognoDB speaks openCypher over Bolt, so the standard Neo4j driver works unmodified).
- **Frontend:** React + Vite.
- **Database:** CognoDB Cloud, free (c0) tier.

---

## Project structure

```
impact-radius/
├── src/main/java/com/wexa/impact_radius/
│   ├── config/       # Neo4j driver bean, CORS config
│   ├── controller/   # REST endpoints (seed, query)
│   ├── service/       # Cypher queries live here
│   └── exception/    # Global error handling
├── frontend/          # React + Vite app
├── Dockerfile         # Multi-stage build for deployment
└── README.md
```

---

## Setup and run instructions

### 1. Create a CognoDB instance

1. Sign up at [console.cognodb.com](https://console.cognodb.com/signup) (no credit card required for the free tier).
2. Create a free (**c0**) instance from the console. It provisions in under a minute.
3. Save the connection URI (`bolt+s://<instance-id>.databases.cognodb.cloud:7687`) and the generated password for user `cognodb` — the password is shown only once.

### 2. Run the backend locally

```bash
# from the project root
export COGNODB_URI=bolt+s://<your-instance>.databases.cognodb.cloud:7687
export COGNODB_USERNAME=cognodb
export COGNODB_PASSWORD=<your-password>

./mvnw spring-boot:run
```

The API will be available at `http://localhost:8080/api`.

### 3. Seed the graph

```bash
curl -X POST http://localhost:8080/api/seed
```

This clears any existing data and loads a small realistic dataset: 2 teams, 3 applications, 5 services, and 4 servers, connected by the relationships described above.

### 4. Run the frontend locally

```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`. By default the frontend points at the deployed backend — update `API_BASE` in `frontend/src/App.jsx` to `http://localhost:8080/api` if you want it to talk to your local backend instead.

---

## API endpoints

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/seed` | Wipes and reloads the sample dataset |
| `GET` | `/api/servers` | Lists all server names |
| `GET` | `/api/blast-radius/{serverName}` | Returns the full impact chain for a given server |

---

## The core query, explained

The blast-radius query is the heart of the application. It's built in stages using Cypher's `WITH` clause as a checkpoint between steps (aggregation functions like `collect()` can't be used directly inside a `WHERE` clause, so each stage collects results, checkpoints them with `WITH`, then continues):

1. Find every `Service` that `RUNS_ON` the target server.
2. Traverse `CALLS` relationships 1–3 hops upstream to find services that depend on those, directly or transitively.
3. Find every `Application` that `USES` any of the affected services.
4. Find every `Team` that `OWNED_BY` those applications.

All query parameters (like the server name) are passed through the driver as bound parameters — never string-concatenated into the Cypher — which is both a security requirement (prevents injection) and standard practice.

---

## Error handling

If CognoDB is unreachable, the backend catches `ServiceUnavailableException` globally and returns a clean `503` JSON response instead of a raw stack trace. All other unexpected errors are caught by a generic handler and returned as a `500` with a safe, non-leaking message.

---

## Screenshots

_[Add screenshots here: empty state, populated results for a server like `srv-01`, and any additional states worth showing.]_

---

## Deployment

- **Backend:** deployed on [Render](https://render.com) via Docker (multi-stage build: Maven compiles the jar in a JDK image, then a lightweight JRE image runs it).
- **Frontend:** deployed on [Vercel](https://vercel.com), built from the `frontend/` subdirectory.
- Both platforms read CognoDB credentials from environment variables set in their respective dashboards — never committed to the repository.
