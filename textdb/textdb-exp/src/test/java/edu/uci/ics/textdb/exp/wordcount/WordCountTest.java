package edu.uci.ics.textdb.exp.wordcount;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.lucene.analysis.Analyzer;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.uci.ics.textdb.exp.wordcount.WordCountPayLoad;
import edu.uci.ics.textdb.exp.wordcount.WordCountIndexSource;
import edu.uci.ics.textdb.exp.wordcount.WordCountIndexSourcePredicate;
import edu.uci.ics.textdb.exp.wordcount.WordCountPayLoadPredicate;
import edu.uci.ics.textdb.api.constants.TestConstants;
import edu.uci.ics.textdb.api.constants.TestConstantsChinese;
import edu.uci.ics.textdb.api.constants.TestConstantsChineseWordCount;
import edu.uci.ics.textdb.api.exception.TextDBException;
import edu.uci.ics.textdb.api.tuple.Tuple;
import edu.uci.ics.textdb.exp.source.scan.ScanBasedSourceOperator;
import edu.uci.ics.textdb.exp.source.scan.ScanSourcePredicate;
import edu.uci.ics.textdb.exp.utils.DataflowUtils;
import edu.uci.ics.textdb.storage.DataWriter;
import edu.uci.ics.textdb.storage.RelationManager;
import edu.uci.ics.textdb.storage.constants.LuceneAnalyzerConstants;

public class WordCountTest {
    public static final String COUNT_TABLE = "wordcount_test";
    public static final String COUNT_CHINESE_TABLE = "wordcount_Chinese_test";
    
    public static HashMap<String, Integer> expectedResult = null;
    public static HashMap<String, Integer> expectedResultChinese = null;
    
    @BeforeClass
    public static void setUp() throws TextDBException {
        cleanUp();
        
        RelationManager relationManager = RelationManager.getRelationManager();
        // Create the people table and write tuples
        relationManager.createTable(COUNT_TABLE, "../index/test_tables/" + COUNT_TABLE, 
                TestConstants.SCHEMA_PEOPLE, LuceneAnalyzerConstants.standardAnalyzerString());
        DataWriter dataWriter = relationManager.getTableDataWriter(COUNT_TABLE);
        dataWriter.open();
        for (Tuple tuple : TestConstants.getSamplePeopleTuples()) {
            dataWriter.insertTuple(tuple);
        }
        dataWriter.close();
        
        expectedResult = computeExpectedResult(TestConstants.getSamplePeopleTuples(), TestConstants.DESCRIPTION,
                LuceneAnalyzerConstants.getStandardAnalyzer());
        
        
        relationManager.createTable(COUNT_CHINESE_TABLE, "../index/test_tables/" + COUNT_CHINESE_TABLE, 
                TestConstantsChineseWordCount.SCHEMA_PEOPLE, LuceneAnalyzerConstants.chineseAnalyzerString());
        DataWriter dataWriterChinese = relationManager.getTableDataWriter(COUNT_CHINESE_TABLE);
        dataWriterChinese.open();
        for (Tuple tuple : TestConstantsChineseWordCount.getSamplePeopleTuples()) {
            dataWriterChinese.insertTuple(tuple);
        }
        dataWriterChinese.close();
        
        expectedResultChinese = computeExpectedResult(TestConstantsChineseWordCount.getSamplePeopleTuples(),
                TestConstantsChineseWordCount.DESCRIPTION, LuceneAnalyzerConstants.getLuceneAnalyzer(
                        LuceneAnalyzerConstants.chineseAnalyzerString()));
    }
    
    @AfterClass
    public static void cleanUp() throws TextDBException {
        RelationManager.getRelationManager().deleteTable(COUNT_TABLE);
        RelationManager.getRelationManager().deleteTable(COUNT_CHINESE_TABLE);
        expectedResult = null;
        expectedResultChinese = null;
    }
    
    //Compute result by tuple's PayLoad.
    public static HashMap<String, Integer> computePayLoadWordCount(String tableName,
            String attribute) throws TextDBException {
        ScanBasedSourceOperator scanSource = new ScanBasedSourceOperator(new ScanSourcePredicate(tableName));
        WordCountPayLoad wordCount = null;
        HashMap<String, Integer> result = new HashMap<String, Integer>();
        
        if (tableName.equals(COUNT_TABLE)) {
            wordCount = new WordCountPayLoad(new WordCountPayLoadPredicate(TestConstants.DESCRIPTION,
                    LuceneAnalyzerConstants.standardAnalyzerString()));
        } else if (tableName.equals(COUNT_CHINESE_TABLE)) {
            wordCount = new WordCountPayLoad(new WordCountPayLoadPredicate(TestConstantsChineseWordCount.DESCRIPTION,
                    LuceneAnalyzerConstants.chineseAnalyzerString()) );
        }
        wordCount.setInputOperator(scanSource);
        
        wordCount.open();
        Tuple tuple;
        while ((tuple = wordCount.getNextTuple()) != null) {
            result.put((String) tuple.getField(WordCountPayLoad.WORD).getValue(), 
                    (Integer) tuple.getField(WordCountPayLoad.COUNT).getValue());
        }
        wordCount.close();

        return result;
    }
    
