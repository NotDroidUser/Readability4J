package net.dankito.readability4j;


import net.dankito.readability4j.extended.Readability4JExtended;
import net.dankito.readability4j.extended.processor.ArticleGrabberExtended;
import net.dankito.readability4j.extended.util.BaseRegexUtilExtended;

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
        ArticleGrabberExtended extended = new ArticleGrabberExtended(readability4J.getOptions(),new BaseRegexUtilExtended());
        readability4J.setArticleGrabber(extended);
    }
}
