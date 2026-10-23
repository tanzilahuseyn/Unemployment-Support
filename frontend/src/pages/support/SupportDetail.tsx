import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Checkbox, DatePicker, Form, Input, InputNumber, Radio, Select } from 'antd';
import dayjs from 'dayjs';
import { SolutionOutlined } from '@ant-design/icons';
import { useDataController } from '@helex/core';
import {
  AppButtonPrimary,
  AppButtonSecondary,
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
  checkLabel,
  reasonCites,
  reasonLabel,
  requires,
  statusColor,
  statusLabel,
  supportApi,
  type Application,
  type AuditEvent,
  type BenefitScheme,
  type CheckType,
  type Decision,
  type DecisionCommand,
  type Evidence,
  type ReasonCode,
  type Verification,
} from '../../supportApi';

const CHECKS: CheckType[] = ['RESIDENCE', 'UNEMPLOYMENT', 'EMPLOYMENT'];

/** A date input that accepts what the form store holds — see DateField in AnimalDetail for why. */
const DateField = ({ value, onChange }: { value?: unknown; onChange?: (next: string | null) => void }) => (
  <DatePicker
    style={{ width: '100%' }}
    value={value ? dayjs(String(value)) : null}
    onChange={(d) => onChange?.(d ? d.format('YYYY-MM-DD') : null)}
  />
);

const Muted = ({ children }: { children: React.ReactNode }) => (
  <div style={{ opacity: 0.65, fontSize: 12 }}>{children}</div>
);

/** One stored evidence record: result, status, source and both timestamps. */
const EvidenceRow = ({ e }: { e: Evidence }) => (
  <div style={{ display: 'grid', gap: 2 }}>
    <div>
      <AppTag color={checkColor[e.result]}>{e.result}</AppTag>
      {e.manual && <AppTag color="purple">MANUAL</AppTag>}
      {e.statusDetail && <span style={{ fontSize: 12 }}>{e.statusDetail}</span>}
    </div>
    <Muted>Source: {e.source}</Muted>
    <Muted>
      Retrieved <AppTimestamp value={e.retrievedAt} fallback="—" />
      {' · '}data as of {e.sourceDataAt ? <AppTimestamp value={e.sourceDataAt} fallback="—" /> : 'unknown'}
    </Muted>
    {e.note && <Muted>Note: {e.note}</Muted>}
  </div>
);

/** Resolve one unavailable or inconclusive check from another authorised source. */
const ManualVerify = ({
  onSubmit,
  busy,
}: {
  onSubmit: (result: 'PASS' | 'FAIL', source: string, note: string) => void;
  busy: boolean;
}) => {
  const [result, setResult] = useState<'PASS' | 'FAIL'>('PASS');
  const [source, setSource] = useState('');
  const [note, setNote] = useState('');
  return (
    <div style={{ display: 'grid', gap: 6, padding: 8, border: '1px dashed #ccc', borderRadius: 6 }}>
      <b style={{ fontSize: 12 }}>Verify manually</b>
      <Radio.Group
        size="small"
        optionType="button"
        value={result}
        onChange={(ev) => setResult(ev.target.value)}
        options={[
          { value: 'PASS', label: 'Verified positive' },
          { value: 'FAIL', label: 'Verified adverse' },
        ]}
      />
      <Input size="small" placeholder="Source used (required)" value={source} onChange={(ev) => setSource(ev.target.value)} />
      <Input.TextArea rows={2} placeholder="Note (required)" value={note} onChange={(ev) => setNote(ev.target.value)} />
      <AppButtonSecondary
        size="small"
        loading={busy}
        disabled={!source.trim() || !note.trim()}
        onClick={() => onSubmit(result, source.trim(), note.trim())}
      >
        Record manual verification
      </AppButtonSecondary>
    </div>
  );
};

