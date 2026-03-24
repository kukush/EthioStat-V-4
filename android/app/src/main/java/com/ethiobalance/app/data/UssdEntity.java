package com.ethiobalance.app.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "ussd_events")
public class UssdEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String request;
    public String response;
    public long timestamp;
    public int simSlot;

    public UssdEntity(String request, String response, long timestamp, int simSlot) {
        this.request = request;
        this.response = response;
        this.timestamp = timestamp;
        this.simSlot = simSlot;
    }
}
