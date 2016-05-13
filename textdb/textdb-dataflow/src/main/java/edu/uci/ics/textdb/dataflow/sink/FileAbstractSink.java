package edu.uci.ics.textdb.dataflow.sink;

import edu.uci.ics.textdb.api.common.ITuple;
import edu.uci.ics.textdb.api.dataflow.IOperator;

import java.io.*;

/**
 * Created by chenli on 5/11/16.
 *
 * This class serializes each tuple from the subtree to a given file.
 */
public class FileAbstractSink extends AbstractSink {

    private PrintWriter printWriter;
    private final File file;

    public FileAbstractSink(IOperator childOperator, File file) throws FileNotFoundException {
        super(childOperator);
        this.file = file;
    }

    @Override
    public void open() throws Exception {
        super.open();
        this.printWriter = new PrintWriter(file);
    }

    @Override
    public void close() throws Exception {
        if (this.printWriter != null){
            this.printWriter.close();
        }
        super.close();
    }

    @Override
    protected void processOneTuple(ITuple nextTuple) {
        printWriter.write(nextTuple.toString());
    }
}
