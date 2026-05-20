/**
 * Shared API mock helpers. All Quarkus/backend calls go to http://localhost:8001.
 * The chat agent calls go to http://localhost:8000.
 * Tests use page.route() interception so no real backend is required.
 */
import { Page } from '@playwright/test';

const API = 'http://localhost:8001';

// ── Film mocks ─────────────────────────────────────────────────────────────────

export const MOCK_FILMS_PAGE = {
  success: true,
  data: [
    { filmId: 1, title: 'ACADEMY DINOSAUR', description: 'A Epic Drama of a Feminist', category: 'Documentary', language: 'English', price: 0.99, length: 86, rating: 'PG', lastUpdate: '2013-05-26T14:50:58.951', actors: 'PENELOPE GUINESS' },
    { filmId: 2, title: 'ACE GOLDFINGER', description: 'A Astounding Epistle of a Database Administrator', category: 'Horror', language: 'English', price: 4.99, length: 48, rating: 'G', lastUpdate: '2013-05-26T14:50:58.951', actors: 'BOB FAWCETT' },
    { filmId: 3, title: 'ADAPTATION HOLES', description: 'A Astounding Reflection of a Lumberjack', category: 'Documentary', language: 'English', price: 2.99, length: 50, rating: 'NC-17', lastUpdate: '2013-05-26T14:50:58.951', actors: 'NICK WAHLBERG' },
  ],
  totalCount: 1000,
};

export const MOCK_SALES_BY_CATEGORY = {
  data: [
    { category: 'Sports',       totalSales: 5314.21 },
    { category: 'Sci-Fi',       totalSales: 4756.98 },
    { category: 'Animation',    totalSales: 4612.62 },
    { category: 'Drama',        totalSales: 4587.39 },
    { category: 'Comedy',       totalSales: 4383.58 },
  ],
};

// ── Actor mocks ────────────────────────────────────────────────────────────────

export const MOCK_ACTORS_PAGE = {
  success: true,
  data: [
    { actorId: 1, firstName: 'PENELOPE', lastName: 'GUINESS', filmInfo: 'Animation, Comedy' },
    { actorId: 2, firstName: 'NICK',     lastName: 'WAHLBERG', filmInfo: 'Action, Drama' },
    { actorId: 3, firstName: 'ED',       lastName: 'CHASE',   filmInfo: 'Children, Family' },
  ],
  totalCount: 200,
};

// ── Customer mocks ─────────────────────────────────────────────────────────────

export const MOCK_CUSTOMERS_PAGE = {
  success: true,
  data: [
    { id: 1, name: 'MARY SMITH',   address: '1913 Hanoi Way', zipCode: '35200', phone: '28303384290', city: 'Sasebo',     country: 'Japan',   notes: 'active', sid: 1 },
    { id: 2, name: 'PATRICIA JOHNSON', address: '1121 Loja Ave', zipCode: '17886', phone: '838635286649', city: 'San Bernardino', country: 'United States', notes: 'active', sid: 1 },
  ],
  totalCount: 599,
};

// ── Store mocks ────────────────────────────────────────────────────────────────

export const MOCK_SALES_BY_STORE = {
  data: [
    { store: 'Mike Hillyer - Lethbridge', manager: 'Mike Hillyer', totalSales: 33726.77 },
    { store: 'Jon Stephens - Woodridge',  manager: 'Jon Stephens', totalSales: 33679.79 },
  ],
};

export const MOCK_STAFF = {
  data: [
    { id: 1, name: 'Mike Hillyer', address: '23 Workhaven Lane', zipCode: '', phone: '14033335568', city: 'Lethbridge', country: 'Canada', sid: 1 },
    { id: 2, name: 'Jon Stephens', address: '1411 Lillydale Drive', zipCode: '', phone: '16177521217', city: 'Woodridge', country: 'Australia', sid: 2 },
  ],
};

// ── Register helpers ───────────────────────────────────────────────────────────

export const MOCK_FILM_DETAIL = {
  filmId: 1,
  title: 'ACADEMY DINOSAUR',
  description: 'A Epic Drama of a Feminist And a Mad Scientist who must Battle a Teacher in The Canadian Rockies.',
  releaseYear: 2006,
  language: 'English',
  originalLanguage: null,
  rentalDuration: 6,
  rentalRate: 0.99,
  length: 86,
  replacementCost: 20.99,
  rating: 'PG',
  lastUpdate: '2013-05-26T14:50:58.951Z',
  specialFeatures: ['Deleted Scenes', 'Behind the Scenes'],
  categories: ['Documentary'],
};

export const MOCK_FILM_ACTORS = {
  success: true,
  data: [
    { actorId: 1, firstName: 'PENELOPE', lastName: 'GUINESS', lastUpdate: '2013-05-26T14:50:58.951Z' },
    { actorId: 10, firstName: 'CHRISTIAN', lastName: 'GABLE', lastUpdate: '2013-05-26T14:50:58.951Z' },
  ],
};

