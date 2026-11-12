import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Modal, Select } from 'antd';
import { BarChartOutlined, PlusOutlined } from '@ant-design/icons';
import {
  AppButtonPrimary,
  AppButtonSecondary,
  AppLink,
  AppTag,
  AppTimestamp,
  ResourceList,
  useAppNotification,
  type ResourceListColumn,
} from '@helex/ui';
import {
  checkColor,
  checkLabel,
  reasonLabel,
  statusColor,
  statusLabel,
  supportApi,
  type Application,
  type ApplicationStatus,
  type BenefitScheme,
  type Evidence,
  type ReasonCode,
  type SummaryReport,
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
      key: 'applicantIsikukood',
      title: 'Personal code',
      dataIndex: 'applicantIsikukood',
      locked: true,
      // The identifier is the way into the record: it links to the application.
      render: (code: string, a: Application) => (
        <AppLink
          href={`/support/${a.id}`}
          onClick={(e) => {
            e.preventDefault();
            e.stopPropagation();
            navigate(`/support/${a.id}`);
          }}
        >
          {code}
        </AppLink>
      ),
    },
    {
      key: 'supportTypeCode',
      title: 'Benefit scheme',
      dataIndex: 'supportTypeCode',
      render: (code: string) => <AppTag>{typeName(code)}</AppTag>,
    },
    {
      key: 'period',
      title: 'Period',
      dataIndex: 'periodStart',
      render: (_: string, a: Application) =>
        a.periodStart === a.periodEnd ? a.periodStart : `${a.periodStart} – ${a.periodEnd}`,
    },
    { key: 'householdSize', title: 'Household', dataIndex: 'householdSize' },
    {
      key: 'status',
      title: 'Status',
      dataIndex: 'status',
      render: (s: Application['status']) => <AppTag color={statusColor[s]}>{statusLabel[s]}</AppTag>,
    },
  ];

  return (
    <>
    <Modal open={!!report} title="Summary report" footer={null} onCancel={() => setReport(null)}>
      {report && <SummaryView report={report} schemeName={typeName} />}
    </Modal>
    <ResourceList<Application>
      title="Unemployment support applications"
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
              <b>{a.applicantIsikukood}</b> <AppTag color={statusColor[a.status]}>{statusLabel[a.status]}</AppTag>
            </div>
            <div>{typeName(a.supportTypeCode)}</div>
            <div>
              {a.periodStart} – {a.periodEnd} · household {a.householdSize}
            </div>
            <b style={{ marginTop: 4 }}>Stored evidence</b>
            {evidence.length === 0 && <div style={{ opacity: 0.65 }}>Not verified yet.</div>}
            {evidence.map((e) => (
              <div key={e.id}>
                <div style={{ fontSize: 12 }}>{checkLabel[e.checkType]}</div>
                <AppTag color={checkColor[e.result]}>{e.result}</AppTag>
                {e.manual && <AppTag color="purple">MANUAL</AppTag>}
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
