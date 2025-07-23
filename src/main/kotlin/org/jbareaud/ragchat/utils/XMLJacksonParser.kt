package org.jbareaud.ragchat.utils

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.dataformat.xml.XmlMapper

private val mapper = XmlMapper()

private const val XML_PAGE = "page"
private const val XML_TITLE = "title"
private const val XML_ID = "id"
private const val XML_REDIRECT = "redirect"
private const val XML_REVISION = "revision"
private const val XML_TEXT = "text"
private const val XML_ROOT_TEXT = ""

fun parseXmlContent(content: String): List<Page> {
    try {
        return mapper.readTree(content).readDocument()
    } catch (err : Throwable) {
        throw RuntimeException("[XMLJacksonUtils:] error while parsing the XML document", err)
    }
}

private fun JsonNode.readDocument(): List<Page> {
    val pages = properties().first { it.key == XML_PAGE }
    return pages.value.mapNotNull { it.parseXmlPage() }
}

private fun JsonNode.parseXmlPage(): Page {
    val props = this.properties()
    val title = props.firstOrNull { it.key == XML_TITLE }?.value?.toString()
        ?.replace("\'", "")
        ?: throw toError(XML_TITLE)
    val id = props.firstOrNull { it.key == XML_ID }?.value?.toString()
        ?: throw toError(XML_ID)
    val text = props.firstOrNull { it.key == XML_REVISION }?.value?.parseXmlText()
        ?: throw toError(XML_REVISION)
    val redirect = props.firstOrNull { it.key == XML_REDIRECT }?.value?.parseXmlRedirect()
    return Page(
        id = id,
        title = title,
        redirect = redirect,
        text = text,
    )
}

private fun JsonNode.parseXmlText(): String {
    return properties()
        .firstOrNull { it.key == XML_TEXT }
        ?.value?.properties()
        ?.firstOrNull { it.key == XML_ROOT_TEXT }?.value?.toString()
            ?: throw toError(XML_TEXT)
}

private fun JsonNode.parseXmlRedirect(): String? {
    return properties().firstOrNull { it.key == XML_TITLE }?.value?.toString()
}

private fun JsonNode.toError(fieldName: String) =
    RuntimeException("No $fieldName node to extract ${this.toLog()}")

private fun JsonNode.toLog() = "[Node:] ${this.toString().take(300)}(...)"

data class Page(
    val title: String,
    val id: String,
    val text: String,
    val redirect: String?
)