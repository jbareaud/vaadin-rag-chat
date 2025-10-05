package org.jbareaud.ragchat.ai.rag

import dev.langchain4j.data.document.Document
import dev.langchain4j.http.client.HttpClientBuilder
import dev.langchain4j.internal.Exceptions
import org.jbareaud.ragchat.ai.ConfigProperties
import org.jbareaud.ragchat.ai.chroma.ChromaClient
import org.jbareaud.ragchat.ai.reranker.ScoringModelProvider
import org.jbareaud.ragchat.logger
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.inputStream

@Service
class PdfRagProvider(
    props: ConfigProperties,
    httpClientBuilder: HttpClientBuilder,
    scoringModelProvider: ScoringModelProvider,
    client: ChromaClient?,
): AugmentedRagProvider(props, httpClientBuilder, client, scoringModelProvider) {

    private val parser = PdfBoxDocumentParser()

    override fun type() = RagType.PDF

    override fun readDocuments(docsLocation: String): List<Document> {
        val directoryPath = Paths.get(docsLocation)
        return if (!Files.isDirectory(directoryPath, *arrayOfNulls<LinkOption>(0))) {
            throw Exceptions.illegalArgument("'%s' is not a directory", *arrayOf<Any>(directoryPath))
        } else {
            val entries = Files.list(directoryPath)
            entries.filter { Files.isRegularFile(it) }
                .map { tryParsePdf(it)}
                .toList()
                .filterNotNull()
        }
    }

    private fun tryParsePdf(file: Path): Document? {
        return try {
            parser.parse(file.inputStream())
        } catch (throwable: Exception) {
            logger().warn("couldn't read PDF file ${file.fileName}; skipping.")
            null
        }
    }
}