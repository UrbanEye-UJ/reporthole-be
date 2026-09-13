/**
 * k6 Regression — Reporthole
 *
 * Flow A  Registration smoke (always runs — no login needed)
 *   A1  POST /auth/register  → verifies 201 and correct error on duplicate email
 *
 * Flow B  Civilian (needs CIVILIAN_* secrets — pre-existing verified account)
 *   B1  POST /auth/login
 *   B2  GET  /user/profile
 *   B3  PATCH /user/profile
 *   B4  POST /incidents/create              (forceCreate — first report)
 *   B5  POST /incidents/create              (same coords, no forceCreate → duplicate)
 *   B6  POST /incidents/{id}/confirm        (confirm the duplicate)
 *   B7  GET  /incidents/my
 *   B8  GET  /incidents/my/search
 *   B9  GET  /incidents/{id}
 *   B10 GET  /incidents/nearby
 *
 * Flow C  Admin + Contractor lifecycle (needs ADMIN_* and CONTRACTOR_* secrets)
 *   C1  Contractor login  → capture contractorUserId
 *   C2  Admin login
 *   C3  POST /incidents/{id}/verify         (REPORTED → VERIFIED)
 *   C4  POST /incidents/{id}/assign         (VERIFIED  → ASSIGNED)
 *   C5  GET  /incidents/my-assignments      (contractor sees it)
 *   C6  POST /incidents/{id}/accept         (ASSIGNED  → IN_PROGRESS)
 *   C7  POST /incidents/{id}/progress       (add a progress note)
 *   C8  POST /incidents/{id}/resolve        (IN_PROGRESS → RESOLVED)
 *   C9  POST /incidents/{id}/still-unresolved (civilian reopens)
 *   C10 GET  /incidents/recent              (admin view)
 *   C11 GET  /incidents/stats
 *   C12 GET  /admin/contractors
 *
 * Secrets required in GitHub (Settings → Secrets → Actions):
 *   APP_BASE_URL          — hosted API base, e.g. https://your-host/api
 *   CIVILIAN_EMAIL        — verified CIVILIAN test account email
 *   CIVILIAN_PASSWORD     — verified CIVILIAN test account password
 *   ADMIN_EMAIL           — verified ADMIN test account email
 *   ADMIN_PASSWORD        — verified ADMIN test account password
 *   CONTRACTOR_EMAIL      — verified CONTRACTOR test account email
 *   CONTRACTOR_PASSWORD   — verified CONTRACTOR test account password
 *
 * Run locally:
 *   k6 run \
 *     -e BASE_URL=http://localhost:8080/api \
 *     -e CIVILIAN_EMAIL=civilian@test.com \
 *     -e CIVILIAN_PASSWORD=Test@1234 \
 *     -e ADMIN_EMAIL=admin@test.com \
 *     -e ADMIN_PASSWORD=Test@1234 \
 *     -e CONTRACTOR_EMAIL=contractor@test.com \
 *     -e CONTRACTOR_PASSWORD=Test@1234 \
 *     k6/regression.js
 */

import http from 'k6/http';
import { check, group } from 'k6';
import { randomString } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

// ── Config ────────────────────────────────────────────────────────────────────

const BASE_URL             = __ENV.BASE_URL             || 'http://localhost:8080/api';
const CIVILIAN_EMAIL       = __ENV.CIVILIAN_EMAIL       || '';
const CIVILIAN_PASSWORD    = __ENV.CIVILIAN_PASSWORD    || '';
const ADMIN_EMAIL          = __ENV.ADMIN_EMAIL          || '';
const ADMIN_PASSWORD       = __ENV.ADMIN_PASSWORD       || '';
const CONTRACTOR_EMAIL     = __ENV.CONTRACTOR_EMAIL     || '';
const CONTRACTOR_PASSWORD  = __ENV.CONTRACTOR_PASSWORD  || '';

export const options = {
  vus: 1,
  iterations: 1,
  thresholds: {
    http_req_failed:   ['rate==0'],
    http_req_duration: ['p(99)<5000'],
    checks:            ['rate==1'],
  },
};

// ── Helpers ───────────────────────────────────────────────────────────────────

const JSON_HEADERS = { 'Content-Type': 'application/json' };

