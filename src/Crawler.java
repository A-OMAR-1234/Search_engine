import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.InsertOneOptions;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
//import org.bson.Document;
import org.jsoup.nodes.Element;
import java.io.*;
import java.util.*;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

class crawlerStore {

    MongoCollection queueCollection;
    MongoCollection visitedCollection;
    public crawlerStore (MongoCollection qc, MongoCollection vc)
    {
        queueCollection = qc;
        visitedCollection = vc;
    }


}
public class Crawler {
    static int CounterHyperLinks = 0;
    static int NoCrPages = 0;
    MongoDatabase database;
    MongoClient mongoClient;
    static List<String> temp;
    static Queue<String> Q;
    static HashMap<String, Boolean> visited;
    private static MongoCollection<org.bson.Document> Collection;
    private static MongoCollection<org.bson.Document> CollectionURLs;

    public Crawler() {
        Q = new LinkedList<>();
        visited = new HashMap<>();
        temp = new ArrayList<>();
        this.mongoClient = MongoClients.create("mongodb://localhost:27017");
        this.database = mongoClient.getDatabase("APT");
        Collection = database.getCollection("APT");
        CollectionURLs = database.getCollection("hyper_links");


    }

    public void crawlMain() throws IOException {
        String[] array1 = readStringArrayFromFile("C:\\Users\\Mohammad\\Desktop\\URLFiles.txt");
        for (String s : array1) {
            Q.add(s);
        }
        Map<String, String> docs = new HashMap<>();
        while (NoCrPages < 5) {
            //Check hena lma Q teba empty , Haruh ageb kmya mn database Wahuthum fl Q
            if(Q.isEmpty())
            {
                    // Retrieve URLs from the CollectionURLs and add them to the Q
                    List<org.bson.Document> urlDocuments = CollectionURLs.find().limit(99).into(new ArrayList<>());
                    for (org.bson.Document urlDocument : urlDocuments) {
                        String url = urlDocument.getString("URL");
                        Q.add(url);
                        CollectionURLs.deleteOne(urlDocument);
                    }
            }

        //Washel el kmya dy brdu mn database
        String url = Q.poll();
            // visited collection
//            String compactedString = compactUrl(url);
//            visited.put(compactedString,true);
        crawl(docs, url);
        NoCrPages++;
        //array1 = readStringArrayFromFile("C:\\Users\\Mohammad\\Desktop\\URLFiles.txt");
        storeDocsInDatabase(docs);
    }

}

    public void storeDocsInDatabase(Map<String, String> docs) {
        for (Map.Entry<String, String> entry : docs.entrySet()) {
            String url = entry.getKey();
            String html = entry.getValue();

            org.bson.Document document = new org.bson.Document();
            document.append("url", url);
            document.append("html", html);

            Collection.insertOne(document, new InsertOneOptions());

            System.out.println("Stored in database: " + url);
        }
        docs.clear();
    }
    public void dropDataBase()
    {
        database.drop();
    }
    private static void crawl(Map<String,String> docs,String url ) throws IOException {
        Document doc = request(url);
        if (doc != null)
        {
            String htmlString = doc.html();
            docs.put(url,htmlString);
            for (Element link : doc.select("a[href]")) {
                String next_link = link.absUrl("href");
                if (!visited.containsKey(next_link) && next_link.startsWith("https") && next_link.startsWith("http"))
                {
                    temp.add(next_link);
//                    appendStringToFile("C:\\Users\\Mohammad\\Desktop\\URLFiles.txt", next_link);
                    System.out.println(NoCrPages);
                    System.out.println( CounterHyperLinks + " : " + doc.title());
                    CounterHyperLinks++ ;
                    System.out.println("Link:" + next_link);
                }
            }
            for (String s : temp)
            {
                org.bson.Document document = new org.bson.Document();
                document.append("URL",s);
                CollectionURLs.insertOne(document);
            }
            temp.clear();
            //Collection.insertOne();
        }

        return;
    }

    private static Document request(String url) {
        try {
            // Make request to URL
            Connection con = Jsoup.connect(url);
            Document doc = con.get();
            if (con.response().statusCode() == 200) {
                // Check if the page is disallowed in robots.txt
                URL urlObj = new URL(url);
                String robotsUrl = urlObj.getProtocol() + "://" + urlObj.getHost() + "/robots.txt";
                Connection robotsCon = Jsoup.connect(robotsUrl);
                Document robotsDoc = robotsCon.get();
                if (robotsCon.response().statusCode() == 200) {
                    String robotsContent = robotsDoc.text();
                    if (robotsContent.contains("Disallow: " + urlObj.getPath())) {
                        return null;
                    }
                }
//                String CompactedURL = compactUrl(url);
//                v.add(CompactedURL);
                return doc;
            }
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    // Reads The URL seed set
    public static String[] readStringArrayFromFile(String path) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(path));
        List<String> list = new ArrayList<>();
        String line;

        while ((line = reader.readLine()) != null) {
            list.add(line);
        }

        reader.close();

        String[] array = new String[list.size()];
        array = list.toArray(array);
        return array;
    }

    // Add URL to seed set
    public static void appendStringToFile(String path, String data) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(path, true));
        writer.write(data);
        writer.newLine();
        writer.close();
    }



    // Compact String
    public static String compactUrl(String urlString) {
        try {
            // Parse and normalize URL
            URL url = new URL(urlString);
            String protocol = url.getProtocol().toLowerCase();
            String host = url.getHost().toLowerCase();
            int port = url.getPort();
            if (port == url.getDefaultPort()) {
                port = -1; // omit default ports
            }
            String path = url.getPath();
            if (path.isEmpty()) {
                path = "/";
            }
            String query = url.getQuery();
            if (query != null) {
                query = normalizeQuery(query);
            }

            // Create canonical URL
            String canonicalUrl = protocol + "://" + host;
            if (port != -1) {
                canonicalUrl += ":" + port;
            }
            canonicalUrl += path;
            if (query != null) {
                canonicalUrl += "?" + query;
            }

            // Hash canonical URL
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(canonicalUrl.getBytes());
            String hashString = bytesToHexString(hash);

            return hashString;
        } catch (Exception e) {
            return null;
        }
    }

    private static String normalizeQuery(String query) {
        // Sort query parameters alphabetically
        Map<String, String> params = new TreeMap<>();
        Arrays.stream(query.split("&"))
                .forEach(p -> {
                    String[] parts = p.split("=", 2);
                    if (parts.length == 2) {
                        params.put(parts[0], parts[1]);
                    }
                });
        return params.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .reduce((p1, p2) -> p1 + "&" + p2)
                .orElse(null);
    }

    private static String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}


