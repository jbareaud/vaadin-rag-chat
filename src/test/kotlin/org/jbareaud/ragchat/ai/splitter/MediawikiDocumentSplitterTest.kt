package org.jbareaud.ragchat.ai.splitter

import dev.langchain4j.data.document.parser.TextDocumentParser
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test


class MediawikiDocumentSplitterTest {

    @Test
    fun `MediawikiSplitter should process a XML document file and return a list of TextSegments`() {
        // GIVEN
        val data = this::class.java.classLoader.getResourceAsStream("xml/0.xml")
        val document = TextDocumentParser().parse(data)
        // WHEN
        val segments = MediawikiDocumentSplitter(
            maxSegmentSizeInChars = 300,
            maxOverlapSizeInChars = 0
        ).split(document)
        // THEN
        Assertions.assertEquals(1, segments.size)
        Assertions.assertTrue(segments.first().text().length < 300)
    }


    @Test
    fun `MediawikiSplitterTest should be able to skip redirected articles when asked to`() {
        // GIVEN
        val data = this::class.java.classLoader.getResourceAsStream("xml/0.xml")
        val document = TextDocumentParser().parse(data)
        // WHEN
        val segments = MediawikiDocumentSplitter(
            maxSegmentSizeInChars = 300,
            maxOverlapSizeInChars = 0,
            skipRedirectedArticles = true
        ).split(document)
        // THEN
        Assertions.assertFalse(segments.first().text().contains("is another name for"))
    }
}