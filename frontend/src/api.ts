// Shared API helpers — the auth header, the error handling and the list shape.
// The Vite dev proxy forwards /api to the backend, so paths are same-origin.
// The Authorization header is set by MockAuthProvider's session (dev).

export interface QueryResult<T> {
  data: T[];
  meta?: { total?: number };
}

export function authHeaders(): Record<string, string> {
  // MockAuthProvider persists the signed-in mock user here.
  const user = localStorage.getItem('helex_mock_user') || 'superadmin';
  return { Authorization: `Bearer ${user}` };
}

export async function handle<T>(response: Response): Promise<T> {
  if (!response.ok) {
    // The backend answers application/problem+json — surface its human text.
    const problem = await response.json().catch(() => null);
    throw new Error(problem?.detail || `HTTP ${response.status}`);
  }
  return response.json();
}
