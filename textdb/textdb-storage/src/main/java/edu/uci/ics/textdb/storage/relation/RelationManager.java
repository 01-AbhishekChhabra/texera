package edu.uci.ics.textdb.storage.relation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import edu.uci.ics.textdb.api.common.Attribute;
import edu.uci.ics.textdb.api.common.FieldType;
import edu.uci.ics.textdb.api.common.IField;
import edu.uci.ics.textdb.api.common.ITuple;
import edu.uci.ics.textdb.api.common.Schema;
import edu.uci.ics.textdb.api.storage.IDataStore;
import edu.uci.ics.textdb.common.constants.LuceneAnalyzerConstants;
import edu.uci.ics.textdb.common.constants.SchemaConstants;
import edu.uci.ics.textdb.common.exception.DataFlowException;
import edu.uci.ics.textdb.common.exception.StorageException;
import edu.uci.ics.textdb.common.field.DataTuple;
import edu.uci.ics.textdb.common.field.IDField;
import edu.uci.ics.textdb.common.utils.Utils;
import edu.uci.ics.textdb.storage.DataReaderPredicate;
import edu.uci.ics.textdb.storage.DataStore;
import edu.uci.ics.textdb.storage.reader.DataReader;
import edu.uci.ics.textdb.storage.writer.DataWriter;

public class RelationManager {
    
    private static volatile RelationManager singletonRelationManager = null;
    
    private RelationManager() throws StorageException {
        if (! checkCatalogExistence()) {
            initializeCatalog();
        }
    }

    public static RelationManager getRelationManager() throws StorageException {
        if (singletonRelationManager == null) {
            synchronized (RelationManager.class) {
                if (singletonRelationManager == null) {
                    singletonRelationManager = new RelationManager();
                }
            }
        }
        return singletonRelationManager;
    }

    /**
     * Checks if a table exists by looking it up in the catalog.
     * 
     * @param tableName
     * @return
     */
    public boolean checkTableExistence(String tableName) {
        try {
            getTableDirectory(tableName);
            getTableSchema(tableName);
            return true;
        } catch (StorageException e) {
            return false;
        }
    }

    /**
     * Creates a new table. 
     *   Table name must be unique.
     *   LuceneAnalyzer must be a valid analyzer string.
     * 
     * The "_id" attribute will be added to the table schema.
     * System automatically generates a unique ID for each tuple inserted to a table,
     *   the generated ID will be in "_id" field.
     * 
     * @param tableName
     * @param indexDirectory
     * @param schema
     * @param luceneAnalyzer
     * @throws StorageException
     */
    public void createTable(String tableName, String indexDirectory, Schema schema, String luceneAnalyzer)
            throws StorageException {
        // table should not exist
        if (checkTableExistence(tableName)) {
            throw new StorageException(String.format("Table %s already exists.", tableName));
        }
        
        Schema tableSchema = Utils.getSchemaWithID(schema);
        DataStore tableDataStore = new DataStore(indexDirectory, tableSchema);
        Analyzer tableAnalyzer = null;
        try {
            tableAnalyzer = LuceneAnalyzerConstants.getLuceneAnalyzer(luceneAnalyzer);
        } catch (DataFlowException e) {
            throw new StorageException("Lucene Analyzer String is not valid.");
        }
        
        // clear table contents
        new DataWriter(tableDataStore, tableAnalyzer).clearData();
                
        // write table catalog
        DataStore tableCatalogStore = new DataStore(CatalogConstants.TABLE_CATALOG_DIRECTORY,
                CatalogConstants.TABLE_CATALOG_SCHEMA);
        insertTupleToDirectory(tableCatalogStore, LuceneAnalyzerConstants.getStandardAnalyzer(),
                CatalogConstants.getTableCatalogTuple(tableName, indexDirectory, luceneAnalyzer));
       
        // write schema catalog
        DataStore schemaCatalogStore = new DataStore(CatalogConstants.SCHEMA_CATALOG_DIRECTORY,
                CatalogConstants.SCHEMA_CATALOG_SCHEMA);
        // each attribute in the table schema will be one row in schema catalog
        for (ITuple tuple : CatalogConstants.getSchemaCatalogTuples(tableName, tableSchema)) {
            insertTupleToDirectory(schemaCatalogStore, LuceneAnalyzerConstants.getStandardAnalyzer(), tuple);
        }

    }

