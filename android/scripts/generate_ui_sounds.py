"""Synthesizes Scoop's short UI feedback sounds into app/src/main/res/raw.

The sounds are generated from plain sine tones here, so they carry no third-party licence and
can be regenerated or tweaked at any time:  python -I android/scripts/generate_ui_sounds.py
"""
import math
import struct
import wave
from pathlib import Path

RATE = 22050
OUT = Path(__file__).resolve().parents[1] / "app/src/main/res/raw"


def tone(freq, start, length, peak, decay, harmonics=(1.0, 0.25, 0.08)):
    """A soft bell-like note: quick attack, exponential decay, a few quiet overtones."""
    def sample(t):
        local = t - start
        if local < 0 or local > length:
            return 0.0
        envelope = min(1.0, local / 0.004) * math.exp(-local / decay)
        return peak * envelope * sum(
            weight * math.sin(2 * math.pi * freq * (index + 1) * local) for index, weight in enumerate(harmonics)
        ) / sum(harmonics)
    return sample


def sweep(start_freq, end_freq, length, peak, decay):
    """A short pitch glide (the 'swish'/'whoop' family), phase-continuous to avoid clicks."""
    def sample(t):
        if t > length:
            return 0.0
        # Exponential glide; integrate frequency to get phase.
        ratio = end_freq / start_freq
        k = math.log(ratio) / length
        phase = 2 * math.pi * start_freq * (math.exp(k * t) - 1) / k
        envelope = min(1.0, t / 0.003) * math.exp(-t / decay)
        return peak * envelope * (math.sin(phase) + 0.2 * math.sin(2 * phase)) / 1.2
    return sample


def render(name, duration, *voices):
    frames = bytearray()
    total = int(RATE * duration)
    fade = int(RATE * 0.02)
    for n in range(total):
        t = n / RATE
        value = sum(voice(t) for voice in voices)
        if n > total - fade:  # tail fade so no sound ends on a click
            value *= (total - n) / fade
        frames += struct.pack("<h", int(max(-1.0, min(1.0, value)) * 32767))
    with wave.open(str(OUT / f"{name}.wav"), "wb") as out:
        out.setnchannels(1)
        out.setsampwidth(2)
        out.setframerate(RATE)
        out.writeframes(bytes(frames))


if __name__ == "__main__":
    OUT.mkdir(parents=True, exist_ok=True)
    # Download complete: a bright rising "ding-ding" (C6 then G6).
    render("sound_download_complete", 0.55,
           tone(1046.5, 0.00, 0.30, 0.42, 0.10),
           tone(1568.0, 0.11, 0.42, 0.42, 0.14))
    # Download failed: a gentle falling "bonk" (C5 then G4), never a harsh buzzer.
    render("sound_download_failed", 0.45,
           tone(523.3, 0.00, 0.22, 0.38, 0.08, harmonics=(1.0, 0.15)),
           tone(392.0, 0.12, 0.30, 0.38, 0.11, harmonics=(1.0, 0.15)))
    # Delete: a quick downward "swish/pop".
    render("sound_delete", 0.18, sweep(1100, 320, 0.16, 0.40, 0.06))
    # Undo: the same glide reversed, a "whoop" as the item comes back.
    render("sound_undo", 0.18, sweep(320, 1100, 0.16, 0.40, 0.07))
    print("Wrote UI sounds to", OUT)
