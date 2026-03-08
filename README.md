# SpanishOverlay

Rootless Android accessibility overlay that replaces English words with Spanish translations
on any screen. Passive ambient vocab acquisition while using your phone normally.

## How It Works

1. An AccessibilityService reads the UI tree of whichever app is on screen
2. A filter pipeline selects eligible English words from visible text nodes
3. Semi-transparent Spanish translation bubbles appear over the original text
4. Bubbles auto-dismiss after a configurable TTL

## Setup

1. Build: `./gradlew assembleDebug`
2. Install the APK
3. Open SpanishOverlay app -> tap "Open Accessibility Settings"
4. Enable "Spanish Overlay Service"
5. (Recommended) Tap the battery warning to disable battery optimization

## Settings

- **Replacement Frequency**: Every Nth word (fraction mode) or fixed count per screen
- **Word Filters**: Min/max length, PoS (noun/verb/adj/adv), complexity level 0-3
- **Appearance**: Overlay opacity, display duration
- **Stop Words**: Add/remove common words to skip
- **Excluded Apps**: Per-app opt-out
- **Advanced**: Event debounce timing, reset to defaults

## Architecture

```
AccessibilityEvent -> EventDebouncer (300ms)
  -> NodeWalker (BFS, leaf nodes only -> NodeSnapshot)
  -> WordFilterPipeline (8 stages, config snapshot per call)
  -> OverlayManager (WindowManager TYPE_ACCESSIBILITY_OVERLAY, view pool)
```

Key design decisions:
- NodeSnapshot: immutable capture before node.recycle() — no raw node refs held
- runCatching on every WindowManager call — WM exceptions kill the a11y service
- handler.removeCallbacksAndMessages(null) BEFORE removeView in clearAll()
- ComposeView fixed 240dp height in XML — prevents infinite measure with LazyColumn
- Config commits on finger-up only — no 60fps SharedPreferences writes during slider drag

## Requirements

- Android 5.1+ (API 22) — TYPE_ACCESSIBILITY_OVERLAY minimum
- No root required
- ~100 dictionary entries (MVP), expandable to 500-800

## Known Limitations

- Banking apps / FLAG_SECURE: tree is empty, no overlays possible
- Screen lock: overlays not shown on keyguard
- adb not available in build container (protobuf dep conflict)
