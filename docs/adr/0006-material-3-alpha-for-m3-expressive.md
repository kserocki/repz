# ADR-0006: Material 3 alpha for M3 Expressive

- **Status:** Accepted (2026-10-28)

## Context
M3 Expressive (MotionScheme, MaterialShapes, shape morphing, LoadingIndicator) is central to the product feel, but in CMP it only ships in the alpha M3 artifact.

## Decision
Use the M3 version shipped with Compose Multiplatform 1.12.x (1.12.0-alpha03). Every M3 usage goes through `core:designsystem`.

## Consequences
API churn is contained in one module. Re-evaluate when M3 Expressive goes stable.
