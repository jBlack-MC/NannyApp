"""Static guardrails for the shared web/mobile theme contract."""
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
web = (ROOT / "NannyApp.Web/assets/css/accessibility.css").read_text(encoding="utf-8")
mobile = (ROOT / "NannyApp.Mobile/NannyApp/app/src/main/java/com/nannyapp/ui/theme/Theme.kt").read_text(encoding="utf-8")
activity = (ROOT / "NannyApp.Mobile/NannyApp/app/src/main/java/com/nannyapp/MainActivity.kt").read_text(encoding="utf-8")

required_web = [
    '[data-theme="dark"]',
    "--ink: #f1f5f9",
    "--surface: #1e293b",
    ".field input",
    ".nav-dropdown-menu",
    ".dash-nav",
    ".filter-row input",
    ":focus-visible",
]
required_mobile = [
    "private val LightColors = lightColorScheme(",
    "private val DarkColors = darkColorScheme(",
    "onBackground =",
    "onSurface =",
    "onSurfaceVariant =",
]

missing = [f"web: {item}" for item in required_web if item not in web]
missing += [f"mobile: {item}" for item in required_mobile if item not in mobile]
if "ThemeMode.SYSTEM" not in activity or "ThemeMode.LIGHT" not in activity or "ThemeMode.DARK" not in activity:
    missing.append("mobile: System/Light/Dark theme selection")

# Guard against accidentally reintroducing literal black as the dark-mode body
# colour in the semantic CSS block.
dark_block = re.search(r'\[data-theme="dark"\]\s*\{(.*?)\n\}', web, re.S)
if not dark_block or "--ink: #000" in dark_block.group(1).lower():
    missing.append("web: dark mode must not define black semantic ink")

def luminance(hex_colour: str) -> float:
    channels = [int(hex_colour[i:i + 2], 16) / 255 for i in (1, 3, 5)]
    linear = [channel / 12.92 if channel <= 0.03928 else ((channel + 0.055) / 1.055) ** 2.4 for channel in channels]
    return 0.2126 * linear[0] + 0.7152 * linear[1] + 0.0722 * linear[2]

def contrast(foreground: str, background: str) -> float:
    light, dark = sorted((luminance(foreground), luminance(background)), reverse=True)
    return (light + 0.05) / (dark + 0.05)

for label, foreground, background in [
    ("web light body", "#0d1f33", "#f3f8fd"),
    ("web dark body", "#f1f5f9", "#0f172a"),
    ("mobile light body", "#0d1f33", "#f3f8fd"),
    ("mobile dark body", "#f1f5f9", "#0f172a"),
]:
    if contrast(foreground, background) < 4.5:
        missing.append(f"{label}: body contrast below WCAG AA")

if missing:
    raise SystemExit("Theme guard failed:\n- " + "\n- ".join(missing))

print("Theme guard passed: web semantic tokens/components and Android light/dark schemes are present.")
