package edu.uci.ics.textdb.dataflow.regexmatch;

import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import edu.uci.ics.textdb.api.common.IPredicate;
import edu.uci.ics.textdb.api.common.ITuple;
import edu.uci.ics.textdb.api.dataflow.ISourceOperator;
import edu.uci.ics.textdb.common.constants.LuceneConstants;
import edu.uci.ics.textdb.common.constants.TestConstants;
import edu.uci.ics.textdb.dataflow.common.RegexPredicate;
import edu.uci.ics.textdb.dataflow.source.ScanBasedSourceOperator;
import edu.uci.ics.textdb.dataflow.utils.TestUtils;
import edu.uci.ics.textdb.storage.LuceneDataStore;

/**
 * Created by chenli on 3/25/16.
 */
public class RegexMatcherTest {
    
    private RegexMatcher regexMatcher;
    private LuceneDataStore dataStore;
    
    @Before
    public void setUp() throws Exception{
        dataStore = new LuceneDataStore(LuceneConstants.INDEX_DIR);
        dataStore.clearData();
        setUpPeople();
 
    }
    
    private void setUpPeople() throws Exception {
        dataStore.storeData(TestConstants.SAMPLE_SCHEMA_PEOPLE, TestConstants.getSamplePeopleTuples());
    }
    
    
    @After
    public void cleanUp() throws Exception{
        dataStore.clearData();
    }
    
    @Test
    public void testNameGetNextTuple() throws Exception{
        String regex = "b.*"; //matches bruce and brad
        String fieldName = TestConstants.FIRST_NAME;
        IPredicate predicate = new RegexPredicate(regex, fieldName);
        String dataDirectory = LuceneConstants.INDEX_DIR;
        ISourceOperator sourceOperator = new ScanBasedSourceOperator(dataDirectory, TestConstants.SAMPLE_SCHEMA_PEOPLE);
        List<ITuple> tuples = TestConstants.getSamplePeopleTuples();
        
        regexMatcher = new RegexMatcher(predicate, sourceOperator);
        regexMatcher.open();
        ITuple nextTuple = null;
        int numTuples = 0;
        while((nextTuple = regexMatcher.getNextTuple()) != null){
            boolean contains = TestUtils.contains(tuples, nextTuple);
            Assert.assertTrue(contains);
            numTuples ++;
        }
        Assert.assertEquals(2, numTuples);
        regexMatcher.close();
    }
    
    @Test
    public void testURLGetNextTuple() throws Exception {
    	String urlRegex = "^(https?:\\/\\/)?([\\da-z\\.-]+)\\.([a-z\\.]{2,6})([\\/\\w \\.-]*)*\\/?$";
    	String fieldName = TestConstants.URL;
    	IPredicate predicate = new RegexPredicate(urlRegex, fieldName);
        String dataDirectory = LuceneConstants.INDEX_DIR;
        ISourceOperator sourceOperator = new ScanBasedSourceOperator(dataDirectory, TestConstants.SAMPLE_SCHEMA_CORP);
        List<ITuple> tuples = TestConstants.getSampleCorpTuples();
        
        regexMatcher = new RegexMatcher(predicate, sourceOperator);
        regexMatcher.open();
        ITuple nextTuple = null;
        int numTuples = 0;
        while((nextTuple = regexMatcher.getNextTuple()) != null){
            boolean contains = TestUtils.contains(tuples, nextTuple);
            Assert.assertTrue(contains);
            numTuples ++;
        }
        Assert.assertEquals(3, numTuples);
        regexMatcher.close();
    }

}