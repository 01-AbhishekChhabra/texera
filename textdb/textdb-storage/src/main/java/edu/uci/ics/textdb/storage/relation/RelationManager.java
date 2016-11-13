package edu.uci.ics.textdb.storage.relation;

import edu.uci.ics.textdb.api.common.IField;
import edu.uci.ics.textdb.api.common.ITuple;
import edu.uci.ics.textdb.api.common.Schema;
import edu.uci.ics.textdb.api.exception.TextDBException;
import edu.uci.ics.textdb.api.storage.IDataReader;
import edu.uci.ics.textdb.api.storage.IRelationManager;

public class RelationManager implements IRelationManager {
    
    private static volatile RelationManager singletonRelationManager = null;
    
    private RelationManager() {
        
    }

    @Override
    public IRelationManager getRelationManager() {
        if (singletonRelationManager == null) {
            synchronized (RelationManager.class) {
                if (singletonRelationManager == null) {
                    singletonRelationManager = new RelationManager();
                }
            }
        }
        return singletonRelationManager;
    }

    @Override
    public boolean checkTableExistence(String tableName) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void createTable(String tableName, String indexDirectory, Schema schema, String luceneAnalyzer)
            throws TextDBException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void deleteTable(String tableName) throws TextDBException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public IField insertTuple(String tableName, ITuple tuple) throws TextDBException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void deleteTuple(String tableName, IField idValue) throws TextDBException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void updateTuple(String tableName, ITuple newTuple, IField idValue) throws TextDBException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public ITuple getTuple(String tableName, IField idValue) throws TextDBException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IDataReader scanTable(String tableName) throws TextDBException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getTableDirectory(String tableName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Schema getTableSchema(String tableName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getTableAnalyzer(String tableName) {
        // TODO Auto-generated method stub
        return null;
    }


}
