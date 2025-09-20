package net.dankito.readability4j

import net.dankito.readability4j.model.ArticleMetadata
import net.dankito.readability4j.model.ReadabilityOptions
import net.dankito.readability4j.processor.ArticleGrabber
import net.dankito.readability4j.processor.MetadataParser
import net.dankito.readability4j.processor.Postprocessor
import net.dankito.readability4j.processor.Preprocessor
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.system.measureNanoTime


open class Readability4J
/**
 * Calls Readability4J with default params if no options provided,
 * this constructor uses the uri for the postprocessing and Jsoup,
 * as differ of js version keeps the url as you cant call in a html
 * text documentUri as they call in the Postprocessor to process the URIs
 * to make them absolute
 *
 * @param uri The uri (for Jsoup and for the Postprocessor) also can be empty string if
 * you want to process manually after the article is served and don't waste that time
 * @param html The page as string (this for Jsoup)
 * @param options optional, if you don't provide it, will be all default options
 * @see net.dankito.readability4j.model.ReadabilityOptions
 * @see net.dankito.readability4j.processor.Postprocessor
 */
@Throws(ExceptionInInitializerError::class)
@JvmOverloads constructor(
    val uri:String,
    val html:String,
    val options: ReadabilityOptions = ReadabilityOptions(),
) {
    private val log: Logger = LoggerFactory.getLogger(Readability4J::class.java)
    var metadataParser: MetadataParser = MetadataParser()
    var preprocessor: Preprocessor = Preprocessor()
    var articleGrabber: ArticleGrabber = ArticleGrabber(options)
    var postprocessor: Postprocessor = Postprocessor()
    // TODO: add IDependencyResolver interface
    //  ???????


    /**
     *
     * Runs readability.
     *
     * Workflow:
     *  1. Prep the document by removing script tags, css, etc.
     *  2. Build readability's DOM tree.
     *  3. Grab the article content from the current dom tree.
     *  4. Replace the current DOM tree with the new one.
     *  5. Read peacefully.
     *
     * @return The actual article if the article exists in the html,
     * else an empty Article with null content
     * @throws RuntimeException if too many elements to parse (As you put in options)
     * @see net.dankito.readability4j.Article
     *
     */
    @Throws(RuntimeException::class)
    open fun parse(): Article {

        val document: Document

        log.info("Time parsing Document:{}",measureNanoTime {
            document= Jsoup.parse(html,uri)
        })

        // Avoid parsing too large documents, as per configuration option
        if (options.maxElemsToParse > 0) {
            val numTags = document.count()
            if(numTags > options.maxElemsToParse) {
                throw RuntimeException("Aborting parsing document; $numTags elements found, but ReadabilityOption.maxElemsToParse is set to ${options.maxElemsToParse}")
            }
        }

        log.info("Time unwraping noscripts :{}",measureNanoTime {
            preprocessor.unwrapNoscriptImages(document)
        })

        var jsonLDMetadata:ArticleMetadata?=null
        if (!options.disableJSONLD){
            log.info("Time Processing Json-LD :{}",measureNanoTime {
                jsonLDMetadata=metadataParser.getJSONLD(document)
            })
        }

        // this one also remove the scripts
        log.info("Time Pre-Processing Document :{}",measureNanoTime {
            preprocessor.prepareDocument(document)
        })

        val metadata: ArticleMetadata
        log.info("Time Parsing Metadata :{}",measureNanoTime {
            metadata = metadataParser.getArticleMetadata(document,jsonLDMetadata)
        })

        val articleContent: Element?
        log.info("Time Grabbing Article :{}",measureNanoTime {
            articleContent = articleGrabber.grabArticle(document, metadata)
        })

        val article = Article()
        if (articleContent==null){
            return article.also { setArticleMetadata(article,metadata,null) }
            // send a empty result, as nothing are found here
        }

        log.debug("Grabbed: {}", articleContent)

        log.info("Time Post-Processing Document :{}",measureNanoTime {
            //this is removing things af
            postprocessor.postProcessContent( articleContent, document.baseUri(), uri, options )
        })
        article.articleContent = articleContent
        setArticleMetadata(article, metadata, articleContent)

        return article
    }

    protected open fun setArticleMetadata(article: Article, metadata: ArticleMetadata, articleContent: Element?) {
        // If we haven't found an excerpt in the article's metadata, use the article's
        // first paragraph as the excerpt. This is used for displaying a preview of
        // the article's content.

        if(metadata.excerpt.isNullOrBlank()) {
            articleContent?.getElementsByTag("p")?.first()?.let { firstParagraph ->
                metadata.excerpt = firstParagraph.wholeText().trim()
            }
        }

        article.title = metadata.title
        article.byline = if(metadata.byline.isNullOrBlank()) articleGrabber.articleByline else metadata.byline
        articleGrabber.articleLang?.let { article.lang= it}
        article.dir = articleGrabber.articleDir
        article.excerpt = metadata.excerpt
        article.siteName = metadata.siteName
        article.publishedTime = metadata.publishedTime
        //article.charset = metadata.charset // this doesn't exist anymore in js
    }

}
