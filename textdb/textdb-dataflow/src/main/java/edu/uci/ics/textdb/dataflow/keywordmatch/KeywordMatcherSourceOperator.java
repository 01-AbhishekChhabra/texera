package edu.uci.ics.textdb.dataflow.keywordmatch;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import edu.uci.ics.textdb.api.common.Attribute;
import edu.uci.ics.textdb.api.common.FieldType;
import edu.uci.ics.textdb.api.common.ITuple;
import edu.uci.ics.textdb.api.common.Schema;
import edu.uci.ics.textdb.api.dataflow.IOperator;
import edu.uci.ics.textdb.api.dataflow.ISourceOperator;
import edu.uci.ics.textdb.api.storage.IDataStore;
import edu.uci.ics.textdb.common.constants.DataConstants.KeywordMatchingType;
import edu.uci.ics.textdb.common.exception.DataFlowException;
import edu.uci.ics.textdb.api.exception.TextDBException;
import edu.uci.ics.textdb.dataflow.common.AbstractSingleInputOperator;
import edu.uci.ics.textdb.dataflow.common.KeywordPredicate;
import edu.uci.ics.textdb.storage.DataReaderPredicate;
import edu.uci.ics.textdb.storage.reader.DataReader;

/**
 * KeywordMatcherSourceOperator is a source operator with a keyword query.
 * 
 * @author Zuozhi Wang
 *
 */
public class KeywordMatcherSourceOperator extends AbstractSingleInputOperator implements ISourceOperator  {
    
    private KeywordPredicate predicate;
    private IDataStore dataStore;
    
    private KeywordMatchingType operatorType;
    private List<String> attributeNames;
    
    private DataReader dataReader;
    private KeywordMatcher keywordMatcher;
    
    private Schema outputSchema;
    
    public KeywordMatcherSourceOperator(KeywordPredicate predicate, IDataStore dataStore) {
        this.predicate = predicate;
        this.dataStore = dataStore;
        
        this.operatorType = predicate.getOperatorType();
        this.attributeNames = prediate.getAttributeNames();
    }

    @Override
    public Schema getOutputSchema() {
        return this.outputSchema;
    }

    @Override
    protected void setUp() throws DataFlowException {
        dataReader = new DataReader(generateDataReaderPredicate(dataStore));
        keywordMatcher = new KeywordMatcher(predicate);
        keywordMatcher.setInputOperator(dataReader);
        inputOperator = this.keywordMatcher;
        
        this.outputSchema = this.keywordMatcher.getOutputSchema();        
    }

    @Override
    protected ITuple computeNextMatchingTuple() throws TextDBException {
        return this.keywordMatcher.getNextTuple();
    }

    @Override
    protected void cleanUp() throws DataFlowException {        
    }
    
    /**
     * Source Operator doesn't need an input operator. Calling setInputOperator won't have any effects.
     */
    @Override
    public void setInputOperator(IOperator inputOperator) {
    }
    
    public KeywordPredicate getPredicate() {
        return this.predicate;
    }
    
    public IDataStore getDataStore() {
        return this.dataStore;
    }
    
    
    /**
     * Creates a Query object as a boolean Query on all attributes Example: For
     * creating a query like (TestConstants.DESCRIPTION + ":lin" + " AND " +
     * TestConstants.LAST_NAME + ":lin") we provide a list of AttributeFields
     * (Description, Last_name) to search on and a query string (lin)
     *
     * @return Query
     * @throws ParseException
     * @throws DataFlowException
     */
    private Query createLuceneQueryObject() throws DataFlowException {
        Query query = null;
        if (this.operatorType == KeywordMatchingType.CONJUNCTION_INDEXBASED) {
            query = buildConjunctionQuery();
        }
        if (this.operatorType == KeywordMatchingType.PHRASE_INDEXBASED) {
            query = buildPhraseQuery();
        }
        if (this.operatorType == KeywordMatchingType.SUBSTRING_SCANBASED) {
            query = buildScanQuery();
        }

        return query;
    }

