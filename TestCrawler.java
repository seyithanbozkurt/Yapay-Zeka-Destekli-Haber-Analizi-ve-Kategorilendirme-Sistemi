import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class TestCrawler {
    public static void main(String[] args) throws Exception {
        System.out.println("Testing CNN Turk...");
        try {
            Document doc = Jsoup.connect("https://www.cnnturk.com/turkiye").userAgent("Mozilla/5.0").get();
            String link = doc.select("a[href*='/turkiye/']").first().attr("abs:href");
            System.out.println("Found link: " + link);
            Document article = Jsoup.connect(link).userAgent("Mozilla/5.0").get();
            System.out.println("Article title: " + article.title());
            System.out.println("Paragraphs: " + article.select("p").size());
            System.out.println("Content: " + article.select("p").first().text());
        } catch(Exception e) { e.printStackTrace(); }

        System.out.println("Testing Anadolu Ajansi...");
        try {
            Document doc = Jsoup.connect("https://www.aa.com.tr/tr/gundem/").userAgent("Mozilla/5.0").get();
            String link = doc.select("a[href*='/tr/gundem/']").first().attr("abs:href");
            System.out.println("Found link: " + link);
            Document article = Jsoup.connect(link).userAgent("Mozilla/5.0").get();
            System.out.println("Article title: " + article.title());
            System.out.println("Paragraphs: " + article.select("p").size());
            System.out.println("Content: " + article.select("p").first().text());
        } catch(Exception e) { e.printStackTrace(); }
    }
}
