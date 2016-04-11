/**
 * 
 */
package edu.uci.ics.textdb.dataflow.source;

import java.text.ParseException;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import edu.uci.ics.textdb.api.common.ITuple;
import edu.uci.ics.textdb.common.constants.LuceneConstants;
import edu.uci.ics.textdb.common.constants.TestConstants;
import edu.uci.ics.textdb.common.exception.DataFlowException;
import edu.uci.ics.textdb.dataflow.utils.TestUtils;
import edu.uci.ics.textdb.storage.DataStore;

/**
 * @author sandeepreddy602
 *
 */
public class ScanBasedSourceOperatorTest {
    
    private DataStore dataStore;
    private ScanBasedSourceOperator scanBasedSourceOperator;
    
    @Before
    public void setUp() throws Exception{
        dataStore = new DataStore(LuceneConstants.INDEX_DIR);
        dataStore.clearData();
        dataStore.storeData(TestConstants.SAMPLE_SCHEMA, TestConstants.getSampleTuples());
        scanBasedSourceOperator = new ScanBasedSourceOperator(
                LuceneConstants.INDEX_DIR, TestConstants.SAMPLE_SCHEMA);
    }
    
    @After
    public void cleanUp() throws Exception{
        dataStore.clearData();
    }
    
    @Test
    public void testFlow() throws DataFlowException, ParseException{
        List<ITuple> tuples = TestConstants.getSampleTuples();
        scanBasedSourceOperator.open();
        ITuple nextTuple = null;
        int numTuples = 0;
        while((nextTuple  = scanBasedSourceOperator.getNextTuple()) != null){
            //Checking if the tuple retrieved is present in the samplesTuples
            boolean contains = TestUtils.contains(tuples, nextTuple);
            Assert.assertTrue(contains);
            numTuples ++;
        }
        Assert.assertEquals(tuples.size(), numTuples);
        scanBasedSourceOperator.close();
    }
    
}
