package org.jbareaud.ragchat

import com.vaadin.flow.server.VaadinServlet
import org.jbareaud.ragchat.ai.ChatService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.context.support.WebApplicationContextUtils

fun Any.logger(): Logger = LoggerFactory.getLogger(this::class.java)

// Substitute for autowired spring services in vaadin components
fun Any.chatService() = WebApplicationContextUtils
    .getWebApplicationContext(VaadinServlet.getCurrent().servletContext)!!
    .getBean(ChatService::class.java)
