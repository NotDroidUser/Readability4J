package net.dankito.readability4j.processor

import net.dankito.readability4j.model.ArticleGrabberOptions
import net.dankito.readability4j.model.ArticleMetadata
import net.dankito.readability4j.model.ReadabilityObject
import net.dankito.readability4j.model.ReadabilityOptions
import net.dankito.readability4j.util.BaseRegexUtil
import net.dankito.readability4j.util.log
import net.dankito.readability4j.util.logDebug
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.slf4j.LoggerFactory
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min


open class ArticleGrabber(options: ReadabilityOptions, override val regex: BaseRegexUtil = BaseRegexUtil()) : ProcessorBase() {

    companion object {
        // Element tags to score by default.
        val DEFAULT_TAGS_TO_SCORE = listOf("section", "h2", "h3", "h4", "h5", "h6", "p", "td", "pre")

        val DIV_TO_P_ELEMS = listOf("blockquote", "dl", "div", "img", "ol", "p", "pre", "table", "ul")
        val UNLIKELY_ROLES = listOf("menu", "menubar", "complementary", "navigation", "alert", "alertdialog", "dialog")
        val ALTER_TO_DIV_EXCEPTIONS = listOf("div", "article", "section", "p", "ol", "ul")
        val PRESENTATIONAL_ATTRIBUTES = listOf("align", "background", "bgcolor", "border",
            "cellpadding", "cellspacing", "frame", "hspace", "rules", "style", "valign",
            "vspace")
        val DEPRECATED_SIZE_ATTRIBUTE_ELEMS = listOf("table", "th", "td", "hr", "pre")
        // The commented out elements qualify as phrasing content but tend to be
        // removed by readability when put into paragraphs, so we ignore them here.
        // "CANVAS", "IFRAME", "SVG", "VIDEO",
        val PHRASING_ELEMS = listOf("abbr", "audio", "b", "bdo", "br", "button", "cite", "code", "data", "datalist", "dfn", "em", "embed", "i", "img", "input", "kbd", "label", "mark", "math", "meter", "noscript", "object", "output", "progress", "q", "ruby", "samp", "script", "select", "small", "span", "strong", "sub", "sup", "textarea", "time", "var", "wbr")
        private val log = LoggerFactory.getLogger(ArticleGrabber::class.java)
    }


    var articleLang: String?= null
        protected set

    var articleTitle: String? = null
        private set

    var articleByline: String? = null
        protected set

    var articleDir: String? = null
        protected set


    private val nbTopCandidates = options.nbTopCandidates
    private val charThreshold = options.charThreshold

    private val readabilityObjects = HashMap<Element, ReadabilityObject>()

    private val readabilityDataTable = HashMap<Element, Boolean>()

    //changed to global because this class is always reinstated
    private val options: ArticleGrabberOptions = ArticleGrabberOptions()
    //changed to global because inside while is always reinstated and has no sense
    private val attempts = arrayListOf<Pair<Element, Int>>()

    open fun grabArticle(doc: Document, metadata: ArticleMetadata, pageElement: Element? = null): Element? {
        log.info("**** grabArticle ****")

        val isPaging = pageElement != null
        val page = pageElement ?: doc.body()

        articleTitle=metadata.title
        articleByline=metadata.byline

        // We can't grab an article if we don't have a page!
        if(page == null) {
            log.info("No body found in document. Abort.")
            return null
        }

        val pageCacheHtml = page.html()

        while(true) {
            log.info("Starting grabArticle loop")
            // First, node prepping. Trash nodes that look cruddy (like ones with the
            // class name "comment", etc), and turn divs into P tags where they have been
            // used inappropriately (as in, where they contain no other block level elements.)
            val elementsToScore = prepareNodes(doc)

            /**
             * Loop through all paragraphs, and assign a score to them based on how content-y they look.
             * Then add their score to their parent node.
             *
             * A score is determined by things like number of commas, class names, etc. Maybe eventually link density.
             **/
            //cnn testcase problem starts here
            val candidates = scoreElements(elementsToScore)

            // After we've calculated scores, loop through all of the possible
            // candidate nodes we found and find the one with the highest score.
            val topCandidateResult = getTopCandidate(page, candidates)
            val topCandidate = topCandidateResult.first
            val neededToCreateTopCandidate= topCandidateResult.second

            // Now that we have the top candidate, look through its siblings for content
            // that might also be related. Things like preambles, content split by ads
            // that we removed, etc.
            var articleContent = createArticleContent(doc, topCandidate, isPaging)


            log.info("Article content pre-prep: {}", articleContent.html().logDebug())
            // So we have all of the content that we need. Now we clean it up for presentation.
            // ok all the bugs left are here or that looks like
            prepArticle(articleContent)
            log.info("Article content post-prep: {}", articleContent.html().logDebug())

            if(neededToCreateTopCandidate) {
                // We already created a fake div thing, and there wouldn't have been any siblings left
                // for the previous loop, so there's no point trying to create a new div, and then
                // move all the children over. Just assign IDs and class names here. No need to append
                // because that already happened anyway.
                topCandidate.id( "readability-page-1")
                topCandidate.classNames(setOf("page"))
            }
            else {
                val div = doc.createElement("div")
                div.id("readability-page-1")
                div.classNames(setOf("page"))

                div.appendChildren(articleContent.childNodes())

                articleContent.appendChild(div)
            }

            log.info("Article content after paging: {}", articleContent.html().logDebug())

            var parseSuccessful = true

            // Now that we've gone through the full algorithm, check to see if
            // we got any meaningful content. If we didn't, we may need to re-run
            // grabArticle with different flags set. This gives us a higher likelihood of
            // finding the content, and the sieve approach gives us a higher likelihood of
            // finding the -right- content.
            val textLength = getInnerText(articleContent, true).length
            if(textLength < this.charThreshold) {
                parseSuccessful = false
                page.html(pageCacheHtml)
                attempts.add(Pair(articleContent, textLength))

                if(options.stripUnlikelyCandidates) {
                    options.stripUnlikelyCandidates = false
                }
                else if(options.weightClasses) {
                    options.weightClasses = false
                }
                else if(options.cleanConditionally) {
                    options.cleanConditionally = false
                }
                else {
                    // No luck after removing flags, just return the longest text we found during the different loops
                    attempts.sortBy { it.second }

                    // But first check if we actually have something
                    if (attempts.isEmpty() || attempts[0].second <= 0) {
                        return null
                    }

                    articleContent = attempts[0].first
                    parseSuccessful = true
                }
            }

            if(parseSuccessful) {
                // Find out text direction from ancestors of final top candidate.
                getTextDirection(topCandidate, doc)

                return articleContent
            }
        }
    }


