package com.fintrack.dto;

public class UnreadNotificationCountResponse {

    private long count;

    public UnreadNotificationCountResponse() {
    }

    public UnreadNotificationCountResponse(long count) {
        this.count = count;
    }

    public long getCount() {
        return count;
    }
}