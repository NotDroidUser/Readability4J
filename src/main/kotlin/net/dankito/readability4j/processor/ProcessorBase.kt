package net.dankito.readability4j.processor

import net.dankito.readability4j.processor.ArticleGrabber.Companion.PHRASING_ELEMS
import net.dankito.readability4j.util.BaseRegexUtil
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.jsoup.select.Elements
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.URISyntaxException

/**
 * Contains common utils for Preprocessor and Postprocessor
 */
abstract class ProcessorBase {

    abstract val regex:BaseRegexUtil
    private val log = LoggerFactory.getLogger(ProcessorBase::class.java)

    companion object {
        var TruncateLogOutput = false
    }

    protected open fun removeNodes(nodeList:Elements, filterFunction: ((Element) -> Boolean)? = null) {
        nodeList.reversed().forEach { node ->
            if(node.parentNode() != null) {
                if(filterFunction == null || filterFunction(node)) {
                    node.remove()
//                    printAndRemove(node, "removeNode('${node.tagName()},${filterFunction?:"null"}')")
                }
            }
        }
    }

//    protected open fun printAndRemove(node: Node, reason: String) {
////        logNodeInfo(node, reason)
//        node.remove()
//    }

//    protected open fun logNodeInfo(node: Node, reason: String) {
//        val nodeToString =
//        if(TruncateLogOutput)
//            node.outerHtml().substring(0, min(node.outerHtml().length, 80)).replace("\n", "")
//        else
//            "\n------\n" + node.outerHtml().split("\n").first() + "\n------\n"
//
////        log.debug("{} [{}]", reason, nodeToString)
//    }

    //TODO: this should be like js one just for make the porting task easier
    protected open fun replaceNodeTags(elements:Elements, newTagName: String) {
        elements.forEach { element ->
            element.tagName(newTagName)
        }
    }


    /**
     * Finds the next element, starting from the given node, and ignoring
     * whitespace in between. If the given node is an element, the same node is
     * returned.
     */
    protected open fun nextNode(node: Node?): Node? {
        var next: Node? = node

        while(next != null &&
            next::class !is Element &&
            (next is TextNode && regex.isWhitespace(next.text()))) {
            next = next.nextSibling()
        }

        return next
    }

    /**
     * Get the inner text of a node - cross browser compatibly.
     * This also strips out any excess whitespace to be found.
     */
    protected open fun getInnerText(e: Element, normalizeSpaces: Boolean = true): String {
        val textContent = e.wholeText().trim()

        if(normalizeSpaces) {
            return regex.normalize(textContent)
        }

        return textContent
    }

    protected open fun textSimilarity(textA: String, textB: String): Double {

        val tokensA=regex.getWords(textA.lowercase())
        val tokensB=regex.getWords(textB.lowercase())
        if (tokensA.size==0 || tokensB.size==0) {
            return 0.0
        }
        val uniqTokensB = tokensB.filter{ token ->
            token !in tokensA
        }
        val distanceB = (uniqTokensB.joinToString(" ").length.toDouble()) / (tokensB.joinToString(" ").length.toDouble())
        return 1.0 - distanceB
    }

    fun isUrl(s: String?): Boolean{
        if (s==null)return false
        try{
            URI(s)
            return true
        }catch (_:URISyntaxException){
            return false
        }
    }

    /**
     * This method is just for make the code translation yet easier
     *
     * @param tags an array of tag names to find in the element
     * @return Elements
     * @see org.jsoup.nodes.Element.select
     */
    fun Element.getAllNodesWithTag(tags: Array<String>): Elements =  this.select(tags.joinToString (separator = ", "))
    /**
     * This method is just for make the translation easier
     *
     * @param tags an array of tag names to find in the element as Jsoup treats almost all nodes as that
     * @return Elements
     * @see org.jsoup.nodes.Element.select
     */
    fun Document.getAllNodesWithTag(tags: Array<String>): Elements = this.select(tags.joinToString (separator = ", "))


    protected open fun isElementWithoutContent(node: Element): Boolean {
        return node.text().isBlank() &&
            (node.children().size == 0 ||
                (node.children().size ==
                    (node.getElementsByTag("br").size +
                     node.getElementsByTag("hr").size)
                )
            )
    }

    /**
     * Check if this node has only whitespace and a single P element
     * Returns false if the DIV node contains non-empty text nodes
     * or if it contains no P or more than 1 element.
     */
    protected open fun hasSingleTagInsideElement(element: Element,tagName: String): Boolean {
        // There should be exactly 1 element child which is a P:
        if(element.children().size != 1 || element.child(0).tagName() != tagName) {
            return false
        }

        // And there should be no text nodes with real content
        return !element.childNodes().any { node ->
            node is TextNode && regex.hasContent(node.text())
        }
    }

//     * @param reason optional, for log to debug the why
    /**
     * Gets next node and remove the pivot one
     *
     * @param node the element to be removed the base to search
     * @return the next node in the tree that aren't inside the pivot or null if not exists more nodes
     **/

    protected open fun removeAndGetNext(node: Element): Element? {
        val nextNode = this.getNextNode(node, true)
        node.remove()
//        printAndRemove(node, reason)
        return nextNode
    }

    /**
     * Traverse the DOM from node to node, starting at the node passed in.
     * Pass true for the second parameter to indicate this node itself
     * (and its kids) are going away, and we want the next node over.
     *
     * Calling this in a loop will traverse the DOM depth-first.
     */
    protected open fun getNextNode(node: Element?, ignoreSelfAndKids: Boolean = false): Element? {
        // First check for kids if those aren't being ignored
        if(!ignoreSelfAndKids) {
            node?.firstElementChild()?.let { return it }
        }

        // Then for siblings...
        node?.nextElementSibling()?.let { return it }

        // And finally, move up the parent chain *and* find a sibling
        // (because this is depth-first traversal, we will have already
        // seen the parent nodes themselves).
        var parent = node?.parent()
        while(parent != null && parent.nextElementSibling() == null) {
            parent = parent.parent()
        }

        return parent?.nextElementSibling()
    }

    fun isPhrasingContent(node: Node): Boolean {
        val tagName = if (node is Element ) node.tagName() else node.nodeName()
        return (
            node is TextNode ||
            (tagName) in PHRASING_ELEMS ||
            ( (tagName in listOf("a","del","ins")) &&
            (node.childNodeSize()==0||node.childNodes().all{ isPhrasingContent(it) }))
        )
    }


    fun isWhitespace(node: Node) =
        (node is TextNode && node.text().isBlank()) ||
            (node is Element && node.tagName() == "br")

}
