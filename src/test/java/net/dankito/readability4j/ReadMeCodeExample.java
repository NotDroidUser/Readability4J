package net.dankito.readability4j;

//import net.dankito.readability4j.extended.util.BaseRegexUtilExtended;

import net.dankito.readability4j.processor.ArticleGrabber;

/**
 * Not a real test, just to have the example used in README.md as real Java code
 */
public class ReadMeCodeExample {

    public void codeExample() {
        String url = "";
        String html = "";

        Readability4J readability4J = new Readability4J(url, html); // url is just needed to resolve relative urls
        Article article = readability4J.parse();
        String extractedContentHtml = article.getContent();
        String extractedContentPlainText = article.getTextContent();
        String title = article.getTitle();
        String byline = article.getByline();
        String excerpt = article.getExcerpt();
//        ArticleGrabber extended = new ArticleGrabber(readability4J.getOptions(),new BaseRegexUtilExtended());
//        readability4J.setArticleGrabber(extended); as this ones must be implemented by user itself
    }
}
