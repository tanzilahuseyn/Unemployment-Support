import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Checkbox, DatePicker, Form, Input, InputNumber, Radio } from 'antd';
import dayjs from 'dayjs';
import { SolutionOutlined } from '@ant-design/icons';
import { useDataController } from '@helex/core';
import {
  AppButtonPrimary,
  AppCard,
  AppTag,
  AppTimestamp,
  FieldItem,
  ResourceForm,
  useAppNotification,
  type ResourceFormSection,
} from '@helex/ui';
import {
  checkColor,
  statusColor,
  supportApi,
  type Application,
  type CheckStatus,
  type Checks,
  type Decision,
  type Outcome,
  type SupportType,
} from '../../supportApi';

/**
 * A date input that accepts what the form store holds — string or dayjs in,
 * `YYYY-MM-DD` out. Same reason as DateField in AnimalDetail: ResourceForm's
 * built-in `date` field crashes on the API's string values.
 */
const DateField = ({ value, onChange }: { value?: unknown; onChange?: (next: string | null) => void }) => (
  <DatePicker
    style={{ width: '100%' }}
    value={value ? dayjs(String(value)) : null}
    onChange={(d) => onChange?.(d ? d.format('YYYY-MM-DD') : null)}
  />
);

const CheckTag = ({ status }: { status: CheckStatus }) => (
  <AppTag color={checkColor[status]}>{status === 'UNAVAILABLE' ? 'UNAVAILABLE' : status}</AppTag>
);

/** The three live registry checks — loaded each time, never stored. */
const ChecksCard = ({ checks, loading }: { checks: Checks | null; loading: boolean }) => (
  <AppCard title="Registry checks (live)" size="small" loading={loading}>
    {checks && (
      <div style={{ display: 'grid', gap: 12 }}>
        <FieldItem label="Residence · Population Register">
          <CheckTag status={checks.residence.status} />
          {checks.residence.firstName && (
            <div>
              {checks.residence.firstName} {checks.residence.lastName}, {checks.residence.address}
              <div style={{ opacity: 0.65, fontSize: 12 }}>municipality: {checks.residence.municipality}</div>
            </div>
          )}
        </FieldItem>
        <FieldItem label="Unemployment · Töötukassa">
          <CheckTag status={checks.unemployment.status} />
          {checks.unemployment.registered != null && (
            <div>
              {checks.unemployment.registered
                ? `Registered since ${checks.unemployment.registeredSince ?? '—'}`
                : 'Not registered as unemployed'}
            </div>
          )}
        </FieldItem>
        <FieldItem label="Employment · Employment Register">
          <CheckTag status={checks.employment.status} />
          {checks.employment.activeEmployment != null && (
            <div>{checks.employment.activeEmployment ? 'Has an active employment' : 'No active employment'}</div>
          )}
        </FieldItem>
        <div style={{ opacity: 0.65, fontSize: 12 }}>
          Looked up over X-Road (imitated locally) each time this page opens. Nothing here is stored.
        </div>
      </div>
    )}
  </AppCard>
);

/** The stored decision, read-only. */
const DecisionView = ({ d }: { d: Decision }) => (
  <AppCard title="Decision" size="small">
    <FieldItem label="Outcome">
      <AppTag color={d.outcome === 'APPROVED' ? 'green' : 'red'}>{d.outcome}</AppTag>
      {d.override && <AppTag color="orange">OVERRIDE</AppTag>}
    </FieldItem>
    {d.reason && <FieldItem label="Reason">{d.reason}</FieldItem>}
    <FieldItem label="Checks at the moment of deciding">
      <div style={{ display: 'grid', gap: 4 }}>
        <span>
          Residence <CheckTag status={d.residenceCheck} />
        </span>
        <span>
          Unemployment <CheckTag status={d.unemploymentCheck} />
        </span>
        <span>
          Employment <CheckTag status={d.employmentCheck} />
        </span>
      </div>
    </FieldItem>
    <FieldItem label="Decided by">{d.sysCreatedBy ?? '—'}</FieldItem>
    <FieldItem label="Decided at">
      <AppTimestamp value={d.sysCreatedAt} fallback="—" />
    </FieldItem>
  </AppCard>
);

