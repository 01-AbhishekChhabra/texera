package edu.uci.ics.textdb.dataflow.regexmatch;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeSet;
import java.util.stream.Collectors;

import edu.uci.ics.textdb.common.constants.DataConstants;


public class GramBooleanQuery {
	
	enum QueryOp {
		NONE, // doesn't match any string
		ANY,  // matches any string

		LEAF, // leaf node, no child
		
		AND,
		OR
	}
	
	QueryOp operator;
	String leaf;
	Set<GramBooleanQuery> subQuerySet;
		
	GramBooleanQuery(QueryOp operator) {
		this.operator = operator;
		leaf = "";
		subQuerySet = new HashSet<GramBooleanQuery>();
	}
	
	GramBooleanQuery(String literal) {
		this.operator = QueryOp.LEAF;
		this.leaf = literal;
		this.subQuerySet = new HashSet<GramBooleanQuery>();
	}
	
	static GramBooleanQuery newLeafNode(String literal) {
		return new GramBooleanQuery(literal);
	}
	
	
	
	
	/* 
	 * logic for adding a list of strings to the query tree
	--------------------------------------------------------- */
	
	static GramBooleanQuery combine(GramBooleanQuery query, List<String> list) {
		return computeConjunction(query, listNode(list));
	}
	
	/**
	 * This method takes a list of strings and adds them to the query tree. <br>
	 * For example, if the list is {abcd, wxyz}, then: <br>
	 * trigrams({abcd, wxyz}) = trigrams(abcd) OR trigrams(wxyz) <br>
	 * OR operator is assumed for a list of strings. <br>
	 * @param list, a list of strings to be added into query.
	 */
	private static GramBooleanQuery listNode(List<String> literalList) {
		if (TranslatorUtils.minLenOfString(literalList) < TranslatorUtils.MIN_GRAM_LENGTH) {
			return new GramBooleanQuery(QueryOp.ANY);
		}
		
		GramBooleanQuery listNode = new GramBooleanQuery(QueryOp.OR);
		for (String literal : literalList) {
			listNode.subQuerySet.add(literalNode(literal));
		}
		return listNode;
	}
	
	/**
	 * This method takes a single string and adds it to the query tree. <br>
	 * The string is converted to multiple n-grams with an AND operator. <br>
	 * For example: if the string is abcd, then: <br>
	 * trigrams(abcd) = abc AND bcd <br>
	 * AND operator is assumed for a single string. <br>
	 * @param literal
	 */
	private static GramBooleanQuery literalNode(String literal) {
		GramBooleanQuery literalNode = new GramBooleanQuery(QueryOp.AND);
		for (String gram : literalToNGram(literal)) {
			literalNode.subQuerySet.add(newLeafNode(gram));
		}
		return literalNode;
	}
	
	/**
	 * This function builds a list of N-Grams that a given literal contains. <br>
	 * If the length of the literal is smaller than N, it returns an empty list. <br>
	 * For example, for literal "textdb", its tri-gram list should be ["tex", "ext", "xtd", "tdb"]
	 */
	private static List<String> literalToNGram(String literal) {
		ArrayList<String> nGrams = new ArrayList<>();
		int gramLength = TranslatorUtils.MIN_GRAM_LENGTH;
		if (literal.length() >= gramLength) {
			for (int i = 0; i <= literal.length()-gramLength; ++i) {
				nGrams.add(literal.substring(i, i+gramLength));
			}
		}
		return nGrams;
	}
	
	
	
	
	/* 
	 * basic boolean logic (conjunction, disjunction)
	--------------------------------------------------------- */
	
	/**
	 * This function "AND"s two query trees together. <br>
	 * It also performs simple simplifications. <br>
	 */
	static GramBooleanQuery computeConjunction(GramBooleanQuery left, GramBooleanQuery right) {		
		if (right.operator == QueryOp.ANY) {
			return deepCopy(left);
		}
		if (right.operator == QueryOp.NONE) {
			return deepCopy(right);
		}
		if (left.operator == QueryOp.ANY) {
			return deepCopy(right);
		}
		if (left.operator == QueryOp.NONE) {
			return deepCopy(left);
		}
		if ((left.operator == QueryOp.AND && right.operator == QueryOp.AND)
			|| (left.operator == QueryOp.AND && right.operator == QueryOp.LEAF)
			|| (left.operator == QueryOp.LEAF && right.operator == QueryOp.AND)) {
			GramBooleanQuery toReturn = new GramBooleanQuery(QueryOp.AND);
			toReturn.mergeIntoSubquery(deepCopy(left));
			toReturn.mergeIntoSubquery(deepCopy(right));
			return toReturn;
		} else {
			GramBooleanQuery toReturn = new GramBooleanQuery(QueryOp.AND);
			toReturn.subQuerySet.add(deepCopy(left));
			toReturn.subQuerySet.add(deepCopy(right));
			return toReturn;
		}
	}
	
