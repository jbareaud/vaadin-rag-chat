package org.jbareaud.ragchat.ai.splitter

import dev.langchain4j.data.document.DocumentSplitter
import dev.langchain4j.data.document.splitter.DocumentSplitters
import dev.langchain4j.data.document.splitter.HierarchicalDocumentSplitter
import dev.langchain4j.internal.ValidationUtils
import dev.langchain4j.model.TokenCountEstimator
import org.jbareaud.ragchat.utils.Page
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
 * skipRedirectArticles can be useful for a Mediawiki knowledge base with
 * a significant number of redirected articles. When this is the case, either skip redirected articles
 * (but knowledge risks to be lost), use a scoring model, or increase the number of max results of
 * the content retriever to take into account the increased number of hits.
 *
 * @see dev.langchain4j.data.document.DocumentSplitter
 * @see dev.langchain4j.data.document.splitter.HierarchicalDocumentSplitter
 */
class MediawikiDocumentSplitter(
    maxSegmentSizeInChars: Int,
    maxOverlapSizeInChars: Int,
    subSplitter: DocumentSplitter? = null,
    tokenCountEstimator: TokenCountEstimator? = null,
    private val skipRedirectedArticles: Boolean = false,
    private val joinDelimiter: String = "\n\n",
): HierarchicalDocumentSplitter(maxSegmentSizeInChars, maxOverlapSizeInChars, tokenCountEstimator, subSplitter) {

    companion object {
        const val PAGENAME_MARKER = "{{PAGENAME}}"
    }

    override fun split(documentText: String?): Array<String> {
        ValidationUtils.ensureNotNull(documentText, "document")
        val segments = mutableListOf<String>()
        val pages = parseXmlContent(requireNotNull(documentText))
        for (page in pages) {
            buildPageText(page)?.let(segments::add)
        }
        return segments.toTypedArray()
    }

    private fun buildPageText(page: Page): String? =
        when {
            page.redirect != null -> {
                if (skipRedirectedArticles) null
                else "<page>${page.title} is another name for ${page.redirect}</page>"
            }
            else -> {
                buildString {
                    append("<page>")
                    append(page.text.replace(PAGENAME_MARKER, page.title))
                    append("</page>")
                }
            }
        }

    override fun joinDelimiter(): String {
        return joinDelimiter
    }

    override fun defaultSubSplitter(): DocumentSplitter {
        return DocumentSplitters.recursive(maxSegmentSize, maxOverlapSize)
    }
}
