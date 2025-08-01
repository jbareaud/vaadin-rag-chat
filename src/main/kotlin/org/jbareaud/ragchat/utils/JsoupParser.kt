package org.jbareaud.ragchat.utils

/*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements


fun jsoupParser(url: String, key: String): String {

    val search = "https://boards.4chan.org/g/catalog#s=lmg"

    val doc: Document = Jsoup.connect(search)
        .ignoreContentType(true)
        .userAgent("Mozilla/5.0 (X11; Linux i686; rv:124.0) Gecko/20100101 Firefox/124.0")
        .get()

    //log(doc.title());

    val elements: Elements = doc.select("#thread div#teaser")
    for (element in elements) {
        println(element.toString())
    }

    return ""
}
*/