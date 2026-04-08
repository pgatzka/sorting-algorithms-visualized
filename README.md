# Sorting Algorithms Visualized

A web application that generates MP4 videos of sorting algorithm visualizations. Built with Spring Boot, Vaadin, and FFmpeg.

## Features

- **Sorting Algorithms**: BubbleSort, QuickSort (extensible)
- **Visualizations**: Bar chart with color-coded comparisons, swaps, and sorted elements (extensible)
- **Video Output**: 1080x1920 (vertical) MP4 at 60 FPS
- **Job Processing**: Spring Batch for background video generation with progress tracking
- **Web UI**: Configure parameters, trigger generation, monitor progress, and download videos

## Quick Start with Docker Compose

```bash
docker compose up --build
```

Open [http://localhost:8090](http://localhost:8090) in your browser.

This starts:
- **PostgreSQL 17** on port 5433 (mapped from container's 5432, to avoid conflicts with a local PostgreSQL)
- **Application** on port 8090 (with FFmpeg included)

Generated videos are persisted in a Docker volume (`videos`).

To stop:

```bash
docker compose down
```

To stop and remove all data (database + videos):

```bash
docker compose down -v
```

## Local Development

### Prerequisites

- Java 21
- PostgreSQL (or use the database from Docker Compose)
- FFmpeg on PATH

### Run with local PostgreSQL

Start only the database:

```bash
docker compose up db
```

Then run the application:

```bash
./mvnw
```

This starts the app on [http://localhost:8090](http://localhost:8090) with the `dev` profile (connects to `localhost:5432/sorting`).

### Build & Test

```bash
./mvnw verify -B          # Run tests + spotless check
./mvnw spotless:apply      # Auto-format code
```

## Tech Stack

- Java 21, Spring Boot 4.0.5, Vaadin 25.1.1
- Spring Batch for job processing
- PostgreSQL + Flyway migrations
- Java 2D for frame rendering, FFmpeg for video encoding
- Google Java Format (Spotless)
