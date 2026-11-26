import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { AppButtonPrimary, AppButtonSecondary, AppCard } from '@helex/ui';
import { formatDate } from '../../supportApi';
import { citizenSession, type Receipt } from '../../citizenApi';

/** The confirmation after applying online — SUPPORT.01 § UI, Citizen confirmation. */
export const CitizenDone = () => {
  const navigate = useNavigate();
  const { id } = useParams();
  const state = useLocation().state as { receipt?: Receipt; schemeName?: string } | null;
  const r = state?.receipt;

  const row = (label: string, value: React.ReactNode) => (
    <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12 }}>
      <span style={{ opacity: 0.7 }}>{label}</span>
      <b style={{ textAlign: 'right' }}>{value}</b>
    </div>
  );

  return (
    <div style={{ maxWidth: 560, margin: '32px auto', padding: '0 16px', display: 'grid', gap: 16 }}>
      <h1 style={{ margin: 0, fontSize: 26 }}>Your application has been received</h1>
      <AppCard title={`Application number ${id}`} size="small">
        <div style={{ display: 'grid', gap: 8 }}>
          {r ? (
            <>
              {row('Support', state?.schemeName ?? r.supportTypeCode)}
              {row(
                'Period',
                r.periodStart === r.periodEnd ? formatDate(r.periodStart) : `${formatDate(r.periodStart)} – ${formatDate(r.periodEnd)}`,
              )}
              {row('Number of family members', r.householdSize)}
            </>
          ) : (
            <div>Keep the application number for your records.</div>
          )}
        </div>
      </AppCard>
      <AppCard title="What happens next" size="small">
        <ol style={{ margin: 0, paddingLeft: 20, display: 'grid', gap: 6 }}>
          <li>A social worker checks the facts in the state registries. You do not need to send documents.</li>
          <li>If a registry does not answer, your application waits until it does. It is never rejected for that.</li>
          <li>You get a decision with its reasons and how to challenge it if you disagree.</li>
        </ol>
      </AppCard>
      <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
        <AppButtonSecondary onClick={() => navigate('/citizen/apply')}>New application</AppButtonSecondary>
        <AppButtonPrimary
          onClick={() => {
            citizenSession.logOut();
            navigate('/citizen');
          }}
        >
          Log out
        </AppButtonPrimary>
      </div>
    </div>
  );
};
