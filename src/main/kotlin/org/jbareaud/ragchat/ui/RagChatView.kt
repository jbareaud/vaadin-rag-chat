package org.jbareaud.ragchat.ui

import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.messages.MessageInput
import com.vaadin.flow.component.messages.MessageInput.SubmitEvent
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.orderedlayout.Scroller
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.theme.lumo.LumoUtility
import jakarta.annotation.PostConstruct
import org.jbareaud.ragchat.ai.AssistantChatService
import org.jbareaud.ragchat.ai.AssistantType
import org.springframework.beans.factory.annotation.Autowired
import org.vaadin.firitin.components.messagelist.MarkdownMessage
import java.io.File
import java.util.UUID


@PageTitle("Chat with LangChain4j")
@Route(value = "", layout = ChatMainLayout::class)
class RagChatView: VerticalLayout() {

    private var chatId: String? = null
    private val messageInput: MessageInput
    private val messageList: VerticalLayout
    @Autowired lateinit var service: AssistantChatService

    init {
        val newChatButton = Button("New Chat")
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

        newChatButton.addClassName("new-chat-button")
        newChatButton.addClickListener { _ -> openNewChatDialog() }

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

            if (chatId != null) {
                val answer = MarkdownMessage("Assistant")
                answer.element.executeJs("this.scrollIntoView()")
                messageList.add(question)
                messageList.add(answer)
                answer.appendChatResponseAsync(questionText)
            } else {
                Notification.show("Can't chat, knowledge base hasn't been properly initialized. Click the 'New Chat' button and pick a valid location.")
            }
        }

        add(newChatButton)

        val scroller = Scroller(messageList)
        scroller.setWidthFull()
        scroller.addClassName(LumoUtility.AlignContent.END)
        addAndExpand(scroller)
        add(messageInput)
    }

    private fun MarkdownMessage.appendChatResponseAsync(questionText: String) {
        val workerUI = ui.get()
        service.streamNewMessage(requireNotNull(chatId), questionText)
            .doOnError {
                workerUI.access {
                    Notification.show("Error during message processing.")
                }
            }
            .subscribe { message ->
                workerUI.access {
                    appendMarkdownAsync(message)
                    element.executeJs("this.scrollIntoView()")
                }
            }
    }

    private fun focusMessageInput() {
        messageInput.element.executeJs("requestAnimationFrame(() => this.querySelector('vaadin-text-area').focus() )")
    }

    private fun openNewChatDialog() {
        val dialog = NewChatDialog(
            service = service,
            createChatWithNewKB = createChatWithNewKB(),
            createChatWithExistingKB = createChatWithExistingKB(),
            cancelNewChat = {
                if (chatId == null)
                    Notification.show("No knowledge base set, unable to chat")
                else
                    Notification.show("Canceled, keeping the previous knowledge base")
            })
        dialog.open()
    }

    private fun createChatWithNewKB(): (type: AssistantType, docLocation: String, chatModelName: String, embeddingModelName: String, useReranker: Boolean) -> Unit =
        { type, location, chatModelName, embeddingModelName, useReranker ->
            val file = File(location)
            if (file.isDirectory) {
                val collectionName = location.split(File.separator).last()
                service.newAssistant(
                    type = type,
                    collectionName = collectionName,
                    createKnowledgeBase = true,
                    chatModelName = chatModelName,
                    embeddingModelName = embeddingModelName,
                    useReranker = useReranker,
                    docsLocation = location
                )
                chatId = UUID.randomUUID().toString()
                messageList.removeAll()
                focusMessageInput()
                Notification.show("Knowledge base processed, ready to chat")
            } else {
                Notification.show("Knowledge base couldn't be created, doc location isn't valid")
            }
        }

    private fun createChatWithExistingKB(): (type: AssistantType, collectionName: String, chatModelName: String, embeddingModelName: String, useReranker: Boolean) -> Unit =
        { type, collectionName, chatModelName, embeddingModelName, useReranker ->
            service.newAssistant(
                type = type,
                collectionName = collectionName,
                createKnowledgeBase = false,
                chatModelName = chatModelName,
                embeddingModelName = embeddingModelName,
                useReranker = useReranker,
                docsLocation = null
            )
            chatId = UUID.randomUUID().toString()
            messageList.removeAll()
            focusMessageInput()
            Notification.show("Using existing knowledge base processed, ready to chat")
        }

    @PostConstruct
    fun initDefaultChat() {
        openNewChatDialog()
    }
}