package net.dankito.readability4j

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import junit.framework.TestCase.assertEquals
import net.dankito.readability4j.model.PageTestData
import net.dankito.readability4j.model.ReadabilityOptions
import net.dankito.readability4j.model.TestMetadata
import org.htmlunit.BrowserVersion
import org.htmlunit.ScriptResult
import org.htmlunit.StringWebResponse
import org.htmlunit.WebClient
import org.htmlunit.WebConsole
import org.htmlunit.WebRequest
import org.htmlunit.WebResponse
import org.htmlunit.html.HtmlPage
import org.htmlunit.javascript.SilentJavaScriptErrorListener
import org.htmlunit.util.FalsifyingWebConnection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URI
import kotlin.time.DurationUnit
import kotlin.time.measureTime

abstract class Readability4JTestBase {

    private val log: Logger = LoggerFactory.getLogger(Readability4J::class.java)

    companion object {

        @Volatile lateinit var MODIFIED_INSTANCE:String

        @Throws(RuntimeException::class)
        protected fun getModifiedScript():String{
            if (!::MODIFIED_INSTANCE.isInitialized) {
                val url = this::class.java.classLoader.getResource("readability/Readability.js")
                val originalScript = url?.toURI()?.let { File(it).readText() }
                if (originalScript == null)
                    throw RuntimeException("The script isn't available on readability folder in resources")
                else {
                    //Making readability a runnable standalone script
                    var modifiedScript: String = originalScript
                    //remove the last part as the
                    modifiedScript =
                        "if(?:\\s+)?\\((?:\\s+)?typeof(?:\\s+)?module(?:\\s+)?===(?:\\s+)?\"object\"(?:\\s+)?\\)(?:\\s+)?\\{".toRegex()
                            .split(modifiedScript)[0]
                    modifiedScript =
                        "function(\\s+)?Readability(?:\\s+)?\\((?:\\s+)?doc(?:\\s+)?,(?:\\s+)?options(?:\\s+)?\\)(?:\\s+)?\\{\n".toRegex()
                            .replace(
                                modifiedScript,
                                "function main(){\nvar readabilityLocal={\n\n  start(doc, options) {"
                            )
                    modifiedScript =
                        "}(\\s+)?Readability\\.prototype(?:\\s+)?=(?:\\s+)?\\{".toRegex()
                            .replace(modifiedScript, "},$1")
                    modifiedScript =
                        modifiedScript.substring(0 until modifiedScript.lastIndexOf(';')) + "; \n" +
                            "readabilityLocal.start(document,{ classesToPreserve: [\"caption\"] , debug:true}); \n" + //todo no ld is executed sadly
                            "return readabilityLocal.parse();" +
                            "\n}" +
                            "\nJSON.stringify(main())"
                    //as Rhino don't support inline deconstruction you must change this to
                    //Array.from($deconstruct-able) call
                    val theBug = "(\\n[^/\\n\\r]*)\\.\\.\\.([a-zA-Z]*.*)(\\n)".toRegex()
                    while (true) {
                        val deconstructionTags = theBug.find(modifiedScript) ?: break
                        val i:MatchResult = deconstructionTags
                        val groups = i.groupValues
                        val before = groups[1]
                        val theDestructed = groups[2]
                        val startOfTheReceiver = before.lastIndexOfAny(charArrayOf('{', '(', '['))
                        var endOfRange = -1
                        var charOfTheIndex = ' '
                        if (startOfTheReceiver != -1) {
                            charOfTheIndex = before[startOfTheReceiver]
                            var charCount = 1
                            for ((index, char) in theDestructed.withIndex()) {
                                if (charCount == 0) {
                                    endOfRange = index - 1
                                    break
                                }
                                if (char == charOfTheIndex) {
                                    charCount += 1
                                } else if (char == ']' && charOfTheIndex == '[') {
                                    charCount -= 1
                                } else if (char == ')' && charOfTheIndex == '(') {
                                    charCount -= 1
                                } else if (char == '}' && charOfTheIndex == '{') {
                                    charCount -= 1
                                }
                            }
                        } else {
                            //now this one isn't needed
                            throw RuntimeException()
                        }
                        if (endOfRange == -1) {
                            //same
                            throw RuntimeException()
                        }
                        //don't create an array of arrays if the
                        modifiedScript = if (charOfTheIndex == '[') {
                            modifiedScript.replaceRange(
                                i.range,
                                "${before.removeRange(startOfTheReceiver - 1..<before.length)} Array.from(" +
                                    theDestructed.substring(0..<endOfRange) + ")" +
                                    theDestructed.substring(endOfRange + 1..<theDestructed.length) + groups[3]
                            )
                        } else {
                            //in other case its inside an object function so no need to remove the array tags
                            modifiedScript.replaceRange(
                                i.range,
                                "$before Array.from(" +
                                    theDestructed.removeRange(endOfRange..<theDestructed.length) + ")" +
                                    theDestructed.removeRange(0..<endOfRange) + groups[3]
                            )
                        }
                    }
                    //remove normal two slash comments because sometimes it explodes here, wtf
                    modifiedScript = "\\n\\s+//.*(\n)".toRegex().replace(modifiedScript, "$1")
                    //rhino don't parse /u in regex
                    modifiedScript = modifiedScript.replace("/iu,", "/i,")
                    MODIFIED_INSTANCE=modifiedScript
                }
            }
            return MODIFIED_INSTANCE
        }

        protected val objectMapper = ObjectMapper().apply {
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }
    }


