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

function escapeHtml(value) {
  return String(value)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

function safeHref(href) {
  try {
    const url = new URL(href, window.location.origin);
    return ["http:", "https:", "mailto:"].includes(url.protocol) ? href : null;
  } catch {
    return null;
  }
}

// Model output is untrusted: escape raw HTML and drop non-http(s)/mailto links.
marked.use({
  renderer: {
    html(token) {
      return escapeHtml(token.text ?? token.raw ?? "");
    },
    link(token) {
      const inner = this.parser.parseInline(token.tokens);
      const href = safeHref(token.href);
      if (!href) return inner;
      return `<a href="${escapeHtml(href)}" target="_blank" rel="noopener noreferrer">${inner}</a>`;
    },
    // Remote images auto-load and can exfiltrate data via the URL: show the alt text only.
    image(token) {
      return escapeHtml(token.text ?? "");
    },
  },
});

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

const TABLE_TOKEN = "PAGILATABLETOKEN";

// Replaces known ```json blocks with placeholder tokens; the (escaped) tables are re-inserted after markdown parsing.
function jsonBlockToTable(text, tables) {
  return text.replace(/```json\n(\{[\s\S]*?\})\n```/g, (match, raw) => {
    try {
      const data = JSON.parse(raw);
      const cols = TABLE_COLUMNS[data.type];
      if (!cols || !Array.isArray(data.items)) return match;
      const header = cols.map(c => `<th>${escapeHtml(c)}</th>`).join("");
      const rows = data.items
        .filter(item => item && typeof item === "object")
        .map(item => `<tr>${cols.map(c => `<td>${escapeHtml(item[c] ?? "")}</td>`).join("")}</tr>`)
        .join("");
      tables.push(`<table class="ai-table table table-sm table-bordered"><thead class="table-light"><tr>${header}</tr></thead><tbody>${rows}</tbody></table>`);
      return `\n\n${TABLE_TOKEN}${tables.length - 1}\n\n`;
    } catch {
      return match;
    }
  });
}

function finalizeMessage(contentEl, rawText) {
  const tables = [];
  let html = marked.parse(jsonBlockToTable(rawText, tables));
  tables.forEach((table, i) => {
    const token = `${TABLE_TOKEN}${i}`;
    html = html.replace(`<p>${token}</p>`, table).replace(token, table);
  });
  contentEl.innerHTML = html;
  scrollToBottom();
}

// ── SSE stream reader ─────────────────────────────────────────────────────────

async function readStream(response, contentEl) {
  if (!response.ok) {
    let detail = null;
    try { detail = (await response.json()).detail; } catch { /* non-JSON error body */ }
    if (detail && Array.isArray(detail.tool_calls)) {
      renderToolConfirm({ tool_calls: detail.tool_calls, confirm_id: detail.confirm_id });
      throw new Error("A tool call is awaiting your confirmation");
    }
    const message = typeof detail === "string" ? detail : detail?.message;
    throw new Error(message ?? `Request failed (HTTP ${response.status})`);
  }
  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let rawText = "";
  let buffer = "";
  let finished = false;

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

      if (event.type === "reset") {
        rawText = "";
        contentEl.textContent = "";
      } else if (event.type === "token") {
        rawText += event.content;
        contentEl.textContent = rawText;
        scrollToBottom();
      } else if (event.type === "tool_start") {
        const area = document.getElementById("chat-messages");
        const badge = document.createElement("div");
        badge.className = "d-flex justify-content-center";
        const label = document.createElement("span");
        label.className = "badge text-bg-warning text-dark";
        const icon = document.createElement("i");
        icon.className = "bi bi-gear-fill";
        label.append(icon, " ", String(event.tool ?? ""));
        badge.appendChild(label);
        area.insertBefore(badge, contentEl.closest(".d-flex"));
        scrollToBottom();
      } else if (event.type === "tool_confirm") {
        finished = true;
        renderToolConfirm(event);
      } else if (event.type === "error") {
        finished = true;
        contentEl.textContent = "Error: " + (event.content ?? event.message ?? "request failed");
        hideTyping();
        setInputEnabled(true);
        // The approval may still be pending (stale/duplicate confirm, dropped run): show the real state.
        restorePendingCard();
      } else if (event.type === "done") {
        finished = true;
        finalizeMessage(contentEl, rawText);
        hideTyping();
        setInputEnabled(true);
        // Fix 3: refresh sidebar so new session appears
        htmx.ajax("GET", "/ui/partials/sessions", { target: "#sessions-sidebar", swap: "innerHTML" });
      }
    }
  }

  // Stream closed without done/error/tool_confirm: don't leave the input disabled.
  if (!finished) {
    hideTyping();
    setInputEnabled(true);
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

let currentConfirmId = null;

// Fetch the server's pending approval (if any) and show it; the server is the source of truth.
function restorePendingCard() {
  return fetch(`/chat/pending/${encodeURIComponent(THREAD_ID)}`)
    .then((r) => (r.ok ? r.json() : null))
    .then((data) => {
      if (data && data.pending) renderToolConfirm({ tool_calls: data.tool_calls, confirm_id: data.confirm_id });
    })
    .catch(() => {});
}

function renderToolConfirm(event) {
  const hasCalls = Array.isArray(event.tool_calls) && event.tool_calls.length > 0;
  const calls = hasCalls ? event.tool_calls : [];
  currentConfirmId = event.confirm_id ?? null;
  setInputEnabled(false);
  const confirmEl = document.getElementById("tool-confirm");
  confirmEl.innerHTML = `
    <div class="card border-primary shadow-sm" style="max-width:600px">
      <div class="card-header bg-primary-subtle d-flex align-items-center gap-2">
        <i class="bi bi-wrench-adjustable text-primary"></i>
        <strong class="small">Tool Confirmation</strong>
      </div>
      <div class="card-body py-2">
        <p class="small text-muted mb-2" id="tool-confirm-hint"></p>
        <div id="tool-confirm-calls"></div>
      </div>
      <div class="card-footer d-flex justify-content-end gap-2 py-2">
        <button class="btn btn-sm btn-outline-secondary" id="tool-reject" onclick="confirmTool(false)">
          <i class="bi bi-x-lg"></i> Reject
        </button>
        <button class="btn btn-sm btn-primary" id="tool-approve" onclick="confirmTool(true)">
          <i class="bi bi-check-lg"></i> Approve
        </button>
      </div>
    </div>`;
  document.getElementById("tool-confirm-hint").textContent = hasCalls
    ? `Approving runs ${calls.length === 1 ? "this tool call" : `all ${calls.length} tool calls`} exactly as shown. Rejecting cancels ${calls.length === 1 ? "it" : "them"} and the assistant continues without the result.`
    : "The pending request could not be displayed. You can only reject it.";
  if (!hasCalls) document.getElementById("tool-approve").disabled = true;
  const list = document.getElementById("tool-confirm-calls");
  calls.forEach((tc) => {
    const name = document.createElement("p");
    name.className = "mb-1 fw-medium small";
    name.textContent = tc.name || "unknown";
    const box = document.createElement("div");
    box.className = "bg-light rounded p-2 mb-2";
    const args = document.createElement("pre");
    args.className = "mb-0 small overflow-auto";
    args.style.maxHeight = "240px";
    args.textContent = JSON.stringify(tc.args || {}, null, 2);
    box.appendChild(args);
    list.append(name, box);
  });
  confirmEl.style.display = "block";
  hideTyping();
}

async function confirmTool(approved) {
  const confirmEl = document.getElementById("tool-confirm");
  document.getElementById("tool-approve")?.setAttribute("disabled", "");
  document.getElementById("tool-reject")?.setAttribute("disabled", "");
  confirmEl.style.display = "none";
  setInputEnabled(false);
  showTyping();

  const aiContentEl = addBubble("ai", "");

  try {
    const response = await fetch(`/chat/confirm/${encodeURIComponent(THREAD_ID)}/stream`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ approved, confirm_id: currentConfirmId }),
    });
    await readStream(response, aiContentEl);
  } catch (err) {
    hideTyping();
    aiContentEl.textContent = "Error: " + err.message;
    setInputEnabled(true);
    restorePendingCard();
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

  // Restore the confirm card if a tool call is still awaiting approval (reload, navigation, dropped stream).
  restorePendingCard();

  // Fix 2: scroll to bottom after HTMX swaps history into #chat-messages
  document.addEventListener("htmx:afterSwap", (e) => {
    if (e.detail.target.id === "chat-messages") {
      scrollToBottom();
    }
  });
});
