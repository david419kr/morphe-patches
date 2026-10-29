# Niconico YT Integration Test

## Stale feed binding recovery (1.49.0-niconico.12)

Select **Fix tablet home feed layout (experimental)** and **Feed layout diagnostics**.
The recovery compares current item metadata with the actual laid-out view. A stable
full-width/cell-width mismatch in a multi-column feed (including portrait two-column grids) triggers a native dataset rebind.
It does not rewrite widths, content, flags or keyed tags. Confirmation and rate limits
avoid transient loading states and rebind loops. Supported: YouTube 21.20.405 and 21.23.492.

A debugger prototype restored the user's broken live feed with a rebind. The automatic
prototype also detected and repaired a controlled stale-width injection on-device.
These validate recovery of the observed state, not elimination of the original YouTube bug.
The packaged patch still needs phone verification. Runtime inspection is optional.

## Live runtime inspection (1.49.0-niconico.12)

Select **Enable feed runtime inspection** together with **Feed layout diagnostics**.
This only enables Android debugger attachment; it adds no rendering workaround.
Android debugging can slow the app and should be omitted from a normal build.
Keep the same original YouTube APK and other patch selections for comparison.

## Tablet feed lifecycle fix (1.49.0-niconico.12)

Backports official Morphe commit `6d4315757768d792614915d0867079437505fab9`.
Released list sections are unregistered so they cannot mutate a shared item list without
updating its replacement section. Grid rows are rebuilt when removal changes a grid to
a single-item section. The unsuccessful .7/.8 width and template overrides are removed.

Update the existing source and repatch the original YouTube APK. The fix is part of the
normal Litho filtering dependency; the old optional experimental entry remains compatible
but no longer forces widths. Keep **Feed layout diagnostics** selected for comparison.
Device verification of this backport is pending.

## Feed layout diagnostics: 1.49.0-niconico.6

Update the existing source at `https://github.com/david419kr/morphe-patches`, and select
the optional **Feed layout diagnostics** patch when repatching the original YouTube APK.
The patch works with or without **Feed card layout**. Capture `MorpheFeedDiag`
with logcat before and after reproducing the problem; the log contains measurements and
object identities, not intentionally video titles or account information. This diagnostic
build does not fix the layout. Rebuild without the optional patch to remove logging.

This fork is a personal test distribution of the current code in
[MorpheApp/morphe-patches PR #4080](https://github.com/MorpheApp/morphe-patches/pull/4080),
locked to commit `81d8b03d6c22af1a0bfc30f2f78c1608827d7ee9`.
It includes the maintainer's changes and is not an official Morphe release.
The personal fork also adds the optional feed layout diagnostics patch.

The clean integration is kept on `codex/niconico-comments-integration`.
The personal distribution branch is `codex/niconico-personal-test`, published
to this fork's `main` so Morphe can read `main/patches-bundle.json`.

GitHub Actions workflows are excluded from the personal distribution branch.
Releases and source metadata are updated manually.

The standalone Niconico source at
https://github.com/david419kr/niconico-yt-morphe-patches remains a separate bundle.

## Phone test

1. In Morphe enable Expert mode.
2. Open **Sources → + → Remote** and add
   `https://github.com/david419kr/morphe-patches`.
3. Confirm the source is **Niconico YT Integration Test**, version
   **1.49.0-niconico.12**.
4. For this test, disable the pre-installed **Morphe Patches** source and the
   standalone **Niconico YT Comments Patches** source. Keep this test source enabled.
   This bundle already contains the upstream patches; do not apply duplicate
   copies of them from other sources.
5. Select YouTube and provide the unmodified **21.23.492 APK** (21.20.405 is also declared compatible).
6. Keep the recommended patches enabled and confirm **Niconico comments** is
   selected. The maintainer changed this patch to be included by default.
7. Patch and install. In YouTube open **Morphe settings → Player → Niconico comments**,
   enable the feature, and restart the app. The feature itself defaults to OFF.
8. Tap the new comments player icon to show/hide comments. Long-press it to adjust
   font size, opacity, travel duration, and maximum comment length.

When returning to normal patching, disable this test source and re-enable the
official and standalone sources.

The maintainer now declares the Niconico patch compatible with all official
YouTube targets. This is a declaration, not evidence that every version has been
tested on a device. Compare this build with your previous test on 21.23.492 first.

This PR snapshot has not yet had user device verification. Check public live chat,
replay/premiere chat, seeking, speed changes, pause/resume, rotation, the new icon,
and all four quick settings sliders. Do not report an OK to the maintainer until
the user has tested this version. Private, members-only, age-restricted, and other
chat requiring sign-in is unsupported.
