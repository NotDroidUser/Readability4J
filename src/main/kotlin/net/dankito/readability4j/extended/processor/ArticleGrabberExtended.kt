package net.dankito.readability4j.extended.processor

import net.dankito.readability4j.extended.util.BaseRegexUtilExtended
import net.dankito.readability4j.model.ReadabilityOptions
import net.dankito.readability4j.processor.ArticleGrabber

open class ArticleGrabberExtended @JvmOverloads constructor(options: ReadabilityOptions, protected val regExExtended: BaseRegexUtilExtended) : ArticleGrabber(options, regExExtended) {

    /*
    todo do better implementation because

     override fun shouldKeepSibling(sibling: Element): Boolean {
        return super.shouldKeepSibling(sibling) || containsImageToKeep(sibling)
    }

    protected open fun containsImageToKeep(element: Element): Boolean {
        val images = element.select("img")
        if(images.size > 0) {
            if(isImageElementToKeep(element)) {
                images.forEach { image ->
                    if(!isImageElementToKeep(image)) {
                        return false
                    }
                }

                return true
            }
        }

        return false
    }

    protected open fun isImageElementToKeep(element: Element): Boolean {
        val matchString = element.id() + " " + element.className()

        return regExExtended.keepImage(matchString)
    }*/

}
