package edu.uci.ics.textdb.dataflow.keywordmatch;

import edu.uci.ics.textdb.api.common.*;
import edu.uci.ics.textdb.common.constants.SchemaConstants;
import edu.uci.ics.textdb.api.common.Schema;
import edu.uci.ics.textdb.common.field.Span;
import edu.uci.ics.textdb.common.field.StringField;
import edu.uci.ics.textdb.common.field.TextField;
import edu.uci.ics.textdb.dataflow.common.KeywordPredicate;
import org.apache.lucene.search.Query;
import edu.uci.ics.textdb.common.field.ListField;
import edu.uci.ics.textdb.common.field.DataTuple;

import edu.uci.ics.textdb.api.dataflow.IOperator;
import edu.uci.ics.textdb.api.dataflow.ISourceOperator;
import edu.uci.ics.textdb.common.exception.DataFlowException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *  @author prakul
 *
 */
public class KeywordMatcher implements IOperator {
    private final KeywordPredicate predicate;
    private ISourceOperator sourceOperator;
    private Query luceneQuery;

    private String regex;
    private Pattern pattern;
    private ArrayList<Pattern> patternList;
    private Matcher matcher;
    private List<Span> spanList;
    private Schema schema;
    private Schema spanSchema;

    private int positionIndex; // next position in the field to be checked.
    private int spanIndexValue; // Starting position of the matched dictionary
    //private int attributeIndex;

    private String documentValue;

    private  String fieldName;
    private  String queryValue;
    private  List<Attribute> attributeList;
    private  ArrayList<String> queryValueArray;
    private ITuple sourceTuple;
    private List<IField> fieldList;
    private boolean foundFlag;
    private boolean schemaDefined;


    public KeywordMatcher(IPredicate predicate, ISourceOperator sourceOperator) {
        this.predicate = (KeywordPredicate)predicate;
        this.sourceOperator = sourceOperator;
        this.schema = schema;
    }

    @Override
    public void open() throws DataFlowException {
        try {

            sourceOperator.open();
            queryValue = predicate.getQuery();
            attributeList = predicate.getAttributeList();
            queryValueArray = predicate.getTokens();
            patternList = new ArrayList<Pattern>();
            for(String token : queryValueArray ){
                regex = "\\b" + token.toLowerCase() + "\\b";
                pattern = Pattern.compile(regex);
                patternList.add(pattern);
            }


            positionIndex = 0;
            //attributeIndex = 0;
            foundFlag = false;
            schemaDefined = false;

            spanList = new ArrayList<>();

        } catch (Exception e) {
            e.printStackTrace();
            throw new DataFlowException(e.getMessage(), e);
        }
    }

    @Override
    public ITuple getNextTuple() throws DataFlowException {
        try {
            sourceTuple = sourceOperator.getNextTuple();
            if(sourceTuple == null){
                return null;
            }
            if(!schemaDefined){
                schemaDefined = true;
                fieldList = sourceTuple.getFields();
                schema = sourceTuple.getSchema();
                spanSchema = createSpanSchema();
            }


            for(int attributeIndex = 0; attributeIndex < attributeList.size(); attributeIndex++){
                IField field = sourceTuple.getField(attributeList.get(attributeIndex).getFieldName());
                String fieldValue = (String) (field).getValue();
                if(field instanceof StringField){
                    //Keyword should match fieldValue entirely

                    if(fieldValue.equals(queryValue.toLowerCase())){
                        spanIndexValue = 0;
                        positionIndex = queryValue.length();
                        addSpanToSpanList(fieldName, spanIndexValue, positionIndex, queryValue, fieldValue);
                        foundFlag = true;
                        /*ArrayList<String> valueTokens = queryTokenizer(this.analyzer,fieldValue);
                         for (String token : this.tokens) {
                            if(!valueTokens.contains(token)){
                            return false;
                             }
                            }
                         return true;
                        */
                    }
                }
                else if(field instanceof TextField) {
                    for (int iter = 0; iter < queryValueArray.size(); iter++) {
                        String query = queryValueArray.get(iter);
                        Pattern p = patternList.get(iter);
                        matcher = p.matcher(fieldValue.toLowerCase());
                        while (matcher.find(positionIndex) != false) {
                            spanIndexValue = matcher.start();
                            positionIndex = spanIndexValue + query.length();
                            documentValue = fieldValue.substring(spanIndexValue, positionIndex);
                            addSpanToSpanList(fieldName, spanIndexValue, positionIndex, query, documentValue);
                            foundFlag = true;

                        }
                    }
                }
                positionIndex = 0;
            }
           /* if(attributeIndex < ){
                }
                attributeIndex++;
                return getNextTuple();
            }
            */
            //If all the 'attributes to be searched' have been processed return the result tuple with span info
            if (foundFlag){
                foundFlag = false;
                positionIndex = 0;
                return getSpanTuple();
            }
            //Search next document if the required predicate did not match previous document
            else if(sourceTuple != null) {
                //fieldList = sourceTuple.getFields();
                //schema = sourceTuple.getSchema();
                //spanSchema = createSpanSchema();
                //attributeIndex = 0;
                positionIndex = 0;
                spanList.clear();

                return getNextTuple();

            }

            // if(sourceTuple == null){
            return null;
            //}

        } catch (Exception e) {
            e.printStackTrace();
            throw new DataFlowException(e.getMessage(), e);
        }

    }

    private ITuple getSpanTuple() {
        IField spanListField = new ListField<Span>(new ArrayList<>(spanList));
        List<IField> fieldListDuplicate = new ArrayList<>(fieldList);
        fieldListDuplicate.add(spanListField);

        IField[] fieldsDuplicate = fieldListDuplicate.toArray(new IField[fieldListDuplicate.size()]);
        return new DataTuple(spanSchema, fieldsDuplicate);
    }

    private Schema createSpanSchema() {
        List<Attribute> sourceTupleAttributes = schema.getAttributes();
        List<Attribute> spanAttributes = new ArrayList<Attribute>(sourceTupleAttributes);
        spanAttributes.add(SchemaConstants.SPAN_LIST_ATTRIBUTE);
        Schema spanSchema = new Schema(spanAttributes);
        return spanSchema;
    }

    private void addSpanToSpanList(String fieldName, int start, int end, String key, String value) {
        Span span = new Span(fieldName, start, end, key, value);
        spanList.add(span);
    }


    @Override
    public void close() throws DataFlowException {
        try {
            sourceOperator.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw new DataFlowException(e.getMessage(), e);
        }
    }
}