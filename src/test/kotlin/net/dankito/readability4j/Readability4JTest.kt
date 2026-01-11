package net.dankito.readability4j

import org.junit.Test

open class Readability4JTest : Readability4JTestBase() {

    companion object {
        const val ReadabilityFakeTestUrl = "http://fakehost/test/page.html"
    }
    /**
     * This test test if the regex-ed readability.js works, if this one dont work no one will
     * */
    @Test
    fun testLocalScriptWorks(){
        val html=getFileContentFromResource("readability/test/test-pages", "mozilla-1", "source.html")
        val scriptExecution = getScriptExecution(html,ReadabilityFakeTestUrl)
        assert(scriptExecution !="")
    }

    @Test
    fun test001() {
        testPage("001")
    }

    @Test
    fun test002() {
        testPage("002")
    }

    @Test
    fun test003MetadataPreferred() {
        testPage("003-metadata-preferred")
    }

    @Test
    fun test004MetadataSpaceSeparatedProperties(){
        testPage("004-metadata-space-separated-properties")
    }
    @Test
    fun test005UnescapeHtmlEntities(){
        testPage("005-unescape-html-entities")
    }

    @Test
    fun testAclu(){
        testPage("aclu")
    }
    @Test
    fun testAktualne(){
        testPage("aktualne")
    }
    @Test
    fun testArchiveOfOurOwn(){
        testPage("archive-of-our-own")
    }

    @Test
    fun testArs1() {
        testPage("ars-1")
    }

    @Test
    fun testBaseUrl() {
        testPage("base-url")
    }

    @Test
    fun testBaseUrlBaseElement() {
        testPage("base-url-base-element")
    }

    @Test
    fun testBaseUrlBaseElementRelative() {
        testPage("base-url-base-element-relative")
    }

    @Test
    fun testBasicTagsCleaning() {
        testPage("basic-tags-cleaning")
    }

    @Test
    fun testBBC1() {
        testPage("bbc-1")
    }

    @Test
    fun testBlogger() {
        testPage("blogger")
    }

    @Test
    fun testBreitbart() {
        testPage("breitbart")
    }

    @Test
    fun testBug1255978() {
        testPage("bug-1255978")
    }

    @Test
    fun testBuzzfeed1() {
        testPage("buzzfeed-1")
    }

    @Test
    fun testCitylab1() {
        testPage("citylab-1")
    }

    @Test
    fun testCleanLinks() {
        testPage("clean-links")
    }

    @Test
    fun testCnet() {
        testPage("cnet")
    }

    @Test
    fun testCnetSvgClasses() {
        testPage("cnet-svg-classes")
    }

    @Test
    fun testCNN() {
        testPage("cnn")
    }

    @Test
    fun testCommentInsideScriptParsing() {
        testPage("comment-inside-script-parsing")
    }

    @Test
    fun testDaringFireball1() {
        testPage("daringfireball-1")
    }

    @Test
    fun testDataUrlImage() {
        testPage("data-url-image")
    }

    @Test
    fun testDev418() {
        testPage("dev418")
    }

    @Test
    fun testDropboxBlog() {
        testPage("dropbox-blog")
    }

    @Test
    fun testEbbOrg() {
        testPage("ebb-org")
    }

    @Test
    fun testEHow1() {
        testPage("ehow-1")
    }

    @Test
    fun testEHow2() {
        testPage("ehow-2")
    }

    @Test
    fun testEmbeddedVideos() {
        testPage("embedded-videos")
    }

    @Test
    fun testEngadget() {
        testPage("engadget")
    }

    @Test
    fun testFirefoxNightlyBlog() {
        testPage("firefox-nightly-blog")
    }

    @Test
    fun testFolha() {
        testPage("folha")
    }

    @Test
    fun testGitlabBlog() {
        testPage("gitlab-blog")
    }

    @Test
    fun testGmw() {
        testPage("gmw")
    }

    @Test
    fun testGoogleSreBook1() {
        testPage("google-sre-book-1")
    }