    /*             First step: prepare nodes           */

    private fun prepareNodes(doc: Document): List<Element> {
        val elementsToScore = ArrayList<Element>()
        var node: Element? = doc
        var shouldRemoveTitleHeader = true

        while(node != null) {
            if (node.tagName() == "html"){
                if (node.hasAttr("lang")) {
                    articleLang = node.attr("lang")
                }
            }

            val matchString = node.className() + " " + node.id()

            // Check if node is visible or no (who knows if you don't have the full page)
            if(!isProbablyVisible(node)) {
                log.info("Removing hidden node {}", matchString)
                node = removeAndGetNext(node)
                continue
            }

            // User is not able to see elements applied with both "aria-modal = true" and "role = dialog"
            if (
                node.attr("aria-modal") == "true" &&
                node.attr("role") == "dialog"
            ) {
                node = removeAndGetNext(node)
                continue
            }

            // If we don't have a byline yet check to see if this node is a byline;
            // if it is store the byline and remove the node.
            if(articleByline == null && isValidByline(node, matchString)) {

                // Find child node matching [itemprop="name"] and use that if it exists for a more accurate author name byline
                val endOfSearchMarkerNode = getNextNode(node, true)
                var itemPropNameNode:Element? = null
                var next = getNextNode(node)
                while (next!=null && next != endOfSearchMarkerNode) {
                    val itemprop = next.attr("itemprop")
                    if (itemprop.isNotEmpty() && itemprop.contains("name")) {
                        itemPropNameNode = next
                        break
                    } else {
                        next = getNextNode(next)
                    }
                }
                articleByline = (itemPropNameNode?:node).text().trim()
                node = removeAndGetNext(node)
                continue
            }

            if ( shouldRemoveTitleHeader  && headerDuplicatesTitle(node)) {
                log.info(
                    "Removing header: {} {}",
                    node.text().trim(),
                    this.articleTitle?.trim()
                )
                shouldRemoveTitleHeader = false
                node = removeAndGetNext(node)
                continue
            }

            // Remove unlikely candidates
            if(options.stripUnlikelyCandidates) {
                if(regex.isUnlikelyCandidate(matchString) &&
                    !regex.okMaybeItsACandidate(matchString) &&
                    !hasAncestorTag(node, "table") &&
                    !hasAncestorTag(node, "code") &&
                    node.tagName() !in listOf("body", "a")) {
                    log.info("Removing unlikely candidate - {}", matchString)

                    node = this.removeAndGetNext(node)
                    continue
                }

                if (node.attr("role") in UNLIKELY_ROLES){
                    log.info(
                        "Removing content with role {}{}{}",
                            node.attr("role"),
                            " - ",
                            matchString
                    )
                    node = removeAndGetNext(node)
                    continue
                }
            }


            // Remove DIV, SECTION, and HEADER nodes without any content(e.g. text, image, video, or iframe).
            if((node.tagName() in
                    listOf("div","section","header","h1","h2","h3","h4","h5","h6"))
                && this.isElementWithoutContent(node)) {
                node = this.removeAndGetNext(node)
                continue
            }

            if(node.tagName() in DEFAULT_TAGS_TO_SCORE) {
                elementsToScore.add(node)
            }

            // Turn all divs that don't have children block level elements into p's
            if(node.tagName() == "div") {
                // Put phrasing content into paragraphs.
                var childNode = node.firstChild()
                while (childNode!=null) {
                    var nextSibling = childNode.nextSibling()
                    if (isPhrasingContent(childNode)) {
                        val fragment=Element("div")
                        // Collect all consecutive phrasing content into a fragment.
                        do {
                            nextSibling = childNode!!.nextSibling()
                            fragment.appendChild(childNode);
                            childNode = nextSibling;
                        } while (childNode!=null && isPhrasingContent(childNode))
                        
                        // Trim leading and trailing whitespace from the fragment.
                        while (
                            fragment.firstChild()!=null &&
                            isWhitespace(fragment.firstChild()!!)
                        ) {
                            fragment.firstChild()?.remove();
                        }
                        var lastChild = fragment.lastChild()
                        while (
                            fragment.lastChild()!=null &&
                            isWhitespace(fragment.lastChild()!!)
                        ) {
                            fragment.lastChild()?.remove();
                        }
                        
                        // If the fragment contains anything, wrap it in a paragraph and
                        // insert it before the next non-phrasing node.
                        if (!regex.isWhitespace(fragment.html())) {
                            val p = doc.createElement("p");
                            p.appendChildren(fragment.childNodes());
                            node.insertChildren(nextSibling?.siblingIndex()?:-1,p)
                        }
                    }
                    childNode = nextSibling
                }

                // Sites like http://mobile.slate.com encloses each paragraph with a DIV
                // element. DIVs with only a P element inside and no text content can be
                // safely converted into plain P elements to avoid confusing the scoring
                // algorithm with DIVs with are, in practice, paragraphs.
                if(hasSingleTagInsideElement(node,"p") &&
                    getLinkDensity(node) < 0.25) {
                    val newNode = node.child(0)
                    node.replaceWith(newNode)
                    node = newNode
                    elementsToScore.add(node)
                }
                else if(!this.hasChildBlockElement(node)) {
                    setNodeTag(node, "p")
                    elementsToScore.add(node)
                }
            }
            node = this.getNextNode(node)
        }

        return elementsToScore
    }



