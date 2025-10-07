package org.jbareaud.ragchat.ai.rag

import dev.langchain4j.data.document.Document
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader
import dev.langchain4j.http.client.HttpClientBuilder
import org.jbareaud.ragchat.ai.ConfigProperties
import org.jbareaud.ragchat.ai.chroma.ChromaClient
import org.jbareaud.ragchat.ai.reranker.ScoringModelProvider
import org.springframework.stereotype.Service

@Service
class PdfRagProvider(
    props: ConfigProperties,
    httpClientBuilder: HttpClientBuilder,
    scoringModelProvider: ScoringModelProvider,
    client: ChromaClient?,
): AugmentedRagProvider(props, httpClientBuilder, client, scoringModelProvider) {

    override fun type() = RagType.PDF

    override fun readDocuments(docsLocation: String): List<Document> =
        FileSystemDocumentLoader.loadDocuments(docsLocation, PdfBoxDocumentParser())
}