package edu.uci.ics.textdb.dataflow.regexmatch;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

import edu.uci.ics.textdb.common.constants.DataConstants;


public class GramBooleanQuery {
	public enum QueryOp {
		NONE, // doesn't match any string
		ANY,  // matches any string
		
		AND,
		OR
	}
	QueryOp operator;
	List<String> operandList;
	List<GramBooleanQuery> subQueryList;
	
	private static int gramNum = 3;
	

	public GramBooleanQuery(QueryOp operator) {
		this.operator = operator;
		operandList = new ArrayList<String>();
		subQueryList = new ArrayList<GramBooleanQuery>();
	}
	
	public boolean equals(GramBooleanQuery query) {
		if (this.operator != query.operator
			|| this.operandList != query.operandList
			|| this.subQueryList.size() != query.subQueryList.size()) {
			return false;
		}
		
		Set<String> operandSet = new HashSet<String>(this.operandList);
		if (!operandSet.equals(new HashSet<String>(query.operandList))) {
			return false;
		}
		
		if (this.subQueryList.size() == 0) {
			return true;
		}
		
		int[] used = new int[this.subQueryList.size()];
		return this.equalsHelper(query, used, 0);
	}
	/**
	 * This is a helper function called by {@code equals} function.
	 * It takes a DFS approach to recursively determine 
	 * whether two {@code TrigramBooleanQuery} list contains same set of elements. 
	 * @param query
	 * @param isUsed
	 * @param index
	 * @return
	 */
	private boolean equalsHelper(GramBooleanQuery query, int[] isUsed, int index) {
		if (index == query.subQueryList.size()) {
			return true;
		}
		
		for (int i = 0; i < query.subQueryList.size(); i++) {
			if (isUsed[i] == 1) continue;
			if (this.subQueryList.get(index).equals(query.subQueryList.get(i))) {
				isUsed[i] = 1;
				if (equalsHelper(query, isUsed, index+1)){
					return true;
				} else {
					isUsed[i] = 0;
				}
			}
		}
		return false;
	}
	
	public void add(ArrayList<String> list) {
		addOrNode(list);
	}
	
	private void addOrNode(ArrayList<String> literalList) {
		GramBooleanQuery tbq = new GramBooleanQuery(GramBooleanQuery.QueryOp.OR);
		for (String literal : literalList) {
			tbq.addAndNode(literal);
		}
		this.subQueryList.add(tbq);
	}
	
	private void addAndNode(String literal) {
		GramBooleanQuery tbq = new GramBooleanQuery(GramBooleanQuery.QueryOp.AND);
		for (String trigram: literalToTrigram(literal)) {
			tbq.operandList.add(trigram);
		}
		this.subQueryList.add(tbq);
	}
	
	/**
	 * This function build a list of N-Grams that a given literal contains. <br>
	 * If the length of the literal is smaller than N, it returns an empty list. <br>
	 * Default gram number is 3. <br>
	 * For example, for literal "textdb", trigram list should be ["tex", "ext", "xtd", "tdb"]
	 * @param literal
	 * @return
	 */
	private List<String> literalToTrigram(String literal) {
		ArrayList<String> trigrams = new ArrayList<>();
		if (literal.length() >= gramNum) {
			for (int i = 0; i <= literal.length()-3; ++i) {
				trigrams.add(literal.substring(i, i+gramNum));
			}
		}
		return trigrams;
	}
	
	/**
	 * This method sets a new gram number. Default gram number is set to 3. <br>
	 * @param gramNum
	 */
	public static void setGramNum(int gramNum) {
		GramBooleanQuery.gramNum = gramNum;
	}
	

	/**
	 * @return boolean expression 
	 */
	public String toString() {
		return this.getQuery();
	}
	
	/**
	 * This function recursively connects 
	 *   operand in {@code operandList} and subqueries in {@code subqueryList} 
	 *   with {@code operator} 
	 * @return boolean expression
	 */
	public String getQuery() {
		if (operator == QueryOp.ANY) {
			return DataConstants.SCAN_QUERY;
		} else if (operator == QueryOp.NONE) {
			return "";
		} else {
			StringJoiner joiner =  new StringJoiner(
					(operator == QueryOp.AND) ? " AND " : " OR ");
			for (String operand : operandList) {
				joiner.add(operand);
			}
			for (GramBooleanQuery subQuery : subQueryList) {
				String subQueryStr = subQuery.getQuery();
				if (! subQueryStr.equals("")) 
					joiner.add(subQueryStr);
			}
			
			if (joiner.length() == 0) {
				return "";
			} else {
				return "("+joiner.toString()+")";
			}
		}
	}
	
	
	
}
