# Releasing

The app version lives in [`gradle/libs.versions.toml`](gradle/libs.versions.toml):

```toml
app-code = "1000001"
app-name = "1.0.0-beta.1"
```

`app-name` is the human-readable version. `app-code` must increase with every
published APK, including betas.

There is no local release tooling dependency. Version bumps, commits, tags, and
pushes are explicit manual actions.

## Beta / staging release

Edit both values in `gradle/libs.versions.toml`:

```toml
app-code = "1000002"
app-name = "1.0.0-beta.2"
```

Then commit and tag:

```bash
git add gradle/libs.versions.toml
git commit -m "chore: release v1.0.0-beta.2"
git tag v1.0.0-beta.2
```

Review, then push when ready:

```bash
git push origin HEAD --follow-tags
```

A **beta** tag builds a staging APK (app id `dev.hdytrfli.omata.staging`) and
creates a GitHub prerelease. It is not marked as the latest release. CI always
signs with the release key from the repository secrets.

## Stable release

Edit both values in `gradle/libs.versions.toml`:

```toml
app-code = "1000100"
app-name = "1.0.0"
```

Then commit and tag:

```bash
git add gradle/libs.versions.toml
git commit -m "chore: release v1.0.0"
git tag v1.0.0
git push origin HEAD --follow-tags
```

A stable tag builds a signed release APK (app id `dev.hdytrfli.omata`) and
creates a normal GitHub Release marked as latest.

## Changelog

GitHub release notes are generated in CI by `git-cliff` from Conventional
Commits. Nothing is committed to the repository; the notes live on the GitHub
Release.

Use commit messages such as:

```text
feat: add ...
fix: correct ...
perf: improve ...
refactor: ...
```

`cliff.toml` controls the grouping and formatting.

## Version rules

```text
1.0.0        stable release
1.0.0-beta.2 staging / GitHub prerelease
```

The tag must exactly match `v` plus `app-name`; the release workflow rejects
mismatches via `:app:verifyReleaseTag`. `app-code` is not part of the tag, but it
must always increase.

## Required GitHub secrets

The local keystore can live at `keystore/omata-release.jks`; the whole
`keystore/` directory is gitignored and never committed.

```text
RELEASE_KEYSTORE_BASE64
RELEASE_KEYSTORE_PASSWORD
RELEASE_KEY_ALIAS
RELEASE_KEY_PASSWORD
```

Both staging and release builds use the same key so beta users can install
updates over each other. Local builds without release credentials fall back to
the debug keystore for `staging` (and produce an unsigned `release`).

## CI behavior

- GitHub Actions runs only for version tags.
- No build or format workflow runs on every push or pull request.
- `v*-beta*` tags run the Staging workflow and publish a prerelease.
- Other `v*` tags run the Release workflow and publish the latest release.
- Kotlin formatting is checked (`ktfmtCheck`) before the build.
- Only `arm64-v8a` is built; the asset is named
  `omata-arm64-v8a-<tag>.apk`.
