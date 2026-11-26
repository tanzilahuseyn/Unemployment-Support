import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Alert, DatePicker, Form, InputNumber, Select } from 'antd';
import type { Dayjs } from 'dayjs';
import { AppButtonPrimary, AppButtonSecondary, AppCard } from '@helex/ui';
import { birthDateFromCode, type BenefitScheme } from '../../supportApi';
import { checksInWords, citizenApi, citizenSession, type Me } from '../../citizenApi';

interface FormValues {
  supportTypeCode: string;
  period: [Dayjs, Dayjs];
  householdSize: number;
}

/**
 * The citizen's application form — SUPPORT.01 § UI, Citizen application. The personal
 * code comes from the login and cannot be changed here.
 */
export const CitizenApply = () => {
  const navigate = useNavigate();
  const [me, setMe] = useState<Me | null>(null);
  const [schemes, setSchemes] = useState<BenefitScheme[]>([]);
  const [chosen, setChosen] = useState<string | undefined>();
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!citizenSession.code()) {
      navigate('/citizen', { replace: true });
      return;
    }
    citizenApi.me().then(setMe).catch(() => navigate('/citizen', { replace: true }));
    citizenApi.schemes().then(setSchemes).catch((e) => setError(e.message));
  }, [navigate]);

  const logOut = () => {
    citizenSession.logOut();
    navigate('/citizen');
  };

  const submit = (v: FormValues) => {
    setSaving(true);
    setError(null);
    citizenApi
      .apply({
        supportTypeCode: v.supportTypeCode,
        periodStart: v.period[0].format('YYYY-MM-DD'),
        periodEnd: v.period[1].format('YYYY-MM-DD'),
        householdSize: v.householdSize,
      })
      .then((receipt) =>
        navigate(`/citizen/done/${receipt.id}`, {
          state: { receipt, schemeName: schemes.find((s) => s.code === receipt.supportTypeCode)?.name },
        }),
      )
      .catch((e) => setError(e.message.charAt(0).toUpperCase() + e.message.slice(1)))
      .finally(() => setSaving(false));
  };

  const name = me?.nameAvailable ? `${me.firstName} ${me.lastName}` : null;
  const checks = checksInWords(schemes.find((s) => s.code === chosen));

  return (
    <div style={{ maxWidth: 560, margin: '32px auto', padding: '0 16px', display: 'grid', gap: 16 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 12 }}>
        <div>
          <h1 style={{ margin: 0, fontSize: 26 }}>{name ? `Hello, ${name}` : 'Hello'}</h1>
          {me && (
            <div style={{ opacity: 0.7 }}>
              Personal code {me.personalCode} · born {birthDateFromCode(me.personalCode)}
            </div>
          )}
          {name && <div style={{ opacity: 0.55, fontSize: 12 }}>Your name comes from the Population Register and is not stored.</div>}
        </div>
        <AppButtonSecondary onClick={logOut}>Log out</AppButtonSecondary>
      </div>
      <AppCard title="Application for unemployment support" size="small">
        <Form<FormValues> layout="vertical" onFinish={submit} requiredMark initialValues={{ householdSize: 1 }}>
          <Form.Item name="supportTypeCode" label="Which support do you apply for?" rules={[{ required: true, message: 'Please choose the support' }]}>
            <Select
              placeholder="Choose"
              options={schemes.map((s) => ({ value: s.code, label: s.name }))}
              onChange={(v: string) => setChosen(v)}
            />
          </Form.Item>
          {checks.length > 0 && (
            <Alert
              type="info"
              style={{ marginBottom: 16 }}
              message="You do not need to send any documents"
              description={`The city checks this itself in the state registries: ${checks.join('; ')}.`}
            />
          )}
          <Form.Item name="period" label="For which period?" rules={[{ required: true, message: 'Please choose the period' }]}>
            <DatePicker.RangePicker style={{ width: '100%' }} format="DD.MM.YYYY" />
          </Form.Item>
          <Form.Item
            name="householdSize"
            label="Number of family members"
            extra="Count yourself and the people who live with you and share your income"
            rules={[{ required: true, message: 'Please enter the number of family members' }]}
          >
            <InputNumber min={1} max={20} style={{ width: '100%' }} />
          </Form.Item>
          {error && <Alert type="error" showIcon style={{ marginBottom: 16 }} message={error} />}
          <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
            <AppButtonPrimary htmlType="submit" loading={saving}>
              Send application
            </AppButtonPrimary>
          </div>
        </Form>
      </AppCard>
    </div>
  );
};
