import ca.rmen.porterstemmer.PorterStemmer;
import java.io.File;

import com.mongodb.client.MongoCollection;
import org.jsoup.Jsoup;
//import org.jsoup.nodes.Document;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import com.mongodb.client.FindIterable;

class Indexer {
    Map<String, Map<String, Map<Integer,Integer>>> invertedIndex; // (word,(URL,(sizeOfDoc,freq)))
    MongoDatabase database;
    MongoClient mongoClient;
    private static MongoCollection<org.bson.Document> Collection;
    private static MongoCollection<org.bson.Document> CollectionIndex;

    private static int documentCounter = 0;

    public Indexer() {
        this.invertedIndex = new HashMap<>();
        this.mongoClient = MongoClients.create("mongodb://localhost:27017");
        this.database = mongoClient.getDatabase("APT");
        Collection = database.getCollection("APT");
        CollectionIndex = database.getCollection("APT_index");
    }
    public void dropDataBase()
    {
        database.drop();
        System.out.println("DataBase dropped.");
    }
    public void storeIndexInDatabase() {
        // Clear the existing collection
        CollectionIndex.drop();

        // Iterate over the inverted index and store each entry in the database
        for (Map.Entry<String, Map<String, Map<Integer, Integer>>> wordEntry : invertedIndex.entrySet()) {
            String word = wordEntry.getKey();
            Map<String, Map<Integer, Integer>> documentMap = wordEntry.getValue();

            // Prepare the list of document URLs for the word
            List<String> documentIds = new ArrayList<>(documentMap.keySet());

            // Create a list to hold document metadata
            List<org.bson.Document> documentMetadataList = new ArrayList<>();

            // Iterate over the document map for the word
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

//                 Create a document to store the document metadata
                org.bson.Document documentMetadata = new org.bson.Document("documentId", documentId)
                        .append("documentSize", documentSize)
                        .append("frequency", frequency);

                documentMetadataList.add(documentMetadata);
            }

            // Create a document to be stored in the database
            org.bson.Document document = new org.bson.Document("word", word)
                    .append("documentIds", documentIds)
                    .append("documents", documentMetadataList);

            // Insert the document into the collection
            CollectionIndex.insertOne(document);
        }

        System.out.println("Inverted index and document metadata stored in the database.");
    }
    public org.bson.Document retrieveWordEntryFromDatabase(String word) {
        // Create a query to find the document with the specified word
        org.bson.Document query = new org.bson.Document("word", word);

        // Retrieve the document from the collection
        org.bson.Document result = CollectionIndex.find(query).first();

        return result;
    }
    public void test(String wordToRetrieve)
    {
        org.bson.Document wordEntry = retrieveWordEntryFromDatabase(wordToRetrieve);

        if (wordEntry != null) {
            String word = wordEntry.getString("word");
            List<String> documentIds = (List<String>) wordEntry.get("documentIds");
            List<org.bson.Document> documents = (List<org.bson.Document>) wordEntry.get("documents");

            // Process the retrieved data as needed
            System.out.println("Word: " + word);
            System.out.println("Document IDs: " + documentIds);
            System.out.println("Documents: " + documents);
        } else {
            System.out.println("No entry found for the word: " + wordToRetrieve);
        }
    }

    public Map<String, Map<String, Map<Integer,Integer>>> retrieveIndexFromDatabase() {
        Map<String, Map<String, Map<Integer,Integer>>> tempInvertedIndex = new HashMap<>();
//        invertedIndex.clear(); // Clear the existing inverted index

        // Retrieve all documents from the collection
        FindIterable<Document> documents = Collection.find();

        // Iterate over the retrieved documents
        for (Document document : documents) {
            String word = document.getString("word");
            String documentId = document.getString("documentId");
            int documentSize = document.getInteger("documentSize", 0);
            int frequency = document.getInteger("frequency", 0);

            // Update the inverted index map with the retrieved values
            tempInvertedIndex
                    .computeIfAbsent(word, k -> new HashMap<>())
                    .computeIfAbsent(documentId, k -> new HashMap<>())
                    .put(documentSize, frequency);
        }

        System.out.println("Inverted index retrieved from the database.");
        return tempInvertedIndex;
    }
    public void printIndex() {
        for (Map.Entry<String, Map<String, Map<Integer, Integer>>> wordEntry : invertedIndex.entrySet()) {
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
    public void printListString(List<String> l)
    {
        for (String s:l)
        {
            System.out.print(s+",");
        }
        System.out.print("\n");
    }
    public static List<String> removeStopWords(List<String> page) throws IOException {
        List<String> stopWords = Files.readAllLines(Paths.get("C:\\Users\\Mohammad\\Desktop\\CMP 2\\seconde semester\\APT\\indexer\\stop_words.txt"));
        List<String> cleanedPage = new ArrayList<>();

        for (String word : page) {
            if (!stopWords.contains(word)) {
                cleanedPage.add(word);
            }
        }

        return cleanedPage;
    }

    public void  parseDoc(List<String> words,String docString) throws IOException {
        // Load the HTML file
        org.jsoup.nodes.Document doc = Jsoup.parse(docString, "UTF-8");
        // Extract textual content from the HTML
        String text = doc.text().trim();
        String cleanedText = text.replaceAll("[^a-zA-Z ]", "");
        // Tokenize the text based on spaces, punctuation marks, and delimiters
        String[] textWords = cleanedText.split("[\\s\\p{Punct}]+");

        for (String word : textWords) {
            words.add(word);
        }
    }
    public void indexMain() {
        // Retrieve all documents from the CollectionURLs collection
        FindIterable<Document> documents = Collection.find();

        // Iterate over the retrieved documents
        for (Document document : documents) {
            String url = document.getString("url");
            String docString = document.getString("html");

            // Call the index method to index the document
            try {
                index(url, docString);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // Store the inverted index in the database
        storeIndexInDatabase();
    }

    public static void applyStemming(List<String> words) {
        PorterStemmer stemmer = new PorterStemmer();
        List<String> stemmedWords = new ArrayList<>();

        for (String word : words) {
           String stemmed = stemmer.stemWord(word);
           if(!stemmedWords.contains(stemmed))
           {
               stemmedWords.add(stemmed);
           }
        }
        // Replace the original words list with the stemmed words
        words.clear();
        words.addAll(stemmedWords);
    }
    private static String applyStemmingToWord(String word) {
        PorterStemmer stemmer = new PorterStemmer();
        return stemmer.stemWord(word);
    }
    public void index(String URL,String doc) throws IOException {
        List<String> words = new ArrayList<>();
        parseDoc(words, doc);
        words = removeStopWords(words);
        applyStemming(words);
        int docSize = words.size();
        String documentId = URL;

        for (String word : words) {
            invertedIndex.computeIfAbsent(word, k -> new HashMap<>())
                    .computeIfAbsent(documentId, k -> new HashMap<>())
                    .merge(docSize, 1, Integer::sum);
        }
    }
    public static List<String> processQuery(String Query) throws IOException {
        List<String> words = Arrays.asList(Query.split(" "));
        List<String> retWords = new ArrayList<>();
        words = removeStopWords(words);
        for(String word:words)
        {
            word = applyStemmingToWord(word);
            if(!retWords.contains(word))
            {
                retWords.add(word);
            }
        }
        return retWords;
    }

}
