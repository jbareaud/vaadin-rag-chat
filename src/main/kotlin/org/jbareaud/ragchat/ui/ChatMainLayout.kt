package org.jbareaud.ragchat.ui

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.applayout.AppLayout
import com.vaadin.flow.component.applayout.DrawerToggle
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.Footer
import com.vaadin.flow.component.html.H1
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.html.Header
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.orderedlayout.Scroller
import com.vaadin.flow.component.sidenav.SideNav
import com.vaadin.flow.component.sidenav.SideNavItem
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.theme.lumo.LumoUtility


class ChatMainLayout: AppLayout() {

    private lateinit var viewTitle:H2
    private lateinit var nav: SideNav
    private val chatTabs = mutableMapOf<String, SideNavItem>()

    init {
        primarySection = Section.DRAWER;
        addDrawerContent();
        addHeaderContent();
    }

    private fun addDrawerContent() {
        val appName = H1("RAGChat")
        appName.addClassNames(
            LumoUtility.FontSize.LARGE,
            LumoUtility.Margin.Vertical.MEDIUM,
            LumoUtility.Margin.Horizontal.MEDIUM
        )
        val header = Header(appName)
        val button = createNewChatButton()
        val scroller = Scroller(createNavigation())
        addToDrawer(header, button, scroller, createFooter())
    }

    private fun createNewChatButton(): Component {
        val button =  Button("New Chat")
        button.addClickListener {
            val dialog = NewChatDialog(
                createNewChatCallback = { chatType, newChatId ->
                    if (!chatTabs.containsKey(newChatId)) {
                        val item = SideNavItem(ChatSessionStore.get(newChatId)?.title, "chat/$newChatId")
                        nav.addItem(item)
                        chatTabs[newChatId] = item
                    }
                    UI.getCurrent().navigate("chat/$newChatId")
                },
                cancelNewChatCallback = {
                    Notification.show("Creation of new chat canceled")
                }
            )
            dialog.open()
        }
        return button
    }


    private fun addHeaderContent() {
        val toggle = DrawerToggle()
        toggle.setAriaLabel("Menu toggle")
        viewTitle = H2()
        viewTitle.addClassNames(LumoUtility.FontSize.LARGE)
        addToNavbar(false, toggle, viewTitle)
    }

    private fun createNavigation(): SideNav {
        nav = SideNav()
        nav.addClassNames(LumoUtility.Margin.SMALL, LumoUtility.Margin.Top.NONE)
        return nav
    }

    private fun createFooter(): Footer {
        val layout = Footer()
        return layout
    }

    override fun afterNavigation() {
        super.afterNavigation()
        viewTitle.text = getCurrentPageTitle()
    }

    private fun getCurrentPageTitle(): String {
        val title = content.javaClass.getAnnotation(
            PageTitle::class.java
        )
        return title?.value ?: ""
    }
}