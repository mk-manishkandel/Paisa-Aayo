# Nepali voice pack

Paisa Aayo speaks Nepali amounts by joining short recorded clips, e.g.
`ne_1 ne_hajar ne_2 ne_saya ne_50 ne_rupaiya ne_prapta_bhayo` →
"एक हजार दुई सय पचास रुपैयाँ प्राप्त भयो".

Until a complete pack is committed, the app falls back to the phone's text-to-speech engine
(Nepali if installed, otherwise Hindi, otherwise English).

## Recording the pack (volunteers welcome)

1. Read every line of [`clips.csv`](clips.csv) — 107 clips: numbers १–९९, सय, हजार, लाख, करोड,
   रुपैयाँ, पैसा, प्राप्त भयो, भुक्तानी भयो.
2. Record in a quiet room, one clip per file, same voice and distance for all clips.
   Trim silence at both ends (≤ 30 ms left), keep a natural, even tone.
3. Export mono, 22.05–48 kHz, as **Ogg Opus/Vorbis** (`.ogg`) or AAC (`.m4a`), named exactly as
   the `clip` column, e.g. `ne_47.ogg`.
4. Put them in `app/src/main/assets/voice/ne/` and open a pull request.
5. The recording must be your own and released under **CC BY 4.0** (state this in the PR).

The app only uses the pack when *every* clip in `clips.csv` is present; partial packs are ignored.
`VoicePackManifestTest` keeps `clips.csv` in sync with the code.

## Quick local testing

`make_placeholder_voice.sh` (macOS) generates robotic placeholder clips into `tools/voice/out/`
(git-ignored). Copy them into the assets folder to try the clip player in a debug build, but never
commit them.
