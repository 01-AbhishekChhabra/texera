package edu.uci.ics.textdb.perftest.sample;

/**
 * Created by Chang on 6/26/17.
 */

import edu.uci.ics.textdb.api.constants.TestConstantsChinese;
import edu.uci.ics.textdb.api.engine.Engine;
import edu.uci.ics.textdb.api.engine.Plan;
import edu.uci.ics.textdb.api.field.StringField;
import edu.uci.ics.textdb.api.field.TextField;
import edu.uci.ics.textdb.api.span.Span;
import edu.uci.ics.textdb.api.tuple.Tuple;
import edu.uci.ics.textdb.exp.dictionarymatcher.*;
import edu.uci.ics.textdb.exp.keywordmatcher.KeywordMatchingType;
import edu.uci.ics.textdb.exp.nlp.entity.NlpEntityOperator;
import edu.uci.ics.textdb.exp.nlp.entity.NlpEntityPredicate;
import edu.uci.ics.textdb.exp.nlp.entity.NlpEntityType;
import edu.uci.ics.textdb.exp.regexmatcher.RegexMatcher;
import edu.uci.ics.textdb.exp.regexmatcher.RegexPredicate;
import edu.uci.ics.textdb.exp.sink.FileSink;
import edu.uci.ics.textdb.exp.sink.tuple.TupleSink;
import edu.uci.ics.textdb.exp.source.scan.ScanBasedSourceOperator;
import edu.uci.ics.textdb.exp.source.scan.ScanSourcePredicate;
import edu.uci.ics.textdb.exp.utils.DataflowUtils;
import edu.uci.ics.textdb.perftest.medline.MedlineIndexWriter;
import edu.uci.ics.textdb.perftest.promed.PromedSchema;
import edu.uci.ics.textdb.perftest.utils.PerfTestUtils;
import edu.uci.ics.textdb.perftest.wikipedia.WikipediaIndexWriter;
import edu.uci.ics.textdb.storage.DataWriter;
import edu.uci.ics.textdb.storage.RelationManager;
import edu.uci.ics.textdb.storage.constants.LuceneAnalyzerConstants;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import sun.misc.Perf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.URISyntaxException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Dictionary;


public class WikipediaExtraction {

    public static final String WIKIPEDIA_SAMPLE_TABLE = "wikipedia";
    public static int number = 0;

