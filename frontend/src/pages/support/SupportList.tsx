import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Modal, Select } from 'antd';
import { BarChartOutlined, PlusOutlined, TeamOutlined } from '@ant-design/icons';
import { checkIcon, ManualTag, ResultTag, SchemeTag, StatusTag, statusIcon, SupportIllustration } from './visuals';
import {
  AppButtonPrimary,
  AppButtonSecondary,
  AppLink,
  AppTimestamp,
  ResourceList,
  useAppNotification,
  type ResourceListColumn,
} from '@helex/ui';
import {
  checkLabel,
  reasonLabel,
  statusLabel,
  supportApi,
  type Application,
  type ApplicationStatus,
  type BenefitScheme,
  type Evidence,
  type ReasonCode,
  type SummaryReport,
  applicationNo,
  birthDateFromCode,
} from '../../supportApi';

/** The summary report: aggregate counts only — the reporting operation, shown in a modal. */
const SummaryView = ({ report, schemeName }: { report: SummaryReport; schemeName: (code: string) => string }) => {
  const row = (label: string, n: number) => (
    <div key={label} style={{ display: 'flex', justifyContent: 'space-between' }}>
      <span>{label}</span>
      <b>{n}</b>
    </div>
  );
  return (
    <div style={{ display: 'grid', gap: 14 }}>
      {row('Applications in total', report.total)}
      <div>
        <b>By status</b>
        {Object.entries(report.byStatus).map(([s, n]) => row(statusLabel[s as ApplicationStatus] ?? s, n))}
      </div>
      <div>
        <b>By benefit scheme</b>
        {Object.entries(report.byScheme).map(([s, n]) => row(schemeName(s), n))}
      </div>
      <div>
        <b>Decisions</b>
        {row('Approved', report.approved)}
        {row('of which exceptions', report.exceptions)}
        {row('Rejected', report.rejected)}
      </div>
      {Object.keys(report.rejectionReasons).length > 0 && (
        <div>
          <b>Rejection reasons</b>
          {Object.entries(report.rejectionReasons).map(([r, n]) => row(reasonLabel[r as ReasonCode] ?? r, n))}
        </div>
      )}
      <div style={{ opacity: 0.65, fontSize: 12 }}>Counts only — the report contains no personal codes.</div>
    </div>
  );
};

/** One clickable count tile in the overview. */
const Tile = ({ label, n, color, icon, active, onClick }: {
  label: string; n: number; color: string; icon: React.ReactNode; active: boolean; onClick: () => void;
}) => (
  <button
    type="button"
    onClick={onClick}
    style={{
      display: 'flex', alignItems: 'center', gap: 10, padding: '8px 12px', minWidth: 132, cursor: 'pointer',
      textAlign: 'left', font: 'inherit', color: 'inherit', borderRadius: 8,
      background: active ? color : '#ffffff', border: `1px solid ${color}`,
    }}
  >
    <span style={{
      display: 'inline-flex', alignItems: 'center', justifyContent: 'center', width: 32, height: 32,
      borderRadius: '50%', fontSize: 16, background: active ? '#ffffff' : color, color: active ? color : '#ffffff',
    }}>
      {icon}
    </span>
    <span>
      <b style={{ fontSize: 18, display: 'block', lineHeight: 1.1, color: active ? '#ffffff' : undefined }}>{n}</b>
      <span style={{ fontSize: 12, color: active ? '#ffffff' : undefined, opacity: active ? 1 : 0.75 }}>{label}</span>
    </span>
  </button>
);

const tileColor: Record<ApplicationStatus, string> = {
  OPEN: '#2468b0',
  PENDING_VERIFICATION: '#d48806',
  READY_FOR_DECISION: '#08979c',
  APPROVED: '#277257',
  REJECTED: '#af4458',
};

