package org.jbareaud.ragchat.ui

import com.vaadin.flow.component.messages.MessageInput
import com.vaadin.flow.component.messages.MessageInput.SubmitEvent
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.orderedlayout.Scroller
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.*
import com.vaadin.flow.theme.lumo.LumoUtility
import org.jbareaud.ragchat.ai.AssistantChatService
import org.jbareaud.ragchat.ai.AssistantException
import org.jbareaud.ragchat.ai.provider.Assistant
import org.jbareaud.ragchat.ai.rag.RagType
import org.jbareaud.ragchat.logger
import org.springframework.beans.factory.annotation.Autowired
import org.vaadin.firitin.components.messagelist.MarkdownMessage


@PageTitle("Chat with LangChain4j")
@Route(value = "chat/:chatId", layout = ChatMainLayout::class)
class ChatView: VerticalLayout(), BeforeEnterObserver {

    private var chatId: String? = null
    private var assistant: Assistant? = null
    private val messageInput: MessageInput
    private val messageList: VerticalLayout
    @Autowired lateinit var service: AssistantChatService

    init {
        messageList = VerticalLayout()
        messageInput = MessageInput()
        focusMessageInput()

        isPadding = false
        isSpacing = false
        messageList.isSpacing = true
        messageList.addClassNames(
            LumoUtility.Padding.Horizontal.SMALL,
            LumoUtility.Margin.Horizontal.AUTO,
            LumoUtility.MaxWidth.SCREEN_MEDIUM
        )

        messageInput.setWidthFull()
        messageInput.addClassNames(
            LumoUtility.Padding.Horizontal.LARGE,
            LumoUtility.Padding.Vertical.MEDIUM,
            LumoUtility.Margin.Horizontal.AUTO,
            LumoUtility.MaxWidth.SCREEN_MEDIUM
        )
        messageInput.addSubmitListener { e: SubmitEvent ->
            val questionText = e.value
            val question = MarkdownMessage(questionText, "You")
            question.addClassName("you")

            val answer = MarkdownMessage("Assistant")
            answer.element.executeJs("this.scrollIntoView()")
            messageList.add(question)
            messageList.add(answer)
            answer.appendChatResponseAsync(questionText)
        }

        val scroller = Scroller(messageList)
        scroller.setWidthFull()
        scroller.addClassName(LumoUtility.AlignContent.END)
        addAndExpand(scroller)
        add(messageInput)
    }

    private fun MarkdownMessage.appendChatResponseAsync(questionText: String) {
        val workerUI = ui.get()
        if (assistant != null) {
            assistant!!.chat(requireNotNull(chatId), questionText)
                .onPartialResponse { partial ->
                    workerUI.access {
                        appendMarkdownAsync(partial)
                        element.executeJs("this.scrollIntoView()")
                    }
                }
                .onCompleteResponse { _ ->
                    logger().info("Response complete")
                }
                .onError { err ->
                    logger().error("Error during streaming of message : $err")
                    workerUI.access {
                        Notification.show("Error during message processing.")
                    }
                }
                .start()

        }
    }

    private fun focusMessageInput() {
        messageInput.element.executeJs("requestAnimationFrame(() => this.querySelector('vaadin-text-area').focus() )")
    }

    override fun beforeEnter(event: BeforeEnterEvent) {
        if (assistant == null) {
            chatId = event.routeParameters.get("chatId").orElseThrow { AssistantException("Invalid chat Id") }
            logger().info("Nouvelle instance de RagChatView pour chatId = $chatId")
            assistant = ChatSessionStore.get(requireNotNull(chatId))?.let { session ->
                service.newAssistant(
                    chatType = session.chatType,
                    ragType = session.settings["ragType"]?.let { RagType.valueOf(it) },
                    chatModelName = session.chatModel,
                    collectionName = session.settings["collectionName"].toString(),
                    createKnowledgeBase = session.settings["createKnowledgeBase"]?.let { it == "true" },
                    embeddingModelName = session.settings["embeddingModelName"],
                    rerankerModelName = session.settings["rerankerModelName"],
                    docsLocation = session.settings["location"],
                )
            }
        }
    }
}
