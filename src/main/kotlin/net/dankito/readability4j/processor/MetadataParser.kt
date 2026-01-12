package net.dankito.readability4j.processor

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import net.dankito.readability4j.model.ArticleMetadata
import net.dankito.readability4j.util.BaseRegexUtil
import org.jsoup.nodes.Document
import org.slf4j.LoggerFactory
import java.util.Deque
import java.util.Queue


open class MetadataParser(override val regex: BaseRegexUtil = BaseRegexUtil()): ProcessorBase() {

    private val log = LoggerFactory.getLogger(MetadataParser::class.java)

    /**
     * Attempts to get excerpt and byline metadata for the article.
     *
     * @param document — the Document
     * @param jsonld — object containing any metadata that
     * could be extracted from JSON-LD object.
     *
     * @return ArticleMetadata with optional "excerpt" and "byline" properties
     */
    open fun getArticleMetadata(document: Document,jsonld:ArticleMetadata?): ArticleMetadata {
        val metadata = ArticleMetadata()
        val values = HashMap<String, String>()
        val metaElements = document.getElementsByTag("meta");

        // property is a space-separated list of values
        val propertyPattern = Regex("\\s*(article|dc|dcterm|og|twitter)\\s*:\\s*(author|creator|description|published_time|title|site_name)\\s*", RegexOption.IGNORE_CASE)

        // name is a single value
        val namePattern = Regex("^\\s*(?:(dc|dcterm|og|twitter|parsely|weibo:(article|webpage))\\s*[-.:]\\s*)?(author|creator|pub-date|description|title|site_name)\\s*$", RegexOption.IGNORE_CASE)

        metaElements.forEach { element ->
            val elementName = element.attr("name")
            val elementProperty = element.attr("property")
            val content = element.attr("content")

            if (content.isBlank()){
                return@forEach
            }

            var name: String? = null
            var matches:MatchResult?=null
            if(elementProperty.isNotBlank()) {
                matches=propertyPattern.find(elementProperty)
                if (matches!=null) {
                    // Convert to lowercase and remove any whitespace
                    // so we can match below.
                    name = matches.groupValues[0].lowercase()
                        .replace("\\s".toRegex(), "")
                    // multiple authors
                    values[name] = content.trim()
                }
            }
            if(matches==null && elementName.isNotEmpty() && namePattern.matches(elementName)) {
                // Convert to lowercase and remove any whitespace
                // so we can match below.
                name = elementName.lowercase()
                    .replace("\\s".toRegex(), "")
                    .replace('.',':')
                values[name] = content.trim()
            }
        }

        // get title
        metadata.title = jsonld?.title ?:
                values["dc:title"] ?:
                values["dcterm:title"] ?:
                values["og:title"] ?:
                values["weibo:article:title"] ?:
                values["weibo:webpage:title"] ?:
                values["title"] ?:
                values["twitter:title"] ?:
                values["parsely-title"]

        if (metadata.title==null) {
            metadata.title = this.getArticleTitle(document)
        }

        val articleAuthor = if (values["article:author"]!=null &&
            !this.isUrl(values["article:author"])) values["article:author"] else null

        // get author
        metadata.byline = jsonld?.byline ?:
                values["dc:creator"] ?:
                values["dcterm:creator"] ?:
                values["author"] ?:
                values["parsely-author"] ?:
                articleAuthor

        // get description
        metadata.excerpt = jsonld?.excerpt ?:
                values["dc:description"] ?:
                values["dcterm:description"] ?:
                values["og:description"] ?:
                values["weibo:article:description"] ?:
                values["weibo:webpage:description"] ?:
                values["description"] ?:
                values["twitter:description"]

        // get site name
        metadata.siteName = jsonld?.siteName ?: values["og:site_name"]

        // get article published time
        metadata.publishedTime = jsonld?.datePublished ?:
                values["article:published_time"] ?:
                values["parsely-pub-date"]

        //not anymore
        //metadata.charset = document.charset().name()
        metadata.title = unescapeHtmlEntities(metadata.title)
        metadata.byline = unescapeHtmlEntities(metadata.byline)
        metadata.excerpt = unescapeHtmlEntities(metadata.excerpt)
        metadata.siteName = unescapeHtmlEntities(metadata.siteName)
        metadata.publishedTime = unescapeHtmlEntities(metadata.publishedTime)

        return metadata
    }

