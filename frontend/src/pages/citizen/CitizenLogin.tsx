import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Alert, Input } from 'antd';
import { AppButtonPrimary, AppButtonSecondary, AppCard } from '@helex/ui';
import { isValidPersonalCode } from '../../supportApi';
import { citizenSession, demoPeople } from '../../citizenApi';

/**
 * Citizen login — SUPPORT.01 § UI, Citizen login. A DEMO standing in for the state
 * login (TARA): in production the personal code comes from ID-card, Mobile-ID or Smart-ID.
 */
export const CitizenLogin = () => {
  const navigate = useNavigate();
  const [code, setCode] = useState('');
  const [error, setError] = useState<string | null>(null);

  const logIn = (c: string) => {
    if (!isValidPersonalCode(c)) {
      setError('This is not a valid Estonian personal code. Please check the 11 digits.');
      return;
    }
    citizenSession.logIn(c);
    navigate('/citizen/apply');
  };

  return (
    <div style={{ maxWidth: 560, margin: '32px auto', padding: '0 16px', display: 'grid', gap: 16 }}>
      <div>
        <h1 style={{ margin: 0, fontSize: 26 }}>Apply for unemployment support</h1>
        <div style={{ opacity: 0.7 }}>City of Tallinn · social services</div>
      </div>
      <AppCard title="Log in" size="small">
        <div style={{ display: 'grid', gap: 14 }}>
          <Alert
            type="info"
            showIcon
            message="Demo login"
            description="In the real service you log in with the state login: ID-card, Mobile-ID or Smart-ID. Here you choose one of the fictional demo people, or type a personal code."
          />
          <div style={{ display: 'grid', gap: 6 }}>
            <b>Choose a demo person</b>
            {demoPeople.map((p) => (
              <AppButtonSecondary key={p.code} onClick={() => logIn(p.code)} style={{ justifyContent: 'space-between' }}>
                <span>{p.name}</span>
                <span style={{ opacity: 0.6 }}>{p.code}</span>
              </AppButtonSecondary>
            ))}
          </div>
          <div style={{ display: 'grid', gap: 6 }}>
            <b>Or type your personal code</b>
            <div style={{ display: 'flex', gap: 8 }}>
              <Input
                value={code}
                maxLength={11}
                inputMode="numeric"
                placeholder="38703140274"
                onChange={(e) => {
                  setCode(e.target.value.trim());
                  setError(null);
                }}
                onPressEnter={() => logIn(code)}
              />
              <AppButtonPrimary onClick={() => logIn(code)}>Log in</AppButtonPrimary>
            </div>
            {error && <div style={{ color: '#c4262e' }}>{error}</div>}
          </div>
        </div>
      </AppCard>
    </div>
  );
};