    public static String wikipediaFilesDirectory = PerfTestUtils.getResourcePath("/sample-data-files/wikipedia");
    public static String wikipediaIndexDirectory = PerfTestUtils.getResourcePath("/index/standard/wikipedia");
    public static String sampleDataFilesDirectory = PerfTestUtils.getResourcePath("/sample-data-files");
//
//    static {
//        try {
//            // Finding the absolute path to the sample data files directory and index directory
//
//            // Checking if the resource is in a jar
//            String referencePath = SampleExtraction.class.getResource("").toURI().toString();
//            if(referencePath.substring(0, 3).equals("jar")) {
//                medlineFilesDirectory = "/textdb-perftest/src/main/resources/sample-data-files/medline/";
//                medlineIndexDirectory = "/textdb-perftest/src/main/resources/index/standard/medline/";
//                sampleDataFilesDirectory = "/textdb-perftest/src/main/resources/sample-data-files/";
//            }
//            else {
//                medlineFilesDirectory = Paths.get(SampleExtraction.class.getResource("/sample-data-files/medline")
//                        .toURI())
//                        .toString();
//                medlineIndexDirectory = Paths.get(SampleExtraction.class.getResource("/index/standard")
//                        .toURI())
//                        .toString() + "/medline";
//                System.out.println(medlineIndexDirectory);
//                sampleDataFilesDirectory = Paths.get(SampleExtraction.class.getResource("/sample-data-files")
//                        .toURI())
//                        .toString();
//            }
//        }
//        catch(URISyntaxException | FileSystemNotFoundException e) {
//            e.printStackTrace();
//        }
//    }
    public static void main(String[] args) throws Exception {
        // write the index of data files
        // index only needs to be written once, after the first run, this function can be commented out
//       writeSampleIndex();
       System.out.println(number);
       String regex;
       regex = "(\\{\\{Cite book\\| last = )([A-Z][A-Za-z0-9\\-\\s]+)(\\| first = )([A-Z][A-Za-z0-9\\-\\s]+)(\\| "
       		+ "title = )([A-Z][A-Za-z0-9\\-\\s]+)(\\| publisher = )([A-Z][A-Za-z0-9\\-\\s]+)(\\| "
       		+ "location = )([A-Z][A-Za-z0-9\\-\\s]+)(\\| year = \\d{4} )(\\| isbn = \\d-\\d{1,10}-\\d{1,10}-\\d )(\\|page=\\d+)(\\}\\})";
       SCAN_REGEX_SINK("("+regex+")"); // 26s
//       SCAN_REGEX_SINK(regex); // 27s
       SCAN_REGEX_SINK("(\\{\\{Cite book\\| last = )");
       SCAN_REGEX_SINK("(\\| first = )");
       SCAN_REGEX_SINK("(\\| title = )");
       SCAN_REGEX_SINK("(\\| publisher = )");
       SCAN_REGEX_SINK("(\\| location = )");
       SCAN_REGEX_SINK("(\\| year = \\d{4} )");
//       
       regex = "(\\{\\{Cite book\\|)([^\\{\\}]*)(\\| isbn = \\d-\\d{1,10}-\\d{1,10}-\\d )(\\|page=\\d+)([^\\{\\}]*)(\\}\\})";
          SCAN_REGEX_SINK("("+regex+")"); // 26s
//          SCAN_REGEX_SINK(regex); // 
       SCAN_REGEX_SINK("(\\{\\{Cite book\\|)");
       SCAN_REGEX_SINK("(\\| isbn = \\d-\\d{1,10}-\\d{1,10}-\\d )");
       SCAN_REGEX_SINK("(\\}\\})");
//
//       regex = "\\{\\{[^\\{\\}]+\\}\\}";
//       SCAN_REGEX_SINK("("+regex+")");
//       SCAN_REGEX_SINK(regex);
       
       regex = "<ref (name= ([A-Za-z0-9]|\\.|\\-|\\s)+)?(>\\{\\{cite journal )(\\| title = )([A-Za-z0-9]|\\.|\\-|\\s)+"
         		+ "(\\| author = )(\\w|\\.|\\-|,|\\s)*(Patrick J\\. Keeling|Charles Short|Laura Wegener Parfrey|"
         		+ "Erika Barbero|Elyse Lasser|Micah Dunthorn|"
         		+ "Debashish Bhattacharya|David J Patterson|Burki F|Shalchian-Tabrizi K|Minge M)"
         		+ "(\\w|\\.|\\-|,|\\s)*\\| [^\\{\\}]*(\\}\\}<\\/ref>)";
       SCAN_REGEX_SINK("("+regex+")"); // 28s
//       SCAN_REGEX_SINK(regex); // 32s
       SCAN_REGEX_SINK("(<ref )");
       SCAN_REGEX_SINK("(>\\{\\{cite journal )");
       SCAN_REGEX_SINK("(\\| title = )");
       SCAN_REGEX_SINK("(\\| author = )");
       SCAN_REGEX_SINK("(Patrick J\\. Keeling|Charles Short|Laura Wegener Parfrey|"
         		+ "Erika Barbero|Elyse Lasser|Micah Dunthorn|"
         		+ "Debashish Bhattacharya|David J Patterson|Burki F|Shalchian-Tabrizi K|Minge M)");
       SCAN_REGEX_SINK("(\\}\\}<\\/ref>)");
       
       regex = "<ref (name= [A-Za-z0-9\\.\\-\\s]+)?(>\\{\\{cite journal )\\|[^\\{\\}]*"
         		+ "(\\| url = )(http:\\/\\/www.[\\w-]+.(org|com|edu|gov|net)(\\/[\\w-\\.])*) \\|[^\\{\\}]*(\\}\\}<\\/ref>)";
       SCAN_REGEX_SINK("("+regex+")"); // 26s
//       SCAN_REGEX_SINK(regex); // 230s
       SCAN_REGEX_SINK("(<ref )");
       SCAN_REGEX_SINK("(>\\{\\{cite journal )");
       SCAN_REGEX_SINK("(\\| url = )");
       SCAN_REGEX_SINK("(\\}\\}<\\/ref>)");
       // perform the extraction task
    }



