import java.util.*;

public class Ranker implements Runnable  {

    public static class Cell {
        public String word;
        public ArrayList<Word_In_doc> all_Word_doc ;
        public double IDF;
        public Cell(String wordX,ArrayList<Word_In_doc> all_Word_docX){
            word=wordX;
            all_Word_doc=all_Word_docX;
            IDF=Math.log(6000.00/all_Word_doc.size());
        }

    }
    public static class Word_In_doc{

        public  int doc_ID;
        public int doc_size;
        public int [] positions;
        //public  int tag_num;
        public  int normal_num;
        public double TF;

        public Word_In_doc(int doc_IDX,int doc_sizeX,int [] positionsX ){
            doc_ID=doc_IDX;
            doc_size=doc_sizeX;
            positions=positionsX;
            normal_num=positionsX.length;
            TF= (double) normal_num /doc_size;

        }

    }
    public static class Site{
        public String link;
        public String doc;
        public int Popularity;
        public Site(String linkX,String docX,int PopularityX){
            link=linkX;
            doc=docX;
            Popularity=PopularityX;
        }
    }
    public static class Ranked_DOC{
        public  int doc_ID;
        public double rank;
        public Ranked_DOC(int id,double rankk){
            doc_ID=id;
            rank=rankk;
        }

    }



    public ArrayList <Cell>  cells;
    public int [] Qwords_pos_in_cells;
    // public Controller DbController;

    public ArrayList<Ranked_DOC> local_ranked_docs;
    public ArrayList<String> query_array;
    public  ArrayList<Site> sites;
    public Ranker(ArrayList<String> QueryArray,ArrayList<Cell> cellss,ArrayList<Ranked_DOC>rankedDocs,ArrayList<Site>sitesX)
    {
        Qwords_pos_in_cells= new int[QueryArray.size()];
        Arrays.fill(Qwords_pos_in_cells, -1);

        //cells=new ArrayList<>();
        cells=cellss;

        //query_array=new ArrayList<>();
        query_array=QueryArray;



        //local_ranked_docs=new ArrayList<>();
        local_ranked_docs=rankedDocs;

        sites=sitesX;

        //DbController=new Controller();
        //cells=DbController.Select_All_Cells();
        // we may send it to constructor to save extraction time
        // cus we will call new ranker every new query




    }
    public  void Pos_search(int start ,int end ){
        for(int i=start;i<end;i++)
            for(int j=0;j<query_array.size();j++)
                if((cells.get(i).word).equals(query_array.get(j)))
                    Qwords_pos_in_cells[j]=i;
    }
    public int IsIncluded(int id,ArrayList<Ranked_DOC> private_ranked_docs ){
        for(int i=0;i<private_ranked_docs.size();i++)
            if(private_ranked_docs.get(i).doc_ID==id)
                return i;
        return -1;
    }
    public int IsIncluded2(int id){
        for(int i=0;i<local_ranked_docs.size();i++)
            if(local_ranked_docs.get(i).doc_ID==id)
                return i;
        return -1;
    }
    // public void run()
    //    {
    //
    //        int ThreadNum=Integer.parseInt(Thread.currentThread().getName());
    //        int startPos= (int) Math.floor(((ThreadNum-1)/query_array.size())*cells.size());
    //        int endPos= (int) Math.ceil(((ThreadNum)/query_array.size())*cells.size());
    //        System.out.println("im thread num "+ThreadNum+" responsible for word "+ query_array.get(ThreadNum - 1) +" and searching  from pos. "+startPos+" to pos"+endPos+" in cells");
    //        Pos_search(startPos,endPos);
    //        Cell wantedCell= cells.get(Qwords_pos_in_cells[ThreadNum - 1]);
    //        for(int i=0;i<wantedCell.all_Word_doc.size();i++){
    //            int dummy=IsIncluded(wantedCell.all_Word_doc.get(i).doc_ID);
    //            double rankk=((wantedCell.all_Word_doc.get(i).normal_num) / (wantedCell.all_Word_doc.get(i).doc_size))*wantedCell.IDF;
    //            if(dummy==-1) {
    //                Ranked_DOC newTemp = new Ranked_DOC(wantedCell.all_Word_doc.get(i).doc_ID, rankk);
    //                private_ranked_docs.add(newTemp);
    //            }
    //            else
    //                private_ranked_docs.get(dummy).rank+=rankk;
    //
    //        }
    //
    //        for(Ranked_DOC d:private_ranked_docs)
    //        System.out.println("im thread num "+ThreadNum+" one of my private ranked is    ID:" +d.doc_ID+"  rank:  "+d.rank + " ");
    //
    //
    //
    //
    //    }
    public  void Popularity_fuc(ArrayList<Ranked_DOC> ranked_docs){
        double max_rank=ranked_docs.get(0).rank;

        for (Ranked_DOC d :ranked_docs){
            double To_add_to_popu=d.rank/max_rank;
            // ADD last popularity
            d.rank+= sites.get(d.doc_ID).Popularity;
            // update the current popularity
            sites.get(d.doc_ID).Popularity+=To_add_to_popu;
        }
    }

