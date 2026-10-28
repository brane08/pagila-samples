"""Input hygiene shared by the MCP servers: LIKE-wildcard escaping and result-size clamping."""

MAX_LIMIT = 100
LIKE_ESCAPE = "\\"


def clamp(limit: int) -> int:
    return max(1, min(int(limit), MAX_LIMIT))


def like(value: str) -> str:
    """Escape LIKE wildcards so user text matches literally; pair with escape=LIKE_ESCAPE."""
    return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
