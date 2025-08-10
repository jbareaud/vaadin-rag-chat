package org.jbareaud.ragchat.ui

import com.vaadin.flow.component.Text
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route

@PageTitle("Landing page for Chat with LangChain4j")
@Route(value = "", layout = ChatMainLayout::class)
class DefaultView: VerticalLayout()  {

    init {
        add(Text("Click New Chat button on the side to initiate a conversation."))
    }
}