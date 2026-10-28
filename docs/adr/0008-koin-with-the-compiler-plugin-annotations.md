# ADR-0008: Koin with the compiler plugin (annotations)

- **Status:** Accepted (2026-10-28)

## Context
We want DI errors at compile time instead of at runtime.

## Decision
Annotations (`@KoinViewModel`, `@Factory`, `@Single`, `@Module @ComponentScan`) on concrete classes. One `@KoinApplication` entry in `app:shared`. Third-party objects are provided in explicit `@Module` classes.

## Consequences
A broken graph doesn't compile. The plugin is young: Kotlin 2.4.21 is not on its verified list (expect a warning). Checked in the spike.