    @Test
    fun testGuardian1() {
        testPage("guardian-1")
    }

    @Test
    fun testHeise() {
        testPage("heise")
    }

    @Test
    fun testHeraldSun1() {
        testPage("herald-sun-1")
    }

    @Test
    fun testHiddenNodes() {
        testPage("hidden-nodes")
    }

    @Test
    fun testHukumusume() {
        testPage("hukumusume")
    }

    @Test
    fun testIab1() {
        testPage("iab-1")
    }

    @Test
    fun testIETF1() {
        testPage("ietf-1")
    }

    @Test
    fun testInvalidAttributes() {
        testPage("invalid-attributes")
    }

    @Test
    fun testJsLinkReplacement() {
        testPage("js-link-replacement")
    }

    @Test
    fun testKeepImages() {
        testPage("keep-images")
    }

    @Test
    fun testKeepTabularData() {
        testPage("keep-tabular-data")
    }


    @Test
    fun testLaNacion() {
        testPage("la-nacion")
    }

    @Test
    fun testLazyImage1() {
        testPage("lazy-image-1")
    }

    @Test
    fun testLazyImage2() {
        testPage("lazy-image-2")
    }

    @Test
    fun testLazyImage3() {
        //todo WTF
        testPage("lazy-image-3")
    }

    @Test
    fun testLeMonde1() {
        testPage("lemonde-1")
    }

    @Test
    fun testLiberation1() {
        testPage("liberation-1")
    }

    @Test
    fun testLifehackerPostCommentLoad() {
        testPage("lifehacker-post-comment-load")
    }

    @Test
    fun testLifehackerWorking() {
        testPage("lifehacker-working")
    }

    @Test
    fun testLinksInTables() {
        testPage("links-in-tables")
    }

    @Test
    fun testLwn1() {
        testPage("lwn-1")
    }

    @Test
    fun testMedicalnewstoday() {
        testPage("medicalnewstoday")
    }

    @Test
    fun testMedium1() {
        testPage("medium-1")
    }

    @Test
    fun testMedium2() {
        testPage("medium-2")
    }

    @Test
    fun testMedium3() {
        testPage("medium-3")
    }
    
    @Test
    fun testMathjax() {
        testPage("mathjax")
    }
    
    @Test
    fun testMercurial() {
        testPage("mercurial")
    }


    @Test
    fun testMetadataContentMissing() {
        testPage("metadata-content-missing")
    }

    @Test
    fun testMissingParagraphs() {
        testPage("missing-paragraphs")
    }

    @Test
    fun testMozilla1() {
        testPage("mozilla-1")
    }

    @Test
    fun testMozilla2() {
        testPage("mozilla-2")
    }

    @Test
    fun testMsn() {
        testPage("msn")
    }

    @Test
    fun testNormalizeSpaces() {
        testPage("normalize-spaces")
    }

    @Test
    fun testNYTimes1() {
        //todo this one fixs the next one
        testPage("nytimes-1")
    }

    @Test
    fun testNYTimes2() {
        testPage("nytimes-2")
    }

    @Test
    fun testNYTimes3() {
        testPage("nytimes-3")
    }

    @Test
    fun testNYTimes4() {
        testPage("nytimes-4")
    }

    @Test
    fun testNYTimes5() {
        testPage("nytimes-5")
    }

    @Test
    fun testOl() {
        testPage("ol")
    }


    @Test
    fun testParselyMetadata() {
        testPage("parsely-metadata")
    }


    @Test
    fun testPixnet() {
        testPage("pixnet")
    }

    @Test
    fun testQQ() {
        testPage("qq")
    }

    @Test
    fun testQuanta1() {
        testPage("quanta-1")
    }

    @Test
    fun testRemoveAriaHidden() {
        //todo
        testPage("remove-aria-hidden")
    }

    @Test
    fun testRemoveExtraBrs() {
        testPage("remove-extra-brs")
    }

    @Test
    fun testRemoveExtraParagraphs() {
        testPage("remove-extra-paragraphs")
    }