    private fun headerDuplicatesTitle(node: Element): Boolean {
        if (articleTitle==null)
            return false
        if (!node.tagName().equals("H1",ignoreCase = true) &&
            !node.tagName().equals("H2",ignoreCase = true)) {
            return false
        }
        val heading = node.text()
        log.info("Evaluating similarity of header: {} {}", heading, articleTitle)
        return this.textSimilarity(articleTitle!!, heading) > 0.75
    }



    private fun isProbablyVisible(node: Element): Boolean {
        return(!node.hasAttr("style") ||
            !node.attr("style")
                .contains(Regex("(display(\\s*)?:(\\s*)?none)|(visibility(\\s*)?:(\\s*)?hidden)"))) &&
            !node.hasAttr("hidden") &&
            (!node.hasAttr("aria-hidden") ||
            (!node.attr("aria-hidden").contains("true") && node.attr("aria-hidden").isNotBlank()) ||
            (node.className().isNotEmpty() &&
                node.className().contains("fallback-image")))
    }


//    protected open fun checkAndSaveByline(node: Element, matchString: String): Boolean {
//
//        return true
//    }

    /**
     * Check whether the input string could be a byline.
     * This verifies that the input is a string, and that the length
     * is less than 100 chars.
     */
    private fun isValidByline(node: Element, matchString: String): Boolean {
        val rel = node.attr("rel")
        val itemprop = node.attr("itemprop")
        val bylineLength = node.wholeText().trim().length

        return ((rel == "author" || itemprop.contains("author")
            || regex.isByline(matchString)) && bylineLength in 1 until 100)
    }


    /**
     * Determine whether element has any children block level elements.
     */
    private fun hasChildBlockElement(element: Element): Boolean {
        return element.children().any { node ->
            node.tagName() in DIV_TO_P_ELEMS || hasChildBlockElement(node)
        }
    }

    private fun setNodeTag(node: Element, tagName: String): Element {
        log.info("setNodeTag {} {}", node.log(), tagName)
        node.tagName(tagName)
        return node
    }


    /*          Second step: Score elements             */

    private fun scoreElements(elementsToScore: List<Element>): List<Element> {
        val candidates = ArrayList<Element>()
        val candidateLog = false
        elementsToScore.forEach { elementToScore ->
            if(elementToScore.parentNode() == null) {
                return@forEach
            }

            // If this paragraph is less than 25 characters, don't even count it.
            val innerText = this.getInnerText(elementToScore)
            if(innerText.length < 25) {
                return@forEach
            }

            // Exclude nodes with no ancestor.
            val ancestors = this.getNodeAncestors(elementToScore, 5)
            if(ancestors.isEmpty()) {
                return@forEach
            }

            var contentScore:Double = 0.0

            // Add a point for the paragraph itself as a base.
            contentScore += 1

            // Add points for any commas within this paragraph.
            contentScore += regex.splitCommas(innerText).size

            // For every 100 characters in this paragraph, add another point. Up to 3 points.
            contentScore += min(floor(innerText.length / 100.0),3.0)

            // Initialize and score ancestors.
            for((level,ancestor) in ancestors.withIndex()) {
                if(ancestor.tagName().isNullOrBlank() && ancestor.parentNode() !=null) {
                    break
                }

                if(ancestor is Document|| ancestor.normalName()=="html") {
                    break
                }

                if (ancestor.id()=="storycontent" && !candidateLog){
                    ancestors.withIndex().forEach { log.info("ancestor level {} {}", it.index, it.value.log()) }
                }

                if(ancestor.readability == null) {
                    initializeNode(ancestor)
                    candidates.add(ancestor)
                }

                // Node score divider:
                // - parent:             1 (no division)
                // - grandparent:        2
                // - great grandparent+: ancestor level * 3
                val scoreDivider:Double =
                    when (level) {
                      0 -> 1.0
                      1 -> 2.0
                      else -> level * 3.0
                    }

                ancestor.readability!!.contentScore += contentScore / scoreDivider
            }
        }

        return candidates
    }

    /**
     * Initialize a node with the readability object. Also checks the
     * className/id for special names to add to its score.
     */
    private fun initializeNode(node: Element) {
        val readability = ReadabilityObject(0.0)

        when(node.tagName()) {
            "div" ->
                readability.contentScore += 5

            "pre",
            "td",
            "blockquote" ->
                readability.contentScore += 3

            "address",
            "ol",
            "ul",
            "dl",
            "dd",
            "dt",
            "li",
            "form" ->
                readability.contentScore -= 3

            "h1",
            "h2",
            "h3",
            "h4",
            "h5",
            "h6",
            "th" ->
                readability.contentScore -= 5
        }

        readability.contentScore += getClassWeight(node)

        node.readability = readability
    }

    /**
     * Get an elements class/id weight. Uses regular expressions to tell if this
     * element looks good or bad.
     */
    private fun getClassWeight(e: Element): Int {
        if(!options.weightClasses) {
            return 0
        }

        var weight = 0

        // Look for a special classname
        if(e.className().isNotBlank()) {
            if(regex.isNegative(e.className())) {
                weight -= 25
            }

            if(regex.isPositive(e.className())) {
                weight += 25
            }
        }

        // Look for a special ID
        if(e.id().isNotBlank()) {
            if(regex.isNegative(e.id())) {
                weight -= 25
            }

            if(regex.isPositive(e.id())) {
                weight += 25
            }
        }

        return weight
    }

