# Niconico YT Integration Test

Personal test build of Niconico-style live/replay comments integrated with the
[upstream patches](https://github.com/MorpheApp/morphe-patches).
This is an independent testing fork, not an official Morphe release.

## 폰에서 테스트

1. Morphe에서 **Expert mode**를 켭니다.
2. **Sources → + → Remote**에 아래 주소를 추가합니다.

   ```text
   https://github.com/david419kr/morphe-patches
   ```

3. **Niconico YT Integration Test**, 버전 **1.49.0-niconico.2**을 확인합니다.
4. 이번 테스트에서는 기존 **Morphe Patches**와 독립 **Niconico YT Comments Patches** 소스를 **Disable**하고, 이 테스트 소스만 켭니다. 공식 패치가 이미 포함돼 있습니다.
5. 원본 **YouTube 21.23.492 APK** (21.20.405도 선택 가능)를 선택합니다.
6. 테스트 소스의 권장 패치를 유지하고 **Niconico-style live/replay comments (Experimental)**를 추가로 선택합니다.
7. 패치 후 설치합니다. 플레이어의 **弾** 버튼을 누르면 댓글을 켜고 끌 수 있고, 길게 누르면 글꼴 크기와 불투명도를 조절할 수 있습니다.

현재 통합본의 니코니코 패치는 **21.20.405·21.23.492를 지원 대상으로 지정**했습니다. 21.04.223은 PC 패치·재구성 검증에 성공했지만 지원 목록에는 추가하지 않았습니다.
이전 테스트 버전은 21.23.492 실기기 정상 동작을 확인했으며, 이번 설정·번역 수정본은 실기기 재검증 전입니다. 비공개·멤버십 전용·연령 제한 등 로그인이 필요한 채팅은 지원하지 않습니다.

## Build and release notes

See [LOCAL_TEST_RELEASE.md](LOCAL_TEST_RELEASE.md) for the branch layout and English instructions.
The clean integration branch is `codex/niconico-comments-integration`; no upstream PR has been opened.
Releases are manual. This fork's `main/patches-bundle.json` points to the personal test bundle.

The existing standalone [Niconico YT Comments Patches](https://github.com/david419kr/niconico-yt-morphe-patches)
source is maintained separately.

## License and credits

The upstream patches and this integration are distributed under the
[GNU General Public License v3.0](LICENSE), with the additional terms in [NOTICE](NOTICE).
Original Morphe and ReVanced notices are preserved. The Niconico extension is adapted from
`david419kr/niconico-yt-morphe-patches` 1.3.3.
