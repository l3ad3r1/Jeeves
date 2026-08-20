package com.hermes.agent.data.llm

/**
 * Compatibility name for code that has not yet moved to the canonical public
 * LLM contract. Both names compile to the same type, so dependency injection
 * and provider implementations share one truthful abstraction.
 */
typealias LlmProvider = com.hermes.agent.domain.llm.LlmProvider