    private fun unescapeHtmlEntities(str:String?):String? {
        if (str==null) {
            return null
        }

        val htmlEscapeMap = mapOf(
            "lt" to "<",
            "gt" to ">",
            "amp" to  "&",
            "quot" to "\"",
            "apos" to "'",
        )
        var unescaped = Regex("&(quot|amp|apos|lt|gt);").replace(str) { result ->
            val tag = result.groupValues[1]
            htmlEscapeMap[tag] ?: result.value
        }
        unescaped=Regex("&#(?:x([0-9a-f]+)|([0-9]+));",RegexOption.IGNORE_CASE)
            .replace(unescaped) { result->
                val hex= result.groups[1]?.value
                val numStr = result.groups[2]?.value
                if (hex!=null||numStr!=null) {

                    var num = hex?.toBigInteger(16)?.toInt() ?: numStr!!.toInt(10)
                    // these character references are replaced by a conforming HTML parser
                    if (num == 0 ||
                        (num > 0x10ffff||num <0||(hex!=null&&hex.length>6)) || //Java max int limit
                        (num in 0xd800..0xdfff)) {
                        num = 0xfffd
                    }

                    return@replace String(intArrayOf(num),0,1)
                }
                "\uD83D\uDE2D \uD83D\uDE2D � �"
                "&amp;#xg; &amp;#x1F62D; &amp;#128557; &amp;#xFFFFFFFF; &amp;#x0;"
                result.value
            }
        return unescaped

    }

    /**
     * Get the article title as an H1.
     *
     * @return string
     **/
    private fun getArticleTitle(doc: Document): String {
        var curTitle = ""
        var origTitle = ""

        try {
            origTitle = doc.title().trim()
            curTitle = origTitle

            // If they had an element with id "title" in their HTML
            if(curTitle.isBlank()) {
                doc.select("title").first()?.let { elementWithIdTitle ->
                    origTitle = getInnerText(elementWithIdTitle)
                    curTitle = origTitle
                }
            }
        } catch(e: Exception) {/* ignore exceptions setting the title. */}

        var titleHadHierarchicalSeparators = false
        val wordCount:( String)->Int= { str->
            str.split("\\s+".toRegex()).size
        }

        // If there's a separator in the title, first remove the final part
        val titleSeparators = "|\\-–—\\\\\\/>»";
        if(curTitle.contains("\\s[${titleSeparators}]\\s".toRegex())) {
            titleHadHierarchicalSeparators = curTitle.contains("\\s[\\\\/>»]\\s".toRegex())
            val allSeparators = ("\\s[${titleSeparators}]\\s".toRegex(setOf(RegexOption.IGNORE_CASE))).findAll(origTitle)
            curTitle = origTitle.substring(0, allSeparators.last().range.last-1 )

            // If the resulting title is too short (3 words or fewer), remove
            // the first part instead:
            if(wordCount(curTitle) < 3) {
                curTitle = origTitle.replace("^[^${titleSeparators}]*[|${titleSeparators}]".toRegex(RegexOption.IGNORE_CASE), "")
            }
        }
        else if(curTitle.contains(": ")) {
            // Check if we have an heading containing this exact string, so we
            // could assume it's the full title.
            val match = doc.getAllNodesWithTag(arrayOf("h1","h2")).any { it.wholeText().trim() == curTitle.trim() }

            // If we don't, let's extract the title out of the original title string.
            if(!match) {
                curTitle = origTitle.substring(origTitle.lastIndexOf(':') + 1)

                // If the title is now too short, try the first colon instead:
                if(wordCount(curTitle) < 3) {
                    curTitle = origTitle.substring(origTitle.indexOf(':') + 1)
                }
                // But if we have too many words before the colon there's something weird
                // with the titles and the H tags so let's just use the original title instead
                else if(wordCount(origTitle.substring(0, origTitle.indexOf(':'))) > 5) {
                    curTitle = origTitle
                }
            }
        }
        else if(curTitle.length > 150 || curTitle.length < 15) {
            val hOnes = doc.getElementsByTag("h1")

            if(hOnes.size == 1) {
                curTitle = getInnerText(hOnes[0])
            }
        }

        curTitle = regex.normalize(curTitle.trim())
        // If we now have 4 words or fewer as our title, and either no
        // 'hierarchical' separators (\, /, > or ») were found in the original
        // title or we decreased the number of words by more than 1 word, use
        // the original title.
        val curTitleWordCount = wordCount(curTitle)
        if(curTitleWordCount <= 4 &&
            (!titleHadHierarchicalSeparators ||
            curTitleWordCount != wordCount(origTitle.replace("\\s[${titleSeparators}]\\s".toRegex(), "")) - 1)) {
            curTitle = origTitle
        }

        return curTitle
    }