    protected open fun getExpectedText(testData: PageTestData): String? {
        return testData.expectedOutput
    }

    protected open fun getActualText(article: Article): String? {
        return article.articleContent?.html()
    }

    protected open fun testPage(url: String, testPageFolderName: String, pageName: String) {
        val testData = loadTestData(testPageFolderName, pageName,url)
        val underTest = createReadability4J(url, testData)

        val article: Article
        log.info("Parse time:{}", measureTime{
            article = underTest.parse()
            }.toInt(DurationUnit.NANOSECONDS)
        )

        val expected: Document? = cleanParseHtml(getExpectedText(testData))
        val actual: Document? = cleanParseHtml(getActualText(article))
        for (i in expected?.select("*")?: arrayListOf()){
            if (!i.attributes().isEmpty){
                val attrib=i.attributes().sortedBy { it.key }
                i.clearAttributes()
                attrib.forEach {i.attr(it.key,it.value)}
            }
        }
        for (i in actual?.select("*")?: arrayListOf()){
            if (!i.attributes().isEmpty){
                val attrib=i.attributes().sortedBy { it.key }
                i.clearAttributes()
                attrib.forEach {i.attr(it.key,it.value)}
            }
        }

        //
        if(!expected?.html()?.let { Regex("\\s").replace(it,"") }.equals(

            actual?.html()?.let { Regex("\\s").replace(it,"").replace("&amp;amp;","&amp;") }
                //for some reason viewbox is changed to viewBox.......
                // and are actually the same
                ,ignoreCase = true))
            assertEquals(expected?.html(), actual?.html())

            testMetadata(testData, article)

    }


    private fun cleanParseHtml(text: String?): Document? =
        text?.let {Jsoup.parse(
//            Jsoup.clean(
                it
//                , Safelist.relaxed())
        )}

    protected open fun createReadability4J(url: String, testData: PageTestData): Readability4J {
        return Readability4J(url, testData.sourceHtml,
                ReadabilityOptions( additionalClassesToPreserve = setOf("caption")))
    }


