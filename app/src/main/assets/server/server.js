// IDEAS local server (Phase 2 placeholder).
// Serves a minimal status page and a /health endpoint.
// Phase 3 will replace this with the full Express + WebSocket server.
"use strict";

const http = require("http");

const PORT = Number(process.env.PORT || 3000);

const PAGE = `<!doctype html>
<html>
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>IDEAS</title>
<style>
  body{margin:0;font-family:system-ui,sans-serif;background:#111;color:#eee;min-height:100vh;display:flex;align-items:center;justify-content:center}
  .card{text-align:center;padding:24px}
  h1{font-size:20px;margin:0 0 8px}
  p{font-size:14px;color:#9a9a9a;margin:4px 0}
  .ok{color:#4caf50;font-size:14px}
</style>
</head>
<body>
  <div class="card">
    <h1>IDEAS</h1>
    <p>Local runtime is up.</p>
    <p class="ok">&#10003; server running on port ${PORT}</p>
    <p>Phase 3 will replace this page with the full IDE.</p>
  </div>
</body>
</html>
`;

const server = http.createServer((req, res) => {
  if (req.url === "/health") {
    res.writeHead(200, { "Content-Type": "application/json" });
    res.end(JSON.stringify({ status: "ok", service: "ideas-server" }));
    return;
  }
  res.writeHead(200, { "Content-Type": "text/html; charset=utf-8" });
  res.end(PAGE);
});

server.on("error", (err) => {
  console.error("server error:", err.message);
  process.exit(1);
});

server.listen(PORT, "127.0.0.1", () => {
  console.log(`IDEAS server listening on http://127.0.0.1:${PORT}`);
});