    private fun getNodeAncestors(node: Element, maxDepth: Int = 0): List<Element> {
        var i = 0
        val ancestors = ArrayList<Element>()
        var next = node

        while(next.parent() != null) {
            val parentNode = next.parent()!!
            ancestors.add(parentNode)
            if((maxDepth>=1 && ++i == maxDepth)) {
                break
            }
            next = parentNode
        }

        return ancestors
    }



    /*          Third step: Get top candidate           */
    
    private fun getTopCandidate(page: Element, candidates: List<Element>): Pair<Element, Boolean> {
        val topCandidates = ArrayList<Element>()

        candidates.forEach { candidate ->
            candidate.readability?.let { readability ->
                // Scale the final candidates score based on link density. Good content
                // should have a relatively small link density (5% or less) and be mostly
                // unaffected by this operation.
                val ld =this.getLinkDensity(candidate)
//                log.info("Before ld score: {}",candidate.readability?.contentScore)

                val candidateScore = readability.contentScore * (1 - ld)
                candidate.readability?.contentScore = candidateScore

                log.info("Candidate:\",\"{}\",\"with score {}\"]", candidate.log(), candidateScore)

                for(t in 0..<nbTopCandidates) {
                    val aTopCandidate = if(topCandidates.size > t) topCandidates[t] else null
                    val topCandidateReadability = aTopCandidate?.readability

                    if(aTopCandidate == null || (topCandidateReadability != null && candidateScore > topCandidateReadability.contentScore)) {
                        topCandidates.add(t, candidate)
                        if(topCandidates.size > this.nbTopCandidates) {
                            topCandidates.removeAt(nbTopCandidates)
                        }
                        break
                    }
                }
            }
        }

        var topCandidate = if(topCandidates.size > 0) topCandidates[0] else null
        var parentOfTopCandidate: Element?

        // If we still have no top candidate, just use the body as a last resort.
        // We also have to copy the body node so it is something we can modify.
        if(topCandidate == null || topCandidate.tagName() == "body") {
            // Move all of the page's children into topCandidate
            topCandidate = Element("div")
            // Move everything (not just elements, also text nodes etc.) into the container
            // so we even include text directly in the body:
            while(page.firstChild()!=null){
//                if (child is Comment){
//                    //javascript ignores it
//                    return@forEach
//                }
                log.info("Moving child out: {}", page.firstChild()?.log())
                page.firstChild()?.let { topCandidate!!.appendChild(it) }
            }

            page.appendChild(topCandidate)

            initializeNode(topCandidate)

            return Pair(topCandidate, true)
        }
        else {
            // Find a better top candidate node if it contains (at least three) nodes which belong to `topCandidates` array
            // and whose scores are quite closed with current `topCandidate` node.
            val alternativeCandidateAncestors = ArrayList<List<Element>>()

            for (otherTopCandidate in topCandidates.filter { it != topCandidate }) {
                topCandidate.readability?.let { topCandidateReadability ->
                    otherTopCandidate.readability?.let {otherTopCandidateReadability->
                        if (((otherTopCandidateReadability.contentScore) /
                                topCandidateReadability.contentScore) >= 0.75
                        ) {
                            alternativeCandidateAncestors.add(
                                this.getNodeAncestors(
                                    otherTopCandidate
                                )
                            )
                        }
                    }
                }
            }

            val MINIMUM_TOPCANDIDATES = 3

            if(alternativeCandidateAncestors.size >= MINIMUM_TOPCANDIDATES) {
                parentOfTopCandidate = topCandidate.parent()
                while(parentOfTopCandidate != null && parentOfTopCandidate.tagName() != "body") {
                    var listsContainingThisAncestor = 0
                    var ancestorIndex = 0
                    while(ancestorIndex < alternativeCandidateAncestors.size &&
                        listsContainingThisAncestor < MINIMUM_TOPCANDIDATES) {

                        if(alternativeCandidateAncestors[ancestorIndex].
                            contains(parentOfTopCandidate)) {
                            listsContainingThisAncestor++
                        }
                        ancestorIndex++
                    }

                    if(listsContainingThisAncestor >= MINIMUM_TOPCANDIDATES) {
                        topCandidate = parentOfTopCandidate
                        break
                    }
                    parentOfTopCandidate = parentOfTopCandidate.parent()
                }
            }


            if(topCandidate!!.readability == null) {
                initializeNode(topCandidate)
            }

            // Because of our bonus system, parents of candidates might have scores
            // themselves. They get half of the node. There won't be nodes with higher
            // scores than our topCandidate, but if we see the score going *up* in the first
            // few steps up the tree, that's a decent sign that there might be more content
            // lurking in other places that we want to unify in. The sibling stuff
            // below does some of that - but only if we've looked high enough up the DOM
            // tree.
            parentOfTopCandidate = topCandidate.parent()
            var lastScore = topCandidate.readability?.contentScore!!
            // The scores shouldn't get too low.
            val scoreThreshold = lastScore / 3.0

            while(parentOfTopCandidate != null && parentOfTopCandidate.tagName() != "body") {
                val parentOfTopCandidateReadability = parentOfTopCandidate.readability
                if(parentOfTopCandidateReadability == null) {
                    parentOfTopCandidate = parentOfTopCandidate.parent()
                    continue
                }

                val parentScore = parentOfTopCandidateReadability.contentScore
                if(parentScore < scoreThreshold) {
                    break
                }
                if(parentScore > lastScore) {
                    // Alright! We found a better parent to use.
                    topCandidate = parentOfTopCandidate
                    break
                }

                lastScore = parentOfTopCandidateReadability.contentScore
                parentOfTopCandidate = parentOfTopCandidate.parent()
            }

            // If the top candidate is the only child, use parent instead. This will help sibling
            // joining logic when adjacent content is actually located in parent's sibling node.
            parentOfTopCandidate = topCandidate!!.parent()
            while(parentOfTopCandidate != null && parentOfTopCandidate.tagName() != "body" && parentOfTopCandidate.children().size == 1) {
                topCandidate = parentOfTopCandidate
                parentOfTopCandidate = topCandidate.parent()
            }

            if(topCandidate!!.readability == null) {
                initializeNode(topCandidate)
            }

            return Pair(topCandidate, false)
        }
    }

