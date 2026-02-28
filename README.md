# StarSetTracker

ADHD-friendly offline-first Android fitness tracker for one user.

## How to run
1. Open project in Android Studio Hedgehog+.
2. Let Gradle sync.
3. Run `app` on an Android 8+ device/emulator.

## 3-day rotation logic
- Rotation repeats forever: **A -> B -> C -> A...**
- Day advances only when a session is finished **and** stars >= 10 (quest clear).
- If session ends below 10 stars, next session repeats same day.
- The selected day determines the 5 machines for that workout.

## Scoring
- Every logged set = **1 star**.
- Quest clear at 10 stars.
- Bonus quest at 15 stars.
- Base points per set: `round(multiplier * weight)`.
- Bonus mode: stars 11..15 get `round(base * 1.5)`.
- After star 15, points return to base.

## Edit machines and weights
- **Machines tab**: rename, multiplier, default weight, active toggle, reorder with arrows.
- **Today tab**: tap Log Set for quick entry with default weight.
- Long press Log Set (or edit icon) to adjust weight quickly.

## Export CSV
- Use **Stats > Export CSV**.
- App creates CSV in cache and opens Android share sheet via `FileProvider`.
- No dangerous permissions required.

## Session detection
- First logged set creates session.
- Session remains active until Finish Workout.
- If idle for >3 hours since last log, session auto-times-out and next log starts a new session.

## GitHub setup
```bash
git init
git add .
git commit -m "Initial StarSetTracker"
```

If GitHub CLI is available:
```bash
gh repo create StarSetTracker --public --source=. --remote=origin --push
```

Else manual remote:
```bash
git remote add origin <repo_url>
git branch -M main
git push -u origin main
```