/** The list's header: illustration, a short explanation, and a tile per status that filters the list. */
const Overview = ({ report, status, onStatus }: {
  report: SummaryReport | null; status?: string; onStatus: (s?: string) => void;
}) => (
  <div style={{
    display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: 16, padding: '12px 16px', margin: '0 0 12px',
    borderRadius: 8, background: 'linear-gradient(90deg, #ebeaef 0%, #fbe3f0 100%)', borderLeft: '4px solid #e4067e',
  }}>
    <SupportIllustration size={96} />
    <div style={{ flex: '1 1 200px' }}>
      <div style={{ fontSize: 18, fontWeight: 700, color: '#342b60' }}>Unemployment support, Tallinn</div>
      <div style={{ opacity: 0.8 }}>Facts verified over X-Road. A registry that does not answer never rejects anyone.</div>
    </div>
    <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
      <Tile label="All applications" n={report?.total ?? 0} color="#342b60" icon={<TeamOutlined />}
        active={!status} onClick={() => onStatus(undefined)} />
      {(Object.keys(tileColor) as ApplicationStatus[]).map((s) => (
        <Tile key={s} label={statusLabel[s]} n={report?.byStatus[s] ?? 0} color={tileColor[s]} icon={statusIcon[s]}
          active={status === s} onClick={() => onStatus(status === s ? undefined : s)} />
      ))}
    </div>
  </div>
);

/**
 * The application list — SUPPORT.01 § UI, Application list. ResourceList with search
 * by personal code, filters for status and benefit scheme, a detail panel with the
 * stored evidence, and the summary report.
 */
