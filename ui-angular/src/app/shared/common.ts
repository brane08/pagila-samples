export class Constants {
  public static API_BASE = "http://localhost:8001/rest";
  /** HTMX chat and analytics UI served by genai-assistant. */
  public static ASSISTANT_UI_URL = "http://localhost:8000/ui";
}

export function defaultPages() {
  return [10, 25, 50, 100];
}
