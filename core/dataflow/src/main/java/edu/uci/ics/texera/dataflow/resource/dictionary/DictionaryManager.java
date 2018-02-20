package edu.uci.ics.texera.dataflow.resource.dictionary;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import edu.uci.ics.texera.api.constants.SchemaConstants;
import edu.uci.ics.texera.api.field.IDField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.TermQuery;

import edu.uci.ics.texera.api.exception.StorageException;
import edu.uci.ics.texera.api.exception.TexeraException;
import edu.uci.ics.texera.api.field.StringField;
import edu.uci.ics.texera.api.tuple.Tuple;
import edu.uci.ics.texera.storage.DataReader;
import edu.uci.ics.texera.storage.DataWriter;
import edu.uci.ics.texera.storage.RelationManager;
import edu.uci.ics.texera.storage.constants.LuceneAnalyzerConstants;
import edu.uci.ics.texera.storage.utils.StorageUtils;

public class DictionaryManager {

    private static Long ID = 0L;
    private static DictionaryManager instance = null;
    private RelationManager relationManager = null;

    private DictionaryManager() throws StorageException {
        relationManager = RelationManager.getInstance();
    }

    public synchronized static DictionaryManager getInstance() throws StorageException {
        if (instance == null) {
            instance = new DictionaryManager();
            instance.createDictionaryManager();
        }
        return instance;
    }

    /**
     * Creates plan store, both an index and a directory for plan objects.
     *
     * @throws TexeraException
     */
    public void createDictionaryManager() throws TexeraException {
        if (! relationManager.checkTableExistence(DictionaryManagerConstants.TABLE_NAME)) {
            relationManager.createTable(DictionaryManagerConstants.TABLE_NAME,
                    DictionaryManagerConstants.INDEX_DIR,
                    DictionaryManagerConstants.SCHEMA,
                    LuceneAnalyzerConstants.standardAnalyzerString());
        }

        if(! Files.exists(DictionaryManagerConstants.DICTIONARY_DIR_PATH)) {
            try {
                Files.createDirectories(DictionaryManagerConstants.DICTIONARY_DIR_PATH);
            } catch (IOException e) {
                throw new StorageException(e);
            }
        }
    }

    /**
     * removes plan store, both an index and a directory for dictionary objects.
     *
     * @throws TexeraException
     */
    public void destroyDictionaryManager() throws TexeraException {
        relationManager.deleteTable(DictionaryManagerConstants.TABLE_NAME);
        StorageUtils.deleteDirectory(DictionaryManagerConstants.DICTIONARY_DIR);
    }

    public void addDictionary(String fileName, String dictionaryContent) throws StorageException {
        addDictionaryByID(fileName, dictionaryContent, new IDField(Long.toString(ID++)));
    }

    public void addDictionaryByID(String fileName, String dictionaryContent, IDField targetID) throws StorageException {
        // write metadata info
        DataWriter dataWriter = relationManager.getTableDataWriter(DictionaryManagerConstants.TABLE_NAME);
        dataWriter.open();

        // clean up the same dictionary metadata if it already exists in dictionary table
        dataWriter.deleteTuple(new TermQuery(new Term(DictionaryManagerConstants.NAME, fileName)));

        // insert new tuple
        dataWriter.insertTupleWithID(new Tuple(DictionaryManagerConstants.SCHEMA, new StringField(fileName)), targetID);

        dataWriter.close();

        // write actual dictionary file
        writeToFile(fileName, dictionaryContent);
    }

    public boolean removeDictionaryByName(String fileName) throws StorageException{
        // write metadata info
        DataWriter dataWriter = relationManager.getTableDataWriter(DictionaryManagerConstants.TABLE_NAME);
        dataWriter.open();

        // clean up the dictionary metadata if it already exists in dictionary table
        dataWriter.deleteTuple(new TermQuery(new Term(DictionaryManagerConstants.NAME, fileName)));
        dataWriter.close();
        return removeFile(fileName);
    }

