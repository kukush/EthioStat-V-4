package com.ethiobalance.app.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface SmsDao {
    @Insert
    void insert(SmsEntity sms);

    @Query("SELECT * FROM sms_events WHERE isSynced = 0 ORDER BY timestamp ASC")
    List<SmsEntity> getUnsyncedSms();

    @Update
    void update(SmsEntity sms);

    @Query("UPDATE sms_events SET isSynced = 1 WHERE id IN (:ids)")
    void markAsSynced(List<Integer> ids);
}