export const SupportList = () => {
  const navigate = useNavigate();
  const notify = useAppNotification();
  const [rows, setRows] = useState<Application[]>([]);
  const [types, setTypes] = useState<BenefitScheme[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState<string | undefined>();
  const [typeCode, setTypeCode] = useState<string | undefined>();
  const [selected, setSelected] = useState<Application | null>(null);
  const [evidence, setEvidence] = useState<Evidence[]>([]);
  const [report, setReport] = useState<SummaryReport | null>(null);
  const [counts, setCounts] = useState<SummaryReport | null>(null);

  // The detail panel shows the STORED evidence only — selecting a row never queries a registry.
  useEffect(() => {
    setEvidence([]);
    if (selected?.id) {
      supportApi
        .evidence(selected.id)
        .then(setEvidence)
        .catch(() => setEvidence([]));
    }
  }, [selected]);

  const openSummary = () =>
    supportApi
      .summary()
      .then(setReport)
      .catch((e) => notify.error('Could not load the summary', e.message));

  useEffect(() => {
    supportApi.schemes().then(setTypes).catch((e) => notify.error('Could not load benefit schemes', e.message));
  }, [notify]);

  // The overview tiles: counts for all periods, refreshed whenever the list reloads.
  useEffect(() => {
    supportApi.summary().then(setCounts).catch(() => setCounts(null));
  }, [rows]);

  const load = useCallback(
    (text: string, st?: string, type?: string) => {
      setLoading(true);
      supportApi
        .list(text || undefined, st, type)
        .then((result) => setRows(result.data))
        .catch((e) => notify.error('Could not load applications', e.message))
        .finally(() => setLoading(false));
    },
    [notify],
  );

  useEffect(() => {
    const t = setTimeout(() => load(search, status, typeCode), 250);
    return () => clearTimeout(t);
  }, [search, status, typeCode, load]);

  const typeName = (code: string) => types.find((t) => t.code === code)?.name ?? code;

  const columns: ResourceListColumn<Application>[] = [
    {
      key: 'applicationNo',
      title: 'Application no.',
      dataIndex: 'id',
      locked: true,
      // The application number is the way into the record: it links to the application.
      render: (_: number, a: Application) => (
        <AppLink
          href={`/support/${a.id}`}
          onClick={(e) => {
            e.preventDefault();
            e.stopPropagation();
            navigate(`/support/${a.id}`);
          }}
        >
          {applicationNo(a)}
        </AppLink>
      ),
    },
    { key: 'applicantIsikukood', title: 'Personal code', dataIndex: 'applicantIsikukood' },
    {
      key: 'birthDate',
      title: 'Date of birth',
      dataIndex: 'applicantIsikukood',
      render: (code: string) => birthDateFromCode(code) ?? '—',
    },
    {
      key: 'supportTypeCode',
      title: 'Benefit scheme',
      dataIndex: 'supportTypeCode',
      render: (code: string) => <SchemeTag code={code} name={typeName(code)} />,
    },
    {
      key: 'period',
      title: 'Period',
      dataIndex: 'periodStart',
      render: (_: string, a: Application) =>
        a.periodStart === a.periodEnd ? a.periodStart : `${a.periodStart} – ${a.periodEnd}`,
    },
    { key: 'householdSize', title: 'Family members', dataIndex: 'householdSize' },
    {
      key: 'status',
      title: 'Status',
      dataIndex: 'status',
      render: (s: Application['status']) => <StatusTag status={s} />,
    },
  ];

  return (
    <>
    <Overview report={counts} status={status} onStatus={setStatus} />
    <Modal open={!!report} title="Summary report" footer={null} onCancel={() => setReport(null)}>
      {report && <SummaryView report={report} schemeName={typeName} />}
    </Modal>
    <ResourceList<Application>
      title="Assignment of unemployment support payment"
      columns={columns}
      dataSource={rows}
      rowKey="id"
      loading={loading}
      search={{ value: search, onChange: setSearch, placeholder: 'Personal code…' }}
      filter={{
        content: (
          <div style={{ display: 'grid', gap: 8 }}>
            <Select
              allowClear
              style={{ width: '100%' }}
              placeholder="Any status"
              value={status}
              onChange={(v?: string) => setStatus(v)}
              options={Object.entries(statusLabel).map(([value, label]) => ({ value, label }))}
            />
            <Select
              allowClear
              style={{ width: '100%' }}
              placeholder="Any benefit scheme"
              value={typeCode}
              onChange={(v?: string) => setTypeCode(v)}
              options={types.map((t) => ({ value: t.code, label: t.name }))}
            />
          </div>
        ),
        onClear: () => {
          setStatus(undefined);
          setTypeCode(undefined);
        },
      }}
      actions={
        <div style={{ display: 'flex', gap: 8 }}>
          <AppButtonSecondary icon={<BarChartOutlined />} onClick={openSummary}>
            Summary
          </AppButtonSecondary>
          <AppButtonPrimary icon={<PlusOutlined />} onClick={() => navigate('/support/new')}>
            Register application
          </AppButtonPrimary>
        </div>
      }
      detailView={{
        selectedRecord: selected,
        onSelectedRecordChange: setSelected,
        title: 'Application',
        render: (a) => (
          <div style={{ display: 'grid', gap: 8 }}>
            <div>
              <b>{applicationNo(a)}</b> <StatusTag status={a.status} />
            </div>
            <div>Personal code {a.applicantIsikukood}</div>
            <div>
              <SchemeTag code={a.supportTypeCode} name={typeName(a.supportTypeCode)} />
            </div>
            <div>
              {a.periodStart} – {a.periodEnd} · family members: {a.householdSize}
            </div>
            <b style={{ marginTop: 4 }}>Stored evidence</b>
            {evidence.length === 0 && <div style={{ opacity: 0.65 }}>Not verified yet.</div>}
            {evidence.map((e) => (
              <div key={e.id}>
                <div style={{ fontSize: 12 }}>
                  {checkIcon[e.checkType]} {checkLabel[e.checkType]}
                </div>
                <ResultTag result={e.result} />
                {e.manual && <ManualTag />}
                <div style={{ opacity: 0.65, fontSize: 12 }}>
                  {e.source} · retrieved <AppTimestamp value={e.retrievedAt} fallback="—" />
                </div>
              </div>
            ))}
            <div style={{ opacity: 0.65, fontSize: 12 }}>No registry is queried from this panel.</div>
            <AppButtonSecondary onClick={() => navigate(`/support/${a.id}`)}>Open application</AppButtonSecondary>
          </div>
        ),
      }}
    />
    </>
  );
};