    /**
     * Deletes a table by its name.
     * If the table doesn't exist, it won't do anything.
     * Deleting system catalog tables is prohibited.
     * 
     * @param tableName
     * @throws StorageException
     */
    public void deleteTable(String tableName) throws StorageException {
        if (isSystemCatalog(tableName)) {
            throw new StorageException("Deleting a system catalog table is prohibited.");
        }
        
        Query catalogTableNameQuery = new TermQuery(new Term(CatalogConstants.TABLE_NAME, tableName));
        
        DataWriter tableCatalogWriter = new DataWriter(CatalogConstants.TABLE_CATALOG_DATASTORE, 
                LuceneAnalyzerConstants.getStandardAnalyzer());
        tableCatalogWriter.deleteTuple(catalogTableNameQuery);
        
        DataWriter schemaCatalogWriter = new DataWriter(CatalogConstants.SCHEMA_CATALOG_DATASTORE,
                LuceneAnalyzerConstants.getStandardAnalyzer());
        schemaCatalogWriter.deleteTuple(catalogTableNameQuery);
    }

    /**
     * Inserts a tuple to a table.
     * A unique ID is automatically generated for each tuple, and put in "_id" field.
     * 
     * @param tableName
     * @param tuple
     * @return IDField, the ID automatically generated by the system.
     * @throws StorageException
     */
    public IDField insertTuple(String tableName, ITuple tuple) throws StorageException {
        if (isSystemCatalog(tableName)) {
            throw new StorageException("Inserting tuples to a system catalog table is prohibited.");
        }
        // table must exist
        if (! checkTableExistence(tableName)) {
            throw new StorageException(String.format("Table %s doesn't exist.", tableName));
        }
        // tuple must not contain _id field
        if (tuple.getSchema().containsField(SchemaConstants._ID)) {
            throw new StorageException("Tuple must not contain _id field. _id must be generated by the system");
        }
        
        DataStore tableDataStore = getTableDataStore(tableName);
        String tableAnalyzerString = getTableAnalyzer(tableName);
        Analyzer luceneAnalyzer = null;
        try {
            luceneAnalyzer = LuceneAnalyzerConstants.getLuceneAnalyzer(tableAnalyzerString);
        } catch (DataFlowException e) {
            throw new StorageException(e);
        }
        
        IDField idField = new IDField(UUID.randomUUID().toString());
        ITuple insertionTuple = getTupleWithID(tuple, idField);
        
        insertTupleToDirectory(tableDataStore, luceneAnalyzer, insertionTuple);
        
        return idField;
    }
        
    // this is a helper function to insert a tuple to a directory
    // the caller must make sure the table schema is consistent with tuple's schema
    private void insertTupleToDirectory(IDataStore dataStore, Analyzer luceneAnalyzer, ITuple tuple) throws StorageException {
        String tableDirectory = dataStore.getDataDirectory();
        Schema tableSchema = dataStore.getSchema();                       

        if (! tableSchema.equals(tuple.getSchema())) {
            throw new StorageException("Tuple's schema is inconsistent with table schema.");
        }
        
        DataWriter dataWriter = new DataWriter(new DataStore(tableDirectory, tableSchema), luceneAnalyzer);
        dataWriter.insertTuple(tuple);
    }

    /**
     * Deletes a tuple by its _id value.
     * It won't do anything if the tuple doesn't exist.
     * 
     * @param tableName
     * @param idValue
     * @throws StorageException
     */
    public void deleteTuple(String tableName, IField idValue) throws StorageException {
        if (isSystemCatalog(tableName)) {
            throw new StorageException("Deleting tuples from a system catalog table is prohibited.");
        }
        try {
            DataStore tableDataStore = getTableDataStore(tableName);
            String tableAnalyzerString = getTableAnalyzer(tableName);
            Analyzer luceneAnalyzer = LuceneAnalyzerConstants.getLuceneAnalyzer(tableAnalyzerString);
            
            Query tupleIDQuery = new TermQuery(new Term(SchemaConstants._ID, idValue.getValue().toString()));
            DataWriter tableDataWriter = new DataWriter(tableDataStore, luceneAnalyzer);
            tableDataWriter.deleteTuple(tupleIDQuery);   
        } catch (DataFlowException e) {
            throw new StorageException(e);
        }   
    }
    
    /**
     * Deletes tuples by the deletion query, tuples match the deletionQuery will be deleted.
     * 
     * @param tableName
     * @param deletionQueries
     * @throws StorageException
     */
    public void deleteTuples(String tableName, Query deletionQuery) throws StorageException {
        if (isSystemCatalog(tableName)) {
            throw new StorageException("Deleting tuples from a system catalog table is prohibited.");
        }
        try {
            DataStore tableDataStore = getTableDataStore(tableName);
            Analyzer luceneAnalyzer = LuceneAnalyzerConstants.getLuceneAnalyzer(getTableAnalyzer(tableName));
            
            DataWriter tableDataWriter = new DataWriter(tableDataStore, luceneAnalyzer);
            tableDataWriter.deleteTuple(deletionQuery);
        } catch (DataFlowException e) {
            throw new StorageException(e);
        }
    }
    

