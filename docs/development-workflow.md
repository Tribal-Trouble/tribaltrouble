# Development Workflow

How we branch, who can merge where, and how we ship without drowning in PRs. For the
mechanics of actually cutting and publishing a release, see [Releasing](releasing.md).

## Goals

- Ship features fast. The core maintainers should not have to open a PR to land their own
  work.
- Keep the gate up for everyone else. Other collaborators cannot push to `main`; their work
  goes through a PR that a maintainer approves.
- Be able to test not-ready features on Steam early (e.g. boats) without forcing them
  into the next release.

## Branch model

| Branch | Role | Who merges here |
|---|---|---|
| `main` | Integration trunk. Everything we want in the next feature drop lands here. | Maintainers directly. Others via approved PR. |
| `release` | The cut. Pushing here publishes to every platform's **prerelease** location; a separate promote step moves it to stable. See [Releasing](releasing.md). | Maintainers, by merging `main` |
| `beta` | Optional preview channel for showing testers a feature that is not in the next cut yet. Disposable. | Maintainers, by merging `main` + a feature branch |
| `feature/*`, `fix/*` | Day-to-day work. Branch off `main`, merge back into `main`. | The author |
| `hotfix/*` | Urgent fix to the live build. Branch off `release`. | The author, then merge to `release` and forward to `main` |

`main` is our `develop`-style integration branch, not a continuously-shippable trunk. We
batch features into it and cut releases from it on our own schedule.

## Where work goes (the rules)

1. **Features branch off `main` and merge back into `main`.** Do not branch off `release`
   or an older point to "avoid main's baggage"; that just leaves you stale on everything
   already merged and you pay for it at merge time.
2. **Only merge a feature into `main` when it is individually shippable.** If the code is
   done but we are not ready to expose it, merge it **behind a feature flag that is off by
   default**. If it is still in progress, keep it on its own branch. This is what keeps a
   release cut as simple as "snapshot `main` and ship it" instead of a scramble to check
   whether everything in `main` is actually ready.
3. **A release cut is a point-in-time snapshot of `main`.** Promoting `main` to `release`
   takes everything in `main`. There is no selective "ship feature Y but not feature X"
   unless X is flagged off or was never merged. Plan accordingly.
4. **Hotfixes branch off `release`.** Fix on the hotfix branch, merge into `release` to
   ship, then forward the same fix into `main` so it is not lost on the next cut.

## Who can merge (access policy)

We want maintainers to move fast while still gating everyone else. GitHub rulesets with a
**bypass list** do exactly this. The bypass is keyed to the **Admin** role, so "maintainer"
here means "holds the Admin role on the repo".

- **Maintainers (Admin role):** push or merge to `main` directly, no PR required.
- **Other collaborators (Write role):** must open a PR that a code owner approves and that
  passes CI before it can merge.
- **CI is required on PRs.** The admin bypass is "always", so it skips CI on direct pushes
  too; that is the accepted tradeoff for fast shipping. The discipline that replaces the gate
  is on the maintainers: do not push a broken build straight to `main`.

### How this is set up

The repo lives in the **Tribal-Trouble** org, and bypass is keyed to the **Admin** role,
not to named users. The `main` ruleset (Settings → Rules → Rulesets) is already active with
most of this in place.

1. **Roles** (Settings → Collaborators and teams):
   - Maintainers: **Admin**. The Admin role is what puts them on the ruleset bypass, so they
     push to `main` directly with no PR.
   - Everyone else: **Write**. Write does not bypass, so they must open a PR.
   - The Admin role is the access boundary: granting it to a collaborator also grants direct,
     unreviewed push to `main`. Keep Admin limited to the maintainers and add people there
     deliberately, not for convenience.
2. **`main` ruleset** (active):
   - Require a pull request before merging (on), at least 1 approval (on)
   - **Require review from Code Owners** (turn on) so the required approval must come from a
     maintainer, not one Write collaborator approving another's PR
   - **Require status checks to pass** (turn on) and select the CI jobs (`format-check`,
     `api-guard`, the platform builds) so PRs must be green. The admin bypass skips this,
     which is the accepted tradeoff for direct push.
   - Bypass: `Organization admin` + `Repository admin`, both "always". Leave as is.
   - Restrict deletions + block force pushes: on. Leave as is.
3. **CODEOWNERS** at `.github/CODEOWNERS` routes the required review to the core team:

   ```
   *  @ryan-linehan @OmarAMokhtar
   ```

With this in place: maintainers push to `main` with zero review ceremony, while a Write
collaborator's PR cannot merge without a code owner approving it and CI passing.

### `release` access

`release` should be core-only. Its ruleset currently blocks deletion and force-push but
does **not** restrict normal pushes, so any Write collaborator can push to it. Add a
**Restrict updates** rule to `release` so only the admin bypass can push there. See also the
branch-protection note in [Releasing](releasing.md#security-notes); the environment approval
gate is defense-in-depth, not the primary access control.

## Shipping faster

Most PR tedium comes from process gates, not branch topology. Levers, in order of impact:

- **Maintainers do not PR their own routine work.** With the bypass above, they merge
  directly. Reserve PRs for changes that genuinely benefit from a second pair of eyes, and
  for outside contributions.
- **Merge small and often behind flags** instead of parking work on a long-lived branch
  that lands as one giant PR at the end.
- **Keep heavy CI on the `release` cut, lighter checks on PRs**, if CI latency is what
  makes day-to-day merging slow.

## Beta preview channel

`beta` lets us put a not-ready feature in front of testers on Steam without committing it
to the next release. It is a **side channel**, not a step on the way to `release`.

The one rule that keeps it from becoming a mess: **`beta` is disposable, and you never
merge back from it.** Treat it as a build artifact that happens to be a branch.

- Build it: `beta = main + <feature branch>` (for example merge `boats_on_steam` into a
  fresh `beta`). Deploy `beta` to a Steam beta branch.
- Keep developing the feature **on its own branch**, never on `beta`.
- To update the preview, re-merge the feature branch (and latest `main`) into `beta` and
  redeploy.
- A tester finds a bug: the fix goes on the **feature branch**, then flows into `beta` on
  the next re-merge. Never commit fixes directly on `beta`; they are orphaned the moment
  it is rebuilt.
- When the feature is ready, merge the feature branch into `main` and ship it through the
  normal release cut. Then reset `beta` back to `main` or rebuild it with the next preview.

Because `beta` carries code that is not in `main`, **never promote `beta` to `release`**;
that would ship the preview feature early. Releases always come from `main`.

### Wiring note (not yet set up)

The release flow already uses the Steam `prerelease` branch (see [Releasing](releasing.md)),
so the beta preview channel needs its **own** Steam beta branch and deploy so the two do
not collide. Until that is wired, the lightest option for previewing a single feature is to
point a manual Steam beta deploy at the feature branch directly (for example
`boats_on_steam`) without a dedicated `beta` git branch.

## Quick reference

- New feature: branch `feature/x` off `main`, build it, merge back to `main` (flag it off
  if it is done but should stay hidden).
- Ready to release: bump version if needed, merge `main` into `release`, follow
  [Releasing](releasing.md).
- Live bug: branch `hotfix/x` off `release`, fix, merge to `release`, forward to `main`.
- Show testers something early: merge the feature branch into a disposable `beta`, deploy
  to the Steam beta branch, keep working on the feature branch.
- Outside contribution: PR into `main`, core-team approval required.
