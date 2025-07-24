package org.jbareaud.ragchat.ui

import com.vaadin.flow.component.ClickEvent
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.Text
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.combobox.ComboBox
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.tabs.Tab
import com.vaadin.flow.component.tabs.TabSheet
import com.vaadin.flow.component.textfield.TextField
import org.jbareaud.ragchat.ai.AssistantChatService
import org.jbareaud.ragchat.ai.AssistantType
import org.jbareaud.ragchat.logger
import java.io.File


class NewChatDialog(
    private val service: AssistantChatService,
    private val createNewChat: () -> Unit,
    private val cancelNewChat: () -> Unit,
): Dialog() {

    private lateinit var tabSheet: TabSheet
    private lateinit var tabCreate: Tab
    private lateinit var tabSelect: Tab
    private lateinit var comboChatType: ComboBox<AssistantType>
    private lateinit var comboChats: ComboBox<String>
    //private lateinit var checkReranker: Checkbox
    private lateinit var comboRerankers: ComboBox<String>
    private lateinit var comboEmbeddings: ComboBox<String>
    private lateinit var comboKnowledgeBases: ComboBox<String>
    private lateinit var docLocationTextField: TextField
    private lateinit var textCollectionName: Text

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

        comboChatType = ComboBox("Chat Type", service.chatTypes())
        comboChatType.value = AssistantType.SIMPLE
        comboChatType.setWidthFull()
        dialogLayout.add(comboChatType)


        tabSheet = TabSheet()
        tabCreate = tabSheet.add("Create", tabCreate())
        tabSelect = tabSheet.add("Select", tabSelect())
        tabSelect.isEnabled = service.dataStores().isNotEmpty()
        dialogLayout.add(tabSheet)


        comboChats = ComboBox("Chat models", service.chatModels())
        comboChats.value = service.defaultChatModel()
        comboChats.setWidthFull()
        dialogLayout.add(comboChats)

        comboEmbeddings = ComboBox("Embedding", service.embeddingModels())
        comboEmbeddings.value = null
        comboEmbeddings.setWidthFull()
        dialogLayout.add(comboEmbeddings)

        comboRerankers = ComboBox("Reranker", service.rerankerModels())
        comboRerankers.value = null
        comboRerankers.setWidthFull()
        dialogLayout.add(comboRerankers)
    }

    private fun addFooterContent() {
        val saveButton = Button("OK") { _: ClickEvent<Button> ->
            try {
                when (tabSheet.selectedTab) {
                    tabCreate -> {
                        createChatWithNewKB(
                            comboChatType.value,
                            docLocationTextField.value,
                            comboChats.value,
                            comboEmbeddings.value.sanitize(),
                            comboRerankers.value.sanitize(),
                        )
                        createNewChat()
                    }
                    tabSelect -> {
                        createChatWithExistingKB(
                            comboChatType.value,
                            comboKnowledgeBases.value,
                            comboChats.value,
                            comboEmbeddings.value.sanitize(),
                            comboRerankers.value.sanitize(),
                        )
                        createNewChat()
                    }
                    else -> {
                        logger().error("Impossible to create new chat, unknown NewChatDialog case")
                    }
                }
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

    private fun createChatWithNewKB(type: AssistantType, location: String, chatModelName: String, embeddingModelName: String?, rerankerModelName: String?) {
        val file = File(location)
        if (file.isDirectory) {
            val collectionName = location.split(File.separator).last()
            service.newAssistant(
                type = type,
                collectionName = collectionName,
                createKnowledgeBase = true,
                chatModelName = chatModelName,
                embeddingModelName = embeddingModelName,
                rerankerModelName = rerankerModelName,
                docsLocation = location
            )
        } else {
            Notification.show("Knowledge base couldn't be created, doc location isn't valid")
        }
    }

    private fun createChatWithExistingKB(type: AssistantType, collectionName: String, chatModelName: String, embeddingModelName: String?, rerankerModelName: String?) {
        service.newAssistant(
            type = type,
            collectionName = collectionName,
            createKnowledgeBase = false,
            chatModelName = chatModelName,
            embeddingModelName = embeddingModelName,
            rerankerModelName = rerankerModelName,
            docsLocation = null
        )
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