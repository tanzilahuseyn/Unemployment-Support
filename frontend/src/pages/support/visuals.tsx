import type { ReactNode } from 'react';
import {
  CalendarOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  CloseCircleOutlined,
  ExclamationCircleOutlined,
  FileDoneOutlined,
  GiftOutlined,
  HomeOutlined,
  HourglassOutlined,
  IdcardOutlined,
  ShopOutlined,
  SyncOutlined,
  ToolOutlined,
} from '@ant-design/icons';
import { AppTag } from '@helex/ui';
import {
  checkColor,
  statusColor,
  statusLabel,
  type ApplicationStatus,
  type CheckStatus,
  type CheckType,
} from '../../supportApi';

/**
 * Colour and icons for the support screens: one place, so every screen tells the
 * same story — the same status has the same colour and icon everywhere.
 */

export const statusIcon: Record<ApplicationStatus, ReactNode> = {
  OPEN: <ClockCircleOutlined />,
  PENDING_VERIFICATION: <HourglassOutlined />,
  READY_FOR_DECISION: <FileDoneOutlined />,
  APPROVED: <CheckCircleOutlined />,
  REJECTED: <CloseCircleOutlined />,
};

/** A status tag with its icon. */
export const StatusTag = ({ status }: { status: ApplicationStatus }) => (
  <AppTag color={statusColor[status]} icon={statusIcon[status]}>
    {statusLabel[status]}
  </AppTag>
);

const schemeLook: Record<string, { color: string; icon: ReactNode }> = {
  MONTHLY: { color: 'geekblue', icon: <CalendarOutlined /> },
  ONE_OFF: { color: 'magenta', icon: <GiftOutlined /> },
};

/** A benefit scheme tag: monthly support blue with a calendar, one-off support pink with a gift. */
export const SchemeTag = ({ code, name }: { code: string; name: string }) => {
  const look = schemeLook[code] ?? { color: 'default', icon: undefined };
  return (
    <AppTag color={look.color} icon={look.icon}>
      {name}
    </AppTag>
  );
};

/** The registry behind each check: home for residence, ID card for Töötukassa, shop for employment. */
export const checkIcon: Record<CheckType, ReactNode> = {
  RESIDENCE: <HomeOutlined />,
  UNEMPLOYMENT: <IdcardOutlined />,
  EMPLOYMENT: <ShopOutlined />,
};

const resultIcon: Record<CheckStatus, ReactNode> = {
  PASS: <CheckCircleOutlined />,
  FAIL: <CloseCircleOutlined />,
  UNAVAILABLE: <SyncOutlined />,
  INCONCLUSIVE: <ExclamationCircleOutlined />,
};

/** A check result tag with its icon. */
export const ResultTag = ({ result }: { result: CheckStatus }) => (
  <AppTag color={checkColor[result]} icon={resultIcon[result]}>
    {result}
  </AppTag>
);

/** The tag marking evidence a worker entered by hand. */
export const ManualTag = () => (
  <AppTag color="purple" icon={<ToolOutlined />}>
    MANUAL
  </AppTag>
);

/**
 * The list's illustration, drawn for this app: a home, a person and a payment
 * card, in the TalTech colours.
 */
export const SupportIllustration = ({ size = 120 }: { size?: number }) => (
  <svg width={size} height={(size * 90) / 120} viewBox="0 0 120 90" role="img" aria-label="A home, a person and a support payment">
    <circle cx="60" cy="48" r="40" fill="#ebeaef" />
    {/* home */}
    <path d="M18 52 L38 34 L58 52 Z" fill="#342b60" />
    <rect x="23" y="51" width="30" height="24" rx="2" fill="#ffffff" stroke="#342b60" strokeWidth="2" />
    <rect x="34" y="60" width="8" height="15" fill="#4dbed2" />
    {/* person */}
    <circle cx="74" cy="34" r="8" fill="#e4067e" />
    <path d="M60 75 C60 55 88 55 88 75 Z" fill="#e4067e" />
    {/* payment card */}
    <g transform="rotate(-10 96 56)">
      <rect x="82" y="46" width="30" height="20" rx="3" fill="#ffffff" stroke="#342b60" strokeWidth="2" />
      <rect x="82" y="51" width="30" height="4" fill="#342b60" />
      <circle cx="105" cy="61" r="2.5" fill="#e4067e" />
    </g>
    {/* check mark */}
    <circle cx="96" cy="22" r="9" fill="#277257" />
    <path d="M91.5 22 L95 25.5 L101 18.5" stroke="#ffffff" strokeWidth="2.4" fill="none" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
);
