package edu.uci.ics.textdb.dataflow.regexmatch;

import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import edu.uci.ics.textdb.api.common.IPredicate;
import edu.uci.ics.textdb.api.common.ITuple;
import edu.uci.ics.textdb.api.dataflow.ISourceOperator;
import edu.uci.ics.textdb.api.storage.IDataReader;
import edu.uci.ics.textdb.api.storage.IDataStore;
import edu.uci.ics.textdb.api.storage.IDataWriter;
import edu.uci.ics.textdb.common.constants.LuceneConstants;
import edu.uci.ics.textdb.common.constants.TestConstants;
import edu.uci.ics.textdb.dataflow.common.RegexPredicate;
import edu.uci.ics.textdb.dataflow.source.ScanBasedSourceOperator;
import edu.uci.ics.textdb.dataflow.utils.TestUtils;
import edu.uci.ics.textdb.storage.LuceneDataStore;
import edu.uci.ics.textdb.storage.reader.LuceneDataReader;
import edu.uci.ics.textdb.storage.writer.LuceneDataWriter;

/**
 * Created by chenli on 3/25/16.
 */
public class RegexMatcherTest {

	private RegexMatcher regexMatcher;
	private IDataWriter dataWriter;
	private IDataReader dataReader;
	private IDataStore dataStore;

	public void setUpPeople() throws Exception {
		dataStore = new LuceneDataStore(LuceneConstants.INDEX_DIR,
				TestConstants.SAMPLE_SCHEMA_PEOPLE);
		dataWriter = new LuceneDataWriter(dataStore);
		dataWriter.clearData();
		dataWriter.writeData(TestConstants.getSamplePeopleTuples());
	}

	public void setUpCorp() throws Exception {
		dataStore = new LuceneDataStore(LuceneConstants.INDEX_DIR,
				TestConstants.SAMPLE_SCHEMA_CORP);
		dataWriter = new LuceneDataWriter(dataStore);
		dataWriter.clearData();
		dataWriter.writeData(TestConstants.getSampleCorpTuples());
	}

	public void setUpStaff() throws Exception {
		dataStore = new LuceneDataStore(LuceneConstants.INDEX_DIR,
				TestConstants.SAMPLE_SCHEMA_STAFF);
		dataWriter = new LuceneDataWriter(dataStore);
		dataWriter.clearData();
		dataWriter.writeData(TestConstants.getSampleStaffTuples());

	}

	@After
	public void cleanUp() throws Exception {
		dataWriter.clearData();
	}

	
	@Test
	public void testNameGetNextTuple() throws Exception {
		setUpPeople();
		dataReader = new LuceneDataReader(dataStore,
				LuceneConstants.SCAN_QUERY, TestConstants.FIRST_NAME);

		String regex = "b.*"; // matches bruce and brad
		String fieldName = TestConstants.FIRST_NAME;
		IPredicate predicate = new RegexPredicate(regex, fieldName);
		ISourceOperator sourceOperator = new ScanBasedSourceOperator(dataReader);
		List<ITuple> tuples = TestConstants.getSamplePeopleTuples();

		regexMatcher = new RegexMatcher(predicate, sourceOperator);
		regexMatcher.open();
		ITuple nextTuple = null;
		int numTuples = 0;
		while ((nextTuple = regexMatcher.getNextTuple()) != null) {
			boolean contains = TestUtils.contains(tuples, nextTuple, TestConstants.SAMPLE_SCHEMA_PEOPLE);
			Assert.assertTrue(contains);
			numTuples++;
		}
		Assert.assertEquals(2, numTuples);
		regexMatcher.close();
	}

	@Test
	public void testURLGetNextTuple() throws Exception {
		setUpCorp();
		dataReader = new LuceneDataReader(dataStore,
				LuceneConstants.SCAN_QUERY, TestConstants.URL);

		String urlRegex = "^(https?:\\/\\/)?([\\da-z\\.-]+)\\.([a-z\\.]{2,6})([\\/\\w \\.-]*)*\\/?$";
		String fieldName = TestConstants.URL;
		IPredicate predicate = new RegexPredicate(urlRegex, fieldName);
		ISourceOperator sourceOperator = new ScanBasedSourceOperator(dataReader);
		List<ITuple> tuples = TestConstants.getSampleCorpTuples();

		regexMatcher = new RegexMatcher(predicate, sourceOperator);
		regexMatcher.open();
		ITuple nextTuple = null;
		int numTuples = 0;
		while ((nextTuple = regexMatcher.getNextTuple()) != null) {
			boolean contains = TestUtils.contains(tuples, nextTuple, TestConstants.SAMPLE_SCHEMA_CORP);
			Assert.assertTrue(contains);
			numTuples++;
		}
		Assert.assertEquals(3, numTuples);
		regexMatcher.close();
	}

	@Test
	public void testIPGetNextTuple() throws Exception {
		setUpCorp();
		dataReader = new LuceneDataReader(dataStore,
				LuceneConstants.SCAN_QUERY, TestConstants.IP_ADDRESS);

		String urlRegex = "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
		String fieldName = TestConstants.IP_ADDRESS;
		IPredicate predicate = new RegexPredicate(urlRegex, fieldName);
		ISourceOperator sourceOperator = new ScanBasedSourceOperator(dataReader);
		List<ITuple> tuples = TestConstants.getSampleCorpTuples();

		regexMatcher = new RegexMatcher(predicate, sourceOperator);
		regexMatcher.open();
		ITuple nextTuple = null;
		int numTuples = 0;
		while ((nextTuple = regexMatcher.getNextTuple()) != null) {
			boolean contains = TestUtils.contains(tuples, nextTuple, TestConstants.SAMPLE_SCHEMA_CORP);
			Assert.assertTrue(contains);
			numTuples++;
		}
		Assert.assertEquals(3, numTuples);
		regexMatcher.close();
	}

	@Test
	public void testEmailGetNextTuple() throws Exception {
		setUpStaff();
		dataReader = new LuceneDataReader(dataStore,
				LuceneConstants.SCAN_QUERY, TestConstants.EMAIL);

		String urlRegex = "^([a-z0-9_\\.-]+)@([\\da-z\\.-]+)\\.([a-z\\.]{2,6})$";
		String fieldName = TestConstants.EMAIL;
		IPredicate predicate = new RegexPredicate(urlRegex, fieldName);
		ISourceOperator sourceOperator = new ScanBasedSourceOperator(dataReader);
		List<ITuple> tuples = TestConstants.getSampleStaffTuples();

		regexMatcher = new RegexMatcher(predicate, sourceOperator);
		regexMatcher.open();
		ITuple nextTuple = null;
		int numTuples = 0;
		while ((nextTuple = regexMatcher.getNextTuple()) != null) {
			boolean contains = TestUtils.contains(tuples, nextTuple, TestConstants.SAMPLE_SCHEMA_STAFF);
			Assert.assertTrue(contains);
			numTuples++;
		}
		Assert.assertEquals(4, numTuples);
		regexMatcher.close();
	}

}