    public static Tuple parsePromedHTML(String fileName, String content) {
        try {
            Document parsedDocument = Jsoup.parse(content);
            String mainText = parsedDocument.getElementById("preview").text();
            Tuple tuple = new Tuple(PromedSchema.PROMED_SCHEMA, new StringField(fileName), new TextField(mainText));
            return tuple;
        } catch (Exception e) {
            return null;
        }
    }

    public static void writeSampleIndex() throws Exception {
        // parse the original file
        File sourceFileFolder = new File(wikipediaFilesDirectory);
        ArrayList<Tuple> fileTuples = new ArrayList<>();
        for (File htmlFile : sourceFileFolder.listFiles()) {
            StringBuilder sb = new StringBuilder();
            Scanner scanner = new Scanner(htmlFile);
            while (scanner.hasNext()) {
                Tuple tuple = WikipediaIndexWriter.recordToTuple(scanner.nextLine());
                if (tuple != null) {
                    fileTuples.add(tuple);
                }
            }
            scanner.close();
        }

        // write tuples into the table
        RelationManager relationManager = RelationManager.getRelationManager();

        relationManager.deleteTable(WIKIPEDIA_SAMPLE_TABLE);
        relationManager.createTable(WIKIPEDIA_SAMPLE_TABLE, wikipediaIndexDirectory,
                WikipediaIndexWriter.SCHEMA_WIKIPEDIA, LuceneAnalyzerConstants.standardAnalyzerString());

        DataWriter dataWriter = relationManager.getTableDataWriter(WIKIPEDIA_SAMPLE_TABLE);
        dataWriter.open();
        for (Tuple tuple : fileTuples) {
            dataWriter.insertTuple(tuple);
            number += 1;
        }
        dataWriter.close();
    }
    