/** The decision form — only while the application is OPEN. */
const DecideCard = ({
  allPass,
  deciding,
  onDecide,
}: {
  allPass: boolean;
  deciding: boolean;
  onDecide: (outcome: Outcome, override: boolean, reason: string) => void;
}) => {
  const [outcome, setOutcome] = useState<Outcome>('APPROVED');
  const [override, setOverride] = useState(false);
  const [reason, setReason] = useState('');
  const needsOverride = outcome === 'APPROVED' && !allPass;
  const needsReason = outcome === 'REJECTED' || (needsOverride && override);
  const blocked = (needsOverride && !override) || (needsReason && reason.trim() === '');

  return (
    <AppCard title="Decide" size="small">
      <div style={{ display: 'grid', gap: 10 }}>
        <Radio.Group
          value={outcome}
          onChange={(e) => setOutcome(e.target.value as Outcome)}
          optionType="button"
          options={[
            { value: 'APPROVED', label: 'Approve' },
            { value: 'REJECTED', label: 'Reject' },
          ]}
        />
        {needsOverride && (
          <Checkbox checked={override} onChange={(e) => setOverride(e.target.checked)}>
            Override: approve although a check did not pass
          </Checkbox>
        )}
        <Input.TextArea
          rows={3}
          maxLength={2000}
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          placeholder={needsReason ? 'Reason (required)' : 'Reason (optional)'}
        />
        <AppButtonPrimary disabled={blocked} loading={deciding} onClick={() => onDecide(outcome, override, reason)}>
          Decide
        </AppButtonPrimary>
        <div style={{ opacity: 0.65, fontSize: 12 }}>
          The server runs the three checks again when you decide and records their outcome.
        </div>
      </div>
    </AppCard>
  );
};

/**
 * One application — SUPPORT.01 § UI, Application page. ResourceForm + useDataController
 * like AnimalDetail; Edit and Withdraw only while OPEN. The sidebar holds the live
 * registry checks, the decision (a form while open, read-only after), and metadata.
 */