    public boolean removeDictionaryByID(int targetID) throws StorageException{
        // Get FileName By ID
        DataReader dataReader = relationManager.getTableDataReader(DictionaryManagerConstants.TABLE_NAME,
                new TermQuery(new Term(SchemaConstants._ID, Integer.toString(targetID))));
        dataReader.open();
        Tuple tuple;
        if ((tuple = dataReader.getNextTuple()) == null) {
            dataReader.close();
            throw new StorageException("Dictionary " + targetID + " does not exist");
        }
        String fileName = tuple.getField(DictionaryManagerConstants.NAME).getValue().toString();

        dataReader.close();

        // write metadata info
        DataWriter dataWriter = relationManager.getTableDataWriter(DictionaryManagerConstants.TABLE_NAME);
        dataWriter.open();

        // clean up the dictionary metadata if it already exists in dictionary table
        dataWriter.deleteTupleByID(new IDField(Integer.toString(targetID)));
        dataWriter.close();
        return removeFile(fileName);
    }

    /**
     * Write uploaded file at the given location (if the file exists, remove it and write a new one.)
     *
     * @param dictionaryContent
     * @param fileName
     */
    private void writeToFile(String fileName, String dictionaryContent)  throws StorageException {
        try {
            Path filePath = DictionaryManagerConstants.DICTIONARY_DIR_PATH.resolve(fileName);;
            Files.deleteIfExists(filePath);
            Files.createFile(filePath);
            Files.write(filePath, dictionaryContent.getBytes());
        } catch (IOException e) {
            throw new StorageException("Error occurred whlie uploading dictionary");
        }
    }

    private boolean removeFile(String fileName) {
        try {
            Path filePath = DictionaryManagerConstants.DICTIONARY_DIR_PATH.resolve(fileName);;
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            return false;

        }
    }

    public List<String> getDictionaries() throws StorageException {
        List<String> dictionaries = new ArrayList<>();

        DataReader dataReader = relationManager.getTableDataReader(DictionaryManagerConstants.TABLE_NAME, new MatchAllDocsQuery());
        dataReader.open();

        Tuple tuple;
        while ((tuple = dataReader.getNextTuple()) != null) {
            dictionaries.add(tuple.getField(DictionaryManagerConstants.NAME).getValue().toString() +
                            tuple.getField(SchemaConstants._ID).getValue().toString());
        }
        dataReader.close();

        return dictionaries;
    }

    public String getDictionary(String dictionaryName) throws StorageException {
        DataReader dataReader = relationManager.getTableDataReader(DictionaryManagerConstants.TABLE_NAME,
                new TermQuery(new Term(DictionaryManagerConstants.NAME, dictionaryName)));
        dataReader.open();
        if (dataReader.getNextTuple() == null) {
            dataReader.close();
            throw new StorageException("Dictionary " + dictionaryName + "does not exist");
        }
        dataReader.close();

        try {
            return Files.lines(DictionaryManagerConstants.DICTIONARY_DIR_PATH.resolve(dictionaryName))
                    .collect(Collectors.joining(","));
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    public String getDictionaryByID(int targetID) throws StorageException {
        // Get FileName By ID
        DataReader dataReader = relationManager.getTableDataReader(DictionaryManagerConstants.TABLE_NAME,
                new TermQuery(new Term(SchemaConstants._ID, Integer.toString(targetID))));
        dataReader.open();
        Tuple tuple;
        if ((tuple = dataReader.getNextTuple()) == null) {
            dataReader.close();
            throw new StorageException("Dictionary " + targetID + "does not exist");
        }
        String fileName = tuple.getField(DictionaryManagerConstants.NAME).getValue().toString();
        dataReader.close();

        return getDictionary(fileName);
    }

    public void updateDictionaryByID(String fileName, String dictionaryContent, int targetID)
            throws StorageException {
        removeDictionaryByID(targetID);
        addDictionaryByID(fileName, dictionaryContent, new IDField(Integer.toString(targetID)));
    }

}