    /**
     * Get the density of links as a percentage of the content
     * This is the amount of text that is inside a link divided by the total text in the node.
     */
    private fun getLinkDensity(element: Element): Double {
        val textLength = this.getInnerText(element).length
        if(textLength == 0) {
            return 0.0
        }

        var linkLength = 0.0

        // XXX implement _reduceNodeList?
        element.getElementsByTag("a").forEach { linkNode ->
            val href = linkNode.attr("href")
            val coefficient = if (href.isNotBlank() && regex.isHashUrl(href)) 0.3 else 1.0
            linkLength += this.getInnerText(linkNode).length.toDouble() * coefficient
        }

        return linkLength / textLength.toDouble()
    }



    /*          Forth step: Create articleContent           */

    private fun createArticleContent(doc: Document, topCandidate: Element, isPaging: Boolean): Element {
        val articleContent = doc.createElement("div")
        if(isPaging) {
            articleContent.attr("id", "readability-content")
        }

        val topCandidateReadability = topCandidate.readability ?: return articleContent

        val siblingScoreThreshold = max(10.0, topCandidateReadability.contentScore * 0.2)
        // Keep potential top candidate's parent node to try to get text direction of it later.
        // parentOfTopCandidate may is null, see issue #12
        topCandidate.parent()?.let {parentOfTopCandidate->
            val siblings = parentOfTopCandidate.children()

            ArrayList(siblings).forEach { sibling -> // make a copy of children as the may get modified below -> we can get rid of s -= 1 sl -= 1 compared to original source
                // make a copy of children as the may get modified below -> we can get rid of s -= 1 sl -= 1 compared to original source
                val siblingReadability = sibling.readability
                var append = false

                log.info(
                    "Looking at sibling node: {} with score {}",
                    sibling.log(),
                    siblingReadability?.contentScore ?: "Unknown"
                )

                log.info(
                    "Sibling has score {}",
                    siblingReadability?.contentScore ?: "Unknown"
                )

                if (sibling == topCandidate) {
                    append = true
                }
                else {
                    var contentBonus = 0.0

                    // Give a bonus if sibling nodes and top candidates have the example same classname
                    if (sibling.className() == topCandidate.className() &&
                        topCandidate.className() != "") {
                        contentBonus += topCandidateReadability.contentScore * 0.2
                    }

                    if (siblingReadability != null &&
                        ((siblingReadability.contentScore + contentBonus) >=
                            siblingScoreThreshold)
                    ) {
                        append = true
                    } else if (shouldKeepSibling(sibling)) {
                        val linkDensity = this.getLinkDensity(sibling)
                        val nodeContent = this.getInnerText(sibling)
                        val nodeLength = nodeContent.length

                        if (nodeLength > 80 && linkDensity < 0.25) {
                            append = true
                        } else if (
                            nodeLength in 1..79 &&
                            linkDensity == 0.0 &&
                            nodeContent.contains("\\.( |$)".toRegex())
                        ) {
                            append = true
                        }
                    }
                }

                if (append) {
                    log.info("Appending node: {}", sibling.log())

                    if (sibling.tagName() !in ALTER_TO_DIV_EXCEPTIONS) {
                        // We have a node that isn't a common block level element, like a form or td tag.
                        // Turn it into a div so it doesn't get filtered out later by accident.
                        log.info("Altering sibling: {} to div.", sibling.log())

                        setNodeTag(sibling, "div")
                    }

                    articleContent.appendChild(sibling)
                }
            }
        }
        return articleContent
    }

    open fun shouldKeepSibling(sibling: Element): Boolean {
        return sibling.tagName() == "p"
    }



    /*          Fifth step: Prepare article            */

    /**
     * Prepare the article node for display. Clean out any inline styles,
     * iframes, forms, strip extraneous <p> tags, etc.
     */
    private fun prepArticle(articleContent: Element) {
        //removed metadata as it isn't used anymore
        cleanStyles(articleContent)

        // Check for data tables before we continue, to avoid removing items in
        // those tables, which will often be isolated even though they're
        // visually linked to other content-ful elements (text, images, etc.).
        markDataTables(articleContent)

        fixLazyImages(articleContent)

        // Clean out junk from the article content
        this.cleanConditionally(articleContent, "form")
        this.cleanConditionally(articleContent, "fieldset")
        this.clean(articleContent, "object")
        this.clean(articleContent, "embed")
        this.clean(articleContent, "footer")
        this.clean(articleContent, "link")
        this.clean(articleContent, "aside")

        // Clean out elements have "share" in their id/class combinations from final top candidates,
        // which means we don't remove the top candidates even they have "share".

        val shareElementThreshold = ReadabilityOptions.DEFAULT_CHAR_THRESHOLD

        articleContent.children().forEach { topCandidate ->
            cleanMatchedNodes(topCandidate){ node, className->
                regex.isShareElement(className) &&
                node.text().length < shareElementThreshold
            }
        }

        this.clean(articleContent, "iframe")
        this.clean(articleContent, "input")
        this.clean(articleContent, "textarea")
        this.clean(articleContent, "select")
        this.clean(articleContent, "button")
        this.cleanHeaders(articleContent)

        // Do these last as the previous stuff may have removed junk
        // that will affect these
        this.cleanConditionally(articleContent, "table")
        this.cleanConditionally(articleContent, "ul")
        this.cleanConditionally(articleContent, "div")

        // replace H1 with H2 as H1 should be only title that is displayed separately
        this.replaceNodeTags(articleContent.getElementsByTag("h1"), "h2")

        // Remove extra paragraphs
        removeNodes(articleContent.getElementsByTag("p")) { paragraph ->
            // At this point, nasty iframes have been removed; only embedded video
            // ones remain.
            val contentElementCount = paragraph.getAllNodesWithTag(arrayOf(
                "img",
                "embed",
                "object",
                "iframe"
            )).size

            return@removeNodes contentElementCount == 0 && getInnerText(
                paragraph,
                normalizeSpaces = false
            ).isEmpty()
        }

        articleContent.select("br").forEach { br ->
            val next = nextNode(br.nextSibling())
            if(next != null && next is Element && next.tagName() == "p") {
                br.remove()
            }
        }
        // Remove single-cell tables
        articleContent.getElementsByTag("table").forEach { table ->
            val tbody = if (this.hasSingleTagInsideElement(table, "tbody"))
                table.firstElementChild()
            else table
            if (tbody?.let { this.hasSingleTagInsideElement(it, "tr") } == true) {
                val row = tbody.firstElementChild()
                if (row?.let { hasSingleTagInsideElement(it, "td") } == true) {
                    row.firstElementChild()?.let { cell ->
                        cell.tagName(if (cell.children().all { isPhrasingContent(it) }) "p" else "div")
                        table.replaceWith(cell)
                    }
                }
            }
        }
    }

