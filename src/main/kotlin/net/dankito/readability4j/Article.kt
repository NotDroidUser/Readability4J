package net.dankito.readability4j

import org.jsoup.nodes.Element


open class Article(

        /**
         * Original uri object that was passed to constructor
         * that has no usage and also is in the same context that the Readability4J is called
         */
) {
        constructor(uri:String):this(){
            this.uri=uri
        }

        @Deprecated("This has no sense as you has the url in the context you call Readability4J",
            level =  DeprecationLevel.WARNING)
        var uri: String=""
        /**
         * Article title
         */
        var title: String? = null

        /**
         * The actual html object of the article
         * */
        var articleContent: Element? = null

        /**
         * Content lang (from html tag) default to empty
         */
        var lang:String? = null

        /**
         * HTML string of processed article content in a &lt;div> element.
         *
         * Therefore no encoding is applied as intended in the js library,
         * @see contentWithUtf8Encoding
         * @see <a href="https://github.com/dankito/Readability4J/issues/1">The github issue</a>.
         */
        val content: String?
            get() = articleContent?.outerHtml()


        var siteName:String? = null

        var publishedTime:String? = null
        /**
         * [content] returns a &lt;div> element.
         *
         * As the only way in HTML to set an encoding is via &lt;head>&lt;meta charset=""> tag, therefore no explicit
         * encoding is applied to it.
         * As a result non-ASCII characters may get displayed incorrectly.
         *
         * So this method wraps [content] in &lt;html>&lt;head>&lt;meta charset="utf-8"/>&lt;/head>&lt;body>&lt;!--
         * content-->&lt;/body>&lt;/html> so that UTF-8 encoding gets applied.
         *
         * @see <a href="https://github.com/dankito/Readability4J/issues/1">The issue for more info.</a>
         */
        val contentWithUtf8Encoding: String?
                get() = getContentWithEncoding("utf-8")

        /**
         * Returns the content wrapped in an <html> element with charset set to document's charset. Or if that is not set in UTF-8.
         * @see [contentWithUtf8Encoding] for more details.
         */
        val contentWithDocumentsCharsetOrUtf8: String?
                get() = getContentWithEncoding(charset ?: "utf-8")

        /**
         * Content text (only text)
         */
        val textContent: String?
                get() = articleContent?.text()

        /**
         * Length of article, in characters
         */
        val length: Int
            get() = textContent?.length ?: -1

        /**
         * Article description, or short excerpt from content
         */
        var excerpt: String? = null

        /**
         * Author metadata
         */
        var byline: String? = null

        /**
         * Content direction
         */
        var dir: String? = null

        /**
         * Article's charset
         */
        @Deprecated("Right now all sites uses utf-8", level = DeprecationLevel.WARNING)
        var charset: String? = null


        /**
         * [content] returns a &lt;div> element.
         *
         * As the only way in HTML to set an encoding is via &lt;head>&lt;meta charset=""> tag, therefore no explicit
         * encoding is applied to it.
         * As a result non-ASCII characters may get displayed incorrectly.
         *
         * So this method wraps [content] in &lt;html>&lt;head>&lt;meta charset="[encoding]"/>&lt;/head>&lt;body>&lt;!--
         * content-->&lt;/body>&lt;/html> so that encoding gets applied.
         *
         * See [https://github.com/dankito/Readability4J/issues/1] for more info.
         */
        fun getContentWithEncoding(encoding: String): String? {
                content?.let { content ->
                        return "<html>\n  <head>\n    <meta charset=\"$encoding\"/>\n  </head>\n  <body>\n    " +
                                "$content\n  </body>\n</html>"
                }?: return null
        }


}
