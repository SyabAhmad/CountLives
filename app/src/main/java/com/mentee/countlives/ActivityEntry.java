package com.mentee.countlives;

public class ActivityEntry {
    public String type;
    public int durationMinutes;
    public String imageUrl;
    public long timestamp;

    public ActivityEntry() {}

    public ActivityEntry(String type, int durationMinutes, String imageUrl, long timestamp) {
        this.type = type;
        this.durationMinutes = durationMinutes;
        this.imageUrl = imageUrl;
        this.timestamp = timestamp;
    }
}
