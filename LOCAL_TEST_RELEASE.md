# Niconico YT Integration Test

This fork is a personal testing build based on MorpheApp/morphe-patches `dev`
at `c5f9f94ee71a480be00a432ee364f4e7099b9537`, with Niconico live/replay comments
integrated into the upstream patch and extension modules. It is not an official
Morphe release. No upstream pull request has been opened.

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
   **1.49.0-niconico.2**.
4. For this test, disable the pre-installed **Morphe Patches** source and the
   standalone **Niconico YT Comments Patches** source. Keep this test source enabled.
   This bundle already contains the upstream patches; do not apply duplicate
   copies of them from other sources.
5. Select YouTube and provide the unmodified **21.23.492 APK** (21.20.405 is also declared compatible).
6. Keep the test source's recommended patches enabled, and manually enable
   **Niconico-style live/replay comments (Experimental)**.
7. Patch and install. The `弾` player button toggles comments; long-press it to
   change the font size and opacity.

When returning to normal patching, disable this test source and re-enable the
official and standalone sources.

The Niconico patch is declared compatible with YouTube 21.20.405 and 21.23.492.
21.04.223 passed PC patch/rebuild tests but is not added to the compatibility list.
Other upstream app targets do not imply Niconico compatibility.
The earlier test build was reported working on 21.23.492; the new settings migration
and localization changes still need device testing. Private,
members-only, age-restricted, and other chat requiring sign-in is unsupported.
