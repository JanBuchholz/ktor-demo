# Ktor Demo

This repository contains two applications:

- `api/` is the Kotlin/Ktor backend.
- `gui/` is an Angular frontend.

## Start the full application

Build and start both containers from the repository root:

```sh
docker compose up --build
```

Open the forwarded GUI port (4200) in Codespaces, or <http://localhost:4200> locally.
The GUI calls `/api` on its own origin; its development server and the Compose Nginx
container proxy those requests to the API, so the browser does not need direct access
to a separately forwarded API port.
The API container also inherits `HTTPS_PROXY`/`HTTP_PROXY` from the environment
running Docker Compose, which lets it reach the upstream user service in proxied
Codespaces environments.
Stop both services with `Ctrl+C`; run `docker compose down` to remove the containers.

## Run locally

Start the API from the repository root:

```sh
./gradlew :api:run
```

In another terminal, install and start the Angular GUI:

```sh
cd gui
npm install
npm start
```

Open <http://localhost:4200>. The Angular development server proxies GUI API requests
to the Ktor API at <http://localhost:8080>.

## API endpoints

- `GET /` returns the demo service message.
- `GET /health` returns the API health status as JSON.
- `GET /users` returns the user list fetched from JSONPlaceholder, loading it on demand if the cache is not ready yet.
- `POST /echo` echoes a JSON message.

## Build the API container only

```sh
docker build -t ktor-demo .
docker run --rm -p 8080:8080 ktor-demo
```
