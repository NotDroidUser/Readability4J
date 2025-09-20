package net.dankito.readability4j.model



open class ArticleMetadata() //As the class itself its always called without any args removed them
{

    /**
     * Just for retrocompatibility
     *
     * */
    //but saved old constructor
    constructor(title: String?=null,
                byline: String?=null,
                excerpt: String?=null,
                dir: String?=null,
                charset: String?=null) : this() {
        this.title=title
        this.byline=byline
        this.excerpt=excerpt
        this.dir=dir
        this.charset=charset
    }

    var title: String? = null
    var byline: String? = null
    var excerpt: String? = null
    var siteName:String? = null
    var publishedTime:String? = null

    //this is text direction that in
    @Deprecated("This is always gotten from the ArticleGrabber Object," +
        "don't use this one except for testing")
    var dir :String?=null
    @Deprecated("This is always utf-8 right now")
    var charset: String? = "utf-8"

    override fun toString() = buildString {
        arrayOf(title).joinToString()
    }

    //JSONLDCompatibility
    var datePublished:String? get(){
            return publishedTime
        } set(value){
            publishedTime=value
        }
}
