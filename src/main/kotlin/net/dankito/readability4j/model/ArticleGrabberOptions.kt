package net.dankito.readability4j.model

/**
 * This class represents the flags
 * FLAG_STRIP_UNLIKELYS,
 * FLAG_WEIGHT_CLASSES,
 * FLAG_CLEAN_CONDITIONALLY
 * on Readability.js
 * */
open class ArticleGrabberOptions(var stripUnlikelyCandidates: Boolean = true,
                                 var weightClasses: Boolean = true,
                                 var cleanConditionally: Boolean = true)