function auth(token) {
  return { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` };
}

/** Minimal 1×1 white PNG — same one used by the Spring integration tests. */
const PNG =
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwADhQGAWjR9awAAAABJRU5ErkJggg==';

function loginAs(email, password, label) {
  const res = http.post(
    `${BASE_URL}/auth/login`,
    JSON.stringify({ email, password }),
    { headers: JSON_HEADERS }
  );
  check(res, {
    [`${label} login → 200`]: (r) => r.status === 200,
  });
  try {
    return { token: res.json('data.token') || '', userId: String(res.json('data.userId') || '') };
  } catch {
    return { token: '', userId: '' };
  }
}

// ── Flow A — Registration smoke ───────────────────────────────────────────────

function flowRegistration() {
  const email    = `k6-${randomString(8)}@reporthole-test.local`;
  const password = 'K6test@123';

  // A1a — Valid registration returns 201
  group('A1a. Register (valid)', () => {
    const res = http.post(`${BASE_URL}/auth/register`, JSON.stringify({
      firstName: 'K6', lastName: 'Test',
      email, role: 'CIVILIAN', password,
      phoneNumber: '0600000000',
    }), { headers: JSON_HEADERS });

    check(res, { 'register valid → 201': (r) => r.status === 201 });
  });

  // A1b — Active-account duplicate returns 400
  // (PENDING re-registration returns 201 with a resent verification email — not a useful 400 test)
  group('A1b. Register (active-account duplicate → 400)', () => {
    const activeEmail = CIVILIAN_EMAIL || 'civilian@reporthole-test.local';
    const res = http.post(`${BASE_URL}/auth/register`, JSON.stringify({
      firstName: 'K6', lastName: 'Test',
      email: activeEmail, role: 'CIVILIAN', password,
      phoneNumber: '0600000000',
    }), { headers: JSON_HEADERS, responseCallback: http.expectedStatuses(400) });

    check(res, { 'register active duplicate → 400': (r) => r.status === 400 });
  });

  // A1c — Missing required field returns 400
  group('A1c. Register (missing field → 400)', () => {
    const res = http.post(`${BASE_URL}/auth/register`, JSON.stringify({
      email: `k6-${randomString(6)}@reporthole-test.local`,
      password,
      role: 'CIVILIAN',
      // firstName, lastName, phoneNumber omitted intentionally
    }), { headers: JSON_HEADERS, responseCallback: http.expectedStatuses(400) });

    check(res, { 'register incomplete → 400': (r) => r.status === 400 });
  });
}

// ── Flow B — Civilian ─────────────────────────────────────────────────────────

function flowCivilian(state) {
  if (!CIVILIAN_EMAIL || !CIVILIAN_PASSWORD) return;

  // B1 — Login
  group('B1. Civilian login', () => {
    const res = http.post(`${BASE_URL}/auth/login`,
      JSON.stringify({ email: CIVILIAN_EMAIL, password: CIVILIAN_PASSWORD }),
      { headers: JSON_HEADERS });

    check(res, {
      'civilian login → 200': (r) => r.status === 200,
      'civilian login → token': (r) => { try { return typeof r.json('data.token') === 'string'; } catch { return false; } },
      'civilian login → role CIVILIAN': (r) => { try { return r.json('data.role') === 'CIVILIAN'; } catch { return false; } },
    });

    try { state.civilianToken = res.json('data.token'); } catch { /* */ }
  });

  if (!state.civilianToken) return;

  // B2 — Get profile
  group('B2. GET /users/profile', () => {
    const res = http.get(`${BASE_URL}/users/profile`, { headers: auth(state.civilianToken) });

    check(res, {
      'profile → 200': (r) => r.status === 200,
      'profile → data object': (r) => { try { return typeof r.json('data') === 'object'; } catch { return false; } },
    });
  });

  // B3 — Update profile
  group('B3. PATCH /users/profile', () => {
    const res = http.patch(`${BASE_URL}/users/profile`,
      JSON.stringify({ firstName: 'K6', lastName: 'Regression', phoneNumber: '0611111111' }),
      { headers: auth(state.civilianToken) });

    check(res, { 'update profile → 200': (r) => r.status === 200 });
  });

  // B4 — Create incident (forceCreate bypasses duplicate check)
  group('B4. Create incident', () => {
    const res = http.post(`${BASE_URL}/incidents/create`, JSON.stringify({
      incidentType: 'POTHOLE',
      description: 'k6 regression pothole — safe to delete',
      source: 'MANUAL',
      latitude: -26.2041, longitude: 28.0473,
      imageBase64: PNG,
      forceCreate: true,
      locationAddress: 'Regression St, Johannesburg',
    }), { headers: auth(state.civilianToken) });

    check(res, {
      'create → 201': (r) => r.status === 201,
      'create → incidentId': (r) => { try { return typeof r.json('data.incidentId') === 'string'; } catch { return false; } },
    });

    try { state.incidentId = res.json('data.incidentId'); } catch { /* */ }
  });

  // B5 — Duplicate detection
  group('B5. Create duplicate (expect duplicate flag)', () => {
    const res = http.post(`${BASE_URL}/incidents/create`, JSON.stringify({
      incidentType: 'POTHOLE',
      description: 'k6 duplicate probe',
      source: 'MANUAL',
      latitude: -26.2041, longitude: 28.0473,
      imageBase64: PNG,
      forceCreate: false,
    }), { headers: auth(state.civilianToken) });

    check(res, {
      'duplicate → 200': (r) => r.status === 200,
      'duplicate → flagged': (r) => { try { return r.json('data.duplicate') === true; } catch { return false; } },
    });

    try { state.existingIncidentId = res.json('data.existingIncidentId'); } catch { /* */ }
  });

  // B6 — Confirm duplicate
  if (state.existingIncidentId) {
    group('B6. Confirm duplicate', () => {
      const res = http.post(
        `${BASE_URL}/incidents/${state.existingIncidentId}/confirm`,
        null,
        { headers: auth(state.civilianToken) }
      );

      check(res, {
        'confirm → 200': (r) => r.status === 200,
        // reportCount is the de-duplication tally; the same user cannot be added to reporterCount twice
        'confirm → reportCount >= 1': (r) => { try { return (r.json('data.reportCount') ?? 0) >= 1; } catch { return false; } },
      });
    });
  }

  // B7 — My incidents
  group('B7. GET /incidents/my', () => {
    const res = http.get(`${BASE_URL}/incidents/my`, { headers: auth(state.civilianToken) });

    check(res, {
      'my incidents → 200': (r) => r.status === 200,
      'my incidents → array': (r) => { try { return Array.isArray(r.json('data')); } catch { return false; } },
      'my incidents → contains created': (r) => {
        try { return r.json('data').some((i) => i.incidentId === state.incidentId); } catch { return false; }
      },
    });
  });

  // B8 — Search my incidents
  group('B8. GET /incidents/my/search', () => {
    const res = http.get(
      `${BASE_URL}/incidents/my/search?keyword=pothole`,
      { headers: auth(state.civilianToken) }
    );

    check(res, {
      'search → 200': (r) => r.status === 200,
      'search → array': (r) => { try { return Array.isArray(r.json('data')); } catch { return false; } },
    });
  });

  // B9 — Get by ID
  if (state.incidentId) {
    group('B9. GET /incidents/{id}', () => {
      const res = http.get(`${BASE_URL}/incidents/${state.incidentId}`, { headers: auth(state.civilianToken) });

      check(res, {
        'get by id → 200': (r) => r.status === 200,
        'get by id → correct id': (r) => { try { return r.json('data.incidentId') === state.incidentId; } catch { return false; } },
      });
    });
  }

  // B10 — Nearby
  group('B10. GET /incidents/nearby', () => {
    const res = http.get(
      `${BASE_URL}/incidents/nearby?latitude=-26.2041&longitude=28.0473&radiusMeters=1000`,
      { headers: auth(state.civilianToken) }
    );

    check(res, {
      'nearby → 200': (r) => r.status === 200,
      'nearby → array': (r) => { try { return Array.isArray(r.json('data')); } catch { return false; } },
    });
  });
}

// ── Flow C — Admin + Contractor lifecycle ─────────────────────────────────────

function flowAdminContractor(state) {
  if (!ADMIN_EMAIL || !ADMIN_PASSWORD || !CONTRACTOR_EMAIL || !CONTRACTOR_PASSWORD) return;
  if (!state.incidentId) return;

  let adminToken       = '';
  let contractorToken  = '';
  let contractorUserId = '';

  // C1 — Contractor login (need userId for the assign call)
  group('C1. Contractor login', () => {
    const { token, userId } = loginAs(CONTRACTOR_EMAIL, CONTRACTOR_PASSWORD, 'contractor');
    contractorToken  = token;
    contractorUserId = userId;

    check({ token, userId }, {
      'contractor → userId present': ({ userId: u }) => typeof u === 'string' && u.length > 0,
    });
  });

  // C2 — Admin login
  group('C2. Admin login', () => {
    const res = http.post(`${BASE_URL}/auth/login`,
      JSON.stringify({ email: ADMIN_EMAIL, password: ADMIN_PASSWORD }),
      { headers: JSON_HEADERS });

    check(res, {
      'admin login → 200': (r) => r.status === 200,
      'admin login → role ADMIN': (r) => { try { return r.json('data.role') === 'ADMIN'; } catch { return false; } },
    });

    try { adminToken = res.json('data.token'); } catch { /* */ }
  });

  if (!adminToken || !contractorToken || !contractorUserId) return;

  // C3 — Verify (REPORTED → VERIFIED)
  group('C3. Verify incident', () => {
    const res = http.post(`${BASE_URL}/incidents/${state.incidentId}/verify`, null, { headers: auth(adminToken) });

    check(res, {
      'verify → 200': (r) => r.status === 200,
      'verify → VERIFIED': (r) => { try { return r.json('data.status') === 'VERIFIED'; } catch { return false; } },
    });
  });

  // C4 — Assign (VERIFIED → ASSIGNED)
  group('C4. Assign to contractor', () => {
    const res = http.post(
      `${BASE_URL}/incidents/${state.incidentId}/assign`,
      JSON.stringify({ contractorId: contractorUserId }),
      { headers: auth(adminToken) }
    );

    check(res, {
      'assign → 200': (r) => r.status === 200,
      'assign → ASSIGNED': (r) => { try { return r.json('data.status') === 'ASSIGNED'; } catch { return false; } },
    });
  });

  // C5 — Contractor sees the assignment
  group('C5. GET /incidents/my-assignments', () => {
    const res = http.get(`${BASE_URL}/incidents/my-assignments`, { headers: auth(contractorToken) });

    check(res, {
      'my-assignments → 200': (r) => r.status === 200,
      'my-assignments → contains incident': (r) => {
        try { return r.json('data').some((i) => i.incidentId === state.incidentId); } catch { return false; }
      },
    });
  });

  // C6 — Accept (ASSIGNED → IN_PROGRESS)
  group('C6. Accept assignment', () => {
    const res = http.post(`${BASE_URL}/incidents/${state.incidentId}/accept`, null, { headers: auth(contractorToken) });

    check(res, {
      'accept → 200': (r) => r.status === 200,
      'accept → IN_PROGRESS': (r) => { try { return r.json('data.status') === 'IN_PROGRESS'; } catch { return false; } },
    });
  });

  // C7 — Progress note
  group('C7. POST /incidents/{id}/progress', () => {
    const res = http.post(
      `${BASE_URL}/incidents/${state.incidentId}/progress`,
      JSON.stringify({ note: 'k6 regression: repair crew on site' }),
      { headers: auth(contractorToken) }
    );

    check(res, { 'progress → 200': (r) => r.status === 200 });
  });

  // C8 — Resolve (IN_PROGRESS → RESOLVED)
  group('C8. Resolve incident', () => {
    const res = http.post(
      `${BASE_URL}/incidents/${state.incidentId}/resolve`,
      JSON.stringify({ note: 'k6 regression: pothole filled', photoBase64: PNG }),
      { headers: auth(contractorToken) }
    );

    check(res, {
      'resolve → 200': (r) => r.status === 200,
      'resolve → RESOLVED': (r) => { try { return r.json('data.status') === 'RESOLVED'; } catch { return false; } },
    });
  });

  // C9 — Civilian reports it still unresolved
  if (state.civilianToken) {
    group('C9. POST /incidents/{id}/still-unresolved', () => {
      const res = http.post(
        `${BASE_URL}/incidents/${state.incidentId}/still-unresolved`,
        null,
        { headers: auth(state.civilianToken) }
      );

      check(res, { 'still-unresolved → 200': (r) => r.status === 200 });
    });
  }

  // C10 — Admin: recent
  group('C10. GET /incidents/recent', () => {
    const res = http.get(`${BASE_URL}/incidents/recent`, { headers: auth(adminToken) });

    check(res, {
      'recent → 200': (r) => r.status === 200,
      'recent → array': (r) => { try { return Array.isArray(r.json('data')); } catch { return false; } },
    });
  });

  // C11 — Admin: stats
  group('C11. GET /incidents/stats', () => {
    const res = http.get(`${BASE_URL}/incidents/stats`, { headers: auth(adminToken) });

    check(res, {
      'stats → 200': (r) => r.status === 200,
      'stats → data object': (r) => { try { return typeof r.json('data') === 'object'; } catch { return false; } },
    });
  });

  // C12 — Admin: contractor list
  group('C12. GET /admin/contractors', () => {
    const res = http.get(`${BASE_URL}/admin/contractors`, { headers: auth(adminToken) });

    check(res, {
      'admin contractors → 200': (r) => r.status === 200,
      'admin contractors → array': (r) => { try { return Array.isArray(r.json('data')); } catch { return false; } },
    });
  });
}

// ── Entry point ───────────────────────────────────────────────────────────────

export default function () {
  const state = {
    civilianToken:      '',
    incidentId:         '',
    existingIncidentId: '',
  };

  flowRegistration();
  flowCivilian(state);
  flowAdminContractor(state);
}
