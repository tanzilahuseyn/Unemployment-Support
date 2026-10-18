// The support component's API client — thin fetch wrappers, like api.ts for animals.

import { authHeaders, handle, type QueryResult } from './api';

export type ApplicationStatus = 'OPEN' | 'APPROVED' | 'REJECTED';
export type CheckStatus = 'PASS' | 'FAIL' | 'UNAVAILABLE';
export type Outcome = 'APPROVED' | 'REJECTED';

export interface Application {
  id: number;
  applicantIsikukood: string;
  supportTypeCode: string;
  periodStart: string;
  periodEnd: string;
  householdSize: number;
  status: ApplicationStatus;
  /** Platform columns, filled by the database trigger; read-only. */
  sysVersion?: number;
  sysCreatedAt?: string;
  sysCreatedBy?: string;
  sysModifiedAt?: string;
  sysModifiedBy?: string;
}

export type ApplicationBody = Pick<
  Application,
  'applicantIsikukood' | 'supportTypeCode' | 'periodStart' | 'periodEnd' | 'householdSize'
>;

/** The three live registry checks — shown, never stored. */
export interface Checks {
  residence: {
    status: CheckStatus;
    firstName?: string | null;
    lastName?: string | null;
    address?: string | null;
    municipality?: string | null;
  };
  unemployment: { status: CheckStatus; registered?: boolean | null; registeredSince?: string | null };
  employment: { status: CheckStatus; activeEmployment?: boolean | null };
}

export interface Decision {
  id: number;
  applicationId: number;
  outcome: Outcome;
  override: boolean;
  reason?: string | null;
  residenceCheck: CheckStatus;
  unemploymentCheck: CheckStatus;
  employmentCheck: CheckStatus;
  sysCreatedAt?: string;
  sysCreatedBy?: string;
}

export interface SupportType {
  code: string;
  name: string;
}

const json = { 'Content-Type': 'application/json' };
const base = '/api/support';

export const supportApi = {
  list: (textContains?: string, status?: string, supportTypeCode?: string): Promise<QueryResult<Application>> => {
    const q = new URLSearchParams({ limit: '50', sort: '-periodStart' });
    if (textContains) q.set('textContains', textContains);
    if (status) q.set('status', status);
    if (supportTypeCode) q.set('supportTypeCode', supportTypeCode);
    return fetch(`${base}/applications?${q}`, { headers: authHeaders() }).then((r) =>
      handle<QueryResult<Application>>(r),
    );
  },

  get: (id: number): Promise<Application> =>
    fetch(`${base}/applications/${id}`, { headers: authHeaders() }).then((r) => handle<Application>(r)),

  create: (body: ApplicationBody): Promise<Application> =>
    fetch(`${base}/applications`, {
      method: 'POST',
      headers: { ...authHeaders(), ...json },
      body: JSON.stringify(body),
    }).then((r) => handle<Application>(r)),

  /** PUT — open applications only; the personal code is immutable. */
  update: (id: number, body: ApplicationBody): Promise<Application> =>
    fetch(`${base}/applications/${id}`, {
      method: 'PUT',
      headers: { ...authHeaders(), ...json },
      body: JSON.stringify(body),
    }).then((r) => handle<Application>(r)),

  /** DELETE — withdraw an open application (soft delete). */
  withdraw: (id: number): Promise<void> =>
    fetch(`${base}/applications/${id}`, { method: 'DELETE', headers: authHeaders() }).then((r) =>
      r.ok ? undefined : handle<void>(r),
    ),

  /** The checks, live from the three registries. A down registry is UNAVAILABLE, not an error. */
  checks: (id: number): Promise<Checks> =>
    fetch(`${base}/applications/${id}/checks`, { headers: authHeaders() }).then((r) => handle<Checks>(r)),

  /** Decide. No check results are sent: the server runs the checks itself. */
  decide: (id: number, outcome: Outcome, override: boolean, reason?: string): Promise<Decision> =>
    fetch(`${base}/applications/${id}/decision`, {
      method: 'POST',
      headers: { ...authHeaders(), ...json },
      body: JSON.stringify({ outcome, override, reason }),
    }).then((r) => handle<Decision>(r)),

  decision: (id: number): Promise<Decision> =>
    fetch(`${base}/applications/${id}/decision`, { headers: authHeaders() }).then((r) => handle<Decision>(r)),

  types: (): Promise<SupportType[]> =>
    fetch(`${base}/types`, { headers: authHeaders() }).then((r) => handle<SupportType[]>(r)),
};

/** Tag colour per status — green passes, red fails, grey could not be checked. */
export const checkColor: Record<CheckStatus, string> = { PASS: 'green', FAIL: 'red', UNAVAILABLE: 'default' };
export const statusColor: Record<ApplicationStatus, string> = { OPEN: 'blue', APPROVED: 'green', REJECTED: 'red' };
