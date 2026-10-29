# Niconico YT Integration Test

## Stale feed binding recovery (1.49.0-niconico.11)

Select **Fix tablet home feed layout (experimental)** and **Feed layout diagnostics**.
The recovery compares current item metadata with the actual laid-out view. A stable
full-width/cell-width mismatch in a multi-column feed triggers a native dataset rebind.
It does not rewrite widths, content, flags or keyed tags. Confirmation and rate limits
avoid transient loading states and rebind loops. Supported: YouTube 21.20.405 and 21.23.492.

A debugger prototype restored the user's broken live feed with a rebind. The automatic
prototype also detected and repaired a controlled stale-width injection on-device.
These validate recovery of the observed state, not elimination of the original YouTube bug.
The packaged patch still needs phone verification. Runtime inspection is optional.

## Live runtime inspection (1.49.0-niconico.11)

Select **Enable feed runtime inspection** together with **Feed layout diagnostics**.
This only enables Android debugger attachment; it adds no rendering workaround.
Android debugging can slow the app and should be omitted from a normal build.
Keep the same original YouTube APK and other patch selections for comparison.

## Tablet feed lifecycle fix (1.49.0-niconico.11)

Backports official Morphe commit `6d4315757768d792614915d0867079437505fab9`.
Released list sections are unregistered so they cannot mutate a shared item list without
updating its replacement section. Grid rows are rebuilt when removal changes a grid to
a single-item section. The unsuccessful .7/.8 width and template overrides are removed.

Update the existing source and repatch the original YouTube APK. The fix is part of the
normal Litho filtering dependency; the old optional experimental entry remains compatible
but no longer forces widths. Keep **Feed layout diagnostics** selected for comparison.
Device verification of this backport is pending.

## Feed layout diagnostics (1.49.0-niconico.6)

This personal build adds the optional **Feed layout diagnostics** patch for investigating
tablet feed layout problems. Update the existing source URL, select this patch in Morphe,
and apply the bundle to an unmodified supported YouTube APK. The diagnostic patch is not
selected by default and works with or without **Feed card layout**. It does not fix the layout.

Once the patched app is running, record logcat while reproducing the problem:

```text
adb -s 127.0.0.1:17555 logcat -v threadtime -s MorpheFeedDiag:I *:S
```

The log contains view identities, grid/layout-parameter integer fields, and item positions
and dimensions; it does not intentionally record video titles, channel names, or account
information. The field names are obfuscated and must be interpreted against the tested APK.
Remove the diagnostic patch when building a normal APK to stop diagnostic collection.

Personal test build of Niconico-style live/replay comments integrated with the
[upstream patches](https://github.com/MorpheApp/morphe-patches).
This is an independent testing fork, not an official Morphe release.

## 폰에서 테스트

1. Morphe에서 **Expert mode**를 켭니다.
2. **Sources → + → Remote**에 아래 주소를 추가합니다.

   ```text
   https://github.com/david419kr/morphe-patches
   ```

3. **Niconico YT Integration Test**, 버전 **1.49.0-niconico.11**을 확인합니다.
4. 이번 테스트에서는 기존 **Morphe Patches**와 독립 **Niconico YT Comments Patches** 소스를 **Disable**하고, 이 테스트 소스만 켭니다. 공식 패치가 이미 포함돼 있습니다.
5. 원본 **YouTube 21.23.492 APK** (21.20.405도 선택 가능)를 선택합니다.
6. 테스트 소스의 권장 패치에서 **Niconico comments**가 선택돼 있는지 확인합니다. 이번 PR 상태에서는 기본 포함됩니다.
7. 패치 후 설치하고 **YouTube의 Morphe 설정 → Player → Niconico comments**에서 기능을 켠 뒤 앱을 재시작합니다. 패치 기본 포함과 별개로 기능의 초기값은 OFF입니다.
8. 플레이어의 새 댓글 아이콘을 누르면 댓글 표시를 켜고 끌 수 있고, 길게 누르면 글꼴 크기·불투명도·이동 시간·최대 댓글 길이를 조절할 수 있습니다.

이 버전은 [공식 PR #4080](https://github.com/MorpheApp/morphe-patches/pull/4080)의 **81d8b03d6c22af1a0bfc30f2f78c1608827d7ee9**를 반영하고 선택형 피드 레이아웃 진단 패치를 추가한 개인 테스트 배포입니다.
지원 선언은 메인테이너가 공식 YouTube 대상 목록 전체로 확대했습니다. 이는 모든 버전의 실기기 동작을 검증했다는 뜻은 아닙니다. 우선 기존에 사용하던 **21.23.492**로 비교 테스트해 주세요.
이번 PR 수정 상태는 아직 사용자의 실기기 검증 전입니다. 라이브·다시보기·탐색·배속, 새 아이콘의 토글·길게 누르기, 네 가지 슬라이더, 가로·세로 전환을 확인한 뒤 메인테이너에게 결과를 전달하면 됩니다. 비공개·멤버십 전용·연령 제한 등 로그인이 필요한 채팅은 지원하지 않습니다.

## Build and release notes

See [LOCAL_TEST_RELEASE.md](LOCAL_TEST_RELEASE.md) for the branch layout and English instructions.
The upstream PR branch is `codex/niconico-comments-integration`; this test follows the maintainer's current changes in PR #4080 without modifying that branch.
Releases are manual. This fork's `main/patches-bundle.json` points to the personal test bundle.

The existing standalone [Niconico YT Comments Patches](https://github.com/david419kr/niconico-yt-morphe-patches)
source is maintained separately.

## License and credits

The upstream patches and this integration are distributed under the
[GNU General Public License v3.0](LICENSE), with the additional terms in [NOTICE](NOTICE).
Original Morphe and ReVanced notices are preserved. The Niconico extension is adapted from
`david419kr/niconico-yt-morphe-patches` 1.3.3.
