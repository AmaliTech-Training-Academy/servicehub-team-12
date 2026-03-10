# Frontend Architecture

## Overview

This project uses **Vite** as a build tool — not as an application framework. There is no Single-Page Application (SPA). Instead, the backend serves multiple server-rendered pages via **Spring Boot + Thymeleaf**, and Vite's only job is to:

1. Compile TypeScript
2. Bundle npm packages (Tailwind CSS, Preline UI, etc.)
3. Output a single `main.js` and `main.css` into Spring Boot's static resources directory

Every Thymeleaf template includes these two built files, giving every page access to the full set of installed npm packages.

---

## Directory Structure

```
heat-on-heat-boys/
├── frontend/                        ← Vite project (build tool only)
│   ├── docs/                        ← This documentation
│   ├── public/                      ← Static assets copied as-is to the build output
│   ├── src/
│   │   ├── main.ts                  ← JS entry point — import npm packages here
│   │   └── style.css                ← CSS entry point — import Tailwind, Preline, etc.
│   ├── vite.config.ts               ← Vite configuration
│   ├── tsconfig.json                ← TypeScript configuration
│   └── package.json
│
└── backend/
    └── src/main/resources/
        ├── templates/               ← Thymeleaf HTML pages (server-rendered)
        │   └── index.html           ← Example page
        └── static/                  ← Vite build output (gitignored)
            └── assets/
                ├── main.js          ← Compiled TypeScript bundle
                └── main.css         ← Compiled CSS bundle
```

---

## How It Works

### Build flow

```
frontend/src/main.ts
frontend/src/style.css
        │
        ▼
   Vite + Rollup
   (tsc type-checks, then Vite bundles)
        │
        ▼
backend/src/main/resources/static/assets/
        ├── main.js
        └── main.css
```

Spring Boot automatically serves everything inside `src/main/resources/static/` at the root URL, so `assets/main.js` is reachable at `http://localhost:8080/assets/main.js`.

### Why asset filenames have no hash

By default Vite appends a content hash to filenames (e.g. `main-Dh3x9a1b.js`). This is disabled in `vite.config.ts`:

```ts
output: {
  entryFileNames: 'assets/main.js',
  chunkFileNames: 'assets/[name].js',
  assetFileNames: 'assets/[name].[ext]',
}
```

This allows every Thymeleaf template to reference the assets by a fixed, predictable path without needing a manifest lookup.

### Why `index.html` is deleted after build

Vite always emits an `index.html` alongside the built assets. Since Thymeleaf owns all HTML serving, a stray `index.html` in `static/` would conflict (Spring Boot's static resource handler would intercept requests before Thymeleaf gets them). A custom Vite plugin in `vite.config.ts` deletes it automatically at the end of every build.

---

## Adding a Thymeleaf Page

Every new Thymeleaf template should include the two asset references in its `<head>` and just before `</body>`:

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Page Title</title>
    <link rel="stylesheet" th:href="@{/assets/main.css}" />
</head>
<body>

    <!-- page content -->

    <script type="module" th:src="@{/assets/main.js}"></script>
</body>
</html>
```

The `th:href` and `th:src` attributes let Thymeleaf prepend the correct context path automatically, so the app works regardless of whether it is deployed at the root or under a sub-path.

---

## Installing npm Packages

### CSS packages (e.g. Tailwind CSS)

Install the package, then import it in `src/style.css`:

```css
@import 'tailwindcss';
```

### JS packages (e.g. Preline UI)

Install the package, then import and initialise it in `src/main.ts`:

```ts
import 'preline';
```

After adding any new import, run a fresh build (see below) so the compiled output in `static/assets/` is updated.

---

## Local Development Workflow

### 1. Start the Spring Boot backend

```bash
cd backend
./mvnw spring-boot:run
# Listening on http://localhost:8080
```

### 2. Start the Vite dev server

```bash
cd frontend
npm run dev
# Listening on http://localhost:5173
```

During development, open the app on **port 5173** (the Vite dev server), not 8080. Vite proxies all `/api/**` requests to Spring Boot on port 8080, so API calls work transparently without any CORS configuration.

The proxy is configured in `vite.config.ts`:

```ts
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
    },
  },
},
```

> **Note:** In dev mode the assets are served from Vite's in-memory dev server. The `static/assets/` directory in the backend is only populated when you run `npm run build`.

### 3. Build for production / Spring Boot integration testing

```bash
cd frontend
npm run build
```

This runs `tsc` (type checking) followed by a Vite production build. The output is written directly to `backend/src/main/resources/static/assets/`. You can then start Spring Boot normally and it will serve both the Thymeleaf pages and the compiled assets from the same origin.

---

## Configuration Reference

### `vite.config.ts`

| Option | Value | Purpose |
|---|---|---|
| `build.outDir` | `../backend/src/main/resources/static` | Writes build output into Spring Boot's static resources |
| `build.emptyOutDir` | `true` | Clears stale files from the output directory before each build |
| `rollupOptions.input` | `src/main.ts` | Single entry point for the entire frontend bundle |
| `entryFileNames` | `assets/main.js` | Stable filename (no hash) for the JS bundle |
| `assetFileNames` | `assets/[name].[ext]` | Stable filenames (no hash) for CSS and other assets |
| `server.proxy./api` | `http://localhost:8080` | Forwards API requests to Spring Boot during dev |

### `tsconfig.json`

TypeScript is configured in **bundler mode** — `tsc` only performs type checking (`noEmit: true`); Vite handles the actual transpilation and bundling. Strict mode is enabled throughout.

---

## What This Architecture Is Not

- **Not a SPA** — there is no client-side router. Navigation between pages is handled by Spring MVC / Thymeleaf as normal HTTP requests.
- **Not a separate frontend service** — in production, Spring Boot serves both the HTML pages and the compiled static assets from a single JAR.
- **Not using the Vite dev server in production** — `npm run dev` is a local development convenience only.