/** The verification card: the stored evidence per required check, Verify, and manual verification. */
const VerificationCard = ({
  scheme,
  evidence,
  person,
  undecided,
  busy,
  onVerify,
  onManual,
}: {
  scheme?: BenefitScheme;
  evidence: Evidence[];
  person: Verification['person'];
  undecided: boolean;
  busy: boolean;
  onVerify: () => void;
  onManual: (t: CheckType, result: 'PASS' | 'FAIL', source: string, note: string) => void;
}) => (
  <AppCard title="Verification (X-Road)" size="small">
    <div style={{ display: 'grid', gap: 12 }}>
      {undecided && (
        <AppButtonPrimary loading={busy} onClick={onVerify}>
          {evidence.length ? 'Verify again' : 'Verify'}
        </AppButtonPrimary>
      )}
      {person && (
        <div style={{ background: '#f6f6f6', padding: 8, borderRadius: 6 }}>
          {person.firstName} {person.lastName}, {person.address}
          <Muted>Live from the Population Register — shown now, not stored.</Muted>
        </div>
      )}
      {CHECKS.map((t) => {
        const e = evidence.find((x) => x.checkType === t);
        const required = requires(scheme, t);
        return (
          <FieldItem key={t} label={checkLabel[t]}>
            {!required ? (
              <Muted>Not required by this scheme — not queried.</Muted>
            ) : !e ? (
              <Muted>Not verified yet.</Muted>
            ) : (
              <div style={{ display: 'grid', gap: 6 }}>
                <EvidenceRow e={e} />
                {undecided && (e.result === 'UNAVAILABLE' || e.result === 'INCONCLUSIVE') && (
                  <ManualVerify busy={busy} onSubmit={(r, s, n) => onManual(t, r, s, n)} />
                )}
              </div>
            )}
          </FieldItem>
        );
      })}
    </div>
  </AppCard>
);

/** The decision form — only while undecided. */
const DecideCard = ({
  status,
  evidence,
  busy,
  onDecide,
}: {
  status: Application['status'];
  evidence: Evidence[];
  busy: boolean;
  onDecide: (cmd: DecisionCommand) => void;
}) => {
  const [outcome, setOutcome] = useState<'APPROVED' | 'REJECTED'>('APPROVED');
  const [exception, setException] = useState(false);
  const [reasonCode, setReasonCode] = useState<ReasonCode | undefined>();
  const [explanation, setExplanation] = useState('');
  const adverse = evidence.some((e) => e.result === 'FAIL');
  const ready = status === 'READY_FOR_DECISION';
  const failed = (t?: CheckType) => !t || evidence.some((e) => e.checkType === t && e.result === 'FAIL');

  const blocked =
    outcome === 'APPROVED'
      ? !ready || (adverse && (!exception || !explanation.trim()))
      : !reasonCode || !explanation.trim();

  return (
    <AppCard title="Decide" size="small">
      <div style={{ display: 'grid', gap: 10 }}>
        <Radio.Group
          value={outcome}
          onChange={(e) => setOutcome(e.target.value)}
          optionType="button"
          options={[
            { value: 'APPROVED', label: 'Approve' },
            { value: 'REJECTED', label: 'Reject' },
          ]}
        />
        {outcome === 'APPROVED' && !ready && (
          <Muted>
            {status === 'PENDING_VERIFICATION'
              ? 'Pending verification: resolve the unavailable or inconclusive checks first.'
              : 'Verify the application first.'}
          </Muted>
        )}
        {outcome === 'APPROVED' && ready && adverse && (
          <Checkbox checked={exception} onChange={(e) => setException(e.target.checked)}>
            Exception: approve although a required fact is verified adverse
          </Checkbox>
        )}
        {outcome === 'REJECTED' && (
          <Select<ReasonCode>
            placeholder="Reason code (required)"
            value={reasonCode}
            onChange={setReasonCode}
            options={(Object.keys(reasonLabel) as ReasonCode[]).map((c) => ({
              value: c,
              label: reasonLabel[c],
              // a registry reason needs that fact verified adverse
              disabled: !failed(reasonCites[c]),
            }))}
          />
        )}
        <Input.TextArea
          rows={3}
          maxLength={2000}
          value={explanation}
          onChange={(e) => setExplanation(e.target.value)}
          placeholder={
            outcome === 'REJECTED' || exception ? 'Explanation of the material facts (required)' : 'Explanation (optional)'
          }
        />
        <AppButtonPrimary
          disabled={blocked}
          loading={busy}
          onClick={() =>
            onDecide({
              outcome,
              exception: outcome === 'APPROVED' && exception,
              reasonCode: outcome === 'REJECTED' ? reasonCode : undefined,
              explanation: explanation.trim() || undefined,
            })
          }
        >
          Decide
        </AppButtonPrimary>
        <Muted>The legal basis, rule version and review information are taken from the benefit scheme.</Muted>
      </div>
    </AppCard>
  );
};