    /* convert images and figures that have properties like data-src into images that can be loaded without JS */
    private fun fixLazyImages(root: Element) {

        root.getAllNodesWithTag(arrayOf("img","picture","figure"))
            .forEach function@ { elem ->

            // In some sites (e.g. Kotaku), they put 1px square image as base64 data uri in the src attribute.
            // So, here we check if the data uri is too short, just might as well remove it.
            var attributes = elem.attributes().toList()
            if (elem.attr("src").isNotBlank() && regex.isB64Data(elem.attr("src"))) {
                // Make sure it's not SVG, because SVG can have a meaningful image in under 133 bytes.
                val parts = regex.getB64Matches(elem.attr("src"))
                val dataType = parts?.groups?.get(1)?.value
                if ( dataType == "image/svg+xml") {
                    return@function
                }

                // Make sure this element has other attributes which contains image.
                // If it doesn't, then this src is important and shouldn't be removed.
                var srcCouldBeRemoved = false
                attributes.forEach { attr->
                    if (!srcCouldBeRemoved && (attr.key != "src") &&
                        "\\.(jpg|jpeg|png|webp)"
                        .toRegex(RegexOption.IGNORE_CASE)
                        .containsMatchIn(attr.value)) {
                        srcCouldBeRemoved = true
                    }
                }

                // Here we assume if image is less than 100 bytes (or 133 after encoded to base64)
                // it will be too small, therefore it might be placeholder image.
                if (srcCouldBeRemoved) {
                    //if you get there this isn't possible to be null
                    val b64starts = parts?.groups?.get(0)?.value?.length!!
                    val b64length = elem.attr("src").length - b64starts
                    if (b64length < 133) {
                        elem.removeAttr("src")
                    }
                }
            }

            // also check for "null" to work around https://github.com/jsdom/jsdom/issues/2580
            // but this one only applies to js
            if (
                (elem.attr("src").isNotBlank() || elem.attr("srcset").isNotBlank()) &&
                "lazy" !in elem.className().lowercase()
            ) {
                return@function
            }
            attributes=elem.attributes().toList()
            attributes.forEach attrs@{ attr->
                if (
                    attr.key == "src" ||
                    attr.key == "srcset" ||
                    attr.key == "alt"
                ) {
                    return@attrs
                }
                var copyTo:String? = null
                if ( "\\.(jpg|jpeg|png|webp)\\s+\\d".toRegex().containsMatchIn(attr.value)) {
                    copyTo = "srcset"
                } else if ("^\\s*\\S+\\.(jpg|jpeg|png|webp)\\S*\\s*$".toRegex().containsMatchIn(attr.value)) {
                    copyTo = "src"
                }
                if (copyTo!=null) {
                    //if this is an img or picture, set the attribute directly
                    if (elem.tagName() == "img" || elem.tagName() == "picture") {
                        elem.attr(copyTo, attr.value)
                    } else if (
                        elem.tagName() == "figure" &&
                        (elem.getAllNodesWithTag(arrayOf("img","picture"))).isEmpty()
                    ) {
                        //if the item is a <figure> that does not contain an image or picture, create one and place it inside the figure
                        //see the nytimes-3 testcase for an example
                        val img = Element("img")
                        img.attr(copyTo, attr.value)
                        elem.appendChild(img)
                    }
                }
            }
        }
    }

    /**
     * Remove the style attribute on every e and under.
     * TODO: Test if getElementsByTagName(*) is faster.
     */
    private fun cleanStyles(e: Element) {
        if(e.tagName() == "svg") {
            return
        }

//      Not in Readability.js
//        if(e.className() != "readability-styled") {
        // Remove `style` and deprecated presentational attributes
        PRESENTATIONAL_ATTRIBUTES.forEach { attributeName ->
            e.removeAttr(attributeName)
        }

        if(DEPRECATED_SIZE_ATTRIBUTE_ELEMS.contains(e.tagName())) {
            e.removeAttr("width")
            e.removeAttr("height")
        }
//        }

        e.children().forEach { child ->
            cleanStyles(child)
        }
    }