    public void run() {
        int ThreadNum = Integer.parseInt(Thread.currentThread().getName());
        int startPos = (int) Math.floor(((double) (ThreadNum - 1) / query_array.size()) * cells.size());
        int endPos = (int) Math.ceil(((double) (ThreadNum) / query_array.size()) * cells.size());
        System.out.println("im thread num " + ThreadNum + " responsible for word " + query_array.get(ThreadNum - 1) + " and searching  from pos. " + startPos + " to pos" + endPos + " in cells");
        Pos_search(startPos, endPos);


        if(Qwords_pos_in_cells[ThreadNum - 1] !=-1) {

            Cell wantedCell = cells.get(Qwords_pos_in_cells[ThreadNum - 1]);
            ArrayList<Ranked_DOC> private_ranked_docs = new ArrayList<>();
            for (int i = 0; i < wantedCell.all_Word_doc.size(); i++) {
                int dummy = IsIncluded(wantedCell.all_Word_doc.get(i).doc_ID, private_ranked_docs);
                double rankk = ((double) (wantedCell.all_Word_doc.get(i).normal_num) / (wantedCell.all_Word_doc.get(i).doc_size)) * wantedCell.IDF;
                if (dummy == -1) {
                    Ranked_DOC newTemp = new Ranked_DOC(wantedCell.all_Word_doc.get(i).doc_ID, rankk);
                    synchronized (this) {
                        private_ranked_docs.add(newTemp);
                    }
                } else {
                    synchronized (this) {
                        private_ranked_docs.get(dummy).rank += rankk;
                    }
                }
            }

            synchronized (this) {
                for (Ranked_DOC doc : private_ranked_docs) {
                    int idx = IsIncluded2(doc.doc_ID);
                    if (idx == -1) {
                        local_ranked_docs.add(doc);
                    } else {
                        local_ranked_docs.get(idx).rank += doc.rank;
                    }
                }
            }

            for (Ranked_DOC d : private_ranked_docs)
                System.out.println("im thread num " + ThreadNum + " one of my private ranked is    ID:" + d.doc_ID + "  rank:  " + d.rank + " ");
        }

    }

    public static void main(String[] args) throws InterruptedException {
        /* test case CHATGPT
        // create a sample query array
        ArrayList<String> query = new ArrayList<>();
        query.add("word_1");
        query.add("word_5");
        query.add("word_8");

        // create some sample cells with words and associated documents
        ArrayList<Cell> cells = new ArrayList<>();

        Random rand = new Random();
        for (int i = 0; i < 10000; i++) {
            int docSize = rand.nextInt(1000) + 1;
            int numWords = rand.nextInt(10) + 1;
            ArrayList<Word_In_doc> wordDocs = new ArrayList<>();
            for (int j = 0; j < numWords; j++) {
                int numPositions = rand.nextInt(docSize) + 1;
                int[] positions = new int[numPositions];
                for (int k = 0; k < numPositions; k++) {
                    positions[k] = rand.nextInt(docSize) + 1;
                }
                wordDocs.add(new Word_In_doc(i, docSize, positions));
            }
            cells.add(new Cell("word_" + i, wordDocs));
        }

        // create some sample ranked documents
        ArrayList<Ranked_DOC> ranked_docs = new ArrayList<>();

        // create a new Ranker instance and run it
        Ranker ranker = new Ranker(query, cells, ranked_docs);

        ArrayList<Thread> threads = new ArrayList<>();
        for (int i = 0; i < query.size(); i++) {
            Thread thread = new Thread(ranker);
            thread.setName(String.valueOf(i + 1));
            threads.add(thread);
        }

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        // print out the ranked documents
        for (Ranked_DOC d : ranked_docs) {
            System.out.println("ID: " + d.doc_ID + " Rank: " + d.rank);
        }
        */


        // create a sample query array
        ArrayList<String> query = new ArrayList<>();
        query.add("hello");
        query.add("world");

        // create some sample cells with words and associated documents
        ArrayList<Cell> cells = new ArrayList<>();
        ArrayList<Word_In_doc> wordDocs1 = new ArrayList<>();
        wordDocs1.add(new Word_In_doc(1, 100, new int[] {1, 5, 10}));
        wordDocs1.add(new Word_In_doc(2, 200, new int[] {2, 6, 10, 15}));
        Cell cell1 = new Cell("hello", wordDocs1);
        cells.add(cell1);

        ArrayList<Word_In_doc> wordDocs2 = new ArrayList<>();
        wordDocs2.add(new Word_In_doc(1, 100, new int[] {1, 5, 10}));
        wordDocs2.add(new Word_In_doc(3, 150, new int[] {2, 20, 30}));
        Cell cell2 = new Cell("world", wordDocs2);
        cells.add(cell2);

        // create some sample ranked documents
        ArrayList<Ranked_DOC> ranked_docs = new ArrayList<>();
        // ranked_docs.add(new Ranked_DOC(1, 0.5));
        // ranked_docs.add(new Ranked_DOC(2, 0.3));
        // ranked_docs.add(new Ranked_DOC(3, 0.2));
        ArrayList<Site>sites=new ArrayList<>();

        // create a new Ranker instance and run it
        Ranker ranker = new Ranker(query, cells, ranked_docs,sites);

        Thread t1 = new Thread(ranker);
        Thread t2 = new Thread(ranker);

        t1.setName("1");
        t2.setName("2");

        t1.start();
        t2.start();

        t1.join();
        t2.join();



        Comparator<Ranked_DOC> rankComparator = new Comparator<Ranked_DOC>() {
            @Override
            public int compare(Ranked_DOC p1, Ranked_DOC p2) {
                return Double.compare(p2.rank, p1.rank);
            }
        };

        Collections.sort(ranked_docs, rankComparator);

        ranker.Popularity_fuc(ranked_docs); // add the last popularity and edit to the new popularity

        for (Ranked_DOC d :ranked_docs)
            System.out.println(" ID:" + d.doc_ID + "  rank:  " + d.rank + " ");



}


}