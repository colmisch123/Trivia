package net;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import intr.IQuestion;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class BuzzerServer {
    public enum Phase {
        REGISTRATION,
        BOARD,
        QUESTION,
        REWARD,
        GAME_CLOSED
    }

    private static BuzzerServer instance;

    private HttpServer server;
    private int port = 8080;
    private String hostAddress = "127.0.0.1";
    private boolean isRunning = false;
    private String sessionId = UUID.randomUUID().toString().substring(0, 8);
    private boolean shutdownHookAdded = false;

    // Game state
    private volatile Phase currentPhase = Phase.REGISTRATION;
    private volatile boolean buzzerOpen = false;
    private volatile boolean currentQuestionBuzzable = true;
    private volatile String currentQuestionText = "";
    private final AtomicReference<String> currentBuzzerWinner = new AtomicReference<>(null);
    private volatile String lastSuccessfulWinner = null;

    private final List<String> registeredPlayers = new CopyOnWriteArrayList<>();
    private final Set<String> blockedPlayers = ConcurrentHashMap.newKeySet();
    private final Set<String> kickedPlayers = ConcurrentHashMap.newKeySet();

    private BuzzerServer() {
        findHostAddress();
    }

    public static synchronized BuzzerServer getInstance() {
        if (instance == null) {
            instance = new BuzzerServer();
        }
        return instance;
    }

    private void findHostAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            InetAddress preferred = null;
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || !ni.isUp()) continue;

                String name = ni.getDisplayName().toLowerCase();
                if (name.contains("virtual") || name.contains("wsl") || name.contains("vethernet") || name.contains("pseudo")) {
                    continue;
                }

                Enumeration<InetAddress> addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (!addr.isLoopbackAddress() && addr instanceof Inet4Address && addr.isSiteLocalAddress()) {
                        if (name.contains("wi-fi") || name.contains("wireless") || name.contains("wlan")) {
                            preferred = addr;
                            break;
                        } else if (preferred == null) {
                            preferred = addr;
                        }
                    }
                }
                if (preferred != null && (name.contains("wi-fi") || name.contains("wireless") || name.contains("wlan"))) {
                    break;
                }
            }
            if (preferred != null) {
                hostAddress = preferred.getHostAddress();
            } else {
                hostAddress = InetAddress.getLocalHost().getHostAddress();
            }
        } catch (Exception e) {
            hostAddress = "127.0.0.1";
        }
    }

    public synchronized void start() {
        if (isRunning) return;

        sessionId = UUID.randomUUID().toString().substring(0, 8);
        currentPhase = Phase.REGISTRATION;

        if (!shutdownHookAdded) {
            shutdownHookAdded = true;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    BuzzerServer.getInstance().stop();
                } catch (Exception ignored) {}
            }));
        }

        int tryPort = 8080;
        while (server == null && tryPort < 8090) {
            try {
                server = HttpServer.create(new InetSocketAddress(tryPort), 0);
                this.port = tryPort;
            } catch (IOException e) {
                tryPort++;
            }
        }

        if (server == null) {
            System.err.println("BuzzerServer: Could not bind to port 8080-8089");
            return;
        }

        server.setExecutor(Executors.newCachedThreadPool());

        // Context handlers
        server.createContext("/", new RootHandler());
        server.createContext("/api/register", new RegisterHandler());
        server.createContext("/api/reconnect", new ReconnectHandler());
        server.createContext("/api/poll", new PollHandler());
        server.createContext("/api/buzz", new BuzzHandler());

        server.start();
        isRunning = true;
        System.out.println("BuzzerServer started at " + getUrl());
    }

    public synchronized void stop() {
        if (server != null) {
            currentPhase = Phase.GAME_CLOSED;
            try {
                // Brief pause so ongoing polling requests can receive GAME_CLOSED state
                Thread.sleep(350);
            } catch (InterruptedException ignored) {}
            server.stop(0);
            server = null;
        }
        isRunning = false;
    }

    public String getUrl() {
        return "http://" + hostAddress + ":" + port;
    }

    public String getHostAddress() {
        return hostAddress;
    }

    public int getPort() {
        return port;
    }

    // Game lifecycle controls
    public void setPhase(Phase phase) {
        this.currentPhase = phase;
        if (phase != Phase.QUESTION) {
            this.buzzerOpen = false;
        }
    }

    public Phase getCurrentPhase() {
        return currentPhase;
    }

    public void openBuzzer(IQuestion question) {
        this.currentPhase = Phase.QUESTION;
        this.currentQuestionText = question != null ? question.getQuestion() : "";
        this.currentQuestionBuzzable = question == null || question.isBuzzable();
        this.blockedPlayers.clear();
        this.currentBuzzerWinner.set(null);
        this.lastSuccessfulWinner = null;
        this.buzzerOpen = this.currentQuestionBuzzable;
    }

    public void closeBuzzer() {
        this.buzzerOpen = false;
    }

    public void markCurrentWinnerWrong() {
        String winner = currentBuzzerWinner.get();
        if (winner != null) {
            blockedPlayers.add(winner.toLowerCase());
            currentBuzzerWinner.set(null);
            lastSuccessfulWinner = null;
            if (currentQuestionBuzzable) {
                buzzerOpen = true; // Reopen for remaining players
            }
        }
    }

    public void resetBuzzer() {
        currentBuzzerWinner.set(null);
        lastSuccessfulWinner = null;
        blockedPlayers.clear();
        if (currentQuestionBuzzable) {
            buzzerOpen = true;
        }
    }

    public boolean isBuzzerOpen() {
        return buzzerOpen;
    }

    public Set<String> getBlockedPlayers() {
        return Collections.unmodifiableSet(blockedPlayers);
    }

    public String getCurrentBuzzerWinner() {
        return currentBuzzerWinner.get();
    }

    public String getLastSuccessfulWinner() {
        return lastSuccessfulWinner != null ? lastSuccessfulWinner : currentBuzzerWinner.get();
    }

    public String getSessionId() {
        return sessionId;
    }

    public List<String> getRegisteredPlayers() {
        return registeredPlayers;
    }

    public Set<String> getKickedPlayers() {
        return Collections.unmodifiableSet(kickedPlayers);
    }

    public void resetPlayers() {
        registeredPlayers.clear();
        blockedPlayers.clear();
        kickedPlayers.clear();
        currentBuzzerWinner.set(null);
        lastSuccessfulWinner = null;
        currentPhase = Phase.REGISTRATION;
    }

    public boolean registerPlayer(String name) {
        if (name == null || name.isBlank()) return false;
        String clean = name.trim();
        // Registering clears any prior kicked status
        kickedPlayers.remove(clean.toLowerCase());
        for (String p : registeredPlayers) {
            if (p.equalsIgnoreCase(clean)) {
                return true; // Already registered
            }
        }
        registeredPlayers.add(clean);
        return true;
    }

    public void kickPlayer(String name) {
        if (name == null || name.isBlank()) return;
        String clean = name.trim();
        kickedPlayers.add(clean.toLowerCase());
        registeredPlayers.removeIf(p -> p.equalsIgnoreCase(clean));
        blockedPlayers.remove(clean.toLowerCase());

        String winner = currentBuzzerWinner.get();
        if (winner != null && winner.equalsIgnoreCase(clean)) {
            currentBuzzerWinner.set(null);
            lastSuccessfulWinner = null;
            if (currentQuestionBuzzable && currentPhase == Phase.QUESTION) {
                buzzerOpen = true;
            }
        }
        System.out.println("BuzzerServer: Player kicked -> " + clean);
    }

    public boolean isKicked(String name) {
        if (name == null || name.isBlank()) return false;
        return kickedPlayers.contains(name.trim().toLowerCase());
    }

    // HTTP Handlers
    private class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }
            byte[] responseBytes = getClientHtml().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }
    }

    private class RegisterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> params = parseParams(exchange);
            String name = params.get("name");
            if (name == null || name.isBlank()) {
                sendJsonResponse(exchange, 400, "{\"success\":false,\"error\":\"Name required\"}");
                return;
            }
            boolean ok = registerPlayer(name);
            sendJsonResponse(exchange, 200, "{\"success\":" + ok + ",\"player\":\"" + escapeJson(name.trim()) + "\",\"sessionId\":\"" + sessionId + "\"}");
        }
    }

    private class ReconnectHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> params = parseParams(exchange);
            String name = params.get("name");
            if (name == null || name.isBlank()) {
                sendJsonResponse(exchange, 400, "{\"success\":false,\"error\":\"Name required\"}");
                return;
            }
            String clean = name.trim();
            if (isKicked(clean)) {
                sendJsonResponse(exchange, 200, "{\"success\":false,\"kicked\":true,\"sessionId\":\"" + sessionId + "\"}");
                return;
            }
            boolean found = false;
            for (String p : registeredPlayers) {
                if (p.equalsIgnoreCase(clean)) {
                    found = true;
                    break;
                }
            }
            // Auto-register if registration is currently open
            if (!found && currentPhase == Phase.REGISTRATION) {
                found = registerPlayer(clean);
            }
            sendJsonResponse(exchange, 200, "{\"success\":" + found + ",\"player\":\"" + escapeJson(clean) + "\",\"sessionId\":\"" + sessionId + "\"}");
        }
    }

    private class PollHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> params = parseParams(exchange);
            String name = params.get("name");
            String cleanName = name != null ? name.trim() : "";

            if (currentPhase == Phase.GAME_CLOSED) {
                sendJsonResponse(exchange, 200, "{\"status\":\"GAME_CLOSED\",\"phase\":\"GAME_CLOSED\",\"sessionId\":\"" + sessionId + "\"}");
                return;
            }

            if (isKicked(cleanName)) {
                sendJsonResponse(exchange, 200, "{\"status\":\"KICKED\",\"kicked\":true,\"phase\":\"" + currentPhase.name() + "\",\"sessionId\":\"" + sessionId + "\"}");
                return;
            }

            String winner = currentBuzzerWinner.get();
            boolean isWinner = winner != null && winner.equalsIgnoreCase(cleanName);
            boolean isBlocked = !cleanName.isEmpty() && blockedPlayers.contains(cleanName.toLowerCase());

            String clientStatus;
            if (currentPhase == Phase.REGISTRATION) {
                clientStatus = "REGISTRATION";
            } else if (currentPhase == Phase.BOARD || currentPhase == Phase.REWARD) {
                clientStatus = "BOARD";
            } else { // Phase.QUESTION
                if (!currentQuestionBuzzable) {
                    clientStatus = "NOT_BUZZABLE";
                } else if (isWinner) {
                    clientStatus = "YOU_BUZZED";
                } else if (winner != null) {
                    clientStatus = "OTHER_BUZZED";
                } else if (isBlocked) {
                    clientStatus = "LOCKED_OUT";
                } else if (buzzerOpen) {
                    clientStatus = "BUZZ_OPEN";
                } else {
                    clientStatus = "QUESTION_LOCKED";
                }
            }

            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"status\":\"").append(clientStatus).append("\",");
            json.append("\"phase\":\"").append(currentPhase.name()).append("\",");
            json.append("\"sessionId\":\"").append(sessionId).append("\",");
            json.append("\"buzzable\":").append(currentQuestionBuzzable).append(",");
            json.append("\"buzzerOpen\":").append(buzzerOpen).append(",");
            json.append("\"winner\":\"").append(winner != null ? escapeJson(winner) : "").append("\",");
            json.append("\"isWinner\":").append(isWinner).append(",");
            json.append("\"isBlocked\":").append(isBlocked);
            json.append("}");

            sendJsonResponse(exchange, 200, json.toString());
        }
    }

    private class BuzzHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> params = parseParams(exchange);
            String name = params.get("name");
            if (name == null || name.isBlank()) {
                sendJsonResponse(exchange, 400, "{\"success\":false,\"error\":\"Name required\"}");
                return;
            }

            String clean = name.trim();
            if (isKicked(clean)) {
                sendJsonResponse(exchange, 200, "{\"success\":false,\"reason\":\"You have been kicked\"}");
                return;
            }

            if (currentPhase != Phase.QUESTION || !currentQuestionBuzzable || !buzzerOpen) {
                sendJsonResponse(exchange, 200, "{\"success\":false,\"reason\":\"Buzzer not open\"}");
                return;
            }

            if (blockedPlayers.contains(clean.toLowerCase())) {
                sendJsonResponse(exchange, 200, "{\"success\":false,\"reason\":\"You are locked out for this question\"}");
                return;
            }

            // Atomic buzz race
            if (currentBuzzerWinner.compareAndSet(null, clean)) {
                buzzerOpen = false;
                lastSuccessfulWinner = clean;
                sendJsonResponse(exchange, 200, "{\"success\":true,\"winner\":\"" + escapeJson(clean) + "\"}");
            } else {
                sendJsonResponse(exchange, 200, "{\"success\":false,\"winner\":\"" + escapeJson(currentBuzzerWinner.get()) + "\"}");
            }
        }
    }

    private Map<String, String> parseParams(HttpExchange exchange) throws IOException {
        Map<String, String> result = new HashMap<>();

        // Parse query string
        URI uri = exchange.getRequestURI();
        String query = uri.getQuery();
        if (query != null) {
            for (String pair : query.split("&")) {
                int eq = pair.indexOf('=');
                if (eq > 0) {
                    String k = URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8);
                    String v = URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
                    result.put(k, v);
                }
            }
        }

        // Parse POST body if present
        if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            try (InputStream is = exchange.getRequestBody()) {
                String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                if (!body.isBlank()) {
                    for (String pair : body.split("&")) {
                        int eq = pair.indexOf('=');
                        if (eq > 0) {
                            String k = URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8);
                            String v = URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
                            result.put(k, v);
                        }
                    }
                }
            }
        }

        return result;
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String text) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", "");
    }

    private String getClientHtml() {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
<title>Jeopardy Buzzer</title>
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; }
  body {
    background-color: #041054;
    color: #ffffff;
    min-height: 100vh;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: space-between;
    padding: 20px 15px;
    text-align: center;
    user-select: none;
    -webkit-user-select: none;
  }
  header { margin-top: 10px; }
  h1 { color: #facc15; font-size: 2.2rem; text-shadow: 2px 2px 4px rgba(0,0,0,0.6); letter-spacing: 2px; }
  .player-tag { font-size: 0.95rem; color: #93c5fd; margin-top: 4px; }
  
  .content-card {
    background: rgba(255, 255, 255, 0.08);
    border-radius: 16px;
    padding: 24px 20px;
    width: 100%;
    max-width: 380px;
    box-shadow: 0 8px 30px rgba(0,0,0,0.4);
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    margin: auto 0;
  }

  input[type="text"] {
    width: 100%;
    padding: 14px 16px;
    font-size: 1.2rem;
    border-radius: 10px;
    border: 2px solid #3b82f6;
    background: #0f172a;
    color: white;
    text-align: center;
    margin-bottom: 16px;
    outline: none;
  }
  input[type="text"]:focus { border-color: #facc15; }

  .btn {
    width: 100%;
    padding: 14px;
    font-size: 1.15rem;
    font-weight: bold;
    border-radius: 10px;
    border: none;
    cursor: pointer;
    transition: transform 0.1s, opacity 0.2s;
  }
  .btn:active { transform: scale(0.97); }
  .btn-join { background: #facc15; color: #041054; }

  /* Giant Buzzer Button */
  .buzzer-btn {
    width: 230px;
    height: 230px;
    border-radius: 50%;
    border: 8px solid #facc15;
    background: radial-gradient(circle, #ef4444 0%, #b91c1c 80%, #7f1d1d 100%);
    color: white;
    font-size: 2rem;
    font-weight: 900;
    box-shadow: 0 10px 30px rgba(239, 68, 68, 0.6), inset 0 -6px 12px rgba(0,0,0,0.4);
    cursor: pointer;
    touch-action: manipulation;
    display: flex;
    align-items: center;
    justify-content: center;
    animation: pulse 1.6s infinite;
    text-shadow: 2px 2px 4px rgba(0,0,0,0.8);
  }
  .buzzer-btn:active {
    transform: scale(0.93);
    box-shadow: 0 4px 15px rgba(239, 68, 68, 0.8), inset 0 4px 8px rgba(0,0,0,0.6);
  }

  @keyframes pulse {
    0% { box-shadow: 0 0 0 0 rgba(239, 68, 68, 0.7); }
    70% { box-shadow: 0 0 0 20px rgba(239, 68, 68, 0); }
    100% { box-shadow: 0 0 0 0 rgba(239, 68, 68, 0); }
  }

  .status-box {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 12px;
  }
  .status-title { font-size: 1.6rem; font-weight: bold; }
  .status-desc { font-size: 1rem; color: #cbd5e1; }
  
  .success-banner { color: #4ade80; }
  .warning-banner { color: #facc15; }
  .danger-banner { color: #f87171; }

  footer { font-size: 0.85rem; color: #64748b; margin-top: 15px; }
  .change-link { color: #94a3b8; text-decoration: underline; cursor: pointer; margin-top: 8px; font-size: 0.85rem; }
</style>
</head>
<body>
  <header>
    <h1>JEOPARDY!</h1>
    <div id="playerHeader" class="player-tag" style="display:none;"></div>
  </header>

  <!-- View 1: Register -->
  <div id="viewRegister" class="content-card">
    <h2 style="margin-bottom:16px; color:#facc15;">Enter Your Name</h2>
    <input type="text" id="nameInput" placeholder="Contestant Name" maxlength="20" autocomplete="off" autocorrect="off">
    <button class="btn btn-join" onclick="register()">JOIN GAME</button>
  </div>

  <!-- View 2: Buzzer Active -->
  <div id="viewBuzzer" class="content-card" style="display:none; background:transparent; box-shadow:none;">
    <button class="buzzer-btn" onclick="buzz()">BUZZ IN!</button>
  </div>

  <!-- View 3: Status / Waiting Screens -->
  <div id="viewStatus" class="content-card" style="display:none;">
    <div class="status-box">
      <div id="statusIcon" style="font-size:3rem;">&#9203;</div>
      <div id="statusTitle" class="status-title">Waiting...</div>
      <div id="statusDesc" class="status-desc">Looking at the main board</div>
      <button id="statusActionBtn" class="btn btn-join" style="display:none; margin-top:14px;">Rejoin Game</button>
    </div>
  </div>

  <footer>
    <div id="connectionStatus">Connected to Local Host</div>
    <div id="changeNameLink" class="change-link" style="display:none;" onclick="changeName()">Change Name</div>
  </footer>

<script>
  let playerName = "";
  let currentSessionId = "";
  let pollInterval = null;
  let consecutiveErrors = 0;

  window.addEventListener('load', () => {
    const saved = localStorage.getItem('jeopardy_player_name');
    const savedSession = localStorage.getItem('jeopardy_session_id');
    if (saved && saved.trim()) {
      playerName = saved.trim();
      currentSessionId = savedSession || "";
      reconnect(playerName);
    } else {
      showView('viewRegister');
    }
  });

  function showView(viewId) {
    document.getElementById('viewRegister').style.display = viewId === 'viewRegister' ? 'flex' : 'none';
    document.getElementById('viewBuzzer').style.display = viewId === 'viewBuzzer' ? 'flex' : 'none';
    document.getElementById('viewStatus').style.display = viewId === 'viewStatus' ? 'flex' : 'none';
    document.getElementById('changeNameLink').style.display = playerName ? 'block' : 'none';
  }

  function register() {
    const input = document.getElementById('nameInput');
    const val = input.value.trim();
    if (!val) return;
    
    fetch('/api/register?name=' + encodeURIComponent(val), { method: 'POST' })
      .then(res => res.json())
      .then(data => {
        if (data.success) {
          playerName = data.player;
          if (data.sessionId) {
            currentSessionId = data.sessionId;
            localStorage.setItem('jeopardy_session_id', currentSessionId);
          }
          localStorage.setItem('jeopardy_player_name', playerName);
          onLoggedIn();
        } else {
          alert(data.error || 'Registration failed');
        }
      })
      .catch(err => {
        alert('Could not connect to host. Make sure you are on the same Wi-Fi!');
      });
  }

  function reconnect(name) {
    fetch('/api/reconnect?name=' + encodeURIComponent(name))
      .then(res => res.json())
      .then(data => {
        if (data.sessionId) {
          if (currentSessionId && currentSessionId !== data.sessionId) {
            handleGameClosed('A new game was started on the host computer.');
            return;
          }
          currentSessionId = data.sessionId;
          localStorage.setItem('jeopardy_session_id', currentSessionId);
        }

        if (data.kicked) {
          handleKicked();
        } else if (data.success) {
          playerName = data.player;
          onLoggedIn();
        } else {
          showView('viewRegister');
        }
      })
      .catch(() => {
        // If host temporarily unreachable, still show logged in state and poll will retry
        onLoggedIn();
      });
  }

  function onLoggedIn() {
    document.getElementById('playerHeader').textContent = 'Contestant: ' + playerName;
    document.getElementById('playerHeader').style.display = 'block';
    startPolling();
  }

  function changeName() {
    localStorage.removeItem('jeopardy_player_name');
    localStorage.removeItem('jeopardy_session_id');
    playerName = "";
    document.getElementById('playerHeader').style.display = 'none';
    if (pollInterval) clearInterval(pollInterval);
    showView('viewRegister');
  }

  function buzz() {
    if (navigator.vibrate) navigator.vibrate(80);
    fetch('/api/buzz?name=' + encodeURIComponent(playerName), { method: 'POST' })
      .then(res => res.json())
      .then(data => {
        poll(); // immediate poll to update view
      });
  }

  function startPolling() {
    if (pollInterval) clearInterval(pollInterval);
    consecutiveErrors = 0;
    poll();
    pollInterval = setInterval(poll, 280);
  }

  function poll() {
    if (!playerName) return;
    fetch('/api/poll?name=' + encodeURIComponent(playerName))
      .then(res => res.json())
      .then(state => {
        consecutiveErrors = 0;

        if (state.sessionId) {
          if (currentSessionId && currentSessionId !== state.sessionId) {
            handleGameClosed('The previous game ended and a new game has started.');
            return;
          }
          currentSessionId = state.sessionId;
          localStorage.setItem('jeopardy_session_id', currentSessionId);
        }

        if (state.status === 'GAME_CLOSED') {
          handleGameClosed('The host closed the game on the computer.');
          return;
        }

        if (state.status === 'KICKED') {
          handleKicked();
          return;
        }

        renderState(state);
      })
      .catch(() => {
        consecutiveErrors++;
        if (consecutiveErrors >= 12) {
          handleGameClosed('Connection lost. The host application was closed.');
        } else {
          document.getElementById('connectionStatus').textContent = 'Connecting...';
        }
      });
  }

  function handleKicked() {
    if (pollInterval) clearInterval(pollInterval);
    localStorage.removeItem('jeopardy_player_name');
    localStorage.removeItem('jeopardy_session_id');
    const oldName = playerName;
    playerName = "";
    document.getElementById('playerHeader').style.display = 'none';
    document.getElementById('changeNameLink').style.display = 'none';

    showView('viewStatus');
    const icon = document.getElementById('statusIcon');
    const title = document.getElementById('statusTitle');
    const desc = document.getElementById('statusDesc');
    const actionBtn = document.getElementById('statusActionBtn');

    icon.innerHTML = '&#128683;';
    title.textContent = 'YOU WERE KICKED';
    title.className = 'status-title danger-banner';
    desc.textContent = (oldName ? oldName + ', you' : 'You') + ' were removed from the lobby by the host.';
    actionBtn.style.display = 'block';
    actionBtn.textContent = 'Rejoin Game';
    actionBtn.onclick = () => {
      location.reload();
    };
    document.getElementById('connectionStatus').textContent = 'Removed by host';
  }

  function handleGameClosed(reason) {
    if (pollInterval) clearInterval(pollInterval);
    localStorage.removeItem('jeopardy_player_name');
    localStorage.removeItem('jeopardy_session_id');
    playerName = "";
    document.getElementById('playerHeader').style.display = 'none';
    document.getElementById('changeNameLink').style.display = 'none';

    showView('viewStatus');
    const icon = document.getElementById('statusIcon');
    const title = document.getElementById('statusTitle');
    const desc = document.getElementById('statusDesc');
    const actionBtn = document.getElementById('statusActionBtn');

    icon.innerHTML = '&#127937;';
    title.textContent = 'Game Ended';
    title.className = 'status-title warning-banner';
    desc.textContent = reason || 'The game has closed on the host computer.';
    actionBtn.style.display = 'block';
    actionBtn.textContent = 'Join New Game';
    actionBtn.onclick = () => {
      location.reload();
    };
    document.getElementById('connectionStatus').textContent = 'Game Ended';
  }

  function renderState(state) {
    document.getElementById('connectionStatus').textContent = 'Connected: Wi-Fi Live';
    document.getElementById('statusActionBtn').style.display = 'none';
    
    if (state.status === 'BUZZ_OPEN') {
      showView('viewBuzzer');
    } else {
      showView('viewStatus');
      const icon = document.getElementById('statusIcon');
      const title = document.getElementById('statusTitle');
      const desc = document.getElementById('statusDesc');

      if (state.status === 'REGISTRATION') {
        icon.innerHTML = '&#9203;';
        title.textContent = 'Ready!';
        title.className = 'status-title warning-banner';
        desc.textContent = 'Waiting for host to start the game...';
      } else if (state.status === 'BOARD') {
        icon.innerHTML = '&#128250;';
        title.textContent = 'Look at Board';
        title.className = 'status-title';
        desc.textContent = 'A contestant is choosing a tile...';
      } else if (state.status === 'YOU_BUZZED') {
        icon.innerHTML = '&#128276;';
        title.textContent = 'YOU BUZZED IN!';
        title.className = 'status-title success-banner';
        desc.textContent = 'State your answer now!';
        if (navigator.vibrate) navigator.vibrate([100, 50, 100]);
      } else if (state.status === 'OTHER_BUZZED') {
        icon.innerHTML = '&#128064;';
        title.textContent = state.winner + ' Buzzed!';
        title.className = 'status-title warning-banner';
        desc.textContent = 'Listening to their answer...';
      } else if (state.status === 'LOCKED_OUT') {
        icon.innerHTML = '&#10060;';
        title.textContent = 'Incorrect';
        title.className = 'status-title danger-banner';
        desc.textContent = 'You are locked out for this question.';
      } else if (state.status === 'NOT_BUZZABLE') {
        icon.innerHTML = '&#127942;';
        title.textContent = 'Competitive Round';
        title.className = 'status-title';
        desc.textContent = 'No buzzers for this challenge. Follow host instructions!';
      } else {
        icon.innerHTML = '&#10067;';
        title.textContent = 'Question Active';
        title.className = 'status-title';
        desc.textContent = 'Listen carefully to the clue...';
      }
    }
  }
</script>
</body>
</html>
""";
    }
}