    /**
     * Look for 'data' (as opposed to 'layout') tables, for which we use
     * similar checks as
     * https://searchfox.org/mozilla-central/rev/f82d5c549f046cb64ce5602bfd894b7ae807c8f8/accessible/generic/TableAccessible.cpp#19
     */
    private fun markDataTables(root: Element) {
        root.getElementsByTag("table").forEach outer@ { table ->
            val role = table.attr("role")
            if(role == "presentation") {
                table._readabilityDataTable=false
                return@outer
            }
            val datatable = table.attr("datatable")
            if(datatable == "0") {
                table._readabilityDataTable=false
                return@outer
            }
            val summary = table.attr("summary")
            if(summary.isNotBlank()) {
                table._readabilityDataTable=true
                return@outer
            }

            val caption = table.getElementsByTag("caption")
            if(caption.size > 0 && caption[0].childNodeSize() > 0) {
                table._readabilityDataTable=true
                return@outer
            }


            // If the table has a descendant with any of these tags, consider a data table
            val dataTableDescendants = listOf("col", "colgroup", "tfoot", "thead", "th")
            dataTableDescendants.forEach { tag ->
                if(table.getElementsByTag(tag).size > 0) {
                    log.info("Data table because found data-y descendant")
                    table._readabilityDataTable=true
                    return@outer
                }
            }

            // Nested tables indicate a layout table:
            // Js dont look for the same element or that looks like
            if(table.getElementsByTag("table").size > 1) {
                table._readabilityDataTable= false
                return@outer
            }

            val sizeInfo = getRowAndColumnCount(table)

            if (sizeInfo.second == 1 || sizeInfo.first == 1) {
                // single colum/row tables are commonly used for page layout purposes.
                table._readabilityDataTable = false
                return@outer
            }

            if (sizeInfo.first >= 10 || sizeInfo.second > 4) {
                table._readabilityDataTable= true
                return@outer
            }

            // Now just go by size entirely:
            table._readabilityDataTable= (sizeInfo.first * sizeInfo.second > 10)
        }
    }

    /**
     * Return an object indicating how many rows and columns this table has.
     */
    private fun getRowAndColumnCount(table: Element): Pair<Int, Int> {
        var rows = 0
        var columns = 0

        table.getElementsByTag("tr").forEach { row ->
            rows +=
                row.attr("rowspan")
                  .takeIf { "^\\d.".toRegex().matches(it) }?.toInt() ?:1

            // Now look for column-related info
            var columnsInThisRow = 0
            row.getElementsByTag("td").forEach { cell ->
                columnsInThisRow += cell.attr("colspan")
                    .takeIf { "^\\d.".toRegex().matches(it) }?.toInt() ?:1
            }

            columns = max(columns, columnsInThisRow)
        }

        return Pair(rows, columns)
    }

    private fun cleanConditionally(e: Element, tag: String) {
        if(!options.cleanConditionally)
            return

        // Gather counts for other typical elements embedded within.
        // Traverse backwards so we can remove nodes at the same time
        // without effecting the traversal.
        //
        // TODO: Consider taking into account original contentScore here.
        removeNodes(e.getElementsByTag(tag)) filterFunction@ { node ->

            // First check if this node IS data table, in which case don't remove it.
            val isDataTable: (Element) -> Boolean = { it._readabilityDataTable }

            var isList = tag == "ul" || tag == "ol"

            if (!isList) {
                var listLength = 0
                val listNodes = node.getAllNodesWithTag(arrayOf("ul", "ol"))
                listNodes.forEach{ list ->
                    listLength += getInnerText(list).length
                }
                    val nodeTextLength = getInnerText(node).length
                if (nodeTextLength!=0)
                    isList = listLength / nodeTextLength > 0.9
            }


            if (tag == "table" && isDataTable(node)) {
                return@filterFunction false
            }

            // Next check if we're inside a data table, in which case don't remove it as well.
            if(hasAncestorTag(node, "table", -1, isDataTable)) {
                return@filterFunction false
            }

            if (hasAncestorTag(node, "code")) {
                return@filterFunction false
            }

            // keep element if it has a data tables
            if (node.getElementsByTag("table").any { tbl -> tbl._readabilityDataTable }) {
                return@filterFunction false
            }

            val weight = getClassWeight(node)

            log.info("Cleaning Conditionally {}", node.log())

            val contentScore = 0

            if(weight + contentScore < 0) {
                return@filterFunction true
            }

            if(getCharCount(node, ',') < 10) {
                // If there are not very many commas, and the number of
                // non-paragraph elements is more than paragraphs or other
                // ominous signs, remove the element.
                val p = node.getElementsByTag("p").size
                val img = node.getElementsByTag("img").size
                val li = node.getElementsByTag("li").size - 100
                val input = node.getElementsByTag("input").size
                val headingDensity = getTextDensity(node, arrayOf(
                    "h1",
                    "h2",
                    "h3",
                    "h4",
                    "h5",
                    "h6"
                ))

                var embedCount = 0
                node.getAllNodesWithTag(arrayOf("object", "embed", "iframe")).forEach { embed->
                    // If this embed has attribute that matches video regex, don't delete it.
                    if (embed.attributes().any { attr->
                        attr.value.let {
                            regex.hasAllowedVideo(it)
                        }
                    }){
                        return@filterFunction false
                    }
                    // For embed with <object> tag, check inner HTML as well.
                    if(embed.tagName() == "object" &&
                        regex.hasAllowedVideo(embed.html())
                    ){
                        return@filterFunction false
                    }

                    embedCount += 1
                }

                val innerText = getInnerText(node)

                // toss any node whose inner text contains nothing but suspicious words
                if (
                    regex.hasAdWords(innerText) ||
                    regex.hasLoadingWords(innerText)
                ) {
                    return@filterFunction true
                }

                val contentLength = innerText.length
                val linkDensity = getLinkDensity(node)
                val textishTags = arrayOf("span", "li", "td")+ DIV_TO_P_ELEMS
                val textDensity = getTextDensity(node,textishTags)
                val isFigureChild = hasAncestorTag(node,"figure")
                val shouldRemoveNode: () -> Boolean = {
                    val errs= arrayListOf<String>()
                    if (!isFigureChild && img > 1 && p.toDouble() / img.toDouble() < 0.5 ) {
                        errs.add("Bad p to img ratio (img=${img}, p=${p})")
                    }
                    if (!isList && li > p){
                        errs.add("Too many li's outside of a list. (li=${li} > p=${p})")
                    }
                    if(input > floor(p/3.0)){
                        errs.add("Too many inputs per p. (input=${input}, p=${p})")
                    }
                    if(!isList &&
                        !isFigureChild &&
                        headingDensity < 0.9 &&
                        contentLength < 25 &&
                        (img == 0 || img>2) &&
                        linkDensity > 0) {
                        errs.add("Suspiciously short. (headingDensity=${headingDensity}, img=${img}, linkDensity=${linkDensity})")
                    }
                    if(!isList && weight < 25 && linkDensity > 0.2){
                        errs.add("Low weight and a little linky. (linkDensity=${linkDensity})")
                    }
                    if(weight >= 25 && linkDensity > 0.5){
                        errs.add("High weight and mostly links. (linkDensity=${linkDensity})")
                    }
                    if((embedCount == 1 && contentLength < 75) || embedCount > 1){
                        errs.add("Suspicious embed. (embedCount=${embedCount}, contentLength=${contentLength})")
                    }
                    if(img == 0 && textDensity == 0.0){
                        errs.add("No useful content. (img=${img}, textDensity=${textDensity})")
                    }

                    if (errs.size>0){
                        log.info("Checks failed {}",errs.joinToString(", ","["," ]"))
                    }

                    errs.size!=0
                }

                val haveToRemove = shouldRemoveNode()

                if (isList && haveToRemove){
                    node.children().forEach {  child->
                        // Don't filter in lists with li's that contain more than one child
                        if (child.children().size > 1) {
                            @Suppress("KotlinConstantConditions")
                            // just for make it "exact" as js code for the reader
                            return@filterFunction haveToRemove
                        }
                    }
                    val liCount = node.getElementsByTag("li").size

                    // Only allow the list to remain if every li contains an image
                    if (img == liCount) {
                        return@filterFunction false
                    }
                }
                return@filterFunction haveToRemove
            }
            return@filterFunction false
        }
    }

