package edu.buffalo.cse.cse486586.simpledht;

import java.io.Serializable;

/**
 * Created by ianno_000 on 3/31/2015.
 */
public class Pair implements Serializable {

    private String key;
    private String value;

    public Pair(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
