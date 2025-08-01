package org.jbareaud.ragchat.ai.rag

import dev.langchain4j.data.document.splitter.DocumentSplitters
import dev.langchain4j.http.client.HttpClientBuilder
import org.jbareaud.ragchat.ai.ConfigProperties
import org.jbareaud.ragchat.ai.splitter.MediawikiDocumentSplitter
import org.jbareaud.ragchat.ai.chroma.ChromaClient
import org.jbareaud.ragchat.ai.reranker.ScoringModelProvider
import org.springframework.stereotype.Service

@Service
class MediawikiRagProvider(
    props: ConfigProperties,
    httpClientBuilder: HttpClientBuilder,
    scoringModelProvider: ScoringModelProvider,
    client: ChromaClient?,
): AugmentedRagProvider(props, httpClientBuilder, client, scoringModelProvider) {

    override fun type() = RagType.MEDIAWIKI

    override fun documentSplitter() =
        MediawikiDocumentSplitter(
            maxSegmentSizeInChars = requireNotNull(props.splitter?.maxChars),
            maxOverlapSizeInChars = requireNotNull(props.splitter?.overlapChars),
            subSplitter = DocumentSplitters.recursive(requireNotNull(props.splitter?.maxChars), requireNotNull(props.splitter?.overlapChars)),
        )
}