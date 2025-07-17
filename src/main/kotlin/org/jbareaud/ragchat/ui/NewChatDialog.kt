package org.jbareaud.ragchat.ui

import com.vaadin.flow.component.ClickEvent
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.Text
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.combobox.ComboBox
import com.vaadin.flow.component.dialog.Dialog
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
    private val createChatWithNewKB: (type:AssistantType, docLocation: String, chatModelName: String, embeddingModelName: String, useReranker: Boolean) -> Unit,
    private val createChatWithExistingKB: (type:AssistantType, collectionName: String, chatModelName: String, embeddingModelName: String, useReranker: Boolean) -> Unit,
    private val cancelNewChat: () -> Unit,
): Dialog() {

    private lateinit var tabSheet: TabSheet
    private lateinit var tabCreate: Tab
    private lateinit var tabSelect: Tab
    private lateinit var comboChatType: ComboBox<AssistantType>
    private lateinit var comboChats: ComboBox<String>
    private lateinit var checkReranker: Checkbox
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

        comboChatType = ComboBox("Chat Type", service.types())
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

        comboEmbeddings = ComboBox("Embedding", service.embeddings())
        comboEmbeddings.value = null
        comboEmbeddings.setWidthFull()
        dialogLayout.add(comboEmbeddings)


        checkReranker = Checkbox("Use reranker if possible", false)
        if (!service.hasReranker()) {
            checkReranker.isEnabled = false
        }
        dialogLayout.add(checkReranker)
    }

    private fun addFooterContent() {
        val saveButton = Button("OK") { _: ClickEvent<Button> ->
            when (tabSheet.selectedTab) {
                tabCreate -> {
                    createChatWithNewKB(
                        comboChatType.value,
                        docLocationTextField.value,
                        comboChats.value,
                        comboEmbeddings.value,
                        checkReranker.value,
                    )
                }
                tabSelect -> {
                    createChatWithExistingKB(
                        comboChatType.value,
                        comboKnowledgeBases.value,
                        comboChats.value,
                        comboEmbeddings.value,
                        checkReranker.value,
                    )
                }
                else -> {
                    logger().error("Impossible to create new chat, unknown NewChatDialog case")
                }
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
}

private fun textCollectionNameMessage(value: String?) =
    "Collection name : ${
        if (value.isNullOrEmpty()) {
            "<none>"
        } else {
            value.split(File.separator).last()
        }
    }"
