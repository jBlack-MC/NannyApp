# Theme and contrast handling

Nanny-App uses one theme contract across the web UI and Android app:

- Light mode uses dark ink on light backgrounds.
- Dark mode uses light text on dark surfaces, with separate `on*` colours for
  buttons, containers, menus, forms and status messages.
- Controls must use semantic theme tokens rather than hard-coded black/white
  text, except for text intentionally placed on a known dark gradient or image.
- The web theme is selected before first paint from `localStorage.na_theme`,
  then falls back to the operating system preference. The toggle persists the
  choice across page navigation.
- Android supports `System`, `Light` and `Dark` in Settings. The choice is
  stored locally and applied through the Material 3 `ColorScheme`.

## Manual test pass

Check both light and dark modes on a small phone, desktop browser and a narrow
browser width. Visit the welcome, login/register, dashboard, discovery,
booking, messaging, profile, settings and admin/support screens. Confirm:

1. Headings, body copy, muted labels and links remain readable.
2. Primary/secondary/danger buttons have readable labels and disabled states.
3. Inputs, placeholders, selects, radio buttons, menus and dialogs keep a
   visible surface, border, caret and focus ring.
4. Tables, cards, notifications, chat bubbles, badges and error/success
   messages preserve readable foreground/background pairs.
5. Switching themes does not require a page reload and remains selected after
   navigation or app recreation.

The static check can be run from the repository root:

```text
python tests/check-theme-contrast.py
```

This verifies the required semantic tokens and major component selectors. It
does not replace a browser screenshot/accessibility pass or Android TalkBack
testing; those remain release evidence requirements.