	/**
	 * This function "OR"s two query trees together. <br>
	 * It also performs simple simplifications. <br>
	 */
	static GramBooleanQuery computeDisjunction(GramBooleanQuery left, GramBooleanQuery right) {
		if (right.operator == QueryOp.ANY) {
			return deepCopy(right);
		}
		if (right.operator == QueryOp.NONE) {
			return deepCopy(left);
		}
		if (left.operator == QueryOp.ANY) {
			return deepCopy(left);
		}
		if (left.operator == QueryOp.NONE) {
			return deepCopy(right);
		}
		if ((left.operator == QueryOp.OR && right.operator == QueryOp.OR)
			|| (left.operator == QueryOp.OR && right.operator == QueryOp.LEAF)
			|| (left.operator == QueryOp.LEAF && right.operator == QueryOp.OR)) {
			GramBooleanQuery toReturn = new GramBooleanQuery(QueryOp.OR);
			toReturn.mergeIntoSubquery(deepCopy(left));
			toReturn.mergeIntoSubquery(deepCopy(right));
			return toReturn;
		} else {
			GramBooleanQuery toReturn = new GramBooleanQuery(QueryOp.OR);
			toReturn.subQuerySet.add(deepCopy(left));
			toReturn.subQuerySet.add(deepCopy(right));
			return toReturn;
		}
	}
	
	private void mergeIntoSubquery(GramBooleanQuery that) {
		if (that.operator == QueryOp.LEAF) {
			this.subQuerySet.add(that);
		} else {
			this.subQuerySet.addAll(that.subQuerySet);
		}
	}
	


	
	/* 
	 * transform tree to Disjunctive Normal Form (DNF)
	--------------------------------------------------------- */	
	
	
	/**
	 * The query tree generated by the translator is messy with possibly lots of redundant information.
	 * This function transforms it into Disjunctive normal form (DNF), which is an OR of different ANDs. <br>
	 * <br>
	 * To transform a tree to DNF form, the following laws are applied recursively from bottom to top: <br>
	 * Associative laws: (a OR b) OR c = a OR (b OR c) = a OR b OR c, when transforming OR nodes. <br>
	 * Distributive laws: a AND (b OR c) = (a AND b) OR (a AND c), when transforming AND nodes. <br>
	 * <br>
	 * For each node, its children will be transformed to DNF form first, then 
	 * if it's OR, apply associative laws, if it's AND, apply distributive laws. <br>
	 * Then recursively apply the same rules all the way up to the top node.
	 * <br>
	 * The result is NOT simplified. Must call simplifyDNF() to obtain the optimal tree. <br>
	 * 
	 * @param query
	 * @return DNFQuery
	 */
	static GramBooleanQuery toDNF(GramBooleanQuery query) {
		GramBooleanQuery result = new GramBooleanQuery(QueryOp.OR);
		
		if (query.operator == QueryOp.ANY || query.operator == QueryOp.NONE) {
			return result;
		}
		if (query.operator == QueryOp.AND) {
			for (GramBooleanQuery subQuery : query.subQuerySet) {
				result = dnfConjunction(result, toDNF(subQuery));
			}
		}
		if (query.operator == QueryOp.OR) {
			for (GramBooleanQuery subQuery : query.subQuerySet) {
				result.subQuerySet.addAll(toDNF(subQuery).subQuerySet);
			}
		}
		if (query.operator == QueryOp.LEAF) {
			result.subQuerySet.add(deepCopy(query));
		}

		return result;	
	}

	
	/*
	 * "AND" two DNF trees (trees are assumed to be in DNF form)
	 *  Apply distributive laws:
	 *  a AND (b OR c) = (a AND b) OR (a AND c)
	 *  (a OR b) AND (c OR d) = (a AND c) OR (a AND d) OR (b AND c) OR (c AND d)
	 */
	private static GramBooleanQuery dnfConjunction(GramBooleanQuery left, GramBooleanQuery right) {
		if (left.isEmpty()) {
			return right;
		}
		if (right.isEmpty()) {
			return left;
		}

		GramBooleanQuery resultQuery = new GramBooleanQuery(QueryOp.OR);

		for (GramBooleanQuery leftSubQuery : left.subQuerySet) {
			for (GramBooleanQuery rightSubQuery : right.subQuerySet) {
				GramBooleanQuery conjunction = computeConjunction(leftSubQuery, rightSubQuery);
				resultQuery.subQuerySet.add(conjunction);
			}
		}

		return resultQuery;
	}
	

