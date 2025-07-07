package org.jbareaud.ragchat

import com.vaadin.flow.component.page.AppShellConfigurator
import com.vaadin.flow.component.page.Push
import com.vaadin.flow.theme.Theme
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@Push
@Theme(value = "rag-chat")
@SpringBootApplication
class RagChatApplication: AppShellConfigurator

fun main(args: Array<String>) {
	runApplication<RagChatApplication>(*args)
}
