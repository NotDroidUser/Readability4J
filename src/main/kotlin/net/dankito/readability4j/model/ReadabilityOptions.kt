package net.dankito.readability4j.model

import net.dankito.readability4j.util.BaseRegexUtil


open class ReadabilityOptions
@JvmOverloads
constructor(val maxElemsToParse: Int = DEFAULT_MAX_ELEMS_TO_PARSE,
                              val nbTopCandidates: Int = DEFAULT_N_TOP_CANDIDATES,
                              val charThreshold: Int = DEFAULT_CHAR_THRESHOLD,
                              //changed to set as readability as you shouldn't have duplicates here
                              val additionalClassesToPreserve: Set<String> = setOf(),
                              val allowedVideoRegex: Regex=Regex(BaseRegexUtil.VideosDefaultPattern),
                              val linkDensityModifier: Double=0.0,
                              val disableJSONLD:Boolean = false ,
                              val keepClasses: Boolean = false ,
                              val debug:Boolean = false) {

    @Deprecated("",
        replaceWith = ReplaceWith("charThreshold"),
        level = DeprecationLevel.WARNING)
    val wordThreshold:Int get() {
        return charThreshold
    }

    companion object {
        // Max number of nodes supported by this parser. Default: 0 (no limit)
        const val DEFAULT_MAX_ELEMS_TO_PARSE = 0

        // The number of top candidates to consider when analysing how
        // tight the competition is among candidates.
        const val DEFAULT_N_TOP_CANDIDATES = 5

        // The default number of words an article must have in order to return a result
        const val DEFAULT_CHAR_THRESHOLD = 500

        @Deprecated("Changed to DEFAULT_CHAR_THRESHOLD",
            replaceWith = ReplaceWith("DEFAULT_CHAR_THRESHOLD"),
            level = DeprecationLevel.WARNING)
        const val DEFAULT_WORD_THRESHOLD = 500
    }

}
