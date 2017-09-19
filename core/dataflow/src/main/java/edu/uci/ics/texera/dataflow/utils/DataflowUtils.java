package edu.uci.ics.texera.dataflow.utils;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

import edu.uci.ics.texera.api.exception.DataflowException;
import edu.uci.ics.texera.api.schema.AttributeType;
import edu.uci.ics.texera.api.span.Span;
import edu.uci.ics.texera.api.tuple.*;
import edu.uci.ics.texera.storage.constants.LuceneAnalyzerConstants;

public class DataflowUtils {
    
    public static final String LUCENE_SCAN_QUERY = "*:*";

    public static ArrayList<String> tokenizeQuery(String luceneAnalyzerStr, String query) {
        return tokenizeQuery(LuceneAnalyzerConstants.getLuceneAnalyzer(luceneAnalyzerStr), query);
    }

    /**
     * Tokenizes the query string using the given analyser
     *
     * @param luceneAnalyzer
     * @param query
     * @return ArrayList<String> list of results
     */
    public static ArrayList<String> tokenizeQuery(Analyzer luceneAnalyzer, String query) {
        ArrayList<String> result = new ArrayList<String>();
        TokenStream tokenStream = luceneAnalyzer.tokenStream(null, new StringReader(query));
        CharTermAttribute term = tokenStream.addAttribute(CharTermAttribute.class);
        try {
            tokenStream.reset();
            while (tokenStream.incrementToken()) {
                result.add(term.toString());
            }
            tokenStream.close();
        } catch (IOException e) {
            throw new DataflowException(e);
        }
        return result;
    }

    public static ArrayList<String> tokenizeQueryWithStopwords(String query) {
        ArrayList<String> result = new ArrayList<String>();
        CharArraySet emptyStopwords = new CharArraySet(1, true);
        Analyzer luceneAnalyzer = new StandardAnalyzer(emptyStopwords);
        TokenStream tokenStream = luceneAnalyzer.tokenStream(null, new StringReader(query));
        CharTermAttribute term = tokenStream.addAttribute(CharTermAttribute.class);

        try {
            tokenStream.reset();
            while (tokenStream.incrementToken()) {
                String token = term.toString();
                int tokenIndex = query.toLowerCase().indexOf(token);
                // Since tokens are converted to lower case,
                // get the exact token from the query string.
                String actualQueryToken = query.substring(tokenIndex, tokenIndex + token.length());
                result.add(actualQueryToken);
            }
            tokenStream.close();
        } catch (IOException e) {
            throw new DataflowException(e);
        } finally {
            luceneAnalyzer.close();
        }
        
        return result;
    }

    public static List<Span> generatePayloadFromTuple(Tuple tuple, String luceneAnalyzer) throws DataflowException {
        return generatePayloadFromTuple(tuple, LuceneAnalyzerConstants.getLuceneAnalyzer(luceneAnalyzer));
    }

    public static List<Span> generatePayloadFromTuple(Tuple tuple, Analyzer luceneAnalyzer) {
        List<Span> tuplePayload = tuple.getSchema().getAttributes().stream()
                .filter(attr -> (attr.getType() == AttributeType.TEXT)) // generate payload only for TEXT field
                .map(attr -> attr.getName())
                .map(attributeName -> generatePayload(attributeName, tuple.getField(attributeName).getValue().toString(),
                        luceneAnalyzer))
                .flatMap(payload -> payload.stream()) // flatten a list of lists to a list
                .collect(Collectors.toList());

        return tuplePayload;
    }

    public static List<Span> generatePayload(String attributeName, String fieldValue, Analyzer luceneAnalyzer) {
        List<Span> payload = new ArrayList<>();

        try {
            TokenStream tokenStream = luceneAnalyzer.tokenStream(null, new StringReader(fieldValue));
            OffsetAttribute offsetAttribute = tokenStream.addAttribute(OffsetAttribute.class);
            CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
            PositionIncrementAttribute positionIncrementAttribute =
                    tokenStream.addAttribute(PositionIncrementAttribute.class);

            int tokenPositionCounter = -1;
            tokenStream.reset();
            while (tokenStream.incrementToken()) {
                tokenPositionCounter += positionIncrementAttribute.getPositionIncrement();

                int tokenPosition = tokenPositionCounter;
                int charStart = offsetAttribute.startOffset();
                int charEnd = offsetAttribute.endOffset();
                String analyzedTermStr = charTermAttribute.toString();
                String originalTermStr = fieldValue.substring(charStart, charEnd);

                payload.add(new Span(attributeName, charStart, charEnd, analyzedTermStr, originalTermStr, tokenPosition));
            }
            tokenStream.close();
        } catch (IOException e) {
            throw new DataflowException(e);
        }

        return payload;
    }

