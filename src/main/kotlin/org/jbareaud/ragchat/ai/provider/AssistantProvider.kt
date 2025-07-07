package org.jbareaud.ragchat.ai.provider

interface AssistantProvider {

    fun newAssistant(docsLocation: String): RagAssistant
}