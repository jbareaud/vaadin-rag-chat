package org.jbareaud.ragchat.ai.provider

import dev.langchain4j.data.document.splitter.DocumentSplitters
import dev.langchain4j.http.client.HttpClientBuilder
import dev.langchain4j.model.scoring.ScoringModel
import org.jbareaud.ragchat.ai.ConfigProperties
import org.jbareaud.ragchat.ai.AssistantType
import org.jbareaud.ragchat.ai.splitter.MediawikiDocumentSplitter
import org.jbareaud.ragchat.ai.chroma.ChromaClient
import org.springframework.stereotype.Service

@Service
class MediawikiAssistantProvider(
    props: ConfigProperties,
    httpClientBuilder: HttpClientBuilder,
    scoringModel: ScoringModel?,
    client: ChromaClient?,
): AugmentedAssistantProvider(props, httpClientBuilder, client, scoringModel) {

    override fun type() = AssistantType.MEDIAWIKI

    override fun documentSplitter() =
        MediawikiDocumentSplitter(
            maxSegmentSizeInChars = props.splitterMaxChars,
            maxOverlapSizeInChars = props.splitterOverlapChars,
            subSplitter = DocumentSplitters.recursive(props.splitterMaxChars, props.splitterOverlapChars),
        )
}