    /***
     *
     * @param inputTuple
     * @param attributeNames
     * @param queryKeyword
     * @param matchingResults
     * @throws DataflowException
     */
    public static void appendSubstringMatchingSpans(Tuple inputTuple, List<String> attributeNames, String queryKeyword, List<Span> matchingResults) throws DataflowException {
        for (String attributeName : attributeNames) {
            //  AttributeType attributeType = this.inputSchema.getAttribute(attributeName).getAttributeType();
            String fieldValue = inputTuple.getField(attributeName).getValue().toString();
            AttributeType attributeType = inputTuple.getSchema().getAttribute(attributeName).getType();
            // types other than TEXT and STRING: throw Exception for now
            if (attributeType != AttributeType.STRING && attributeType != AttributeType.TEXT) {
                throw new DataflowException("KeywordMatcher: Fields other than STRING and TEXT are not supported yet");
            }

            // for STRING type, the query should match the fieldValue completely
            if (attributeType == AttributeType.STRING) {
                if (fieldValue.equals(queryKeyword)) {
                    matchingResults.add(new Span(attributeName, 0, queryKeyword.length(), queryKeyword, fieldValue));
                }
            }

            if (attributeType == AttributeType.TEXT) {

                String fieldValueLowerCase = fieldValue.toLowerCase();
                String queryKeywordLowerCase = queryKeyword.toLowerCase();
                for (int i = 0; i < fieldValueLowerCase.length(); i++) {
                    int index = -1;
                    if ((index = fieldValueLowerCase.indexOf(queryKeywordLowerCase, i)) != -1) {
                        matchingResults.add(new Span(attributeName, index, index + queryKeyword.length(), queryKeyword,
                                fieldValue.substring(index, index + queryKeyword.length())));
                        i = index + 1;
                    } else {
                        break;
                    }
                }

            }
        }
    }

    /**
     * This function is used to generate the SpanList for phrase matching type in both dictionarymatcher and keywordmatcher.
     * @param attributeName
     * @param fieldValue
     * @param queryKeyword
     * @param fieldSpanList
     * @param queryTokenListWithStopwords
     * @param queryTokenList
     * @return
     */

    public static List<Span> constructPhraseMatchingSpans(String attributeName, String fieldValue, String queryKeyword, List<Span> fieldSpanList, List<String> queryTokenListWithStopwords, List<String> queryTokenList){
        List<Span> matchingResults = new ArrayList<>();
        // Sort current field's span list by token offset for later use
        Collections.sort(fieldSpanList, (span1, span2) -> span1.getTokenOffset() - span2.getTokenOffset());
        List<Integer> queryTokenOffset = new ArrayList<>();
        for (int i = 0; i < queryTokenListWithStopwords.size(); i++) {
            if (queryTokenList.contains(queryTokenListWithStopwords.get(i))) {
                queryTokenOffset.add(i);
            }
        }
        int iter = 0; // maintains position of term being checked in
        // spanForThisField list
        while (iter < fieldSpanList.size()) {
            if (iter > fieldSpanList.size() - queryTokenList.size()) {
                break;
            }

            // Verify if span in the spanForThisField correspond to our
            // phrase query, ie relative position offsets should be
            // similar
            // and the value should be same.
            boolean isMismatchInSpan = false;// flag to check if a
            // mismatch in spans occurs

            // To check all the terms in query are verified
            for (int i = 0; i < queryTokenList.size() - 1; i++) {
                Span first = fieldSpanList.get(iter + i);
                Span second = fieldSpanList.get(iter + i + 1);
                if (!(second.getTokenOffset() - first.getTokenOffset() == queryTokenOffset.get(i + 1)
                        - queryTokenOffset.get(i) && first.getValue().equalsIgnoreCase(queryTokenList.get(i))
                        && second.getValue().equalsIgnoreCase(queryTokenList.get(i + 1)))) {
                    iter++;
                    isMismatchInSpan = true;
                    break;
                }
            }
            
            if (isMismatchInSpan) {
                continue;
            }
            
            int combinedSpanStartIndex = fieldSpanList.get(iter).getStart();
            int combinedSpanEndIndex = fieldSpanList.get(iter + queryTokenList.size() - 1).getEnd();

            Span combinedSpan = new Span(attributeName, combinedSpanStartIndex, combinedSpanEndIndex, queryKeyword,
                    fieldValue.substring(combinedSpanStartIndex, combinedSpanEndIndex));
            matchingResults.add(combinedSpan);
            iter = iter + queryTokenList.size();
        }
        return matchingResults;
    }

    public static boolean isAllQueryTokensPresent(List<Span> fieldSpanList, Set<String> queryTokenSet) {
        Set<String> fieldSpanKeys = fieldSpanList.stream().map(span -> span.getKey()).collect(Collectors.toSet());

        return fieldSpanKeys.equals(queryTokenSet);
    }
}
