package net.dankito.readability4j.processor

import net.dankito.readability4j.model.ReadabilityOptions
import net.dankito.readability4j.util.BaseRegexUtil
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.URISyntaxException


open class Postprocessor(override val regex:BaseRegexUtil= BaseRegexUtil()):ProcessorBase() {

    companion object {
        // These are the classes that readability sets itself.
        val CLASSES_TO_PRESERVE = listOf("page")
        private val log = LoggerFactory.getLogger(Postprocessor::class.java)
    }

    open fun postProcessContent(articleContent: Element,baseUri: String, documentUri: String,
                                options: ReadabilityOptions) {

        // Readability cannot open relative uris so we convert them to absolute uris.
        fixRelativeUris(articleContent, baseUri, documentUri)
        simplifyNestedElements(articleContent)

        if (!options.keepClasses) {
            // Remove classes.
            this.cleanClasses(articleContent,options.additionalClassesToPreserve)
        }
    }

    private fun simplifyNestedElements(articleContent: Element) {
        var node:Element?= articleContent

        while (node!=null){
            if (
                node.parentNode()!=null &&
                node.tagName() in arrayOf("div", "section") &&
                !(node.id().isNotBlank() && node.id().startsWith("readability"))
            ) {
                if (this.isElementWithoutContent(node)) {
                    node = this.removeAndGetNext(node)
                    continue
                } else if (
                    this.hasSingleTagInsideElement(node, "div") ||
                    this.hasSingleTagInsideElement(node, "section")
                ) {
                    val child = node.children()[0]
                    for (i in node.attributes()){
                        child.attr(i.key,i.value)
                    }
                    node.replaceWith(child)
                    node = child
                    continue
                }
            }

            node = getNextNode(node)
        }
    }


    /**
     * Converts each <a> and <img> uri in the given element to an absolute uri,
     * ignoring #ref uris.
     */
    open fun fixRelativeUris(element: Element, baseUri: String, documentUri: String) {
        try {
            var realBaseUri=baseUri
            //this because if no base tag the same behavior is in base javascript
            if (baseUri.isBlank()&&documentUri.isBlank())
                return //nothing to do if no documentUri neither base tag
            else if (baseUri.isBlank()) {
                realBaseUri = documentUri
            }
            if (URI(realBaseUri).isAbsolute){
                fixRelativeAnchorUris(element, realBaseUri,documentUri)
                fixRelativeImageUris(element, realBaseUri,documentUri)
            }
        }catch (e: URISyntaxException ){
            //this one is just the java variant of the error just in case
            log.error("Could not fix relative uri for element:$element with base uri documentUri:$documentUri because it don't look a valid uri", e)
        } catch(e: Exception) {
            log.error("Could not fix relative uri for $element with base uri $documentUri", e)
        }
    }

    protected open fun fixRelativeAnchorUris(element: Element, baseURI:String, documentURI:String) {
        element.getElementsByTag("a").forEach { link ->
            val href = link.attr("href").trim()
            if(href.isNotBlank()) {
                // Replace links with javascript: URIs with text content, since
                // they won't work after scripts have been removed from the page.
                if(href.indexOf("javascript:") == 0) {
                    if (
                        link.childNodes().size == 1 &&
                        link.childNodes()[0] is TextNode
                    ) {
                        val text = TextNode(link.wholeText())
                        link.replaceWith(text)
                    }else{
                        // if the link has multiple children, they should all be preserved
                        val container = Element("span")
                        while (link.firstChild()!=null) {
                            link.firstChild()?.let { container.appendChild(it) }
                        }
                        link.replaceWith(container)
                    }
                }
                else {
                    link.attr("href", toAbsoluteURI(href, baseURI,documentURI))
                }
            }
        }
    }

    protected open fun fixRelativeImageUris(element: Element, baseUri:String, docUri: String) {
        val medias = element.getAllNodesWithTag(arrayOf(
            "img",
            "picture",
            "figure",
            "video",
            "audio",
            "source",
            ))

        medias.forEach { media ->
            val src = media.attr("src").trim()
            val poster = media.attr("poster").trim()
            val srcset = media.attr("srcset").trim()

            if(src.isNotBlank()) {
                media.attr("src", toAbsoluteURI(src,baseUri,docUri))
            }

            if (poster.isNotBlank()) {
                media.attr("poster", toAbsoluteURI(poster,baseUri,docUri))
            }
            if (srcset.isNotBlank()) {
                var newSrcset = ""
                regex.getSrcSetMatches(srcset).map { it.groups }.forEach { group ->
                    val srcSetBaseUri=group[1]?.value
                    val srcSetSize=group[2]?.value
                    val srcSetSeparator=group[3]?.value
                    if (srcSetBaseUri!=null&&srcSetSeparator!=null){
                        newSrcset+= toAbsoluteURI(srcSetBaseUri,baseUri,docUri)+(srcSetSize?:"")+srcSetSeparator
                    }
                }

                media.attr("srcset", newSrcset)
            }
        }
    }

    protected open fun toAbsoluteURI(uri: String, baseURI:String, documentURI:String): String {

        // Leave hash links alone if the base URI matches the document URI:
        if(baseURI==documentURI && uri[0] == '#') {
            return uri
        }

        // Otherwise, resolve against base URI:
        try {
            //Zero width space breaks the Java URI match algorithm and and Redability.js just don't mind it
            //in really really weird cases it can be in the url
            //at least the 95% of code here is because javascript and java resolves god know how the uris but different
            if(uri.startsWith("\u200B")||uri.startsWith("%E2%80%8B")){
                return URI(baseURI).resolve("").toString()+uri
            }else if(uri.contains(Regex("^\\.\\./\\.\\./\\.\\./(\\.\\./)+"))){
                return URI(baseURI).resolve(uri.replace(Regex("^(\\.\\./)+"),"../../")).toString()
            }else if(uri.contains(Regex("^file:"))){
                return uri.replaceFirst("file:///","file:/") //dont resolve file uris
            }else {
                val realUri = URI(baseURI).resolve(uri).toString()

                //also add to the latest part the / just for testing proposes as URI doesn't add it
                val isOnlyFirstPart = Regex("^(?:http|https)://[a-zA-Z.0-9-_]+(?::\\d+)?(/)?\$")
                return if (isOnlyFirstPart.matches(uri) && uri.last() != '/') {
                    "$realUri/"
                } else {
                    val firstPartNeedsSlash = Regex("^(?:http|https)://[a-zA-Z.0-9-_]+(?::\\d+)?(/)?")
                    if(firstPartNeedsSlash.find(realUri)?.groups?.let { it[1] ==null }==true)
                        firstPartNeedsSlash.replace(realUri,"$0/")
                    else
                        realUri
                }
            }
        } catch (_: Exception) {
            // Something went wrong, just return the original:
        }

        return uri
    }

    /**
     * Removes the class="" attribute from every element in the given
     * subtree, except those that match CLASSES_TO_PRESERVE and
     * the classesToPreserve array from the options object.
     *
     * @param node Element to clean it and their children
     * @param classesToPreserve Set of Strings of ReadabilityOptions.classesToPreserve
     * @return void
     */
    protected open fun cleanClasses(node: Element, classesToPreserve: Set<String>) {
        val classNames = node.classNames().filter { it in (classesToPreserve + CLASSES_TO_PRESERVE) }

        if(classNames.isNotEmpty()) {
            node.classNames(classNames.toMutableSet())
        }
        else {
            node.classNames(setOf())
        }

        node.children().forEach { child ->
            cleanClasses(child, classesToPreserve)
        }
    }

}
