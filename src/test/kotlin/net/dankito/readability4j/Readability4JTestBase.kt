package net.dankito.readability4j

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import junit.framework.TestCase.assertEquals
import net.dankito.readability4j.model.ArticleMetadata
import net.dankito.readability4j.model.PageTestData
import net.dankito.readability4j.model.ReadabilityOptions
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.safety.Safelist
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.util.*

abstract class Readability4JTestBase {

    companion object {

        protected val objectMapper = ObjectMapper()

        init {
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }
    }


    protected open fun getExpectedText(testData: PageTestData): String? {
        return testData.expectedOutput
    }

    protected open fun getActualText(article: Article, testData: PageTestData): String? {
        return article.content
    }

    protected open fun getExpectedTitle(testData: PageTestData): String? {
        return testData.expectedMetadata.title
    }

    protected open fun getExpectedExcerpt(testData: PageTestData): String? {
        return testData.expectedMetadata.excerpt
    }

    protected open fun getActualExcerpt(testData: PageTestData, article: Article): String? {
        return article.excerpt
    }

    protected open fun getExpectedByline(testData: PageTestData): String? {
        return testData.expectedMetadata.byline
    }



    protected open fun testPage(url: String, testPageFolderName: String, pageName: String): Article {
        val testData = loadTestData(testPageFolderName, pageName)

        return testPage(url, testData)
    }

    protected open fun testPage(url: String, testData: PageTestData): Article {
        val underTest = createReadability4J(url, testData)

        val article = underTest.parse()


        val expected: Document = cleanParseHtml(getExpectedText(testData))
        val actual: Document = cleanParseHtml(getActualText(article, testData))

        assertEquals(expected.html(), actual.html())

        testMetadata(testData, article)

        return article
    }

    private fun cleanParseHtml(text: String?): Document =
        Jsoup.parse(Jsoup.clean(text!!, Safelist.relaxed()))

    protected open fun createReadability4J(url: String, testData: PageTestData): Readability4J {
        // Provide one class name to preserve, which we know appears in a few
        // of the test documents.
        return Readability4J(url, testData.sourceHtml,
                ReadabilityOptions(additionalClassesToPreserve = Arrays.asList("caption")))
    }


    protected open fun testMetadata(testData: PageTestData, article: Article) {
        val expectedTitle = getExpectedTitle(testData)
        assert(expectedTitle == article.title) { "Title doesn't match\n\nExpected:\n${expectedTitle}\n\nActual:\n${article.title}" }

        val expectedExcerpt = getExpectedExcerpt(testData)
        val actualExcerpt = getActualExcerpt(testData, article)
        assert(expectedExcerpt == actualExcerpt) { "Excerpt doesn't match\n\nExpected:\n${expectedExcerpt}\n\nActual:\n${actualExcerpt}" }

        val expectedByline = getExpectedByline(testData)
        assert(expectedByline == article.byline) { "Byline doesn't match\n\nExpected:\n${expectedByline}\n\nActual:\n${article.byline}" }

        if(testData.expectedMetadata.dir != null) {
            assert(testData.expectedMetadata.dir == article.dir) { "Dir doesn't match\n\nExpected:\n${testData.expectedMetadata.dir}\n\nActual:\n${article.dir}" }
        }
    }


    protected open fun loadTestData(testPageFolderName: String, pageName: String): PageTestData {
        val sourceHtml = getFileContentFromResource(testPageFolderName, pageName, "source.html")
        val expectedOutput = getFileContentFromResource(testPageFolderName, pageName, "expected.html")

        val expectedMetadataString = getFileContentFromResource(testPageFolderName, pageName, "expected-metadata.json")
        val expectedMetadata = objectMapper.readValue<ArticleMetadata>(expectedMetadataString, ArticleMetadata::class.java)


        return PageTestData(pageName, sourceHtml, expectedOutput, expectedMetadata)
    }

    protected open fun getFileContentFromResource(testPageFolderName: String, pageName: String, resourceFilename: String): String {
        val url = this.javaClass.classLoader.getResource("$testPageFolderName/$pageName/$resourceFilename")
        val file = File(url.toURI())

        val reader = FileReader(file) // TODO: set encoding
        val fileContent = BufferedReader(reader).readText()

        reader.close()

        return fileContent
    }

}