    public static void SCAN_REGEX_SINK(String regex) throws Exception {
    	ScanSourcePredicate scanSourcePredicate = new ScanSourcePredicate(WIKIPEDIA_SAMPLE_TABLE);
        ScanBasedSourceOperator scanBasedSourceOperator = new ScanBasedSourceOperator(scanSourcePredicate);
        List<String> attributeNames = Arrays.asList(WikipediaIndexWriter.TEXT);
    
        RelationManager relationManager = RelationManager.getRelationManager();
        String luceneAnalyzerStr = relationManager.getTableAnalyzerString(WIKIPEDIA_SAMPLE_TABLE);
        
        RegexPredicate regexPredicate = 
        		new RegexPredicate(regex, 
        				attributeNames, "report");
        
        RegexMatcher regexMatcher = new RegexMatcher(regexPredicate);
        //      regexMatcher.setInputOperator(dictionaryMatcher);
        regexMatcher.setInputOperator(scanBasedSourceOperator);


        TupleSink tupleSink = new TupleSink();
        //      tupleSink.setInputOperator(regexMatcher);
        tupleSink.setInputOperator(regexMatcher);

        long startMatchTime = System.currentTimeMillis();
        tupleSink.open();
        List<Tuple> result = tupleSink.collectAllTuples();
        tupleSink.close();
        long endMatchTime = System.currentTimeMillis();
        double matchTime = (endMatchTime - startMatchTime) / 1000.0;

//        for(Tuple t: result){
//        	System.out.println(t.getField(0).getValue().toString());
//        	System.out.println(t.getField("abstract").toString());
//        	for(Span span: (List<Span>) t.getField("report").getValue()){
//        		System.out.println(span.getAttributeName() + " " + span.getStart() + " " + span.getEnd() + " " + span.getValue());
//        	}
//        	//         System.out.println("This is for another dictionary matcher");
//        	//          for(Span span: (List<Span>) t.getField(disease).getValue()){
//        	//              System.out.println(span.getAttributeName() + " " + span.getStart() + " " + span.getEnd() + " " + span.getValue());
//        	//          }
//        }
        int count = result.size();
        System.out.println("================================================");
        System.out.println(regex);
        System.out.println("Done_number of tuples" + count);
        System.out.println("Total matching time: " + matchTime);
    }
    
    
    public static void extractDrugsDiseases() throws Exception {
    	
    	ScanSourcePredicate scanSourcePredicate = new ScanSourcePredicate(WIKIPEDIA_SAMPLE_TABLE);
        ScanBasedSourceOperator scanBasedSourceOperator = new ScanBasedSourceOperator(scanSourcePredicate);
        List<String> attributeNames = Arrays.asList(MedlineIndexWriter.ABSTRACT);
        
        String drug = "drug";
        String disease = "disease";
        
        RelationManager relationManager = RelationManager.getRelationManager();
        String luceneAnalyzerStr = relationManager.getTableAnalyzerString(WIKIPEDIA_SAMPLE_TABLE);
        
        String dic_drugs_path = PerfTestUtils.getResourcePath("/dictionary/drugs_dict.txt");
        List<String> list_drugs = tokenizeFile(dic_drugs_path);
        
        String dic_disease_path = PerfTestUtils.getResourcePath("/dictionary/disease_dict.txt");
        List<String> list_disease = tokenizeFile(dic_disease_path);
        
        edu.uci.ics.textdb.exp.dictionarymatcher.Dictionary dic_drugs = 
        		new edu.uci.ics.textdb.exp.dictionarymatcher.Dictionary(list_drugs);
        edu.uci.ics.textdb.exp.dictionarymatcher.Dictionary dic_disease = 
        		new edu.uci.ics.textdb.exp.dictionarymatcher.Dictionary(list_disease);
        
        System.out.println("size of drugs dictionary" + dic_drugs.getDictionaryEntries().size());
        System.out.println(" size of disease dictionary" + dic_disease.getDictionaryEntries().size());
        
        
//        DictionaryPredicate dictionaryPredicate = 
//        		new DictionaryPredicate(dic_drugs, attributeNames, luceneAnalyzerStr, 
//        				KeywordMatchingType.SUBSTRING_SCANBASED, drug);
//        DictionaryMatcher dictionaryMatcher = new DictionaryMatcher(dictionaryPredicate);
//        
//        dictionaryMatcher.setInputOperator(scanBasedSourceOperator);
        
//        DictionaryPredicate dictionaryPredicate1 = 
//        		new DictionaryPredicate(dic_disease, attributeNames, luceneAnalyzerStr, 
//        				KeywordMatchingType.SUBSTRING_SCANBASED, disease);
//        DictionaryMatcher dictionaryMatcher1 = new DictionaryMatcher(dictionaryPredicate1);
//        
//        dictionaryMatcher1.setInputOperator(dictionaryMatcher);

//        RegexPredicate regexPredicate = 
//        		new RegexPredicate("(In )?\\d+ cases?(?: of)?(?: transplantation),? *(?:the recipient was made)", 
//        				attributeNames, "report");
        RegexPredicate regexPredicate = 
        		new RegexPredicate("((taking|injecting|injections?|usage|using|dose|dosage|prescriptions?|prescribing)( of)?( the)? "
        				+ "(?:[A-Za-z0-9]+ *){1,3}"
        				+ " (injection|tablets?|inhalation|vaccine|capsules?|inhibitors?|powder|gel|cream|oinment)).*,", 
        				attributeNames, "report");
//        RegexPredicate regexPredicate = 
//        		new RegexPredicate("\\s*(abcd|pqrs)(word)?([A-Z]\\w){1,2}(?:[A-Za-z0-9]+ *){1,3}\\s*hello", 
//        				attributeNames, "report");
//        RegexPredicate regexPredicate = 
//        		new RegexPredicate("a.*b",
//        				attributeNames, "report");
        

//        RegexPredicate regexPredicate = 
//        		new RegexPredicate("(taking|injecting|injections?|usage|using|dose|dosage|prescriptions?|prescribing)( of)?( the)? "
//        				+ "<drug>"
//        				+ "( )?(injection|tablets?|inhalation|vaccine|capsules?|inhibitors?|powder|gel|cream|oinment)?( )?(can|could|will|would|should)? (cures?|heals?|solves?|resolves?|improves?|reduces?|impacts?|helps?)( the)? "
//        				+ "<disease>"
//        				+ "( )?(syndrome|infection|sickness|deficiency|disorder|defects?|disease|problems?)? (successfully|partially|positively|negatively|completely|gradually|quickly|significantly|slowly)", 
//        				attributeNames, "report");
//        RegexPredicate regexPredicate = 
//        		new RegexPredicate("(The )?surgeon (involved|involving|involves?) <drug> willing to <disease>( of)? their ovarian tissue.", 
//        				attributeNames, "report");
        RegexMatcher regexMatcher = new RegexMatcher(regexPredicate);
//        regexMatcher.setInputOperator(dictionaryMatcher);
        regexMatcher.setInputOperator(scanBasedSourceOperator);
        
        
        TupleSink tupleSink = new TupleSink();
//        tupleSink.setInputOperator(regexMatcher);
        tupleSink.setInputOperator(regexMatcher);
        
        long startMatchTime = System.currentTimeMillis();
        tupleSink.open();
        List<Tuple> result = tupleSink.collectAllTuples();
        tupleSink.close();
        long endMatchTime = System.currentTimeMillis();
        double matchTime = (endMatchTime - startMatchTime) / 1000.0;
        
        for(Tuple t: result){
            System.out.println(t.getField(0).getValue().toString());
            System.out.println(t.getField("abstract").toString());
            for(Span span: (List<Span>) t.getField("report").getValue()){
                System.out.println(span.getAttributeName() + " " + span.getStart() + " " + span.getEnd() + " " + span.getValue());
            }
//           System.out.println("This is for another dictionary matcher");
//            for(Span span: (List<Span>) t.getField(disease).getValue()){
//                System.out.println(span.getAttributeName() + " " + span.getStart() + " " + span.getEnd() + " " + span.getValue());
//            }
        }
        int count = result.size();
        System.out.println("Done_number of tuples" + count);
        System.out.println("Total matching time: " + matchTime);
        
    }
    public static void extractPersonLocation() throws Exception {
    //    ScanSourcePredicate scanSourcePredicate = new ScanSourcePredicate(PEOPLE_TABLE);
     //   ScanBasedSourceOperator scanBasedSourceOperator = new ScanBasedSourceOperator(scanSourcePredicate);
        ScanSourcePredicate scanSourcePredicate = new ScanSourcePredicate(WIKIPEDIA_SAMPLE_TABLE);
        ScanBasedSourceOperator scanBasedSourceOperator = new ScanBasedSourceOperator(scanSourcePredicate);
        List<String> attributeNames = Arrays.asList(MedlineIndexWriter.ABSTRACT);
        String org = "organization";
        String per = "person";
        RelationManager relationManager = RelationManager.getRelationManager();
        String luceneAnalyzerStr = relationManager.getTableAnalyzerString(WIKIPEDIA_SAMPLE_TABLE);
        String dic_org_path = PerfTestUtils.getResourcePath("/dictionary/dic_noun.txt");
        List<String> list_org = tokenizeFile(dic_org_path);
       // List<String> list_org = new ArrayList<>();
        String dic_per_path = PerfTestUtils.getResourcePath("/dictionary/adj_dic.txt");
        List<String> list_per = tokenizeFile(dic_per_path);
        edu.uci.ics.textdb.exp.dictionarymatcher.Dictionary dic_per = 
        		new edu.uci.ics.textdb.exp.dictionarymatcher.Dictionary(list_per);
        edu.uci.ics.textdb.exp.dictionarymatcher.Dictionary dic_org = 
        		new edu.uci.ics.textdb.exp.dictionarymatcher.Dictionary(list_org);
        System.out.println("size of organization" + dic_org.getDictionaryEntries().size());
        System.out.println(" size of person" + dic_per.getDictionaryEntries().size());
        DictionaryPredicate dictionaryPredicate = new DictionaryPredicate(dic_org, attributeNames, luceneAnalyzerStr, KeywordMatchingType.SUBSTRING_SCANBASED, org);
        DictionaryMatcher dictionaryMatcher = new DictionaryMatcher(dictionaryPredicate);
        dictionaryMatcher.setInputOperator(scanBasedSourceOperator);
        DictionaryPredicate dictionaryPredicate1 = new DictionaryPredicate(dic_per, attributeNames, luceneAnalyzerStr, KeywordMatchingType.SUBSTRING_SCANBASED, per);
        DictionaryMatcher dictionaryMatcher1 = new DictionaryMatcher(dictionaryPredicate1);
        dictionaryMatcher1.setInputOperator(dictionaryMatcher);
        String name = "locationspanresult";
     //   NlpEntityPredicate nlpEntityPredicate = new NlpEntityPredicate(NlpEntityType.ADJECTIVE, Arrays.asList(MedlineIndexWriter.ABSTRACT), name);
      //  NlpEntityOperator nlpEntityOperator = new NlpEntityOperator(nlpEntityPredicate);
        //SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy-HH-mm-ss");
        //FileSink fileSink = new FileSink(
          //      new File(sampleDataFilesDirectory + "/person-location-result-"
            //            + sdf.format(new Date(System.currentTimeMillis())).toString() + ".txt"));

        //fileSink.setToStringFunction((tuple -> DataflowUtils.getTupleString(tuple)));
    //    nlpEntityOperator.setInputOperator(scanBasedSourceOperator);
       // fileSink.setInputOperator(nlpEntityOperator);
       // Plan extractPersonPlan = new Plan(fileSink);
       // Engine.getEngine().evaluate(extractPersonPlan);
        TupleSink tupleSink = new TupleSink();
        tupleSink.setInputOperator(dictionaryMatcher1);
        tupleSink.open();
        List<Tuple> result = tupleSink.collectAllTuples();
        for(Tuple t: result){
            System.out.println(t.getField(0).getValue().toString());
            System.out.println(t.getField("abstract").toString());
            for(Span span: (List<Span>) t.getField(org).getValue()){
                System.out.println(span.getAttributeName() + " " + span.getStart() + " " + span.getEnd() + " " + span.getValue());
            }
           System.out.println("This is for another dictionary matcher");
            for(Span span: (List<Span>) t.getField(per).getValue()){
                System.out.println(span.getAttributeName() + " " + span.getStart() + " " + span.getEnd() + " " + span.getValue());
            }
        }
        int count = result.size();
//        List<String> results = tupleSink.collectAttributes(name);
//        FileWriter writer = new FileWriter("adj_dic.txt");
//        int cnt = 0;
//        for(String str: results) {
//            writer.write(str);
//            cnt += 1;
//            writer.write(", ");
//        }
//        writer.close();
        System.out.println("Done_number of tuples" + count);
     //   System.out.print("done_number of dictionary element" + cnt);

    }
    public static List<String> tokenizeFile(String inputfile) {
        List<String> tokenlist = new ArrayList<String>();
        try {
            File file = new File(inputfile);
            FileReader in = new FileReader(file);
            BufferedReader reader = new BufferedReader(in);
            String tmp = null;
            tmp = reader.readLine();
            int count = 1000000;
            while (tmp !=null) {
                String tokenstring = tmp.trim();
                if (tokenstring.contains("[^a-zA-Z0-9' ]")){
                    continue;
                }
                //String change = tokenstring.replaceAll(",", " ").toLowerCase();
                String[] a =tokenstring.trim().split(",");
                List<String> temp = Arrays.asList(a);
                temp.stream().forEach(s -> tokenlist.add(s.trim()));
                if (tokenlist.size() >= count) break;
                //tokenlist.addAll(temp);
                tmp = reader.readLine();
            }
            reader.close();
            in.close();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return tokenlist;


    }




}