    // update a tuple by its id
    public void updateTuple(String tableName, ITuple newTuple, IDField idValue) throws StorageException {
        if (isSystemCatalog(tableName)) {
            throw new StorageException("Updating tuples in a system catalog table is prohibited.");
        }
        if (getTuple(tableName, idValue) == null) {
            throw new StorageException(
                    String.format("Tuple with id %s doesn't exist in table %s.", idValue, tableName));
        }

        ITuple newTupleWithID = getTupleWithID(newTuple, (IDField) idValue);
        if (newTupleWithID.getField(SchemaConstants._ID) != (IDField) idValue) {
            throw new StorageException("New tuple's ID is inconsistent with idValue.");
        }
        
        deleteTuple(tableName, idValue);
        insertTuple(tableName, newTupleWithID);
    }

    
    /**
     * Gets a tuple in a table by its _id field.
     * Returns null if the tuple doesn't exist.
     * 
     * @param tableName
     * @param idValue
     * @return
     * @throws StorageException
     */
    public ITuple getTuple(String tableName, IDField idValue) throws StorageException {
        DataStore tableDataStore = getTableDataStore(tableName);
        
        Query tupleIDQuery = new TermQuery(new Term(SchemaConstants._ID, idValue.getValue().toString()));
        
        DataReaderPredicate dataReaderPredicate = new DataReaderPredicate(tupleIDQuery, tableDataStore);
        dataReaderPredicate.setIsPayloadAdded(false);
        DataReader dataReader = new DataReader(dataReaderPredicate);
        
        dataReader.open(); 
        ITuple tuple = dataReader.getNextTuple();
        dataReader.close();

        return tuple;
    }
    
    /**
     * Gets a list of tuples in a table based on a query.
     * The tuples are returned through a DataReader.
     * 
     * @param tableName
     * @param tupleQuery
     * @return
     * @throws StorageException
     */
    public DataReader getTuples(String tableName, Query tupleQuery) throws StorageException {
        DataStore tableDataStore = getTableDataStore(tableName);
        return new DataReader(new DataReaderPredicate(tupleQuery, tableDataStore));
    }

    /**
     * Scans the table and get all tuples through a DataReader.
     * 
     * @param tableName
     * @return
     * @throws StorageException
     */
    public DataReader scanTable(String tableName) throws StorageException {
        DataStore tableDataStore = getTableDataStore(tableName);
        DataReader dataReader = new DataReader(DataReaderPredicate.getScanPredicate(tableDataStore));
        return dataReader;
    }
    
    /**
     * Gets the DataStore(directory and schema) of a table.
     * 
     * @param tableName
     * @return
     * @throws StorageException
     */
    public DataStore getTableDataStore(String tableName) throws StorageException {
        String tableDirectory = getTableDirectory(tableName);
        Schema tableSchema = getTableSchema(tableName);
        return new DataStore(tableDirectory, tableSchema);
    }

    /**
     * Gets the directory of a table.
     * 
     * @param tableName
     * @return
     * @throws StorageException
     */
    public String getTableDirectory(String tableName) throws StorageException {
        // get the tuples with tableName from the table catalog
        Query tableNameQuery = new TermQuery(new Term(CatalogConstants.TABLE_NAME, tableName));
        DataReader tableDataReader = getTuples(CatalogConstants.TABLE_CATALOG, tableNameQuery);
        
        tableDataReader.open();
        ITuple nextTuple = tableDataReader.getNextTuple();
        tableDataReader.close();
        
        // if the tuple is not found, then the table name is not found
        if (nextTuple == null) {
            throw new StorageException(String.format("The directory for table %s is not found.", tableName));
        }
        
        // get the directory field
        IField directoryField = nextTuple.getField(CatalogConstants.TABLE_DIRECTORY);
        return directoryField.getValue().toString();
    }

