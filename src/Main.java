import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
public class Main {
    public static void printIndexMain(Map<String, Map<String, Map<Integer,Integer>>> tempInvertedIndex)
    {
        for (Map.Entry<String, Map<String, Map<Integer, Integer>>> wordEntry : tempInvertedIndex.entrySet()) {
            String word = wordEntry.getKey();
            Map<String, Map<Integer, Integer>> documentMap = wordEntry.getValue();

            System.out.println("Word: " + word);
            for (Map.Entry<String, Map<Integer, Integer>> docEntry : documentMap.entrySet()) {
                String documentId = docEntry.getKey();
                Map<Integer, Integer> docInfo = docEntry.getValue();
                int documentSize = 0;
                int frequency = 0;

                // Extract document size and frequency from docInfo map
                for (Map.Entry<Integer, Integer> sizeFreqEntry : docInfo.entrySet()) {
                    documentSize = sizeFreqEntry.getKey();
                    frequency = sizeFreqEntry.getValue();
                    break; // We assume there will only be one entry in docInfo
                }

                System.out.println(" - Document: " + documentId + ", Size: " + documentSize + ", Frequency: " + frequency);
            }
        }
    }
    public static void main(String[] args) throws IOException{
        Crawler crawl = new Crawler();
        Indexer indexer = new Indexer();
//        crawl.crawlMain();
//        indexer.indexMain();
//        indexer.printIndex();
        indexer.test("dev");
//        indexer.printIndex(); // for debuging
//        indexer.storeIndexInDatabase();
//        crawl.dropDataBase();
//        indexer.dropDataBase();

    }
}
