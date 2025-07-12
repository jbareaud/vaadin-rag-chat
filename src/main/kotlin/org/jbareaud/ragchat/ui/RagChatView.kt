package org.jbareaud.ragchat.ui

import com.vaadin.flow.component.ClickEvent
import com.vaadin.flow.component.Text
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.combobox.ComboBox
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.messages.MessageInput
import com.vaadin.flow.component.messages.MessageInput.SubmitEvent
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.orderedlayout.Scroller
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.theme.lumo.LumoUtility
import jakarta.annotation.PostConstruct
import org.jbareaud.ragchat.ai.AssistantChatService
import org.jbareaud.ragchat.ai.AssistantType
import org.jbareaud.ragchat.logger
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
        val dialog = Dialog()

        dialog.headerTitle = "New RAG chat"

        val dialogLayout = VerticalLayout()
        val field = TextField("Knowledge base location", "Copy/paste here")
        field.setWidthFull()
        dialogLayout.add(field)

        val text = Text("Warning: import may take some time depending of the size of the knowledge base")
        dialogLayout.add(text)

        val comboChatType = ComboBox("Chat Type", service.available())
        comboChatType.value = AssistantType.SIMPLE
        comboChatType.setWidthFull()
        dialogLayout.add(comboChatType)

        val comboChats = ComboBox("Chat models", service.chatModels())
        comboChats.value = service.defaultChatModel()
        comboChats.setWidthFull()
        dialogLayout.add(comboChats)

        val comboEmbeddings = ComboBox("Embedding", service.embeddings())
        comboEmbeddings.value = null
        comboEmbeddings.setWidthFull()
        dialogLayout.add(comboEmbeddings)

        val checkReranker = Checkbox("Use reranker if possible", false)
        if (!service.hasReranker()) {
            checkReranker.isEnabled = false
        }
        dialogLayout.add(checkReranker)

        dialogLayout.setSizeFull()
        dialog.add(dialogLayout)

        val saveButton = Button("OK") { _: ClickEvent<Button> ->
            val location = field.value
            val file = File(location)
            if (file.isDirectory) {
                try {
                    service.newAssistant(
                        type = comboChatType.value,
                        chatModelName = comboChats.value,
                        embeddingModelName = comboEmbeddings.value,
                        useReranker = checkReranker.value,
                        docsLocation = location
                    )
                    chatId = UUID.randomUUID().toString()
                    messageList.removeAll()
                    focusMessageInput()
                    Notification.show("Knowledge base processed, ready to chat")
                } catch (err: Exception) {
                    Notification.show("Could not create new chat")
                }
            } else {
                val message = "Location of knowledge base is invalid"
                logger().warn("$message: $location")
                Notification.show(message)
            }
            dialog.close()
        }
        val cancelButton = Button("Cancel") { _: ClickEvent<Button> ->
            if (chatId == null)
                Notification.show("No knowledge base set, unable to chat")
            else
                Notification.show("Canceled, keeping the previous knowledge base")
            dialog.close()
        }
        dialog.footer.add(cancelButton)
        dialog.footer.add(saveButton)

        add(dialog)
        dialog.open()
    }

    @PostConstruct
    fun initDefaultChat() {
        openNewChatDialog()
    }
}