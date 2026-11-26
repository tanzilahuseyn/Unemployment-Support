import type { ReactElement } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import { SupportList } from './pages/support/SupportList';
import { SupportCreate } from './pages/support/SupportCreate';
import { SupportDetail } from './pages/support/SupportDetail';
import { CitizenLogin } from './pages/citizen/CitizenLogin';
import { CitizenApply } from './pages/citizen/CitizenApply';
import { CitizenDone } from './pages/citizen/CitizenDone';

/**
 * Components that may be ABSENT from this checkout register their routes by
 * exporting `routes: ReactElement[]` (a list of <Route>s) from
 * `pages/<component>/routes.tsx`. The glob is resolved at build time and is
 * simply empty when no such folder exists — the lecturer's private library
 * module lives there, gitignored. Our own routes are listed below.
 */
const componentRoutes = Object.values(
  import.meta.glob<{ routes: ReactElement[] }>('./pages/*/routes.tsx', { eager: true }),
).flatMap((m) => m.routes);

export const App = () => (
  <Routes>
    <Route path="/" element={<Navigate to="/support" replace />} />
    <Route path="/support" element={<SupportList />} />
    <Route path="/support/new" element={<SupportCreate />} />
    <Route path="/support/:id" element={<SupportDetail />} />
    {/* The citizen e-service — SUPPORT.01, Citizen e-service */}
    <Route path="/citizen" element={<CitizenLogin />} />
    <Route path="/citizen/apply" element={<CitizenApply />} />
    <Route path="/citizen/done/:id" element={<CitizenDone />} />
    {componentRoutes}
  </Routes>
);