    @Test
    fun testRemoveScriptTags() {
        testPage("remove-script-tags")
    }

    @Test
    fun testReorderingParagraphs() {
        testPage("reordering-paragraphs")
    }

    @Test
    fun testReplaceBrs() {
        testPage("replace-brs")
    }

    @Test
    fun testReplaceFontTags() {
        testPage("replace-font-tags")
    }

    @Test
    fun testRoyalRoad() {
        testPage("royal-road")
    }

    @Test
    fun testRTL1() {
        testPage("rtl-1")
    }

    @Test
    fun testRTL2() {
        testPage("rtl-2")
    }

    @Test
    fun testRTL3() {
        testPage("rtl-3")
    }

    @Test
    fun testRTL4() {
        testPage("rtl-4")
    }

    @Test
    fun testSalon1() {
        testPage("salon-1")
    }

    @Test
    fun testSeattleTimes1() {
        testPage("seattletimes-1")
    }

    @Test
    fun testSimplyFound1() {
        testPage("simplyfound-1")
    }

    @Test
    fun testSocialButtons() {
        testPage("social-buttons")
    }

    @Test
    fun testSpiceworks() {
        //todo
        testPage("spiceworks")
    }

    @Test
    fun testStyleTagsRemoval() {
        testPage("style-tags-removal")
    }

    @Test
    fun testSvgParsing() {
        //this one works but closes tags
        testPage("svg-parsing")
    }

    @Test
    fun testTableStyleAttributes() {
        testPage("table-style-attributes")
    }

    @Test
    fun testTelegraph() {
        testPage("telegraph")
    }

    @Test
    fun testTheVerge() {
        testPage("theverge")
    }

    @Test
    fun testTitleAndH1Discrepancy() {
        testPage("title-and-h1-discrepancy")
    }

    @Test
    fun testTitleEnDash() {
        testPage("title-en-dash")
    }
    
    @Test
    fun testTMZ1() {
        testPage("tmz-1")
    }

    @Test
    fun testTocMissing() {
        testPage("toc-missing")
    }

    @Test
    fun testTopicseed1() {
        testPage("topicseed-1")
    }

    @Test
    fun testTumblr() {
        testPage("tumblr")
    }

    @Test
    fun testV8Blog() {
        testPage("v8-blog")
    }

    @Test
    fun testVideos1() {
        testPage("videos-1")
    }

    @Test
    fun testVideos2() {
        testPage("videos-2")
    }

    @Test
    fun testVisibilityHidden() {
        testPage("visibility-hidden")
    }

    @Test
    fun testWapo1() {
        testPage("wapo-1")
    }

    @Test
    fun testWapo2() {
        testPage("wapo-2")
    }

    @Test
    fun testWebmd1() {
        testPage("webmd-1")
    }

    @Test
    fun testWebmd2() {
        testPage("webmd-2")
    }

    @Test
    fun testWikia() {
        testPage("wikia")
    }

    @Test
    fun testWikipedia() {
        testPage("wikipedia")
    }

    @Test
    fun testWikipedia2() {
        testPage("wikipedia-2")
    }

    @Test
    fun testWikipedia3() {
        //this one works but left some tag into div
        testPage("wikipedia-3")
    }

    @Test
    fun testWikipedia4() {
        testPage("wikipedia-4")
    }

    @Test
    fun testWordpress() {
        testPage("wordpress")
    }

    @Test
    fun testYahoo1() {
        testPage("yahoo-1")
    }

    @Test
    fun testYahoo2() {
        testPage("yahoo-2")
    }

    @Test
    fun testYahoo3() {
        testPage("yahoo-3")
    }

    @Test
    fun testYahoo4() {
        testPage("yahoo-4")
    }

    @Test
    fun testYouth() {
        testPage("youth")
    }


    protected open fun testPage(pageName: String) {
        testPage(ReadabilityFakeTestUrl, "readability/test/test-pages", pageName)
    }

}