/** The recorded decision, read-only. */
const DecisionView = ({ d }: { d: Decision }) => (
  <AppCard title="Decision" size="small">
    <FieldItem label="Outcome">
      <AppTag color={d.outcome === 'APPROVED' ? 'green' : 'red'}>{d.outcome}</AppTag>
      {d.exception && <AppTag color="orange">EXCEPTION</AppTag>}
    </FieldItem>
    {d.reasonCode && <FieldItem label="Reason">{reasonLabel[d.reasonCode]}</FieldItem>}
    {d.explanation && <FieldItem label="Explanation">{d.explanation}</FieldItem>}
    <FieldItem label="Legal basis">
      {d.legalBasis ?? '—'}
      <Muted>Rule version {d.ruleVersion ?? '—'}</Muted>
    </FieldItem>
    <FieldItem label="How to challenge">{d.reviewInfo ?? '—'}</FieldItem>
    <FieldItem label="Evidence relied on">
      <div style={{ display: 'grid', gap: 8 }}>
        {d.evidence.map((e) => (
          <div key={e.id}>
            <b style={{ fontSize: 12 }}>{checkLabel[e.checkType]}</b>
            <EvidenceRow e={e} />
          </div>
        ))}
      </div>
    </FieldItem>
    <FieldItem label="Decided by">{d.sysCreatedBy ?? '—'}</FieldItem>
    <FieldItem label="Decided at">
      <AppTimestamp value={d.sysCreatedAt} fallback="—" />
    </FieldItem>
  </AppCard>
);

const AuditCard = ({ events }: { events: AuditEvent[] }) => (
  <AppCard title="Audit log" size="small">
    <div style={{ display: 'grid', gap: 4, fontSize: 12 }}>
      {events.length === 0 && <Muted>No events yet.</Muted>}
      {events.map((e) => (
        <div key={e.id}>
          <AppTimestamp value={e.occurredAt} fallback="—" /> · <b>{e.action}</b> · {e.actor}
          {e.detail && <Muted>{e.detail}</Muted>}
        </div>
      ))}
    </div>
  </AppCard>
);

