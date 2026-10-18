import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Select } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import {
  AppButtonPrimary,
  AppLink,
  AppTag,
  ResourceList,
  useAppNotification,
  type ResourceListColumn,
} from '@helex/ui';
import { statusColor, supportApi, type Application, type SupportType } from '../../supportApi';

/**
 * The application list — SUPPORT.01 § UI, Application list. ResourceList with search
 * by personal code and filters for status and support type, like AnimalList.
 */
export const SupportList = () => {
  const navigate = useNavigate();
  const notify = useAppNotification();
  const [rows, setRows] = useState<Application[]>([]);
  const [types, setTypes] = useState<SupportType[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState<string | undefined>();
  const [typeCode, setTypeCode] = useState<string | undefined>();

  useEffect(() => {
    supportApi.types().then(setTypes).catch((e) => notify.error('Could not load support types', e.message));
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
      title: 'Support type',
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
      render: (s: Application['status']) => <AppTag color={statusColor[s]}>{s}</AppTag>,
    },
  ];

  return (
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
              options={[
                { value: 'OPEN', label: 'Open' },
                { value: 'APPROVED', label: 'Approved' },
                { value: 'REJECTED', label: 'Rejected' },
              ]}
            />
            <Select
              allowClear
              style={{ width: '100%' }}
              placeholder="Any support type"
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
        <AppButtonPrimary icon={<PlusOutlined />} onClick={() => navigate('/support/new')}>
          Register application
        </AppButtonPrimary>
      }
    />
  );
};
