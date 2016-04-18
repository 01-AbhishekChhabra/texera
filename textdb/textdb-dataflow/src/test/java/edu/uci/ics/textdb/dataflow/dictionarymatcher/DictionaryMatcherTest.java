
package edu.uci.ics.textdb.dataflow.dictionarymatcher;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import edu.uci.ics.textdb.api.common.IDictionary;
import edu.uci.ics.textdb.api.common.ITuple;
import edu.uci.ics.textdb.api.dataflow.ISourceOperator;
import edu.uci.ics.textdb.api.storage.IDataReader;
import edu.uci.ics.textdb.api.storage.IDataWriter;
import edu.uci.ics.textdb.common.constants.LuceneConstants;
import edu.uci.ics.textdb.common.constants.SchemaConstants;
import edu.uci.ics.textdb.common.constants.TestConstants;
import edu.uci.ics.textdb.dataflow.source.ScanBasedSourceOperator;
import edu.uci.ics.textdb.dataflow.utils.TestUtils;
import edu.uci.ics.textdb.storage.LuceneDataStore;
import edu.uci.ics.textdb.storage.reader.LuceneDataReader;
import edu.uci.ics.textdb.storage.writer.LuceneDataWriter;

/**
 * @author Sudeep [inkudo] and Rajesh [rajesh9625]
 *
 */
public class DictionaryMatcherTest {

    private DictionaryMatcher dictionaryMatcher;
    private LuceneDataStore dataStore;
    private IDataWriter dataWriter;
    private IDataReader dataReader;

    @Before
    public void setUp() throws Exception {

        dataStore = new LuceneDataStore(LuceneConstants.INDEX_DIR, TestConstants.SAMPLE_SCHEMA_PEOPLE);
        dataWriter = new LuceneDataWriter(dataStore);
        dataWriter.clearData();
        dataWriter.writeData(TestConstants.getSamplePeopleTuples());
        dataReader = new LuceneDataReader(dataStore, LuceneConstants.SCAN_QUERY,
                TestConstants.SAMPLE_SCHEMA_PEOPLE.get(0).getFieldName());

    }

    @After
    public void cleanUp() throws Exception {
        dataWriter.clearData();
    }

    /**
     * Scenario S1:verifies GetNextTuple of Dictionary
     */

    @Test
    public void testGetNextDictionaryItem() throws Exception {

        ArrayList<String> names = new ArrayList<String>(Arrays.asList("brad cooper", "clooney", "george", "lee"));
        IDictionary dictionary = new Dictionary(names);
        int numTuples = 0;
        String dictionaryItem;
        while ((dictionaryItem = dictionary.getNextValue()) != null) {
        	 boolean contains = TestUtils.contains(names, dictionaryItem);
             Assert.assertTrue(contains);
        	 numTuples++;
        }
        Assert.assertEquals(4, numTuples);

    }

    /**
     * Scenario S2:verifies GetNextTuple of DictionaryMatcher and single word queries
     */

    @Test
    public void testGetNextTuple() throws Exception {

        ArrayList<String> names = new ArrayList<String>(Arrays.asList("bruce", "tom", "lee", "brad", "cena","john"));
        IDictionary dict = new Dictionary(names);
        ISourceOperator sourceOperator = new ScanBasedSourceOperator(dataReader);

        dictionaryMatcher = new DictionaryMatcher(dict, sourceOperator);
        dictionaryMatcher.open();
        ITuple iTuple;
        int numTuples = 0;
        while ((iTuple = dictionaryMatcher.getNextTuple()) != null) {
        	String returnedString = (String) iTuple.getField(SchemaConstants.SPAN_KEY).getValue();
            boolean contains = TestUtils.contains(names, returnedString);
            Assert.assertTrue(contains);
            numTuples++;
        }
        Assert.assertEquals(6, numTuples);
        dictionaryMatcher.close();
    }

    /**
     * Scenario S3:verifies ITuple returned by DictionaryMatcher and multiple word queries
     */

    @Test
    public void testTuple() throws Exception {

        ArrayList<String> names = new ArrayList<String>(
                Arrays.asList("bruce banner", "tom hanks", "lee", "brad lie", "clooney", "cena", "christian john wayne","rock", "brande","angelina"));
        IDictionary dict = new Dictionary(names);
        ISourceOperator sourceOperator = new ScanBasedSourceOperator(dataReader);

        dictionaryMatcher = new DictionaryMatcher(dict, sourceOperator);
        dictionaryMatcher.open();

        ITuple iTuple;
        int numTuples = 0;
        while ((iTuple = dictionaryMatcher.getNextTuple()) != null) {

            String returnedString = (String) iTuple.getField(SchemaConstants.SPAN_KEY).getValue();
            boolean contains = TestUtils.contains(names, returnedString);
            Assert.assertTrue(contains);
            numTuples++;

        }
        Assert.assertEquals(8, numTuples);
        dictionaryMatcher.close();
    }

}