    protected open fun testMetadata(testData: PageTestData, article: Article) {
        val meta=testData.expectedMetadata

        assert(meta.title == article.title) { "Title doesn't match\n\nExpected:\n${meta.title}\n\nActual:\n${article.title}" }

        assert(
            meta.byline?.let { Regex("\\s").replace(it,"") } ==
            article.byline?.let { Regex("\\s").replace(it,"") }
        ) { "Byline doesn't match\n\nExpected:\n${meta.byline}\n\nActual:\n${article.byline}" }

        assert(meta.dir == article.dir) { "Text Direction doesn't match\n\nExpected:\n${meta.dir}\n\nActual:\n${article.dir}" }

        assert(meta.lang == article.lang) { "Lang doesn't match\n\nExpected:\n${meta.lang}\n\nActual:\n${article.lang}" }

        assert(
            meta.excerpt?.let { Regex("\\s").replace(it,"") } ==
            article.excerpt?.let { Regex("\\s").replace(it,"") }
        ) { "Excerpt doesn't match\n\nExpected:\n${meta.excerpt}\n\nActual:\n${article.excerpt}" }

        assert(meta.siteName == article.siteName) { "Site Name doesn't match\n\nExpected:\n${meta.siteName}\n\nActual:\n${article.siteName}" }

        assert(meta.publishedTime == article.publishedTime) { "Published Time doesn't match\n\nExpected:\n${meta.publishedTime}\n\nActual:\n${article.publishedTime}" }

    //readerable not yet implemented
//        assert(meta.readerable == ???) { "Readerable doesn't match\n\nExpected:\n${testData.expectedMetadata.dir}\n\nActual:\n${article.dir}" }

    }


    protected open fun loadTestData(testPageFolderName: String, pageName: String, url: String): PageTestData {
        val sourceHtml = getFileContentFromResource(testPageFolderName, pageName, "source.html")
        val expectedJSON = getScriptExecution(getFileContentFromResource(testPageFolderName, pageName, "source.html"),url)

        val article:Article=getArticleFromJson(expectedJSON)
        val metadata =TestMetadata()

        metadata.title=article.title
        metadata.byline=article.byline
        metadata.dir=article.dir
        metadata.lang=article.lang
        metadata.excerpt=article.excerpt
        metadata.siteName=article.siteName
        metadata.readerable=article.content.isNullOrBlank()
        metadata.publishedTime=article.publishedTime

        return PageTestData(pageName, sourceHtml, article.content, metadata)
    }

    private fun getArticleFromJson(expectedJSON: String): Article {
        val nodes = jacksonObjectMapper().readTree(expectedJSON)
        val article=Article()
        for (i in nodes.fields()){
            when(i.key){
                "title"->if (!i.value.isNull) article.title=i.value.textValue()
                "byline"->if (!i.value.isNull) article.byline=i.value.textValue()
                "dir"->if (!i.value.isNull) article.dir=i.value.textValue()
                "lang"->if (!i.value.isNull) article.lang=i.value.textValue()
                "content"-> if (!i.value.isNull) article.articleContent=Jsoup.parse(i.value.textValue()).body().child(0)
                "excerpt"-> if (!i.value.isNull) article.excerpt=i.value.textValue()
                "siteName"-> if (!i.value.isNull) article.siteName=i.value.textValue()
                "publishedTime"->if (!i.value.isNull) article.publishedTime=i.value.textValue()
                "textContent","length"->continue
            }
        }
        return article
    }

    fun getScriptExecution(html: String,uriString:String): String{
        val webClient = WebClient(BrowserVersion.FIREFOX, true,null,-1)
        return webClient.let {
            /*No connection as the test doesn't test if the page loads fully
             just if the script works*/
            it.webConnection = object:FalsifyingWebConnection(it){
                override fun getResponse(request: WebRequest?): WebResponse {
                    return StringWebResponse("",request?.url)
                }
            }
            
            it.javaScriptErrorListener=SilentJavaScriptErrorListener()
            it.options.isJavaScriptEnabled=false

            val webResponse =
                StringWebResponse(html, URI(uriString).toURL())
            val page = HtmlPage(webResponse, it.currentWindow)
            it.currentWindow.enclosedPage = page
            it.pageCreator.htmlParser.parse(webResponse, page, false, false)
            it.options.isJavaScriptEnabled=true
            val result=page.executeJavaScript(getModifiedScript())
            if (!ScriptResult.isUndefined(result)){
                result.javaScriptResult.toString()
            }else{
                throw RuntimeException("The function don't have returned anything")
            }
        }
    }

    protected open fun getFileContentFromResource(testPageFolderName: String, pageName: String, resourceFilename: String): String {
        val url = this.javaClass.classLoader.getResource("$testPageFolderName/$pageName/$resourceFilename")
        return File(url!!.toURI()).readText()
    }

}
