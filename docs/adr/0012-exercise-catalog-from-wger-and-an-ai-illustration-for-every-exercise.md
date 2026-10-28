# ADR-0012: Exercise catalog from wger + an AI illustration for every exercise

- **Status:** Accepted (2026-10-28)

## Context
Every exercise must have a picture in one consistent style, without licensing issues.

## Decision
Curate ~150 exercises from wger.de (CC-BY-SA, attribution in README and About) into a committed snapshot. Generate a capsule-figure illustration per exercise from the design's prompt template. Custom exercises use one of 12 bundled muscle-group fallbacks.

## Consequences
An image pipeline script plus manual review. Coil disk cache + prefetch makes images available offline.
