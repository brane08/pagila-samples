// ── User identity ──────────────────────────────────────────────────────────────

function getOrCreateUserId() {
  let id = localStorage.getItem("pagila_user_id");
  if (!id) {
    id = crypto.randomUUID();
    localStorage.setItem("pagila_user_id", id);
  }
  return id;
}

// ── Helpers ───────────────────────────────────────────────────────────────────

function scrollToBottom() {
  const area = document.getElementById("chat-messages");
  if (area) area.scrollTop = area.scrollHeight;
}

function nowTime() {
  return new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
}

function removePlaceholder() {
  const el = document.getElementById("chat-placeholder");
  if (el) el.remove();
}

function addBubble(role, content) {
  removePlaceholder();
  const area = document.getElementById("chat-messages");
  const wrapper = document.createElement("div");
  wrapper.className = role === "user"
    ? "d-flex flex-column align-items-end"
    : "d-flex flex-column align-items-start";

  const bubble = document.createElement("div");
  if (role === "user") {
    bubble.className = "bubble-user rounded-3 px-3 py-2 text-white bg-primary";
    bubble.style.maxWidth = "75%";
    if (content) bubble.textContent = content;
  } else {
    bubble.className = "bubble-ai rounded-3 px-3 py-2 bg-white border";
    bubble.style.maxWidth = "85%";
    const inner = document.createElement("div");
    inner.className = "ai-content";
    if (content) inner.textContent = content;
    bubble.appendChild(inner);
  }

  const time = document.createElement("div");
  time.className = "bubble-time";
  time.textContent = nowTime();

  wrapper.appendChild(bubble);
  wrapper.appendChild(time);
  area.appendChild(wrapper);
  scrollToBottom();
  return role === "user" ? bubble : bubble.querySelector(".ai-content");
}

function showTyping() {
  const area = document.getElementById("chat-messages");
  const el = document.createElement("div");
  el.id = "typing-indicator";
  el.className = "d-flex align-items-start";
  el.innerHTML = `
    <div class="bubble-ai rounded-3 px-3 py-2 bg-white border">
      <div class="typing-dots"><span></span><span></span><span></span></div>
    </div>`;
  area.appendChild(el);
  scrollToBottom();
}

function hideTyping() {
  const el = document.getElementById("typing-indicator");
  if (el) el.remove();
}

function setInputEnabled(enabled) {
  const btn = document.getElementById("send-btn");
  const input = document.getElementById("user-input");
  if (btn) btn.disabled = !enabled;
  if (input) input.disabled = !enabled;
}

// ── Structured JSON → table ───────────────────────────────────────────────────

const TABLE_COLUMNS = {
  film_list:     ["title", "rating", "rental_rate", "length"],
  actor_list:    ["first_name", "last_name", "film_count"],
  rental_list:   ["title", "rental_date", "return_date", "is_outstanding"],
  customer_list: ["first_name", "last_name", "email", "store_id"],
  store_list:    ["store_id", "city", "manager", "film_count"],
};

function jsonBlockToTable(text) {
  return text.replace(/```json\n(\{[\s\S]*?\})\n```/g, (match, raw) => {
    try {
      const data = JSON.parse(raw);
      const cols = TABLE_COLUMNS[data.type];
      if (!cols || !Array.isArray(data.items)) return match;
      const header = cols.map(c => `<th>${c}</th>`).join("");
      const rows = data.items.map(item =>
        `<tr>${cols.map(c => `<td>${item[c] ?? ""}</td>`).join("")}</tr>`
      ).join("");
      return `<table class="ai-table table table-sm table-bordered"><thead class="table-light"><tr>${header}</tr></thead><tbody>${rows}</tbody></table>`;
    } catch {
      return match;
    }
  });
}

function finalizeMessage(contentEl, rawText) {
  const withTables = jsonBlockToTable(rawText);
  contentEl.innerHTML = marked.parse(withTables);
  scrollToBottom();
}

// ── SSE stream reader ─────────────────────────────────────────────────────────

