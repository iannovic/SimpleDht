package edu.buffalo.cse.cse486586.simpledht;

import android.content.ContentValues;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Created by ianno_000 on 3/26/2015.
 */
public class ChordPojo implements Serializable {

    static final int TYPE_QUERY = 0;
    static final int TYPE_INSERT = 1;
    static final int TYPE_DELETE = 2;
    static final int FIND_PREDECESSOR = 3;
    static final int QUERY_RESULT = 4;
    static final int NODE_JOIN_REQUEST = 5;
    static final int SUCCESSOR_PREDECESSOR_UPDATE = 6;

    int type;
    int second_type;
    int predecessorDifference;
    boolean readyToInsert = false;
    String destinationId;
    private String nodeId;
    private String key;
    private String value;
    private LinkedList<Pair> results;
    private String predecessor;
    private String successor;
    private int currentNodeCount;

    public ChordPojo() {
        results = new LinkedList<Pair>();
    }
    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getDestinationId() {
        return destinationId;
    }

    public void setDestinationId(String destinationId) {
        this.destinationId = destinationId;
    }

    public ContentValues getValues() {
        ContentValues values = new ContentValues();
        values.put("key",key);
        values.put("value",value);
        if (readyToInsert) {
            values.put("predecessor","yes");
        }
        return values;
    }

    public void setValues(ContentValues values) {
        key = (String) values.get("key");
        value = (String) values.get("value");
    }

    public int getPredecessorDifference() {
        return predecessorDifference;
    }

    public void setPredecessorDifference(int predecessorDifference) {
        this.predecessorDifference = predecessorDifference;
    }

    public boolean isReadyToInsert() {
        return readyToInsert;
    }

    public void setReadyToInsert(boolean readyToInsert) {
        this.readyToInsert = readyToInsert;
    }

    public int getSecond_type() {
        return second_type;
    }

    public void setSecond_type(int second_type) {
        this.second_type = second_type;
    }

    public LinkedList<Pair> getResults() {
        return results;
    }

    public void setResults(LinkedList<Pair> results) {
        this.results = results;
    }

    public String getPredecessor() {
        return predecessor;
    }

    public void setPredecessor(String predecessor) {
        this.predecessor = predecessor;
    }

    public String getSuccessor() {
        return successor;
    }

    public void setSuccessor(String successor) {
        this.successor = successor;
    }

    public int getCurrentNodeCount() {
        return currentNodeCount;
    }

    public void setCurrentNodeCount(int currentNodeCount) {
        this.currentNodeCount = currentNodeCount;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }
}

