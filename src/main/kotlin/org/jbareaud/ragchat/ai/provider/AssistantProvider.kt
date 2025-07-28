package org.jbareaud.ragchat.ai.provider


interface AssistantProvider<P, A> {

    fun instantiateAssistant(parameters: P): A
}