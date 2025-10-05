package org.jbareaud.ragchat.ai.rag

import dev.langchain4j.data.document.BlankDocumentException
import dev.langchain4j.data.document.Document
import dev.langchain4j.data.document.DocumentParser
import dev.langchain4j.internal.Utils.isNullOrBlank
import org.apache.pdfbox.Loader
import org.apache.pdfbox.io.RandomAccessReadBuffer
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import java.io.IOException
import java.io.InputStream


class PdfBoxDocumentParser: DocumentParser {

    override fun parse(inputStream: InputStream): Document {
        return try {
            Loader.loadPDF(RandomAccessReadBuffer(inputStream)).use { pdfDocument ->
                val stripper = PDFTextStripper()
                val text = stripper.getText(pdfDocument)
                if (isNullOrBlank(text)) {
                    throw BlankDocumentException()
                }
                Document.from(text, toMetadata(pdfDocument))
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    private fun toMetadata(pdDocument: PDDocument): dev.langchain4j.data.document.Metadata {
        val documentInformation = pdDocument.documentInformation
        val metadata = dev.langchain4j.data.document.Metadata()
        for (metadataKey in documentInformation.metadataKeys) {
            val value = documentInformation.getCustomMetadataValue(metadataKey)
            if (value != null) metadata.put(metadataKey, value)
        }
        return metadata
    }
}