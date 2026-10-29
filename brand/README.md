# brand/

Logo and app-icon sources. Spec: [`LOGO.md`](LOGO.md).

Regenerate everything (Python 3, picosvg outlines the stroked wordmark):

```bash
python3 -m venv .venv && .venv/bin/pip install -r requirements.txt
cd logo/src && python3 build.py && for f in symbol wordmark lockup-stacked lockup-horizontal; do ../../.venv/bin/picosvg $f.svg > ../master/$f.svg; done
cd .. && python3 src/variants.py
```

Never edit files in `logo/master`, `logo/color`, `logo/app-icon` or `logo/motion` by hand.

After `logo/app-icon` changes, regenerate what consumes it on each platform:

```bash
# Android: adaptive-icon + splash VectorDrawables (stdlib only, no venv needed)
python3 logo/src/android_vectors.py

# iOS: AppIcon + LaunchSymbol asset catalog (copies PNG/SVG as-is)
../mobile/iosApp/scripts/sync-brand-assets.sh
```

Both scripts are deterministic: re-running them must leave `git status` clean. Never edit the
files they write (`mobile/app/android/src/main/res/drawable/*.xml`,
`mobile/iosApp/iosApp/Assets.xcassets/`) by hand.
