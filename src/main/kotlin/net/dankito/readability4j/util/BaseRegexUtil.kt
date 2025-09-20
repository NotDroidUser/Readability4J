package net.dankito.readability4j.util

/**
 * old RegExUtil but renamed as BaseRegexUtil
 * */
open class BaseRegexUtil @JvmOverloads constructor(
    unlikelyCandidatesPattern: String = UnlikelyCandidatesDefaultPattern,
    okMaybeItsACandidatePattern: String = OkMaybeItsACandidateDefaultPattern,
    positivePattern: String = PositiveDefaultPattern,
    negativePattern: String = NegativeDefaultPattern,
    extraneousPattern: String = ExtraneousDefaultPattern,
    bylinePattern: String = BylineDefaultPattern,
    normalizePattern: String = NormalizeDefaultPattern,
    videosPattern: String = VideosDefaultPattern,
    sharePattern: String= ShareElementsDefaultPattern,
    nextLinkPattern: String = NextLinkDefaultPattern,
    prevLinkPattern: String = PrevLinkDefaultPattern,
    tokenizerPattern: String = TokenizeDefaultPattern,
    whitespacePattern: String = WhitespaceDefaultPattern,
    hasContentPattern: String = HasContentDefaultPattern,
    hashUrlPattern: String = HashUrlDefaultPattern,
    srcsetPattern: String = SrcSetUrlDefaultPattern,
    b64DataUrlPattern: String= Base64DataUrlDefaultPattern,
    commasPattern: String= CommasDefaultPattern,
    jsonLdArticleTypesPattern: String = JsonLdArticleTypesDefaultPattern,
    adWordsPattern: String = AdWordsDefaultPattern,
    loadingWordsPattern: String = LoadingWordsDefaultPattern
) {

    companion object {

        const val UnlikelyCandidatesDefaultPattern = "-ad-|ai2html|banner|breadcrumbs|combx|" +
            "comment|community|cover-wrap|disqus|extra|footer|gdpr|header|legends|menu|related|" +
            "remark|replies|rss|shoutbox|sidebar|skyscraper|social|sponsor|supplemental|ad-break|" +
            "agegate|pagination|pager|popup|yom-remote"

        const val OkMaybeItsACandidateDefaultPattern = "and|article|body|column|content|main|mathjax|shadow"

        const val PositiveDefaultPattern = "article|body|content|entry|hentry|h-entry|main|page|" +
            "pagination|post|text|blog|story"

        const val NegativeDefaultPattern = "-ad-|hidden|^hid\$| hid\$| hid |^hid |banner|combx|" +
            "comment|com-|contact|footer|gdpr|masthead|media|meta|outbrain|promo|related|scroll|" +
            "share|shoutbox|sidebar|skyscraper|sponsor|shopping|tags|widget"

        const val ExtraneousDefaultPattern = "print|archive|comment|discuss|e[\\-]?mail|" +
            "share|reply|all|login|sign|single|utility"

        const val BylineDefaultPattern = "byline|author|dateline|writtenby|p-author"

        const val ReplaceFontsDefaultPattern = "<(/?)font[^>]*>"

        const val NormalizeDefaultPattern = "\\s{2,}"

        const val VideosDefaultPattern = "\\/\\/(www\\.)?((dailymotion|youtube|youtube-nocookie|" +
            "player\\.vimeo|v\\.qq|bilibili|live.bilibili)\\.com|(archive|upload\\.wikimedia)\\.org|player\\.twitch\\.tv)"

        //CaseInsensitive
        const val ShareElementsDefaultPattern = "(\\b|_)(share|sharedaddy)(\\b|_)"

        const val NextLinkDefaultPattern = "(next|weiter|continue|>([^\\|]|$)|»([^\\|]|$))"

        const val PrevLinkDefaultPattern = "(prev|earl|old|new|<|«)"

        const val TokenizeDefaultPattern = "\\W+"

        const val WhitespaceDefaultPattern = "^\\s*$"

        const val HasContentDefaultPattern = "\\S$"

        const val HashUrlDefaultPattern = "^#.+"

        const val SrcSetUrlDefaultPattern = "(\\S+)(\\s+[\\d.]+[xw])?(\\s*(?:,|\$))"

        const val Base64DataUrlDefaultPattern = "^data:\\s*([^\\s;,]+)\\s*;\\s*base64\\s*,"
        // Commas as used in Latin, Sindhi, Chinese and various other scripts.
        // see: https://en.wikipedia.org/wiki/Comma#Comma_variants
        const val CommasDefaultPattern="\\u002C|\\u060C|\\uFE50|\\uFE10|\\uFE11|\\u2E41|\\u2E34|\\u2E32|\\uFF0C"
        //non global neither case insensitive
        const val JsonLdArticleTypesDefaultPattern = "^Article|AdvertiserContentArticle|NewsArticle|AnalysisNewsArticle|AskPublicNewsArticle|BackgroundNewsArticle|OpinionNewsArticle|ReportageNewsArticle|ReviewNewsArticle|Report|SatiricalArticle|ScholarlyArticle|MedicalScholarlyArticle|SocialMediaPosting|BlogPosting|LiveBlogPosting|DiscussionForumPosting|TechArticle|APIReference\$"
        //case insensitive
        const val AdWordsDefaultPattern="^(ad(vertising|vertisement)?|pub(licité)?|werb(ung)?|广告|Реклама|Anuncio)\$"
        //case insensitive
        const val LoadingWordsDefaultPattern="^((loading|正在加载|Загрузка|chargement|cargando)(…|\\.\\.\\.)?)\$"
    }

    val unlikelyCandidates: Regex = Regex(unlikelyCandidatesPattern, RegexOption.IGNORE_CASE)
    val okMaybeItsACandidate: Regex = Regex(okMaybeItsACandidatePattern, RegexOption.IGNORE_CASE)
    val positive: Regex = Regex(positivePattern, RegexOption.IGNORE_CASE)
    val negative: Regex = Regex(negativePattern, RegexOption.IGNORE_CASE)
    //protected val extraneous: Regex = Regex(extraneousPattern, RegexOption.IGNORE_CASE)
    val byline: Regex = Regex(bylinePattern, RegexOption.IGNORE_CASE)
    val normalize: Regex = Regex(normalizePattern)
    var videos: Regex = Regex(videosPattern, RegexOption.IGNORE_CASE)// todo remember add this one in function to options as in the original one
    val shareElements: Regex = Regex(sharePattern, RegexOption.IGNORE_CASE)
    //protected val nextLink: Regex = Regex(nextLinkPattern, RegexOption.IGNORE_CASE)
    //protected val prevLink: Regex = Regex(prevLinkPattern, RegexOption.IGNORE_CASE)
    val tokenizer: Regex = Regex(tokenizerPattern)
    val whitespace: Regex = Regex(whitespacePattern)
    val hasContent: Regex = Regex(hasContentPattern)
    val hashUrl:Regex = Regex(hashUrlPattern)
    val srcsetUrl:Regex = Regex(srcsetPattern)
    val b64DataUrl:Regex = Regex(b64DataUrlPattern)
    val commas:Regex = Regex(commasPattern)
    val jsonLdArticleTypes:Regex = Regex(jsonLdArticleTypesPattern)
    val adWords:Regex = Regex(adWordsPattern,RegexOption.IGNORE_CASE)
    val loadingWords:Regex = Regex(loadingWordsPattern)


    fun isPositive(matchString: String): Boolean {
        return positive.containsMatchIn(matchString)
    }

    fun isNegative(matchString: String): Boolean {
        return negative.containsMatchIn(matchString)
    }

    fun isUnlikelyCandidate(matchString: String): Boolean {
        return unlikelyCandidates.containsMatchIn(matchString)
    }

    fun okMaybeItsACandidate(matchString: String): Boolean {
        return okMaybeItsACandidate.containsMatchIn(matchString)
    }

    fun isByline(matchString: String): Boolean {
        return byline.containsMatchIn(matchString)
    }

    fun hasContent(matchString: String): Boolean {
        return hasContent.containsMatchIn(matchString)
    }

    fun isWhitespace(matchString: String): Boolean {
        return whitespace.containsMatchIn(matchString)
    }

    fun isHashUrl(string: String):Boolean{
        return hashUrl.matches(string)
    }

    fun normalize(text: String): String {
        return normalize.replace(text," ")
    }

    fun hasAllowedVideo(matchString: String): Boolean {
        return videos.containsMatchIn(matchString)
    }

    fun isJsonLDArticle(matchString: String): Boolean {
        return jsonLdArticleTypes.matches(matchString)
    }

    fun isB64Data(string: String) = b64DataUrl.containsMatchIn(string)

    fun getB64Matches(string: String) = b64DataUrl.matchAt(string,0)

    fun getWords(string: String) = tokenizer.split(string).toMutableList()

    fun splitCommas(string: String)=string.split(commas)

    fun hasAdWords(innerText: String): Boolean {
        return adWords.containsMatchIn(innerText)
    }

    fun isShareElement(innerText: String): Boolean {
        return shareElements.containsMatchIn(innerText)
    }

    fun hasLoadingWords(innerText: String): Boolean {
        return loadingWords.containsMatchIn(innerText)
    }

    fun getSrcSetMatches(srcSetString:String) = srcsetUrl.findAll(srcSetString)

}