async function readStream(response, contentEl) {
  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let rawText = "";
  let buffer = "";

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    buffer += decoder.decode(value, { stream: true });
    const lines = buffer.split("\n");
    buffer = lines.pop();

    for (const line of lines) {
      if (!line.startsWith("data: ")) continue;
      let event;
      try { event = JSON.parse(line.slice(6)); } catch { continue; }

      if (event.type === "token") {
        rawText += event.content;
        contentEl.textContent = rawText;
        scrollToBottom();
      } else if (event.type === "tool_start") {
        const area = document.getElementById("chat-messages");
        const badge = document.createElement("div");
        badge.className = "d-flex justify-content-center";
        badge.innerHTML = `<span class="badge text-bg-warning text-dark"><i class="bi bi-gear-fill"></i> ${event.tool}</span>`;
        area.insertBefore(badge, contentEl.closest(".d-flex"));
        scrollToBottom();
      } else if (event.type === "tool_confirm") {
        renderToolConfirm(event);
      } else if (event.type === "done") {
        finalizeMessage(contentEl, rawText);
        hideTyping();
        setInputEnabled(true);
        // Fix 3: refresh sidebar so new session appears
        htmx.ajax("GET", "/ui/partials/sessions", { target: "#sessions-sidebar", swap: "innerHTML" });
      }
    }
  }
}

// ── Send message ──────────────────────────────────────────────────────────────

async function sendMessage() {
  const input = document.getElementById("user-input");
  const text = input.value.trim();
  if (!text) return;
  input.value = "";
  input.style.height = "auto"; // Fix 5: reset height after send
  setInputEnabled(false);

  addBubble("user", text);
  const aiContentEl = addBubble("ai", "");
  showTyping();

  const userId = getOrCreateUserId();

  try {
    const response = await fetch("/chat/stream", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ message: text, thread_id: THREAD_ID, user_id: userId }),
    });
    await readStream(response, aiContentEl);
  } catch (err) {
    hideTyping();
    aiContentEl.textContent = "Error: " + err.message;
    setInputEnabled(true);
  }
}

// ── Tool confirmation ─────────────────────────────────────────────────────────

function renderToolConfirm(event) {
  const tc = event.tool_calls?.[0] || {};
  const confirmEl = document.getElementById("tool-confirm");
  confirmEl.innerHTML = `
    <div class="card border-primary shadow-sm" style="max-width:600px">
      <div class="card-header bg-primary-subtle d-flex align-items-center gap-2">
        <i class="bi bi-wrench-adjustable text-primary"></i>
        <strong class="small">Tool Confirmation</strong>
      </div>
      <div class="card-body py-2">
        <p class="mb-1 fw-medium small">${tc.name || "unknown"}</p>
        <div class="bg-light rounded p-2">
          <pre class="mb-0 small overflow-auto" style="max-height:90px">${JSON.stringify(tc.args || {}, null, 2)}</pre>
        </div>
      </div>
      <div class="card-footer d-flex justify-content-end gap-2 py-2">
        <button class="btn btn-sm btn-outline-secondary" onclick="confirmTool(false)">
          <i class="bi bi-x-lg"></i> Reject
        </button>
        <button class="btn btn-sm btn-primary" onclick="confirmTool(true)">
          <i class="bi bi-check-lg"></i> Approve
        </button>
      </div>
    </div>`;
  confirmEl.style.display = "block";
  hideTyping();
}

async function confirmTool(approved) {
  const confirmEl = document.getElementById("tool-confirm");
  confirmEl.style.display = "none";
  setInputEnabled(false);
  showTyping();

  const aiContentEl = addBubble("ai", "");

  try {
    const response = await fetch(`/chat/confirm/${THREAD_ID}/stream`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ approved }),
    });
    await readStream(response, aiContentEl);
  } catch (err) {
    hideTyping();
    aiContentEl.textContent = "Error: " + err.message;
    setInputEnabled(true);
  }
}

// ── New session ───────────────────────────────────────────────────────────────

function newSession() {
  const id = crypto.randomUUID();
  window.location.href = `/ui?thread_id=${id}`;
}

// ── Init ──────────────────────────────────────────────────────────────────────

document.addEventListener("DOMContentLoaded", () => {
  const input = document.getElementById("user-input");
  if (input) {
    // Fix 5: Enter to send
    input.addEventListener("keydown", (e) => {
      if (e.key === "Enter" && !e.shiftKey) {
        e.preventDefault();
        sendMessage();
      }
    });
    // Fix 5: auto-resize
    input.addEventListener("input", () => {
      input.style.height = "auto";
      input.style.height = Math.min(input.scrollHeight, 150) + "px";
    });
  }

  // Fix 2: scroll to bottom after HTMX swaps history into #chat-messages
  document.addEventListener("htmx:afterSwap", (e) => {
    if (e.detail.target.id === "chat-messages") {
      scrollToBottom();
    }
  });
});