	/**
	 * Simplify a tree, which is assumed to be already in DNF form. <br>
	 * Apply Absorption laws: a OR (a AND b) -> a <br>
	 * Replace node with its child if it only has one child: (a) -> a <br> 
	 * <br>
	 * Simplifying is important because it enables comparison between two trees. <br>
	 * Two equivalent trees could have different forms. However, after transforming to DNF
	 * and applying simplifications, their forms should be identical.<br>
	 * <br>
	 * <b>Must call toDNF() before calling simplifyDNF()</b><br>
	 * <b>Simplified tree is not necessarily DNF</b><br>
	 * 
	 * @param DNFQuery
	 * @return simplifiedDNFQuery
	 */
	static GramBooleanQuery simplifyDNF(GramBooleanQuery query) {
		GramBooleanQuery result = applyAbsorption(query);
		result = replaceWithChild(result);
		return result;
	}
	
	// Replace node with its child if it only has one child: (a) -> a <br> 
	private static GramBooleanQuery replaceWithChild(GramBooleanQuery query) {
		GramBooleanQuery result = query;
		while (result.subQuerySet.size() == 1) {
			GramBooleanQuery child = result.subQuerySet.iterator().next();
			result = child;
		}
		
		Set<GramBooleanQuery> newSubQuerySet = new HashSet<GramBooleanQuery>();
		for (GramBooleanQuery subQuery : result.subQuerySet) {
			newSubQuerySet.add(replaceWithChild(subQuery));
		}
		
		result.subQuerySet = newSubQuerySet;

		return result;
	}
	
	// Apply Absorption laws: a OR (a AND b) -> a <br>
	private static GramBooleanQuery applyAbsorption(GramBooleanQuery query) {
		GramBooleanQuery result = new GramBooleanQuery(QueryOp.OR);
		
		for (GramBooleanQuery subQuery : query.subQuerySet) {
			if (! isRedundantQuery(subQuery, query.subQuerySet)) {
				result.subQuerySet.add(deepCopy(subQuery));
			}
		}
		
		return result;
	}
	
	private static boolean isRedundantQuery(GramBooleanQuery query, Set<GramBooleanQuery> querySet) {
		if (query.operator == QueryOp.LEAF) {
			return false;
		}
		for (GramBooleanQuery compareTo : querySet) {
			if (query == compareTo) {
				continue;
			}
			if (compareTo.operator == QueryOp.LEAF) {
				if (query.subQuerySet.contains(compareTo)) {
					return true;
				}
			}
			else if (compareTo.operator == QueryOp.AND) {
				if (query.subQuerySet.containsAll(compareTo.subQuerySet)) {
					return true;
				}
			}
		}
		return false;
	}
	
	
	
	
	/* 
	 * class related functions
	--------------------------------------------------------- */
	
	/**
	 * Obtain a deep copy of the query tree. <br>
	 * It traverses the tree and deep copies every node. <br>
	 */
	public static GramBooleanQuery deepCopy(GramBooleanQuery query) {
		if (query.operator == QueryOp.ANY || query.operator == QueryOp.NONE) {
			return new GramBooleanQuery(query.operator);
		} else if (query.operator == QueryOp.LEAF) {
			return newLeafNode(query.leaf);
		} else {
			GramBooleanQuery toReturn = new GramBooleanQuery(query.operator);
			for (GramBooleanQuery subQuery : query.subQuerySet) {
				toReturn.subQuerySet.add(deepCopy(subQuery));
			}
			return toReturn;
		}
	}
	
