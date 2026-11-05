package com.repz.app.core.common.id

interface IdGenerator

// core:domain/core:common are Koin-free (AGENTS §3 rule 1): this class can never carry a Koin
// annotation itself, so a provider function is the only way to bind it (ADR-0008 reason 4).
class UuidV7IdGenerator : IdGenerator
