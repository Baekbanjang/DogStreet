package com.example.firebase;

public class Report {
    public String type;
    public long time;
    public int mode;
    private String userId;
    private String key;
    public Report() { }


    public Report(String type,long time, int mode) {
        this.type = type;
        this.time = time;
        this.mode = mode;
    }
    public String getType() {
        return type;
    }
    public long getTime() {
        return time;
    }

    public int getMode() {
        return mode;
    }
    public String getUserId() {
        return userId;
    }
    public void setKey(String key) {
        this.key = key;
    }
    public String getKey() {
        return key;
    }
}
