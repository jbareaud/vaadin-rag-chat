package org.jbareaud.ragchat.ai

import org.jbareaud.ragchat.ai.provider.AssistantProvider
import org.jbareaud.ragchat.ai.provider.RagAssistant
import org.jbareaud.ragchat.logger
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks


@Service
class ChatService(private val provider: AssistantProvider) {

    private lateinit var assistant: RagAssistant

    fun newChat(docsLocation: String) {
        logger().info("Initializing new assistant")
        assistant = provider.newAssistant(docsLocation)
        logger().info("Finished initializing new assistant")
    }

    fun streamNewMessage(chatId: String, userMessage: String): Flux<String> {
        val sink = Sinks.many().unicast().onBackpressureBuffer<String>()
        assistant.chat(chatId, userMessage)
            .onPartialResponse { partial ->
                sink.tryEmitNext(partial)
            }
            .onCompleteResponse { _ ->
                sink.tryEmitComplete()
            }
            .onError { err ->
                logger().error("$err")
                sink.tryEmitError(err)
            }
            .start()
        return sink.asFlux()
    }
}

