package edu.uci.ics.textdb.dataflow.queryrewriter;

import edu.uci.ics.textdb.api.common.*;
import edu.uci.ics.textdb.api.dataflow.IOperator;
import edu.uci.ics.textdb.common.field.DataTuple;
import edu.uci.ics.textdb.common.field.ListField;

import java.util.List;

/**
 * Created by kishorenarendran on 25/04/16.
 * QueryRewriter is an operator that converts a query search string which
 * has faults in space placement to a list of meaningful search queries
 *
 * An example - "newyork city" - > "new york city"
 *              "christmasday" - > "christmas day"
 *
 * Like other Operators it returns an ITuple which contains a list of strings
 * which are the modified queries.
 *
 */
public class QueryRewriter implements IOperator{

    private String searchQuery;
    private boolean isOpen = false;
    private boolean allSegmentations = false;

    public static final String QUERYLIST = "querylist";
    public static final Attribute QUERYLIST_ATTR = new Attribute(QUERYLIST, FieldType.LIST);
    public static final Schema SCHEMA_QUERY_LIST = new Schema(QUERYLIST_ATTR);

    private ITuple itupleResult;

    /**
     * Parameterized constructor that requires a Search Query String that is to be rewritten
     * @param searchQuery
     */
    public QueryRewriter(String searchQuery) {
        this.searchQuery = searchQuery;
    }

    /**
     * Parameterized constructor that requires a Search Query String that is to be rewritten
     * and boolean flag set to true if all possible tokenizations are to be returned
     *                     and false if only most likely tokenization is to be returned
     * @param searchQuery
     * @param allSegmentations
     */
    public QueryRewriter(String searchQuery, boolean allSegmentations) {
        this.searchQuery = searchQuery;
        this.allSegmentations = allSegmentations;
    }

    /**
     * Overridden Open method of the IOperator interface
     * Query parser object will be created here
     * @throws Exception
     */
    @Override
    public void open() throws Exception {
        this.isOpen = true;
        this.itupleResult = null;
    }

    /**
     * Calls appropriate implementation methods to populate the list
     * of rewritten search queries, and constructing a tuple from it.
     * The class QuerySegmenter implements two methods to rewrite search queries
     * The DP algorithm returns the most likely tokenization - is called if allSegmentations is false
     * The brute force algorithm returns all possible tokenizations - called if allSegmentations is true
     * @return - Tuple with the rewritten queries as a comma separated string
     * @throws Exception
     */
    @Override
    public ITuple getNextTuple() throws Exception {

        boolean endOfResult = (itupleResult != null);   //Ensures you can call QueryRewriter.getNextTuple only once

        if(!isOpen || endOfResult)
            return null;
        else {
            List<String> queryStrings;
            if(allSegmentations)
                queryStrings = QuerySegmenter.bruteGetTokens(searchQuery);
            else
                queryStrings = QuerySegmenter.getTokens(searchQuery);
            IField[] iFieldResult = {new ListField(queryStrings)};
            itupleResult = new DataTuple(SCHEMA_QUERY_LIST, iFieldResult);
            return itupleResult;
        }
    }

    /**
     * Closing the query Parser object
     * @throws Exception
     */
    @Override
    public void close() throws Exception {
        this.isOpen = false;
        this.searchQuery = null;
        this.itupleResult = null;
    }
}