    //Compute result by scanning disk index.
    public static HashMap<String, Integer> computeWordCountIndexSourceResult(String tableName, String attribute)
            throws TextDBException {
        ScanBasedSourceOperator scanSource = new ScanBasedSourceOperator(new ScanSourcePredicate(tableName));
        
        WordCountIndexSource wordCountIndexSource = null;
        HashMap<String, Integer> result = new HashMap<String, Integer>();
        
        if (tableName.equals(COUNT_TABLE)) {
            wordCountIndexSource = new WordCountIndexSource(new WordCountIndexSourcePredicate(tableName, TestConstants.DESCRIPTION));
        } else if (tableName.equals(COUNT_CHINESE_TABLE)) {
            wordCountIndexSource = new WordCountIndexSource(new WordCountIndexSourcePredicate(
                    tableName, TestConstantsChineseWordCount.DESCRIPTION));
        }
        wordCountIndexSource.setInputOperator(scanSource);
        
        wordCountIndexSource.open();
        Tuple tuple;
        while((tuple = wordCountIndexSource.getNextTuple()) != null) {
            result.put((String) tuple.getField(WordCountIndexSource.WORD).getValue(), 
                    (Integer) tuple.getField(WordCountIndexSource.COUNT).getValue());
        }
        wordCountIndexSource.close();
        
        return result;
    }
    
    // Compute result from Constants.
    public static HashMap<String, Integer> computeExpectedResult(List<Tuple> tuplesList, String attribute, Analyzer analyzer) {
        HashMap<String, Integer> resultHashMap = new HashMap<String, Integer>();
        for (Tuple nextTuple : tuplesList) {
            String text = nextTuple.getField(attribute).getValue().toString();
            List<String> terms = DataflowUtils.tokenizeQuery(analyzer, text);
            for (String term : terms) {
                String key = term.toLowerCase();
                resultHashMap.put(key,
                        resultHashMap.get(key)==null ? 1 : resultHashMap.get(key) + 1);
            }
        }
        return resultHashMap;
    }
    
    //Check the equality of two HashMaps.
    public static boolean compareHashMap(Map<String, Integer> hm1, Map<String, Integer> hm2) {
        Map<String, Integer> hm3 = new HashMap<String, Integer>();
        
        if (hm1.size() != hm2.size()) {
            return false;
        }
        for (String key : hm1.keySet()) {
            if (hm2.containsKey(key)) {
                if (hm1.get(key) != hm2.get(key)) {
                    return false;
                }
            }
        }
        return true;
    }
    
    // Test counting by reading disk index method.
    @Test
    public void test1() throws TextDBException {
        HashMap<String, Integer> results = computePayLoadWordCount(COUNT_TABLE,
                TestConstants.DESCRIPTION);
        Assert.assertTrue(results.equals(expectedResult));
    }
    
    // Test WordCountIndexSource 
    @Test
    public void test2() throws TextDBException {
        HashMap<String, Integer> results = computeWordCountIndexSourceResult(COUNT_TABLE,
                TestConstants.DESCRIPTION);
        Assert.assertTrue(results.equals(expectedResult));
    }
    
    // Test counting using reading disk index method on Chinese words .
    @Test
    public void test3() throws TextDBException {
        HashMap<String, Integer> results = computeWordCountIndexSourceResult(COUNT_CHINESE_TABLE,
                TestConstantsChineseWordCount.DESCRIPTION);
//        printHashMap(results);
//        printHashMap(resultInConstantsChinese);
        Assert.assertTrue(results.equals(expectedResultChinese));
    }
    
 // Test words counting using payload reading method on Chinese .
    @Test
    public void test4() throws TextDBException {
        HashMap<String, Integer> results = computePayLoadWordCount(COUNT_CHINESE_TABLE,
                TestConstantsChineseWordCount.DESCRIPTION);
//        printHashMap(results);
//        printHashMap(resultInConstantsChinese);
        Assert.assertTrue(results.equals(expectedResultChinese));
    }
    
    public void printHashMap(HashMap<String, Integer> results) {
        Iterator<Entry<String, Integer>> itor = results.entrySet().iterator();
        System.out.println("\n================================\n "
                + "Print Hash Table elements.\n"
                + "=====================");
        while (itor.hasNext()) {
            Entry<String, Integer> entry = itor.next();
            System.out.println(entry.getKey() + ":" + entry.getValue());
//            Map.Entry pair = (Map.Entry)itor.next();
//            System.out.println(pair.getKey() + " = " + pair.getValue());
        }
    }
}
