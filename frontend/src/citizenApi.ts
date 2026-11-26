// The citizen e-service API — SUPPORT.01, Citizen e-service.
// The citizen's own login is kept apart from the worker's mock user: the token is
// `citizen:<personal code>` (a demo of the state login, TARA), held for this tab only.
import { handle } from './api';
import type { ApplicationStatus, BenefitScheme } from './supportApi';

const KEY = 'support_citizen_code';
const base = '/api/citizen';

export interface Me {
  personalCode: string;
  firstName?: string | null;
  lastName?: string | null;
  birthDate: string;
  nameAvailable: boolean;
}

export interface OnlineApplication {
  supportTypeCode: string;
  periodStart: string;
  periodEnd: string;
  householdSize: number;
}

export interface Receipt extends OnlineApplication {
  id: number;
  status: ApplicationStatus;
}

export const citizenSession = {
  code: (): string | null => {
    try {
      return sessionStorage.getItem(KEY);
    } catch {
      return null;
    }
  },
  logIn: (code: string) => {
    try {
      sessionStorage.setItem(KEY, code);
    } catch {
      /* private mode: the session simply does not survive a reload */
    }
  },
  logOut: () => {
    try {
      sessionStorage.removeItem(KEY);
    } catch {
      /* nothing to clear */
    }
  },
};

const headers = (): Record<string, string> => ({
  Authorization: `Bearer citizen:${citizenSession.code() ?? ''}`,
  'Content-Type': 'application/json',
});

export const citizenApi = {
  me: (): Promise<Me> => fetch(`${base}/me`, { headers: headers() }).then((r) => handle<Me>(r)),
  schemes: (): Promise<BenefitScheme[]> =>
    fetch(`${base}/schemes`, { headers: headers() }).then((r) => handle<BenefitScheme[]>(r)),
  apply: (body: OnlineApplication): Promise<Receipt> =>
    fetch(`${base}/applications`, { method: 'POST', headers: headers(), body: JSON.stringify(body) }).then((r) =>
      handle<Receipt>(r),
    ),
};

/** The fictional demo people the imitated registries know (README, Demo data). */
export const demoPeople = [
  { code: '38703140274', name: 'Kalle Johannes Kask' },
  { code: '49407225134', name: 'Liis Maria Lepp' },
  { code: '37911052485', name: 'Mart Mets' },
  { code: '48302283762', name: 'Anu Kristiina Allik' },
  { code: '36809190424', name: 'Rein Rebane' },
  { code: '60106117391', name: 'Eve Liisa Ilves' },
];

/** What the city checks for a scheme, in plain words for the citizen. */
export const checksInWords = (s?: BenefitScheme): string[] =>
  s
    ? [
        s.requiresResidence && 'that you live in the city (Population Register)',
        s.requiresUnemployment && 'that you are registered as unemployed (Töötukassa)',
        s.requiresNoEmployment && 'that you have no active job (Employment Register)',
      ].filter((x): x is string => !!x)
    : [];