/**
 * One application — SUPPORT.01 revision 2 § UI, Application page. ResourceForm +
 * useDataController like AnimalDetail; the sidebar holds verification, the decision
 * (a form while undecided, read-only after), the audit log and metadata.
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
  const [schemes, setSchemes] = useState<BenefitScheme[]>([]);
  const [evidence, setEvidence] = useState<Evidence[]>([]);
  const [person, setPerson] = useState<Verification['person']>(null);
  const [decision, setDecision] = useState<Decision | null>(null);
  const [audit, setAudit] = useState<AuditEvent[]>([]);
  const [busy, setBusy] = useState(false);

  const reload = useCallback(async () => {
    const a = await supportApi.get(appId);
    dc.load(a);
    form.setFieldsValue(a);
    setEvidence(await supportApi.evidence(appId));
    setAudit(await supportApi.audit(appId));
    setDecision(a.status === 'APPROVED' || a.status === 'REJECTED' ? await supportApi.decision(appId) : null);
    return a;
    // dc and form are stable handles
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [appId]);

  useEffect(() => {
    setLoading(true);
    Promise.all([reload(), supportApi.schemes().then(setSchemes)])
      .catch((e) => {
        notify.error('Could not load the application', e.message);
        navigate('/support');
      })
      .finally(() => setLoading(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [appId, notify]);

  const status = dc.current.status;
  const undecided = status === 'OPEN' || status === 'PENDING_VERIFICATION' || status === 'READY_FOR_DECISION';
  const scheme = schemes.find((s) => s.code === dc.current.supportTypeCode);
  const schemeName = (code: unknown) => schemes.find((s) => s.code === code)?.name ?? String(code ?? '');

  const run = async (label: string, action: () => Promise<unknown>) => {
    setBusy(true);
    try {
      await action();
      await reload();
    } catch (e) {
      notify.error(label, (e as Error).message); // 400/409 problem detail, verbatim
    } finally {
      setBusy(false);
    }
  };

  const save = async (values: Application) => {
    setSaving(true);
    try {
      await supportApi.update(appId, { ...values, applicantIsikukood: dc.current.applicantIsikukood });
      setPerson(null);
      await reload();
      setMode('view');
      notify.success('Saved', 'The application must be verified again.');
    } catch (e) {
      notify.error('Save failed', (e as Error).message);
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

  const sections: ResourceFormSection<Application>[] = [
    {
      key: 'application',
      title: 'Application',
      fields: [
        {
          name: 'applicantIsikukood',
          label: 'Personal code',
          type: 'text',
          modeOverride: 'view',
          tooltip: 'The applicant is identified only by the personal code; name and address come from the registry',
        },
        {
          name: 'supportTypeCode',
          label: 'Benefit scheme',
          type: 'select',
          required: true,
          options: schemes.map((s) => ({ value: s.code, label: s.name })),
          viewRender: (code) => (
            <>
              <AppTag>{schemeName(code)}</AppTag>
              {scheme && <Muted>{scheme.legalBasis} · rules {scheme.ruleVersion}</Muted>}
            </>
          ),
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
          tooltip: 'Context only — no rule uses it',
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
      subtitle={schemeName(dc.current.supportTypeCode)}
      chips={status ? <AppTag color={statusColor[status]}>{statusLabel[status]}</AppTag> : undefined}
      loading={loading}
      saving={saving}
      deleting={deleting}
      form={form}
      dataController={dc}
      initialValues={dc.current}
      sections={sections}
      onBack={() => navigate('/support')}
      backTooltip="Back to the list"
      onEdit={undecided ? () => setMode('edit') : undefined}
      onCancel={() => {
        dc.reset();
        form.setFieldsValue(dc.original ?? dc.current);
        setMode('view');
      }}
      onSave={save}
      onDelete={undecided ? withdraw : undefined}
      deleteLabel="Withdraw"
      deleteConfirmText="Withdraw this application? A soft delete — it disappears from the list."
      sidebar={
        <div style={{ display: 'grid', gap: 12 }}>
          <VerificationCard
            scheme={scheme}
            evidence={evidence}
            person={person}
            undecided={undecided}
            busy={busy}
            onVerify={() =>
              run('Verification failed', async () => {
                const v = await supportApi.verify(appId);
                setPerson(v.person ?? null);
                notify.success('Verified', statusLabel[v.status]);
              })
            }
            onManual={(t, r, s, n) =>
              run('Manual verification refused', async () => {
                const v = await supportApi.verifyManually(appId, t, r, s, n);
                notify.success('Manual verification recorded', statusLabel[v.status]);
              })
            }
          />
          {undecided ? (
            <DecideCard
              key={status}
              status={status}
              evidence={evidence}
              busy={busy}
              onDecide={(cmd) =>
                run('Decision refused', async () => {
                  const d = await supportApi.decide(appId, cmd);
                  notify.success('Decided', `${d.outcome}${d.exception ? ' (exception)' : ''}`);
                })
              }
            />
          ) : (
            decision && <DecisionView d={decision} />
          )}
          <AuditCard events={audit} />
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
