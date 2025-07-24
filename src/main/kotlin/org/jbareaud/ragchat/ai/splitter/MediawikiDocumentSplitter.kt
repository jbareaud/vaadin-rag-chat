package org.jbareaud.ragchat.ai.splitter

import dev.langchain4j.data.document.DocumentSplitter
import dev.langchain4j.data.document.splitter.DocumentSplitters
import dev.langchain4j.data.document.splitter.HierarchicalDocumentSplitter
import dev.langchain4j.internal.ValidationUtils
import dev.langchain4j.model.TokenCountEstimator
import org.jbareaud.ragchat.utils.Page
import org.jbareaud.ragchat.utils.convertWikitextToMarkdown
import org.jbareaud.ragchat.utils.parseXmlContent

/**
 * DocumentSplitter for Mediawiki XML dumps.
 *
 * Split the document into several fragments using Jackson XMLMapper,
 * each fragments representing one page from the original document.
 * The super class will then be responsible for handling long fragments and overlaping,
 * sub-splitting the fragments if necessary.
 *
 * The class uses DocumentSplitters.recursive() as the default subSplitter.
 *
 * The default joinDelimiter is "\n\n" (same as paragraphs).
 *
 * @see dev.langchain4j.data.document.DocumentSplitter
 * @see dev.langchain4j.data.document.splitter.HierarchicalDocumentSplitter
 */
class MediawikiDocumentSplitter(
    maxSegmentSizeInChars: Int,
    maxOverlapSizeInChars: Int,
    subSplitter: DocumentSplitter? = null,
    tokenCountEstimator: TokenCountEstimator? = null,
    private val joinDelimiter: String = "\n\n",
): HierarchicalDocumentSplitter(maxSegmentSizeInChars, maxOverlapSizeInChars, tokenCountEstimator, subSplitter) {

    override fun split(documentText: String?): Array<String> {
        ValidationUtils.ensureNotNull(documentText, "document")
        val segments = mutableListOf<String>()
        val pages = parseXmlContent(requireNotNull(documentText))
        for (page in pages) {
            segments.add(page.toMarkdownText())
        }
        return segments.toTypedArray()
    }

    override fun joinDelimiter(): String {
        return joinDelimiter
    }

    override fun defaultSubSplitter(): DocumentSplitter {
        return DocumentSplitters.recursive(maxSegmentSize, maxOverlapSize)
    }
}

private fun Page.toMarkdownText() =
    if (redirect != null) {
        buildString {
            append("# $title\n\n")
            append("For more information about $title, see the article for $redirect")
        }
    } else {
        convertWikitextToMarkdown(text, title)
    }
