package com.ethiobalance.app.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "sms_events")
public class SmsEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String sender;
    public String body;
    public long timestamp;
    public int simSlot;
    public boolean isSynced; // Whether it has been pushed to the React store

    public SmsEntity(String sender, String body, long timestamp, int simSlot) {
        this.sender = sender;
        this.body = body;
        this.timestamp = timestamp;
        this.simSlot = simSlot;
        this.isSynced = false;
    }
}
