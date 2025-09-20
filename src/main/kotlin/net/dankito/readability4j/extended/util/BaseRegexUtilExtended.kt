package net.dankito.readability4j.extended.util

import net.dankito.readability4j.util.BaseRegexUtil
import java.util.regex.Pattern


open class BaseRegexUtilExtended : BaseRegexUtil {

    companion object {
        const val RemoveImageDefaultPattern = "author|avatar|thumbnail" // CHANGE: this is not in Mozilla's Readability

        const val NegativeDefaultPatternExtended = "|float"
    }


    protected val removeImage: Pattern

    @JvmOverloads
    constructor(unlikelyCandidatesPattern: String = UnlikelyCandidatesDefaultPattern, okMaybeItsACandidatePattern: String = OkMaybeItsACandidateDefaultPattern,
                positivePattern: String = PositiveDefaultPattern, negativePattern: String = NegativeDefaultPattern + NegativeDefaultPatternExtended,
                extraneousPattern: String = ExtraneousDefaultPattern, bylinePattern: String = BylineDefaultPattern,
                replaceFontsPattern: String = ReplaceFontsDefaultPattern, normalizePattern: String = NormalizeDefaultPattern,
                videosPattern: String = VideosDefaultPattern, nextLinkPattern: String = NextLinkDefaultPattern,
                prevLinkPattern: String = PrevLinkDefaultPattern, whitespacePattern: String = WhitespaceDefaultPattern,
                hasContentPattern: String = HasContentDefaultPattern, removeImagePattern: String = RemoveImageDefaultPattern)
    : super(unlikelyCandidatesPattern, okMaybeItsACandidatePattern, positivePattern, negativePattern, extraneousPattern, bylinePattern, replaceFontsPattern, normalizePattern,
            videosPattern, nextLinkPattern, prevLinkPattern, whitespacePattern, hasContentPattern) {
        this.removeImage = Pattern.compile(removeImagePattern)
    }


    open fun keepImage(matchString: String): Boolean { // CHANGE: this is not in Mozilla's Readability
        if((isNegative(matchString) && !isPositive(matchString)) || removeImage.matcher(matchString).find()) {
            return false
        }

        return true
    }

}
