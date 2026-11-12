# Manual · Frontend

The UI layer in one page: how the React app is wired, which components you
build with, and how you add pages. The screen-by-screen contract is
in the [specification](../specifications/SUPPORT.01-unemployment-support.md#ui).

## The component catalogue: Storybook

**<https://emr.helex.dev/storybook>** is the live catalogue of `@helex/ui` —
every component with its props, variants and a rendered example, the same
library the production Helex applications are built from. Keep it open in a
tab while you build. Where to start:

| Group | What is there | Start with |
| --- | --- | --- |
| Documentation | how the library is meant to be used, themes | [Get Started](https://emr.helex.dev/storybook/?path=/docs/documentation-get-started--docs), [Themes](https://emr.helex.dev/storybook/?path=/docs/documentation-themes--docs) |
| Views | whole-screen patterns | [Resource List](https://emr.helex.dev/storybook/?path=/story/views-resource-list--default) — the list page; [Resource Form](https://emr.helex.dev/storybook/?path=/story/views-resource-form--create) — the record page |
| Data Entry | inputs: `AppInput`, `AppSelect`, `AppDatePicker`, `AppForm`, `AppButton`, … | [AppForm](https://emr.helex.dev/storybook/?path=/story/data-entry-appform--default), [AppButton](https://emr.helex.dev/storybook/?path=/story/data-entry-appbutton--default) |
| Data Display | `AppTable`, `AppTag`, `AppStatusTag`, `AppCard`, `AppDescriptions`, `AppTimestamp`, … | [AppTag](https://emr.helex.dev/storybook/?path=/story/data-display-apptag--default) |
| Feedback | `AppNotification`, `AppModal`, `AppDrawer`, `AppPopconfirm`, `AppAlert`, `AppResult` | [AppNotification](https://emr.helex.dev/storybook/?path=/story/feedback-appnotification--all-types) |
| Layout, Navigation | `AppLayout`, `AppHeader`, `AppFormSection`, `AppLink`, `AppMenu`, `AppPagination` | — |

The rule from `AGENTS.md`: **every input is an existing `@helex/ui` or antd
component**. Look it up there first; write nothing of your own that the
catalogue already has.

## Where things are

`frontend/` is a Vite + React 19 + TypeScript app:

| File | Role |
| --- | --- |
| `src/main.tsx` | the provider stack, exactly as production nests it: Redux store → i18n → query client → auth → router → theme → `AppRoot`. You should not need to touch it. |
| `src/App.tsx` | the routes: `/` → `/support`, `/support`, `/support/new`, `/support/:id` |
| `src/api.ts` | shared API helpers: the `Bearer` header, problem-detail errors surfaced as `Error.message`, `QueryResult<T>` |
| `src/supportApi.ts` | the support API client: types and `fetch` wrappers for every endpoint, plus labels and tag colours |
| `src/pages/support/SupportList.tsx` | the list page — `ResourceList` with a detail panel, plus the Summary report in a modal |
| `src/pages/support/SupportCreate.tsx` | the registration form — antd `Form` in an `AppCard` |
| `src/pages/support/SupportDetail.tsx` | the record page — `ResourceForm` + `useDataController`: view / edit / withdraw, with verification, manual verification, the decision and the audit log in the sidebar; the personal code in the list links here |
| `src/theme/taltech.ts` | the TalTech theme, registered **before** first render; `VITE_THEME` in `.env` selects it |
| `vite.config.ts` | dev server on `18640`, `/api` and `/mock-registry` proxied to the backend (same origin, so no CORS anywhere), one React only (`dedupe`) |

`@helex/ui` and friends are the published `@helex-solutions/*` packages,
aliased in `package.json`; nothing is built from source here.

## Required minimum

What the UI must have, and where this component does it:

| Requirement | In this component |
| --- | --- |
| A list page on `ResourceList`: columns, a search field, an action button, a detail panel | `SupportList.tsx` |
| A record page on `ResourceForm` reached from the list's identifier column: view, edit → `PUT`, withdraw → `DELETE` | `SupportDetail.tsx` |
| A create form whose every input is a library component, submitting to the API | `SupportCreate.tsx` — `Input`, `Select` (options from `/schemes`), `DatePicker.RangePicker`, `InputNumber` |
| Backend errors shown to the user **verbatim** from the problem document, in a notification | `notify.error('Could not register the application', e.message)` |
| Data from the external registries displayed, and their absence handled | the check results as tags (an unreachable registry shows as `UNAVAILABLE`, with manual verification offered); the person's name and address shown live after verification, never stored |
| Routes under `App.tsx`, API calls under `supportApi.ts` — no `fetch` inside components | — |
| `npx tsc -b` clean | — |

## Run and check

```bash
cd frontend && npm ci          # once; needs the GitHub Packages token (README § Tools)
./scripts/run-frontend.sh      # http://localhost:18640 — backend must be running
cd frontend && npx tsc -b      # the typecheck, before every PR
```

Signed in automatically: in dev builds `MockAuthProvider` signs you in as
`superadmin` without a login screen and the API client sends
`Authorization: Bearer superadmin` on every call. Production builds use the
real `AuthProvider`. The provider probes `/api/uma/auth/mock-users` and
`/api/uma/userinfo` on startup — `MockUmaController` answers them; do not
remove it.

## The list page pattern

`ResourceList` (see `SupportList.tsx`) takes:

- `columns: ResourceListColumn<T>[]` — `dataIndex`, optional `render`, `locked`
  (cannot be hidden), `defaultVisible: false` (hidden until the user enables it);
- `dataSource`, `rowKey`, `loading`;
- `search={{ value, onChange, placeholder }}` — the page debounces and calls
  the API with `textContains`;
- `actions` — the primary button, `AppButtonPrimary`;
- `detailView={{ selectedRecord, onSelectedRecordChange, title, render }}` —
  a side card. It is **closed by default**; the eye button in the header
  opens it, then a row click fills it. The support list shows the stored
  evidence there — no registry is queried from the panel.

Pagination and column configuration come with the component.

## The record page pattern

`SupportDetail.tsx`: `ResourceForm` in `view` or `edit` mode with `sections` of
typed fields (`text`, `select` with `options`, `custom` with your own input),
fed by `useDataController` from `@helex/core` — `load` the record from the API,
`current` is what view mode shows, `reset` on Cancel. `onSave` receives the
form values for the `PUT`; `onDelete` is the soft delete (withdraw, offered only
while undecided); `onBack` returns to the list; `sidebar` holds what belongs
beside the record — here verification, the decision, the audit log and the
platform's metadata card (`AppCard` of `FieldItem`s: created at / by, modified at / by,
version, from the `sys*` fields), as in Storybook's *Resource Form › With
Sidebar (metadata)*. Two things it needs: the library stylesheet
imported in `main.tsx` (the package does not export it) and, for dates, a
`custom` field — see the pitfalls in `AGENTS.md`.

## The form pattern

`SupportCreate.tsx`: antd `Form layout="vertical"` inside `AppCard`, one
`Form.Item` per field with `rules` for the shape (`required`, `max`, `pattern`)
— the same rules the backend's Bean Validation enforces — and library inputs
inside. `onFinish` maps the values to the API body (dates via
`dayjs.format('YYYY-MM-DD')`), calls `supportApi.create`, shows
`notify.success` and navigates back; the `catch` shows the backend's
`detail` text unchanged, which is where 400/409 messages surface.

## Notifications

`useAppNotification()` gives `notify.success/info/warning/error(title, text)`
— top-right, with the platform's durations, and a "Copy" button on errors. Its
result is stable across renders, so it is safe in hook dependency arrays; the
pages list it there.

## Adding pages

1. Types and calls in `src/supportApi.ts` (or a sibling `<name>Api.ts` in the
   same shape): a `QueryResult<T>` for lists, a body type for create.
2. A list page on `ResourceList` and a create page on `Form` + library inputs,
   under `src/pages/<component>/` — mirror the support pages.
3. Routes in `src/App.tsx`.
4. `npx tsc -b`, then the flow in the browser: list → create → back to the
   list with the new row, and an invalid submit showing the backend's message.

Two Reacts in the bundle produce "Invalid hook call": keep the `overrides` in
`package.json` and the `dedupe` list in `vite.config.ts` as they are. A stale
Vite cache after dependency changes: delete `frontend/node_modules/.vite`.

References: `SupportList.tsx` · `SupportCreate.tsx` · `SupportDetail.tsx` ·
`supportApi.ts` · `api.ts` · `main.tsx` ·
`vite.config.ts` · [the UI section of the spec](../specifications/SUPPORT.01-unemployment-support.md#ui)
· `AGENTS.md` · **[Storybook](https://emr.helex.dev/storybook)**.