    private fun getTextDensity(e: Element, tags: Array<String>): Double {
        val textLength = getInnerText(e,true).length
        if (textLength == 0) {
            return 0.0
        }
        var childrenLength = 0.0
        val children = e.getAllNodesWithTag(tags).filterNot { it == e }
        children.forEach{
            child ->
            childrenLength += this.getInnerText(child, true).length
        }
        return childrenLength / textLength
    }

    /**
     * Check if a given node has one of its ancestor tag name matching the
     * provided one.
     */
    private fun hasAncestorTag(node: Element, tagName: String, maxDepth: Int = 3,
                                  filterFn: ((Element) -> Boolean) = {
                                      true //bc you don't want a null exception
                                  }): Boolean {
        var parent = node
        var depth = 0

        while(parent.parent() != null) {
            if(maxDepth in 1..<depth) {
                return false
            }

            val parentNode = parent.parentNode()
            if(parentNode is Element && parentNode.tagName() == tagName &&
                filterFn(parentNode)) {
                return true
            }

            parent = parent.parent()!!
            depth++
        }

        return false
    }

    /**
     * Get the number of times a string s appears in the node e.
     */
    @Suppress("SameParameterValue") //Readability.js things
    private fun getCharCount(node: Element, c: Char = ','): Int {
        return getInnerText(node).count { it==c }
        //better count them directly than split them count it
    }

    /**
     * Clean a node of all elements of type "tag".
     * (Unless it's a youtube/vimeo video. People love movies.)
     * @param e the Element
     * @param tag String Tag to clean
     */
    private fun clean(e: Element, tag: String) {
        val isEmbed = tag in listOf("object", "embed", "iframe")

        removeNodes(e.getElementsByTag(tag)) filterFunction@ { element ->
            // Allow youtube and vimeo videos through as people usually want to see those.
            if(isEmbed) {
                val attributeValues = element.attributes().joinToString("|") { it.value }

                // First, check the elements attributes to see if any of them contain youtube or vimeo
                if(regex.hasAllowedVideo(attributeValues)) {
                    return@filterFunction false
                }

                // For embed with <object> tag, check inner HTML as well.
                if(element.tagName() == "object" && regex.hasAllowedVideo(element.html())) {
                    return@filterFunction false
                }
            }

            return@filterFunction true
        }
    }

    /**
     * Clean out elements whose id/class combinations match specific string.
     */
    private fun cleanMatchedNodes(e: Element,filterFn: (Element,String) -> Boolean) {
        val endOfSearchMarkerNode = getNextNode(e, true)
        var next = getNextNode(e)

        while(next != null && next != endOfSearchMarkerNode) {
            next = if(filterFn(next,next.className() + " " + next.id())) {
                removeAndGetNext(next)
            } else {
                getNextNode(next)
            }
        }
    }

    /**
     * Clean out spurious headers from an Element. Checks things like classnames and link density.
     */
    private fun cleanHeaders(e: Element) {
        removeNodes(e.getAllNodesWithTag(arrayOf("h1", "h2"))) { node ->
            (getClassWeight(node) < 0).also { if (it)
                log.info("Removing header with low class weight: {}", node.log() )
            }
        }
    }


    private fun getTextDirection(topCandidate: Element, doc: Document) {
        val ancestors = mutableSetOf(topCandidate.parent(), topCandidate)
        ancestors.addAll(topCandidate.parent()?.let { getNodeAncestors(it) }?: listOf())
        ancestors.add(doc.body())
        ancestors.add(doc.selectFirst("html")) // needed as dir is often set on html tag

        ancestors.filterNotNull().forEach { ancestor ->

            val articleDir = ancestor.attr("dir")
            if (articleDir.isNotBlank()) {
                this.articleDir = articleDir
                return
            }
        }
    }


    private var Element.readability : ReadabilityObject?
        set(value) {
            if (value!=null)
            readabilityObjects[this]=value
        }
        get(){
            return readabilityObjects[this]
        }

    private var Element._readabilityDataTable : Boolean
        set(value){
            readabilityDataTable[this]=value
        }
        get(){
            return readabilityDataTable[this] ?: false
        }




}

