package edu.uci.ics.textdb.storage;

import edu.uci.ics.textdb.api.common.Schema;
import edu.uci.ics.textdb.api.storage.IDataStore;

public class DataStore implements IDataStore {
    private String dataDirectory;
    private Schema schema;

    public DataStore(String dataDirectory, Schema schema) {
        this.dataDirectory = dataDirectory;
        this.schema = schema;
    }

    @Override
    public String getDataDirectory() {
        return dataDirectory;
    }

    @Override
    public Schema getSchema() {
        return schema;
    }

}
