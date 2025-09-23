/*
package net.dankito.readability4j.extended

import net.dankito.readability4j.Readability4J
import net.dankito.readability4j.extended.processor.ArticleGrabberExtended
import net.dankito.readability4j.extended.processor.PostprocessorExtended
import net.dankito.readability4j.extended.util.BaseRegexUtilExtended
import net.dankito.readability4j.model.ReadabilityOptions
import net.dankito.readability4j.processor.MetadataParser
import net.dankito.readability4j.processor.Preprocessor


open class Readability4JExtended : Readability4J {

    // for Java interoperability
    */
/**
     * Calls Readability(String, String, ReadabilityOptions) with default ReadabilityOptions
     *//*


    @JvmOverloads
    constructor(uri: String,
                html: String,
                options: ReadabilityOptions = ReadabilityOptions(),
                regExUtil: BaseRegexUtilExtended = BaseRegexUtilExtended(),
                preprocessor: Preprocessor = Preprocessor(regExUtil),
                metadataParser: MetadataParser = MetadataParser(regExUtil),
                articleGrabber: ArticleGrabberExtended = ArticleGrabberExtended(options, regExUtil),
                postprocessor: PostprocessorExtended = PostprocessorExtended())
            : super(uri,html,options){
                this.articleGrabber=articleGrabber
                this.preprocessor=preprocessor
                this.metadataParser=metadataParser
                this.postprocessor=postprocessor
            }

}
*/