    private Query buildConjunctionQuery() throws DataFlowException {
        BooleanQuery.Builder booleanQueryBuilder = new BooleanQuery.Builder();

        for (String attribute : this.attributeNames) {
            String fieldName = attribute.getFieldName();
            FieldType fieldType = attribute.getFieldType();

            // types other than TEXT and STRING: throw Exception for now
            if (fieldType != FieldType.STRING && fieldType != FieldType.TEXT) {
                throw new DataFlowException(
                        "KeywordPredicate: Fields other than STRING and TEXT are not supported yet");
            }

            if (fieldType == FieldType.STRING) {
                Query termQuery = new TermQuery(new Term(fieldName, this.query));
                booleanQueryBuilder.add(termQuery, BooleanClause.Occur.SHOULD);
            }
            if (fieldType == FieldType.TEXT) {
                BooleanQuery.Builder fieldQueryBuilder = new BooleanQuery.Builder();
                for (String token : this.queryTokenSet) {
                    Query termQuery = new TermQuery(new Term(fieldName, token.toLowerCase()));
                    fieldQueryBuilder.add(termQuery, BooleanClause.Occur.MUST);
                }
                booleanQueryBuilder.add(fieldQueryBuilder.build(), BooleanClause.Occur.SHOULD);
            }

        }

        return booleanQueryBuilder.build();
    }

    private Query buildPhraseQuery() throws DataFlowException {
        BooleanQuery.Builder booleanQueryBuilder = new BooleanQuery.Builder();

        for (Attribute attribute : this.attributeList) {
            String fieldName = attribute.getFieldName();
            FieldType fieldType = attribute.getFieldType();

            // types other than TEXT and STRING: throw Exception for now
            if (fieldType != FieldType.STRING && fieldType != FieldType.TEXT) {
                throw new DataFlowException(
                        "KeywordPredicate: Fields other than STRING and TEXT are not supported yet");
            }

            if (fieldType == FieldType.STRING) {
                Query termQuery = new TermQuery(new Term(fieldName, this.query));
                booleanQueryBuilder.add(termQuery, BooleanClause.Occur.SHOULD);
            }
            if (fieldType == FieldType.TEXT) {
                if (queryTokenList.size() == 1) {
                    Query termQuery = new TermQuery(new Term(fieldName, this.query.toLowerCase()));
                    booleanQueryBuilder.add(termQuery, BooleanClause.Occur.SHOULD);
                } else {
                    PhraseQuery.Builder phraseQueryBuilder = new PhraseQuery.Builder();
                    for (int i = 0; i < queryTokensWithStopwords.size(); i++) {
                        if (!StandardAnalyzer.STOP_WORDS_SET.contains(queryTokensWithStopwords.get(i))) {
                            phraseQueryBuilder.add(new Term(fieldName, queryTokensWithStopwords.get(i).toLowerCase()),
                                    i);
                        }
                    }
                    PhraseQuery phraseQuery = phraseQueryBuilder.build();
                    booleanQueryBuilder.add(phraseQuery, BooleanClause.Occur.SHOULD);
                }
            }

        }

        return booleanQueryBuilder.build();
    }

    private Query buildScanQuery() throws DataFlowException {
        for (Attribute attribute : this.attributeList) {
            FieldType fieldType = attribute.getFieldType();

            // types other than TEXT and STRING: throw Exception for now
            if (fieldType != FieldType.STRING && fieldType != FieldType.TEXT) {
                throw new DataFlowException(
                        "KeywordPredicate: Fields other than STRING and TEXT are not supported yet");
            }
        }

        return new MatchAllDocsQuery();
    }

    public DataReaderPredicate generateDataReaderPredicate(IDataStore dataStore) {
        DataReaderPredicate predicate = new DataReaderPredicate(this.luceneQuery, this.query, dataStore,
                this.attributeList, this.luceneAnalyzer);
        predicate.setIsSpanInformationAdded(true);
        return predicate;
    }

}
