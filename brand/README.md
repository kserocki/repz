# brand/

Logo and app-icon sources. Spec: [`LOGO.md`](LOGO.md).

Regenerate everything (Python 3, picosvg outlines the stroked wordmark):

```bash
python3 -m venv .venv && .venv/bin/pip install -r requirements.txt
cd logo/src && python3 build.py && for f in symbol wordmark lockup-stacked lockup-horizontal; do ../../.venv/bin/picosvg $f.svg > ../master/$f.svg; done
cd .. && python3 src/variants.py
```

Never edit files in `logo/master`, `logo/color`, `logo/app-icon` or `logo/motion` by hand.
