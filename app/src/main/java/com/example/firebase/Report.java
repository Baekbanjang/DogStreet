package com.example.firebase;

public class Report {
    String location;
    public int mode;
    String photoPath;
    public long time;
    public String type;
    private String userId;
    private String key;
    public Report() { }


    public Report(String location, int mode, String photoPath, long time, String type) {
        this.location = location;
        this.mode = mode;
        this.photoPath = photoPath;
        this.time = time;
        this.type = type;
    }
    public String getLocation(){
        return location;
    }
    public String getPhotoPath(){
        return photoPath;
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