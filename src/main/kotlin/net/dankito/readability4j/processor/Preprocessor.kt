package net.dankito.readability4j.processor

import net.dankito.readability4j.util.BaseRegexUtil
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.slf4j.LoggerFactory

/**
 * Performs basic sanitization before starting the extraction process.
 */
open class Preprocessor(override val regex: BaseRegexUtil = BaseRegexUtil()) : ProcessorBase() {

    private val log = LoggerFactory.getLogger(Preprocessor::class.java)



    /**
     * Prepare the HTML document for readability to scrape it.
     * This includes things like stripping javascript, CSS, and handling terrible markup.
     */
    open fun prepareDocument(document: Document) {
        log.debug("Starting to prepare document")

        removeScripts(document)

        removeNodes(document.getElementsByTag("style"))

//        removeForms(document) // TODO: this was moved in Mozilla's Readability to on grabArticle

//        removeComments(document) // TODO: this is not in Mozilla's Readability now

        replaceBrs(document)

        replaceNodeTags(document.getElementsByTag("font"), "span")
    }

    /**
     * Removes script tags from the document.
     *
     * @param document
     **/
    private fun removeScripts(document: Document) {
        removeNodes(document.getAllNodesWithTag(arrayOf("script","noscript")))
    }

    /**
     * Check if node is image, or if node contains exactly only one image
     * whether as a direct child or as its descendants.
     *
     * @param noscript Element
     **/
    private fun isSingleImage(noscript: Element): Boolean {
        var element:Element?=noscript
        while (element!=null){
            if (element.tagName() == "img") {
                return true
            }
            if (element.children().size != 1 || element.wholeText().trim() != ""){
                return false
            }
            element = element.child(0)
        }
        return false
    }

    /**
     * Find all <noscript> that are located after <img> nodes, and which contain only one
     * <img> element. Replace the first image with the image from inside the <noscript> tag,
     * and remove the <noscript> tag. This improves the quality of the images we use on
     * some sites (e.g. Medium).
     *
     * @param doc the actual document to process
     **/
    open fun unwrapNoscriptImages(doc: Document) {
        // Find img without source or attributes that might contains image, and remove it.
        // This is done to prevent a placeholder img is replaced by img from noscript in next step.
        val imgs = doc.getElementsByTag("img")
        imgs.forEach {img->
            for (attr in img.attributes()) {

                if (attr.key in arrayOf("src", "srcset", "data-src", "data-srcset")) {
                    return@forEach
                }

                if (Regex("\\.(jpg|jpeg|png|webp)",RegexOption.IGNORE_CASE)
                    .containsMatchIn(attr.value)) {
                    return@forEach
                }

            }

            img.remove()
        }

        // Next find noscript and try to extract its image
        val noscripts = doc.getElementsByTag("noscript")
        noscripts.forEach { noscript->
            // Parse content of noscript and make sure it only contains image
            if (!isSingleImage(noscript)) {
                return@forEach
            }
            val tmp = doc.createElement("div")
            // We're running in the document context, and using unmodified
            // document contents, so doing this should be safe.
            // (Also we heavily discourage people from allowing script to
            // run at all in this document...)
            tmp.html(noscript.html())

            // If noscript has previous sibling and it only contains image,
            // replace it with noscript content. However we also keep old
            // attributes that might contains image.
            val prevElement = noscript.previousElementSibling()
            if (prevElement!=null && isSingleImage(prevElement)) {
                var prevImg = prevElement
                if (prevImg.tagName() != "img") {
                    prevImg = prevElement.getElementsByTag("img")[0]
                }
                prevImg?.let {
                    val newImg = tmp.getElementsByTag("img")[0]
                    for (attr in prevImg.attributes()) {
                        if (attr.value == "") {
                            continue
                        }

                        if (
                            attr.key == "src" ||
                            attr.key == "srcset" ||
                            Regex(
                                "\\.(jpg|jpeg|png|webp)",
                                RegexOption.IGNORE_CASE
                            ).containsMatchIn(attr.value)
                        ) {
                            if (newImg.attr(attr.key) == attr.value) {
                                continue
                            }

                            var attrName = attr.key
                            if (newImg.attr(attrName).isNotEmpty()) {
                                attrName = "data-old-$attrName"
                            }

                            newImg.attr(attrName, attr.value)
                        }
                    }
                }
                // https://developer.mozilla.org/en-US/docs/Web/API/Node/replaceChild#oldChild
                tmp.firstElementChild()?.let { prevElement.replaceWith(it) }
            }
        }
    }

    /*protected open fun shouldKeepImageInNoscriptElement(document: Document, noscript: Element): Boolean {
        val images = noscript.select("img")
        if(images.size > 0) {
            val imagesToKeep = ArrayList(images)

            images.forEach { image ->
                // thanks to swuqi (https://github.com/swuqi) for reporting this bug.
                // see https://github.com/dankito/Readability4J/issues/4
                val source = image.attr("src")
                if(source.isNotBlank() && document.select("img[src=$source]").size > 0) {
                    imagesToKeep.remove(image)
                }
            }

            return imagesToKeep.size > 0
        }

        return false
    }

    protected open fun removeStyles(document: Document) {
        removeNodes(document, "style")
    }

    protected open fun removeForms(document: Document) {
        removeNodes(document, "form")
    }

    protected open fun removeComments(node: Node) {
        var i = 0
        while (i < node.childNodeSize()) {
            val child = node.childNode(i)
            if(child.nodeName() == "#comment") {
                printAndRemove(child, "removeComments")
            }
            else {
                removeComments(child)
                i++
            }
        }
    }*/


    /**
     * Replaces 2 or more successive <br> elements with a single <p>.
     * Whitespace between <br> elements are ignored. For example:
     *   <div>foo<br>bar<br> <br><br>abc</div>
     * will become:
     *   <div>foo<br>bar<p>abc</p></div>
     */
    private fun replaceBrs(document: Document) { //removed RegExUtil because it uses the local one anyways
        document.body().select("br").forEach { br ->
            var next: Node? = br.nextSibling()

            // Whether 2 or more <br> elements have been found and replaced with a
            // <p> block.
            var replaced = false

            // If we find a <br> chain, remove the <br>s until we hit another element
            // or non-whitespace. This leaves behind the first <br> in the chain
            // (which will be replaced with a <p> later).
            next = nextNode(next)
            while(next is Element && next.tagName() == "br") {
                replaced = true
                val brSibling = next.nextSibling()
                next.remove()
                next = nextNode(brSibling)
            }

            // If we removed a <br> chain, replace the remaining <br> with a <p>. Add
            // all sibling nodes as children of the <p> until we hit another <br>
            // chain.
            if(replaced) {
                val p = document.createElement("p")

                br.replaceWith(p)

                next = p.nextSibling()
                while(next is Element) {
                    // If we've hit another <br><br>, we're done adding children to this <p>.
                    if(next.nodeName() == "br") {
                        val nextElem = this.nextNode(next)
                        if(nextElem is Element && nextElem.tagName() == "br")
                            break
                    }

                    if (!isPhrasingContent(next)) {
                        break
                    }

                    // Otherwise, make this node a child of the new <p>.
                    val sibling = next.nextSibling()
                    p.appendChild(next)
                    next = sibling
                }

                while (p.lastChild() != null && p.lastChild()?.let { isWhitespace(it) } == true ) {
                    p.lastChild()?.remove()
                }

                if (p.parentNode()?.nodeName() == "p") {
                    p.parentNode()?.let { parent-> (parent as Element).tagName("div") }
                }
            }
        }
    }

}
