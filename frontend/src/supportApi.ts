// The support component's API client — thin fetch wrappers, like api.ts for animals.
// SUPPORT.01 revision 2: verification with evidence, manual verification, reasoned
// decisions and the audit log.

import { authHeaders, handle, type QueryResult } from './api';

export type ApplicationStatus = 'OPEN' | 'PENDING_VERIFICATION' | 'READY_FOR_DECISION' | 'APPROVED' | 'REJECTED';
export type CheckStatus = 'PASS' | 'FAIL' | 'UNAVAILABLE' | 'INCONCLUSIVE';
export type CheckType = 'RESIDENCE' | 'UNEMPLOYMENT' | 'EMPLOYMENT';
export type Outcome = 'APPROVED' | 'REJECTED';
export type ReasonCode =
  | 'NOT_RESIDENT'
  | 'NOT_REGISTERED_UNEMPLOYED'
  | 'ACTIVE_EMPLOYMENT'
  | 'INCOMPLETE_APPLICATION'
  | 'OTHER';

export interface Application {
  id: number;
  applicantIsikukood: string;
  supportTypeCode: string;
  periodStart: string;
  periodEnd: string;
  householdSize: number;
  status: ApplicationStatus;
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

/** One stored check result — never a name, address or registry payload. */
export interface Evidence {
  id: number;
  checkType: CheckType;
  result: CheckStatus;
  statusDetail?: string | null;
  source: string;
  retrievedAt: string;
  sourceDataAt?: string | null;
  manual: boolean;
  note?: string | null;
  verifiedBy?: string | null;
}

/** The answer to a verification; `person` is live from the Population Register and NOT stored. */
export interface Verification {
  status: ApplicationStatus;
  evidence: Evidence[];
  person?: { firstName?: string; lastName?: string; address?: string; municipality?: string } | null;
}

export interface Decision {
  id: number;
  outcome: Outcome;
  exception: boolean;
  reasonCode?: ReasonCode | null;
  explanation?: string | null;
  legalBasis?: string | null;
  ruleVersion?: string | null;
  reviewInfo?: string | null;
  evidence: Evidence[];
  sysCreatedAt?: string;
  sysCreatedBy?: string;
}

export interface BenefitScheme {
  code: string;
  name: string;
  legalBasis?: string | null;
  ruleVersion?: string | null;
  requiresResidence: boolean;
  requiresUnemployment: boolean;
  requiresNoEmployment: boolean;
  reviewInfo?: string | null;
}

export interface AuditEvent {
  id: number;
  occurredAt: string;
  actor: string;
  action: string;
  detail?: string | null;
}

export interface DecisionCommand {
  outcome: Outcome;
  exception: boolean;
  reasonCode?: ReasonCode;
  explanation?: string;
}

const json = { 'Content-Type': 'application/json' };
const base = '/api/support';
const post = <T>(path: string, body?: unknown): Promise<T> =>
  fetch(`${base}${path}`, {
    method: 'POST',
    headers: { ...authHeaders(), ...json },
    body: body === undefined ? undefined : JSON.stringify(body),
  }).then((r) => handle<T>(r));
const get = <T>(path: string): Promise<T> =>
  fetch(`${base}${path}`, { headers: authHeaders() }).then((r) => handle<T>(r));

export const supportApi = {
  list: (textContains?: string, status?: string, supportTypeCode?: string): Promise<QueryResult<Application>> => {
    const q = new URLSearchParams({ limit: '50', sort: '-periodStart' });
    if (textContains) q.set('textContains', textContains);
    if (status) q.set('status', status);
    if (supportTypeCode) q.set('supportTypeCode', supportTypeCode);
    return get(`/applications?${q}`);
  },
  get: (id: number): Promise<Application> => get(`/applications/${id}`),
  create: (body: ApplicationBody): Promise<Application> => post('/applications', body),
  /** PUT — undecided applications only; it returns to OPEN and must be verified again. */
  update: (id: number, body: ApplicationBody): Promise<Application> =>
    fetch(`${base}/applications/${id}`, {
      method: 'PUT',
      headers: { ...authHeaders(), ...json },
      body: JSON.stringify(body),
    }).then((r) => handle<Application>(r)),
  withdraw: (id: number): Promise<void> =>
    fetch(`${base}/applications/${id}`, { method: 'DELETE', headers: authHeaders() }).then((r) =>
      r.ok ? undefined : handle<void>(r),
    ),
  verify: (id: number): Promise<Verification> => post(`/applications/${id}/verification`),
  evidence: (id: number): Promise<Evidence[]> => get(`/applications/${id}/evidence`),
  verifyManually: (id: number, type: CheckType, result: 'PASS' | 'FAIL', source: string, note: string) =>
    post<Verification>(`/applications/${id}/evidence/${type}/manual`, { result, source, note }),
  decide: (id: number, cmd: DecisionCommand): Promise<Decision> => post(`/applications/${id}/decision`, cmd),
  decision: (id: number): Promise<Decision> => get(`/applications/${id}/decision`),
  audit: (id: number): Promise<AuditEvent[]> => get(`/applications/${id}/audit`),
  schemes: (): Promise<BenefitScheme[]> => get('/schemes'),
};

export const requires = (s: BenefitScheme | undefined, t: CheckType): boolean =>
  !!s && (t === 'RESIDENCE' ? s.requiresResidence : t === 'UNEMPLOYMENT' ? s.requiresUnemployment : s.requiresNoEmployment);

/** Tag colours — green verified positive, red verified adverse, grey unavailable, orange inconclusive. */
export const checkColor: Record<CheckStatus, string> = {
  PASS: 'green',
  FAIL: 'red',
  UNAVAILABLE: 'default',
  INCONCLUSIVE: 'orange',
};
export const statusColor: Record<ApplicationStatus, string> = {
  OPEN: 'blue',
  PENDING_VERIFICATION: 'orange',
  READY_FOR_DECISION: 'cyan',
  APPROVED: 'green',
  REJECTED: 'red',
};
export const statusLabel: Record<ApplicationStatus, string> = {
  OPEN: 'Open',
  PENDING_VERIFICATION: 'Pending verification',
  READY_FOR_DECISION: 'Ready for decision',
  APPROVED: 'Approved',
  REJECTED: 'Rejected',
};
export const checkLabel: Record<CheckType, string> = {
  RESIDENCE: 'Residence · Population Register',
  UNEMPLOYMENT: 'Unemployment · Töötukassa',
  EMPLOYMENT: 'Employment · Employment Register',
};
export const reasonLabel: Record<ReasonCode, string> = {
  NOT_RESIDENT: 'Not resident in the municipality',
  NOT_REGISTERED_UNEMPLOYED: 'Not registered as unemployed',
  ACTIVE_EMPLOYMENT: 'Has an active employment',
  INCOMPLETE_APPLICATION: 'Incomplete application',
  OTHER: 'Other',
};
/** Which check a registry reason code cites (it must be FAIL). */
export const reasonCites: Partial<Record<ReasonCode, CheckType>> = {
  NOT_RESIDENT: 'RESIDENCE',
  NOT_REGISTERED_UNEMPLOYED: 'UNEMPLOYMENT',
  ACTIVE_EMPLOYMENT: 'EMPLOYMENT',
};

/** The official Estonian personal code check: format, birth date and checksum (as the backend). */
export const isValidPersonalCode = (code: string): boolean => {
  if (!/^\d{11}$/.test(code)) return false;
  const first = Number(code[0]);
  if (first < 1 || first > 8) return false;
  const year = 1800 + Math.floor((first - 1) / 2) * 100 + Number(code.slice(1, 3));
  const month = Number(code.slice(3, 5));
  const day = Number(code.slice(5, 7));
  const date = new Date(Date.UTC(year, month - 1, day));
  if (date.getUTCFullYear() !== year || date.getUTCMonth() !== month - 1 || date.getUTCDate() !== day) return false;
  const weighted = (w: number[]) => w.reduce((s, wi, i) => s + wi * Number(code[i]), 0) % 11;
  let check = weighted([1, 2, 3, 4, 5, 6, 7, 8, 9, 1]);
  if (check === 10) {
    check = weighted([3, 4, 5, 6, 7, 8, 9, 1, 2, 3]);
    if (check === 10) check = 0;
  }
  return check === Number(code[10]);
};
