package edu.uci.ics.textdb.dataflow.neextractor;

import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.junit.After;
import org.junit.Test;

import edu.uci.ics.textdb.api.common.IPredicate;
import edu.uci.ics.textdb.api.common.ITuple;
import edu.uci.ics.textdb.api.common.Schema;
import edu.uci.ics.textdb.api.dataflow.ISourceOperator;
import edu.uci.ics.textdb.api.storage.IDataReader;
import edu.uci.ics.textdb.api.storage.IDataStore;
import edu.uci.ics.textdb.api.storage.IDataWriter;
import edu.uci.ics.textdb.common.constants.DataConstants;
import edu.uci.ics.textdb.dataflow.neextrator.NamedEntityExtractor;
import edu.uci.ics.textdb.dataflow.source.ScanBasedSourceOperator;
import edu.uci.ics.textdb.dataflow.utils.TestUtils;
import edu.uci.ics.textdb.storage.DataReaderPredicate;
import edu.uci.ics.textdb.storage.DataStore;
import edu.uci.ics.textdb.storage.reader.DataReader;
import edu.uci.ics.textdb.storage.writer.DataWriter;

/**
 * @author Feng [sam0227]
 */
public class NamedEntityExtractorTest {
    private NamedEntityExtractor namedEntityExtractor;

    private IDataWriter dataWriter;
    private IDataReader dataReader;
    private IDataStore dataStore;

    private Query query;
    private Analyzer analyzer;

    private IPredicate dataReaderPredicate;


    @After
    public void cleanUp() throws Exception {
        dataWriter.clearData();
    }


    /**
     * @param sourceOperator
     * @return
     * @throws Exception
     * @about Using NamedEntityExtractor to get all returned results from sourceOperator,
     * return as a list of tuples
     */
    public List<ITuple> getQueryResults(ISourceOperator sourceOperator) throws Exception {

        namedEntityExtractor = new NamedEntityExtractor(sourceOperator);
        namedEntityExtractor.open();
        ITuple nextTuple = null;
        List<ITuple> results = new ArrayList<ITuple>();
        while ((nextTuple = namedEntityExtractor.getNextTuple()) != null) {
            results.add(nextTuple);
        }
        namedEntityExtractor.close();
        return results;
    }


    /**
     * Scenario 1: Test getNextTuple with only one span in the return list
     * Text : Microsoft is a organization.
     *
     * @throws Exception
     */
    @Test
    public void getNextTupleTest1() throws Exception {
        List<ITuple> data = NEExtractorTestConstants.getTest1Tuple();
        ISourceOperator sourceOperator = getSourceOperator(data.get(0).getSchema(), data);

        List<ITuple> returnedResults = getQueryResults(sourceOperator);

        List<ITuple> expectedResults = NEExtractorTestConstants.getTest1ResultTuples();

        boolean contains = TestUtils.containsAllResults(expectedResults, returnedResults);
        //TODO: enable test while finish implementation
        // Assert.assertTrue(contains);
    }

    /**
     * Scenario 2: Test getNextTuple with more than one span in the return list
     * Text: Microsoft, Google and Facebook are organizations
     */
    @Test
    public void getNextTupleTest2() throws Exception {
        List<ITuple> data = NEExtractorTestConstants.getTest2Tuple();
        ISourceOperator sourceOperator = getSourceOperator(data.get(0).getSchema(), data);

        List<ITuple> returnedResults = getQueryResults(sourceOperator);
        List<ITuple> expectedResults = NEExtractorTestConstants.getTest2ResultTuples();

        boolean contains = TestUtils.containsAllResults(expectedResults, returnedResults);
        //TODO: enable test while finish implementation
        // Assert.assertTrue(contains);

    }

    /**
     * Scenario 3: Test getNextTuple with more than one span in the return list and with different recognized classes.
     * Text: Microsoft, Google and Facebook are organizations and Donald Trump and Barack Obama are persons.
     */
    @Test
    public void getNextTupleTest3() throws Exception {
        List<ITuple> data = NEExtractorTestConstants.getTest3Tuple();
        ISourceOperator sourceOperator = getSourceOperator(data.get(0).getSchema(), data);

        List<ITuple> returnedResults = getQueryResults(sourceOperator);
        List<ITuple> expectedResults = NEExtractorTestConstants.getTest3ResultTuples();

        boolean contains = TestUtils.containsAllResults(expectedResults, returnedResults);

        //TODO: enable test while finish implementation
        //Assert.assertTrue(contains);
    }


    /**
     * Scenario 4:Test getNextTuple with more than one span in the return list and with different recognized classes
     * and more than one fields in the source tuple.
     * <p>
     * Sentence1: Microsoft, Google and Facebook are organizations.
     * Sentence2: Donald Trump and Barack Obama are persons.
     */
    @Test
    public void getNextTupleTest4() throws Exception {
        List<ITuple> data = NEExtractorTestConstants.getTest4Tuple();
        ISourceOperator sourceOperator = getSourceOperator(data.get(0).getSchema(), data);

        List<ITuple> returnedResults = getQueryResults(sourceOperator);

        List<ITuple> expectedResults = NEExtractorTestConstants.getTest4ResultTuples();

        //TODO: expected contains returned  AND returned contains expected ?
        boolean contains = TestUtils.containsAllResults(expectedResults, returnedResults);


        //TODO: enable test while finish implementation
        //Assert.assertTrue(contains);
    }


    /**
     * @param schema The data schema
     * @param data
     * @return
     * @throws Exception
     * @about construct a source operator using given schema and data
     */

    public ISourceOperator getSourceOperator(Schema schema, List<ITuple> data) throws Exception {
        dataStore = new DataStore(DataConstants.INDEX_DIR, schema);
        analyzer = new StandardAnalyzer();
        dataWriter = new DataWriter(dataStore, analyzer);
        dataWriter.writeData(data);

        QueryParser queryParser = new QueryParser(NEExtractorTestConstants.ATTRIBUTES_ONE_SENTENCE.get(0).getFieldName(), analyzer);
        query = queryParser.parse(DataConstants.SCAN_QUERY);
        dataReaderPredicate = new DataReaderPredicate(dataStore, query);
        dataReader = new DataReader(dataReaderPredicate);

        ISourceOperator sourceOperator = new ScanBasedSourceOperator(dataReader);
        return sourceOperator;

    }

}