    /**
     * Try to extract metadata from JSON-LD object.
     * For now, only Schema.org objects of type Article or its subtypes are supported.
     * @return Object with any metadata that could be extracted (possibly none)
     */
    open fun getJSONLD(doc: Document):ArticleMetadata?{
        var metadata:ArticleMetadata?=null

        doc.getElementsByTag("script").forEach{ jsonLdElement->
            if (
                metadata==null &&
                jsonLdElement.attr("type") == "application/ld+json"
            ) {

                try {
                    // Strip CDATA markers if present
                    val content = jsonLdElement.html().replace(
                        Regex("^\\s*<!\\[CDATA\\[|]]>\\s*$"),
                        ""
                    )

                    var parsed = ObjectMapper().readTree(content) ?: return@forEach

                    if (parsed.isArray) {
                        parsed = parsed.jsFind{ node ->
                            node.has("@type") &&
                                regex.isJsonLDArticle(node.get("@type").textValue())
                        }?:return@forEach
                    }

                    val schemaDotOrgRegex = Regex("^https?://schema\\.org/?$")
                    val matches =
                        (parsed["@context"]?.isTextual?.let {
                            if (it)
                                schemaDotOrgRegex.containsMatchIn(parsed["@context"].textValue())
                            else false
                        }==true) ||
                            (parsed.get("@context")?.isObject?.let {
                                if (it) parsed["@context"]["@vocab"]?.isTextual?.let {
                                    schemaDotOrgRegex.containsMatchIn(parsed["@context"]["@vocab"].textValue())
                                }==true // we dont want a null here
                                else false
                            }==true)

                    if (!matches) {
                        return@forEach
                    }

                    if (parsed["@type"]==null && parsed["@graph"]?.let{parsed["@graph"].isArray}==true) {
                        parsed = parsed["@graph"].jsFind{ node ->
                            node.has("@type") &&
                                regex.isJsonLDArticle(node.get("@type").textValue())
                        }?: return@forEach
                    }

                    if (
                        parsed["@type"]==null ||
                        !regex.isJsonLDArticle(parsed["@type"].textValue())
                    ) {
                        return@forEach
                    }

                    val nonNullMetadata = ArticleMetadata()

                    if (
                        parsed["name"]?.isTextual == true &&
                        parsed["headline"]?.isTextual == true &&
                        parsed["name"] != parsed["headline"]
                    ) {
                        // we have both name and headline element in the JSON-LD. They should both be the same but some websites like aktualne.cz
                        // put their own name into "name" and the article title to "headline" which confuses Readability. So we try to check if either
                        // "name" or "headline" closely matches the html title, and if so, use that one. If not, then we use "name" by default.

                        val title = getArticleTitle(doc)
                        val nameMatches = textSimilarity(parsed["name"].textValue(), title) > 0.75
                        val headlineMatches = textSimilarity(parsed["headline"].textValue(), title) > 0.75

                        if (headlineMatches && !nameMatches) {
                            nonNullMetadata.title = parsed["headline"].textValue()
                        } else {
                            nonNullMetadata.title = parsed["name"].textValue()
                        }
                    } else if (parsed["name"]?.isTextual == true) {
                        nonNullMetadata.title = parsed["name"].textValue().trim()
                    } else if (parsed["headline"]?.isTextual == true) {
                        nonNullMetadata.title = parsed["headline"].textValue().trim()
                    }
                    if (parsed.hasNonNull("author") ) {
                        if (parsed["author"]?.get("name")?.isTextual == true) {
                            val name = parsed["author"].get("name").textValue()
                            if(name.isNotBlank())
                                nonNullMetadata.byline = name.trim()
                        } else if (
                            parsed["author"].isArray &&
                            parsed["author"].get(0)?.get("name")?.isTextual == true
                        ) {
                            nonNullMetadata.byline = parsed["author"].filter { author ->
                                author?.get("name")?.isTextual == true
                            }.joinToString(", ") { author ->
                                author["name"].textValue().trim()
                            }
                        }
                    }
                    if (parsed["description"]?.isTextual == true) {
                        nonNullMetadata.excerpt = parsed["description"].textValue().trim()
                    }
                    if (parsed["publisher"]?.isObject==true &&
                        parsed["publisher"]?.get("name")?.isTextual == true) {
                        nonNullMetadata.siteName = parsed["publisher"]["name"].textValue().trim()
                    }
                    if (parsed["datePublished"]?.isTextual == true) {
                        nonNullMetadata.datePublished = parsed["datePublished"].textValue().trim()
                    }
                    metadata=nonNullMetadata
                } catch (err:Exception) {
                    log.error("{}\n\n{}",err.message,err.stackTraceToString())
                    println(err.message+"\n\n"+err.stackTraceToString())
                }
            }
        }

        return metadata
    }

    /**
     * Array.Prototype.find  (this should work like that one)
     *
     * @return null if don't find in the array
     * */
    private fun JsonNode.jsFind(filterFun:(JsonNode)->Boolean):JsonNode?{
        if (this.isArray){
            for (value in this){
                if (filterFun(value))
                    return value
            }
        }
        return null
    }
}
