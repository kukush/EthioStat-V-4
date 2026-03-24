package com.ethiobalance.app.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface UssdDao {
    @Insert
    void insert(UssdEntity ussd);

    @Query("SELECT * FROM ussd_events ORDER BY timestamp DESC")
    List<UssdEntity> getAllUssdEvents();
}