export async function mockFilmDetailApi(page: Page, filmId = 1) {
  await page.route(`${API}/films/${filmId}`, route =>
    route.fulfill({ json: MOCK_FILM_DETAIL })
  );
  await page.route(`${API}/films/${filmId}/actors`, route =>
    route.fulfill({ json: MOCK_FILM_ACTORS })
  );
}

export async function mockFilmsApi(page: Page) {
  await page.route(`${API}/films/@view*`, route =>
    route.fulfill({ json: MOCK_FILMS_PAGE })
  );
  await page.route(`${API}/films/@nicer-view*`, route =>
    route.fulfill({ json: MOCK_FILMS_PAGE })
  );
  await page.route(`${API}/films/@sales-by-category`, route =>
    route.fulfill({ json: MOCK_SALES_BY_CATEGORY })
  );
}

export const MOCK_ACTOR_DETAIL = {
  success: true,
  data: {
    actorId: 1,
    firstName: 'PENELOPE',
    lastName: 'GUINESS',
    filmInfo: 'Animation: ACADEMY DINOSAUR, BLANKET BEVERLY; Comedy: ELEPHANT TROJAN, HALLOWEEN OTHERS'
  }
};

export const MOCK_STORE_DETAIL = {
  success: true,
  data: {
    storeId: 1,
    manager: { staffId: 1, firstName: 'Mike', lastName: 'Hillyer', email: 'Mike.Hillyer@sakilastaff.com', username: 'Mike' },
    address: {
      address: '23 Workhaven Lane',
      address2: null,
      district: 'Alberta',
      postalCode: '',
      phone: '14033335568',
      city: { city: 'Lethbridge', country: { country: 'Canada' } }
    },
    currentStaff: [
      { staffId: 1, firstName: 'Mike', lastName: 'Hillyer', email: 'Mike.Hillyer@sakilastaff.com', username: 'Mike' }
    ]
  }
};

export async function mockActorDetailApi(page: Page, actorId = 1) {
  await page.route(`${API}/actors/${actorId}`, route =>
    route.fulfill({ json: MOCK_ACTOR_DETAIL })
  );
}

export async function mockStoreDetailApi(page: Page, storeId = 1) {
  await page.route(`${API}/stores/${storeId}`, route =>
    route.fulfill({ json: MOCK_STORE_DETAIL })
  );
}

export async function mockActorsApi(page: Page) {
  // Register wildcard fallback first (lower LIFO priority), specific pattern last (wins)
  await page.route(`${API}/actors/*`, route =>
    route.fulfill({ json: MOCK_ACTOR_DETAIL })
  );
  await page.route(`${API}/actors/@view*`, route =>
    route.fulfill({ json: MOCK_ACTORS_PAGE })
  );
}

export async function mockCustomersApi(page: Page) {
  await page.route(`${API}/rentals/@customers*`, route =>
    route.fulfill({ json: MOCK_CUSTOMERS_PAGE })
  );
}

export async function mockStoresApi(page: Page) {
  // Register wildcard fallback first (lower LIFO priority), specific patterns last (win)
  await page.route(`${API}/stores/*`, route =>
    route.fulfill({ json: MOCK_STORE_DETAIL })
  );
  await page.route(`${API}/stores/@sales-by-store`, route =>
    route.fulfill({ json: MOCK_SALES_BY_STORE })
  );
  await page.route(`${API}/stores/@staff`, route =>
    route.fulfill({ json: MOCK_STAFF })
  );
}

export async function mockHomeApi(page: Page) {
  await mockFilmsApi(page);
  await mockStoresApi(page);
}

/** Silently absorb any unmatched backend or chat-agent requests (avoids ECONNREFUSED noise). */
export async function abortBackendRequests(page: Page) {
  await page.route('http://localhost:8001/**', route => route.abort());
  await page.route('http://localhost:8000/**', route => route.abort());
}

// ── Chat agent mocks ───────────────────────────────────────────────────────────

const CHAT_API = 'http://localhost:8000';

export const MOCK_SESSIONS = {
  sessions: [
    { thread_id: 'session-1716000000001', step_count: 4, last_active: '2026-05-18T10:00:00.000Z' },
    { thread_id: 'session-1716000000002', step_count: 2, last_active: '2026-05-17T14:30:00.000Z' },
  ],
  total: 2,
};

/** Builds a minimal SSE response body from an array of event payloads. */
export function sseBody(events: object[]): string {
  return events.map(e => `data: ${JSON.stringify(e)}\n\n`).join('');
}

export async function mockChatStream(page: Page, events: object[]) {
  await page.route(`${CHAT_API}/chat/stream`, route =>
    route.fulfill({
      status: 200,
      headers: { 'Content-Type': 'text/event-stream', 'Cache-Control': 'no-cache' },
      body: sseBody(events),
    })
  );
}

export async function mockSessionsApi(page: Page) {
  await page.route(`${CHAT_API}/sessions`, route =>
    route.fulfill({ json: MOCK_SESSIONS })
  );
  // Absorb individual session GETs and DELETEs
  await page.route(`${CHAT_API}/sessions/**`, route => route.fulfill({ json: { deleted: true } }));
}