export const SupportDetail = () => {
  const { id } = useParams();
  const appId = Number(id);
  const navigate = useNavigate();
  const notify = useAppNotification();
  const [form] = Form.useForm<Application>();
  const dc = useDataController<Application>({} as Application);
  const [mode, setMode] = useState<'view' | 'edit'>('view');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [types, setTypes] = useState<SupportType[]>([]);
  const [checks, setChecks] = useState<Checks | null>(null);
  const [checksLoading, setChecksLoading] = useState(true);
  const [decision, setDecision] = useState<Decision | null>(null);
  const [deciding, setDeciding] = useState(false);

  const loadChecks = useCallback(() => {
    setChecksLoading(true);
    supportApi
      .checks(appId)
      .then(setChecks)
      .catch((e) => notify.error('Could not load the registry checks', e.message))
      .finally(() => setChecksLoading(false));
  }, [appId, notify]);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    Promise.all([supportApi.get(appId), supportApi.types()])
      .then(([a, list]) => {
        if (cancelled) return;
        dc.load(a);
        form.setFieldsValue(a);
        setTypes(list);
        if (a.status !== 'OPEN') {
          supportApi.decision(appId).then((d) => !cancelled && setDecision(d)).catch(() => setDecision(null));
        }
      })
      .catch((e) => {
        notify.error('Could not load the application', e.message);
        navigate('/support');
      })
      .finally(() => !cancelled && setLoading(false));
    loadChecks();
    return () => {
      cancelled = true;
    };
    // dc, form and navigate are stable handles; the record is keyed by the route parameter.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [appId, notify]);

  const open = dc.current.status === 'OPEN';
  const typeName = (code: unknown) => types.find((t) => t.code === code)?.name ?? String(code ?? '');

  const save = async (values: Application) => {
    setSaving(true);
    try {
      const updated = await supportApi.update(appId, { ...values, applicantIsikukood: dc.current.applicantIsikukood });
      dc.load(updated);
      form.setFieldsValue(updated);
      setMode('view');
      notify.success('Saved', updated.applicantIsikukood);
    } catch (e) {
      notify.error('Save failed', (e as Error).message); // 400/409 problem detail, verbatim
    } finally {
      setSaving(false);
    }
  };

  const withdraw = async () => {
    setDeleting(true);
    try {
      await supportApi.withdraw(appId);
      notify.success('Withdrawn', dc.current.applicantIsikukood);
      navigate('/support');
    } catch (e) {
      notify.error('Withdraw failed', (e as Error).message);
      setDeleting(false);
    }
  };

  const decide = async (outcome: Outcome, override: boolean, reason: string) => {
    setDeciding(true);
    try {
      const d = await supportApi.decide(appId, outcome, override, reason.trim() || undefined);
      setDecision(d);
      const a = await supportApi.get(appId);
      dc.load(a);
      form.setFieldsValue(a);
      notify.success('Decided', `${d.outcome}${d.override ? ' (override)' : ''}`);
      loadChecks();
    } catch (e) {
      notify.error('Decision refused', (e as Error).message); // 400/409 problem detail, verbatim
    } finally {
      setDeciding(false);
    }
  };

  const sections: ResourceFormSection<Application>[] = [
    {
      key: 'application',
      title: 'Application',
      fields: [
        {
          name: 'applicantIsikukood',
          label: 'Personal code',
          type: 'text',
          modeOverride: 'view', // identity does not change
          tooltip: 'The applicant is identified only by the personal code; name and address come from the registry',
        },
        {
          name: 'supportTypeCode',
          label: 'Support type',
          type: 'select',
          required: true,
          options: types.map((t) => ({ value: t.code, label: t.name })),
          viewRender: (code) => <AppTag>{typeName(code)}</AppTag>,
        },
        {
          name: 'periodStart',
          label: 'Period start',
          type: 'custom',
          rules: [{ required: true }],
          render: ({ value, onChange }) => <DateField value={value} onChange={onChange} />,
          viewRender: (v) => String(v ?? '—'),
        },
        {
          name: 'periodEnd',
          label: 'Period end',
          type: 'custom',
          tooltip: 'Not before the start',
          rules: [{ required: true }],
          render: ({ value, onChange }) => <DateField value={value} onChange={onChange} />,
          viewRender: (v) => String(v ?? '—'),
        },
        {
          name: 'householdSize',
          label: 'Household size',
          type: 'custom',
          rules: [{ required: true }],
          render: ({ value, onChange }) => (
            <InputNumber
              min={1}
              max={20}
              style={{ width: '100%' }}
              value={value == null ? null : Number(value)}
              onChange={(v) => onChange?.(v)}
            />
          ),
          viewRender: (v) => String(v ?? '—'),
        },
      ],
    },
  ];

  return (
    <ResourceForm<Application>
      mode={mode}
      entityLabel="Application"
      icon={<SolutionOutlined />}
      name={dc.current.applicantIsikukood}
      subtitle={typeName(dc.current.supportTypeCode)}
      chips={dc.current.status ? <AppTag color={statusColor[dc.current.status]}>{dc.current.status}</AppTag> : undefined}
      loading={loading}
      saving={saving}
      deleting={deleting}
      form={form}
      dataController={dc}
      initialValues={dc.current}
      sections={sections}
      onBack={() => navigate('/support')}
      backTooltip="Back to the list"
      onEdit={open ? () => setMode('edit') : undefined}
      onCancel={() => {
        dc.reset();
        form.setFieldsValue(dc.original ?? dc.current);
        setMode('view');
      }}
      onSave={save}
      onDelete={open ? withdraw : undefined}
      deleteLabel="Withdraw"
      deleteConfirmText="Withdraw this application? A soft delete — it disappears from the list."
      sidebar={
        <div style={{ display: 'grid', gap: 12 }}>
          <ChecksCard checks={checks} loading={checksLoading} />
          {open ? (
            <DecideCard allPass={!!checks && isAllPass(checks)} deciding={deciding} onDecide={decide} />
          ) : (
            decision && <DecisionView d={decision} />
          )}
          <AppCard title="Metadata" size="small">
            <FieldItem label="Created at">
              <AppTimestamp value={dc.current.sysCreatedAt} fallback="—" />
            </FieldItem>
            <FieldItem label="Created by">{dc.current.sysCreatedBy ?? '—'}</FieldItem>
            <FieldItem label="Modified at">
              <AppTimestamp value={dc.current.sysModifiedAt} fallback="—" />
            </FieldItem>
            <FieldItem label="Modified by">{dc.current.sysModifiedBy ?? '—'}</FieldItem>
            <FieldItem label="Version">{dc.current.sysVersion ?? '—'}</FieldItem>
          </AppCard>
        </div>
      }
    />
  );
};

const isAllPass = (c: Checks) =>
  c.residence.status === 'PASS' && c.unemployment.status === 'PASS' && c.employment.status === 'PASS';
