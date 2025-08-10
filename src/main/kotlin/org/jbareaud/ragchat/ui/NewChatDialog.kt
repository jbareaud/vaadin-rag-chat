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
import com.vaadin.flow.server.VaadinServlet
import org.jbareaud.ragchat.ai.AssistantChatService
import org.jbareaud.ragchat.ai.ChatType
import org.jbareaud.ragchat.ai.rag.RagType
import org.springframework.web.context.support.WebApplicationContextUtils
import java.io.File
import java.util.*


class NewChatDialog(
    private val createNewChatCallback: (chatType: ChatType, newChatId: String) -> Unit,
    private val cancelNewChatCallback: () -> Unit,
): Dialog() {

    companion object {
        const val TAB_RAG_CREATE_INDEX = 0
        const val TAB_RAG_SELECT_INDEX = 1
    }

    val service = WebApplicationContextUtils
            .getWebApplicationContext(VaadinServlet.getCurrent().servletContext)!!
            .getBean(AssistantChatService::class.java)

    private lateinit var comboRagType: ComboBox<RagType>
    private lateinit var comboChatModel: ComboBox<String>
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

        comboChatModel = ComboBox("Chat models", service.chatModels())
        comboChatModel.value = service.defaultChatModel()
        comboChatModel.setWidthFull()
        add(comboChatModel)

        chatTabSheet = TabSheet()
        chatTabSheet.add("RAG", addRagDialogContent())
        dialogLayout.add(chatTabSheet)
    }

    private fun addRagDialogContent(): Component {
        val dialogRagLayout = VerticalLayout()


        comboRagType = ComboBox("Chat Type", service.ragTypes())
        comboRagType.value = RagType.SIMPLE
        comboRagType.setWidthFull()
        dialogRagLayout.add(comboRagType)

        ragTabSheet = TabSheet()
        ragTabSheet.add("Create", tabCreate())
        ragTabSheet.add("Select", tabSelect()).also {
            //isEnabled = service.dataStores().isNotEmpty() // FIXME
        }
        dialogRagLayout.add(ragTabSheet)

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
            val newChatId = UUID.randomUUID().toString()
            val chatType = ChatType.RAG
            createChatSession(chatType, newChatId)
            createNewChatCallback(chatType, newChatId)
            close()
        }
        val cancelButton = Button("Cancel") { _: ClickEvent<Button> ->
            cancelNewChatCallback()
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

    private fun createChatSession(chatType: ChatType, newChatId: String) {
        when(chatType) {
            ChatType.SIMPLE -> {
                ChatSessionStore.save(
                    ChatSession(
                        chatId = newChatId,
                        title = "Chat",
                        chatType = ChatType.SIMPLE,
                        chatModel = comboChatModel.value,
                    )
                )
            }
            ChatType.RAG -> {
                createRagChatSession(chatType, newChatId)
            }
        }
    }

    private fun createRagChatSession(chatType: ChatType, newChatId: String) {
        val ragId = ragTabSheet.selectedIndex
        when (ragId) {
            (TAB_RAG_CREATE_INDEX) -> {
                val location = docLocationTextField.value
                val file = File(location)
                if (file.isDirectory) {
                    val collectionName = location.split(File.separator).last()
                    ChatSessionStore.save(
                        ChatSession(
                            chatId = newChatId,
                            title = "Rag : $collectionName",
                            chatType = ChatType.RAG,
                            chatModel = comboChatModel.value,
                            settings = mutableMapOf(
                                "ragType" to comboRagType.value.toString(),
                                "createKnowledgeBase" to "true",
                                "collectionName" to collectionName,
                                "location" to docLocationTextField.value
                            ).apply {
                                comboEmbeddings.value.sanitize()?.let { put("embeddingModelName", it) }
                                comboRerankers.value.sanitize()?.let { put("rerankerModelName", it) }
                            }
                        )
                    )
                } else {
                    Notification.show("Knowledge base cannot be created, doc location isn't valid")
                }
            }
            (TAB_RAG_SELECT_INDEX) -> {
                val collectionName = comboKnowledgeBases.value
                ChatSessionStore.save(
                    ChatSession(
                        chatId = newChatId,
                        title = "Rag : $collectionName",
                        chatType = ChatType.RAG,
                        chatModel = comboChatModel.value,
                        settings = mutableMapOf(
                            "ragType" to comboRagType.value.toString(),
                            "createKnowledgeBase" to "false",
                            "collectionName" to collectionName,
                        ).apply {
                            comboEmbeddings.value.sanitize()?.let { put("embeddingModelName", it) }
                            comboRerankers.value.sanitize()?.let { put("rerankerModelName", it) }
                        }
                    )
                )
            }
        }
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
