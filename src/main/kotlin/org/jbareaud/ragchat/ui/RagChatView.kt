package org.jbareaud.ragchat.ui

import com.vaadin.flow.component.ClickEvent
import com.vaadin.flow.component.Text
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
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
import org.jbareaud.ragchat.ai.ChatService
import org.jbareaud.ragchat.logger
import org.springframework.beans.factory.annotation.Autowired
import org.vaadin.firitin.components.messagelist.MarkdownMessage
import reactor.core.scheduler.Schedulers
import java.io.File
import java.util.*
import java.util.concurrent.Executors


@PageTitle("Chat with LangChain4j")
@Route(value = "", layout = ChatMainLayout::class)
class RagChatView: VerticalLayout() {

    private var chatId: String = "" // use empty string as a marker for uninitialized rag chat
    private val messageInput: MessageInput
    private val messageList: VerticalLayout
    @Autowired lateinit var service: ChatService

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

            if (chatId != "") {
                val answer = MarkdownMessage("Assistant")
                answer.element.executeJs("this.scrollIntoView()")
                messageList.add(question)
                messageList.add(answer)
                newMessage(questionText, answer)
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

    private fun newMessage(questionText: String, answer: MarkdownMessage) {
        val workerUI = ui.get()
        service.streamNewMessage(chatId, questionText)
            .doOnError {
                workerUI.access {
                    Notification.show("Error during message processing.")
                }
            }
            .subscribe { message ->
                workerUI.access {
                    answer.appendMarkdownAsync(message)
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

        dialogLayout.setSizeFull()
        dialog.add(dialogLayout)

        val saveButton = Button("OK") { _: ClickEvent<Button> ->
            val location = field.value
            val file = File(location)
            if (file.isDirectory) {
                chatId = UUID.randomUUID().toString()
                messageList.removeAll()
                focusMessageInput()
                service.newChat(location)
                Notification.show("Knowledge base processed, ready to chat")
            } else {
                val message = "Location of knowledge base is invalid"
                logger().warn("$message: $location")
                Notification.show(message)
            }
            dialog.close()
        }
        val cancelButton = Button("Cancel") { _: ClickEvent<Button> ->
            if (chatId == "")
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