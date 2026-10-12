# Ktor Demo

This repository contains two applications:

- `api/` is the Kotlin/Ktor backend.
- `gui/` is an Angular frontend.

## Start the full application

Build and start both containers from the repository root:

```sh
docker compose up --build
```

Open <http://localhost:4200> for the GUI or <http://localhost:8080> for the API.
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

Open <http://localhost:4200>. The GUI calls the API at <http://localhost:8080>.

## API endpoints

- `GET /` returns the demo service message.
- `GET /health` returns the API health status as JSON.
- `GET /users` returns the cached user list fetched from JSONPlaceholder.
- `POST /echo` echoes a JSON message.

## Build the API container only

```sh
docker build -t ktor-demo .
docker run --rm -p 8080:8080 ktor-demo
```
