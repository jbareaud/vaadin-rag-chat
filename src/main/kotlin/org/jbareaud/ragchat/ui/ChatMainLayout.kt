package org.jbareaud.ragchat.ui

import com.vaadin.flow.component.applayout.AppLayout
import com.vaadin.flow.component.applayout.DrawerToggle
import com.vaadin.flow.component.html.Footer
import com.vaadin.flow.component.html.H1
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.html.Header
import com.vaadin.flow.component.orderedlayout.Scroller
import com.vaadin.flow.component.sidenav.SideNav
import com.vaadin.flow.component.sidenav.SideNavItem
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.theme.lumo.LumoUtility
import org.vaadin.lineawesome.LineAwesomeIcon


class ChatMainLayout: AppLayout() {

    private lateinit var viewTitle:H2

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
        val scroller = Scroller(createNavigation())
        addToDrawer(header, scroller, createFooter())
    }

    private fun addHeaderContent() {
        val toggle = DrawerToggle()
        toggle.setAriaLabel("Menu toggle")
        viewTitle = H2()
        viewTitle.addClassNames(LumoUtility.FontSize.LARGE)
        addToNavbar(false, toggle, viewTitle)
    }

    private fun createNavigation(): SideNav {
        val nav = SideNav()
        nav.addClassNames(LumoUtility.Margin.SMALL, LumoUtility.Margin.Top.NONE)
        nav.addItem(SideNavItem("Chat", RagChatView::class.java, LineAwesomeIcon.COMMENTS.create()))
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