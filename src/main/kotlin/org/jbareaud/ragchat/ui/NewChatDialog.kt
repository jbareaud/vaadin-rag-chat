package org.jbareaud.ragchat.ui

import com.vaadin.flow.component.ClickEvent
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.Text
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.combobox.ComboBox
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.tabs.TabSheet
import com.vaadin.flow.component.textfield.TextField
import org.jbareaud.ragchat.ai.AssistantChatService
import org.jbareaud.ragchat.ai.ChatType
import org.jbareaud.ragchat.ai.RagType
import org.jbareaud.ragchat.logger
import java.io.File


class NewChatDialog(
    private val service: AssistantChatService,
    private val createNewChat: () -> Unit,
    private val cancelNewChat: () -> Unit,
): Dialog() {

    companion object {
        const val TAB_CHAT_INDEX = 0
        const val TAB_RAG_INDEX = 1

        const val TAB_RAG_CREATE_INDEX = 0
        const val TAB_RAG_SELECT_INDEX = 1
    }

    private lateinit var comboChatType: ComboBox<RagType>
    private lateinit var comboRagChat: ComboBox<String>
    private lateinit var comboSimpleChat: ComboBox<String>
    private lateinit var comboRerankers: ComboBox<String>
    private lateinit var comboEmbeddings: ComboBox<String>
    private lateinit var comboKnowledgeBases: ComboBox<String>
    private lateinit var docLocationTextField: TextField
    private lateinit var textCollectionName: Text
    private lateinit var chatTabSheet: TabSheet
    private lateinit var ragTabSheet: TabSheet

    init {
        headerTitle = "New RAG chat"
        width = "40%"
        height = "70%"

        addDialogContent()
        addFooterContent()
    }

    private fun addDialogContent() {
        val dialogLayout = VerticalLayout()
        add(dialogLayout)

        chatTabSheet = TabSheet()
        chatTabSheet.add("Simple Chat", addSimpleChatDialogContent())
        chatTabSheet.add("RAG", addRagDialogContent())
        dialogLayout.add(chatTabSheet)
    }

    private fun addSimpleChatDialogContent(): Component {
        val tabLayout = VerticalLayout()

        comboSimpleChat = ComboBox("Chat models", service.chatModels())
        comboSimpleChat.value = service.defaultChatModel()
        comboSimpleChat.setWidthFull()
        tabLayout.add(comboSimpleChat)

        return tabLayout
    }

    private fun addRagDialogContent(): Component {
        val dialogRagLayout = VerticalLayout()

        comboChatType = ComboBox("Chat Type", service.chatTypes())
        comboChatType.value = RagType.SIMPLE
        comboChatType.setWidthFull()
        dialogRagLayout.add(comboChatType)

        ragTabSheet = TabSheet()
        ragTabSheet.add("Create", tabCreate())
        ragTabSheet.add("Select", tabSelect()).also {
            //isEnabled = service.dataStores().isNotEmpty() // FIXME
        }
        dialogRagLayout.add(ragTabSheet)


        comboRagChat = ComboBox("Chat models", service.chatModels())
        comboRagChat.value = service.defaultChatModel()
        comboRagChat.setWidthFull()
        dialogRagLayout.add(comboRagChat)

        comboEmbeddings = ComboBox("Embedding", service.embeddingModels())
        comboEmbeddings.value = null
        comboEmbeddings.setWidthFull()
        dialogRagLayout.add(comboEmbeddings)

        comboRerankers = ComboBox("Reranker", service.rerankerModels())
        comboRerankers.value = null
        dialogRagLayout.setWidthFull()
        dialogRagLayout.add(comboRerankers)

        return dialogRagLayout
    }

    private fun addFooterContent() {
        val saveButton = Button("OK") { _: ClickEvent<Button> ->
            try {
                val chatId = chatTabSheet.selectedIndex
                val ragId = ragTabSheet.selectedIndex
                when(chatId to ragId) {
                    (TAB_CHAT_INDEX to TAB_RAG_CREATE_INDEX), (TAB_CHAT_INDEX to TAB_RAG_SELECT_INDEX) -> {
                        createSimpleChat(comboSimpleChat.value)
                    }
                    (TAB_RAG_INDEX to TAB_RAG_CREATE_INDEX) -> {
                            createRagChatWithNewKB(
                                comboChatType.value,
                                docLocationTextField.value,
                                comboRagChat.value,
                                comboEmbeddings.value.sanitize(),
                                comboRerankers.value.sanitize(),
                            )
                        }
                    (TAB_RAG_INDEX to TAB_RAG_SELECT_INDEX) -> {
                            createRagChatWithExistingKB(
                                comboChatType.value,
                                comboKnowledgeBases.value,
                                comboRagChat.value,
                                comboEmbeddings.value.sanitize(),
                                comboRerankers.value.sanitize(),
                            )
                        }
                    else -> logger().error("Impossible to create new chat, unknown NewChatDialog case")
                }
                createNewChat()
            } catch (err: Throwable) {
                Notification.show("There was an error during the creation of the new chat")
            }
            close()
        }
        val cancelButton = Button("Cancel") { _: ClickEvent<Button> ->
            cancelNewChat()
            close()
        }
        footer.add(cancelButton)
        footer.add(saveButton)
    }

    private fun tabCreate(): Component {

        val tabLayout = VerticalLayout()
        docLocationTextField = TextField("Knowledge base location", "Copy/paste here")
        docLocationTextField.setSizeFull()
        tabLayout.add(docLocationTextField)

        textCollectionName = Text(textCollectionNameMessage(null))
        tabLayout.add(textCollectionName)

        tabLayout.setSizeFull()

        docLocationTextField.addValueChangeListener { event ->
            textCollectionName.text = textCollectionNameMessage(event.value)
        }

        return tabLayout
    }

    private fun tabSelect(): Component {
        val tabLayout = VerticalLayout()

        val dataStoresList = service.dataStores()

        comboKnowledgeBases = ComboBox("Select existing knowledge base", dataStoresList)
        comboKnowledgeBases.isEnabled = dataStoresList.isNotEmpty()
        comboKnowledgeBases.setSizeFull()
        tabLayout.add(comboKnowledgeBases)

        return tabLayout
    }

    private fun createSimpleChat(chatModelName: String) {
        service.newAssistant(
            chatType = ChatType.SIMPLE,
            chatModelName = chatModelName,
        )
    }

    private fun createRagChatWithNewKB(type: RagType, location: String, chatModelName: String, embeddingModelName: String?, rerankerModelName: String?) {
        val file = File(location)
        if (file.isDirectory) {
            val collectionName = location.split(File.separator).last()
            service.newAssistant(
                chatType = ChatType.RAG,
                ragType = type,
                collectionName = collectionName,
                createKnowledgeBase = true,
                chatModelName = chatModelName,
                embeddingModelName = embeddingModelName,
                rerankerModelName = rerankerModelName,
                docsLocation = location,
            )
        } else {
            Notification.show("Knowledge base couldn't be created, doc location isn't valid")
        }
    }

    private fun createRagChatWithExistingKB(type: RagType, collectionName: String, chatModelName: String, embeddingModelName: String?, rerankerModelName: String?) {
        service.newAssistant(
            chatType = ChatType.RAG,
            ragType = type,
            collectionName = collectionName,
            createKnowledgeBase = false,
            chatModelName = chatModelName,
            embeddingModelName = embeddingModelName,
            rerankerModelName = rerankerModelName,
            docsLocation = null,
        )
    }
}

private fun textCollectionNameMessage(value: String?) =
    "Collection name : ${
        if (value.isNullOrEmpty()) {
            "<none>"
        } else {
            value.split(File.separator).last()
        }
    }"

private fun String?.sanitize(): String? = this?.ifBlank { null }