    /**
     * Gets the schema of a table.
     * 
     * @param tableName
     * @return
     * @throws StorageException
     */
    public Schema getTableSchema(String tableName) throws StorageException {
        // get the tuples with tableName from the schema catalog
        Query tableNameQuery = new TermQuery(new Term(CatalogConstants.TABLE_NAME, tableName));
        DataReader tableDataReader = getTuples(CatalogConstants.SCHEMA_CATALOG, tableNameQuery);
        
        // read the tuples into a list
        tableDataReader.open();    
        List<ITuple> tableAttributeTuples = new ArrayList<>();
        ITuple nextTuple;
        while ((nextTuple = tableDataReader.getNextTuple()) != null) {
            tableAttributeTuples.add(nextTuple);
        }

        // if the list is empty, then the schema is not found
        if (tableAttributeTuples.isEmpty()) {
            throw new StorageException(String.format("The schema of table %s is not found.", tableName));
        }
        
        // convert the unordered list of tuples to an order list of attributes
        List<Attribute> tableSchemaData = tableAttributeTuples.stream()
                // sort the tuples based on the attributePosition field.
                .sorted((tuple1, tuple2) -> Integer.compare((int) tuple1.getField(CatalogConstants.ATTR_POSITION).getValue(), 
                        (int) tuple2.getField(CatalogConstants.ATTR_POSITION).getValue()))
                // map one tuple to one attribute
                .map(tuple -> new Attribute(tuple.getField(CatalogConstants.ATTR_NAME).getValue().toString(),
                        convertAttributeType(tuple.getField(CatalogConstants.ATTR_TYPE).getValue().toString())))
                .collect(Collectors.toList());
        
        return new Schema(tableSchemaData.stream().toArray(Attribute[]::new));
    }

    /**
     * Gets the Lucene analyzer string of a table.
     * Lucene analyzer string can be further converted to a lucene analyzer object
     *   by calling LuceneAnalyzerConstants.getLuceneAnalyzer
     *   
     * @param tableName
     * @return
     * @throws StorageException
     */
    public String getTableAnalyzer(String tableName) throws StorageException {
        // get the tuples with tableName from the table catalog
        Query tableNameQuery = new TermQuery(new Term(CatalogConstants.TABLE_NAME, tableName));
        DataReader tableDataReader = getTuples(CatalogConstants.TABLE_CATALOG, tableNameQuery);
        
        tableDataReader.open();
        ITuple nextTuple = tableDataReader.getNextTuple();
        tableDataReader.close();
        
        // if the tuple is not found, then the table name is not found
        if (nextTuple == null) {
            throw new StorageException(String.format("The analyzer for table %s is not found.", tableName));
        }
        
        IField analyzerField = nextTuple.getField(CatalogConstants.TABLE_LUCENE_ANALYZER);
        return analyzerField.getValue().toString();
    }
    
    /*
     * This is a helper function to check if the system catalog tables exist physically on the disk.
     */
    private static boolean checkCatalogExistence() {
        return DataReader.checkIndexExistence(CatalogConstants.TABLE_CATALOG_DIRECTORY)
                && DataReader.checkIndexExistence(CatalogConstants.SCHEMA_CATALOG_DIRECTORY);
    }
    
    /*
     * This is a helper function to check if the table is a system catalog table.
     */
    private static boolean isSystemCatalog(String tableName) {
        return tableName.equals(CatalogConstants.TABLE_CATALOG) || tableName.equals(CatalogConstants.SCHEMA_CATALOG);
    }
    
    /*
     * Initializes the system catalog tables.
     */
    private void initializeCatalog() throws StorageException {
        createTable(CatalogConstants.TABLE_CATALOG, 
                CatalogConstants.TABLE_CATALOG_DIRECTORY, 
                CatalogConstants.TABLE_CATALOG_SCHEMA,
                LuceneAnalyzerConstants.standardAnalyzerString());
        createTable(CatalogConstants.SCHEMA_CATALOG,
                CatalogConstants.SCHEMA_CATALOG_DIRECTORY,
                CatalogConstants.SCHEMA_CATALOG_SCHEMA,
                LuceneAnalyzerConstants.standardAnalyzerString());
    }
    
    
    /*
     * Converts a attributeTypeString to FieldType (case insensitive). 
     * It returns null if string is not a valid type.
     * 
     */
    private static FieldType convertAttributeType(String attributeTypeStr) {
        return Stream.of(FieldType.values())
                .filter(typeStr -> typeStr.toString().toLowerCase().equals(attributeTypeStr.toLowerCase()))
                .findAny().orElse(null);
    }
    
    /*
     * Adds the _id to the front of the tuple.
     */
    private static ITuple getTupleWithID(ITuple tuple, IDField _id) {
        ITuple tupleWithID = tuple;
        
        Schema tupleSchema = tuple.getSchema();
        if (! tupleSchema.containsField(SchemaConstants._ID)) {
            tupleSchema = Utils.getSchemaWithID(tupleSchema);
            List<IField> newTupleFields = new ArrayList<>();
            newTupleFields.add(_id);
            newTupleFields.addAll(tuple.getFields());
            tupleWithID = new DataTuple(tupleSchema, newTupleFields.stream().toArray(IField[]::new));
        }
        
        return tupleWithID;
    }

}
