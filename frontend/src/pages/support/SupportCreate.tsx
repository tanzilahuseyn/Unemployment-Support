import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { DatePicker, Form, Input, InputNumber, Select } from 'antd';
import type { Dayjs } from 'dayjs';
import { AppButtonPrimary, AppButtonSecondary, AppCard, useAppNotification } from '@helex/ui';
import { supportApi, type SupportType } from '../../supportApi';

interface FormValues {
  applicantIsikukood: string;
  supportTypeCode: string;
  period: [Dayjs, Dayjs];
  householdSize: number;
}

/**
 * The register form — field by field SUPPORT.01 § UI, Register form. Only the personal
 * code identifies the applicant: name and address come live from the registries later.
 */
export const SupportCreate = () => {
  const navigate = useNavigate();
  const notify = useAppNotification();
  const [types, setTypes] = useState<SupportType[]>([]);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    supportApi.types().then(setTypes).catch((e) => notify.error('Could not load support types', e.message));
  }, [notify]);

  const submit = (v: FormValues) => {
    setSaving(true);
    supportApi
      .create({
        applicantIsikukood: v.applicantIsikukood,
        supportTypeCode: v.supportTypeCode,
        periodStart: v.period[0].format('YYYY-MM-DD'),
        periodEnd: v.period[1].format('YYYY-MM-DD'),
        householdSize: v.householdSize,
      })
      .then((created) => {
        notify.success('Registered', created.applicantIsikukood);
        navigate(`/support/${created.id}`);
      })
      .catch((e) => notify.error('Could not register the application', e.message)) // 400/409 verbatim
      .finally(() => setSaving(false));
  };

  return (
    <AppCard title="Register an application for unemployment support" style={{ maxWidth: 640, margin: '24px auto' }}>
      <Form<FormValues> layout="vertical" onFinish={submit} requiredMark initialValues={{ householdSize: 1 }}>
        <Form.Item
          name="applicantIsikukood"
          label="Applicant's personal code"
          rules={[
            { required: true },
            { pattern: /^\d{11}$/, message: 'Exactly 11 digits' },
          ]}
          extra="Try 39001010011 — the imitated registries know this person and all checks pass"
        >
          <Input placeholder="39001010011" />
        </Form.Item>
        <Form.Item name="supportTypeCode" label="Support type" rules={[{ required: true }]}>
          <Select options={types.map((t) => ({ value: t.code, label: t.name }))} placeholder="Choose a support type" />
        </Form.Item>
        <Form.Item name="period" label="Period" rules={[{ required: true }]} extra="The end may not be before the start">
          <DatePicker.RangePicker style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item name="householdSize" label="Household size" rules={[{ required: true }]}>
          <InputNumber min={1} max={20} style={{ width: '100%' }} />
        </Form.Item>
        <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
          <AppButtonSecondary onClick={() => navigate('/support')}>Cancel</AppButtonSecondary>
          <AppButtonPrimary htmlType="submit" loading={saving}>
            Register
          </AppButtonPrimary>
        </div>
      </Form>
    </AppCard>
  );
};