	/**
	 * This returns a GramBooleanQuery's hash code. <br>
	 * It won't traverse the whole tree, instead, 
	 * it only calculates the hashcode of direct leafs. <br>
	 * 
	 */
	@Override
	public int hashCode() {
		int hashCode = this.operator.toString().hashCode();
		if (operator == QueryOp.LEAF) {
			hashCode = hashCode ^ this.leaf.hashCode();
		}
		// hashCode is required to be immutable in Java
		// otherwise, it will cause errors equals() in hash based collections
		// since subQuerySet may change, using it in hash function will cause hash code to change
//		else {
//			hashCode = hashCode * this.subQuerySet.size();
//		}
		
		return hashCode;
	}
	
	/**
	 * This overrides "equals" function. Whenever a GramBooleanQuery 
	 * object is compared to another object, this function will be called. <br>
	 * It recursively traverses the query tree and compares 
	 * the set of sub-queries (order doesn't matter). <br>
	 * It internally uses a HashSet to compare sub-queries. <br>
	 */
	@Override
	public boolean equals(Object compareTo) {
		if (! (compareTo instanceof GramBooleanQuery)) {
			return false;
		}
		
		GramBooleanQuery that = (GramBooleanQuery) compareTo;
		if (this.operator != that.operator) {
			return false;
		}
		if (this.operator == QueryOp.ANY || this.operator == QueryOp.NONE) {
			return true;
		}
		if (this.operator == QueryOp.LEAF) {
			return this.leaf.equals(that.leaf);
		}
		if (!this.subQuerySet.equals(that.subQuerySet)) {
			return false;
		}
		
		return true;
	}
	
	public boolean isEmpty() {
		if (this.operator == QueryOp.LEAF) {
			return this.leaf.isEmpty();
		}
		for (GramBooleanQuery subQuery : this.subQuerySet) {
			if (! subQuery.isEmpty()) {
				return false;
			}
		}
		return true;
	}
	
	
	
	
	/* 
	 * string representations of the query tree
	--------------------------------------------------------- */
	
	/**
	 * This function generates a string representing the query that can be directly parsed by Lucene. <br>
	 * Same as getLuceneQueryString().
	 * @return boolean expression string
	 */
	@Override
	public String toString() {
		return this.getLuceneQueryString();
	}
	
	/**
	 * This function generates a string representing the query that can be directly parsed by Lucene.
	 * @return boolean expression string
	 */
	public String getLuceneQueryString() {
		String luceneQueryString = toLuceneQueryString(this);
		if (luceneQueryString.isEmpty()) {
			return DataConstants.SCAN_QUERY;
		} else {
			return luceneQueryString;
		}
	}
	
	private static String toLuceneQueryString(GramBooleanQuery query) {
		if (query.operator == QueryOp.ANY) {
			return "";
		}
		if (query.operator == QueryOp.NONE) {
			return "";
		}
		if (query.operator == QueryOp.LEAF) {
			return query.leaf;
		} 

		StringJoiner joiner =  new StringJoiner(
				(query.operator == QueryOp.AND) ? " AND " : " OR ");
		
		for (GramBooleanQuery subQuery : query.subQuerySet) {
			String subQueryStr = toLuceneQueryString(subQuery);
			if (! subQueryStr.equals("")) 
				joiner.add(subQueryStr);
		}
		
		if (joiner.length() == 0) {
			return "";
		}
		return "("+joiner.toString()+")";
	}
	
	
	/**
	 * This function returns a String that visualizes the query tree.
	 */
	String printQueryTree() {
		return queryTreeToString(this, 0, "  ");
	}
	
	private static String queryTreeToString(GramBooleanQuery query, int indentation, String indentStr) {
		String s = "";
		
		for (int i = 0; i < indentation; i++) {
			s += indentStr;
		}

		if (query.operator == QueryOp.LEAF) {
			s += query.leaf;
			s += "\n";
			return s; 
		}

		s += query.operator.toString();
		s += "\n";
		
		indentation++;
		for (GramBooleanQuery subQuery : query.subQuerySet) {
			s += queryTreeToString(subQuery, indentation, indentStr);
		}
		indentation--;
		return s;
	}
	
	
}
