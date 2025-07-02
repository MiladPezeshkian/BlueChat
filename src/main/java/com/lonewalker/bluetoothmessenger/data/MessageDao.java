package com.lonewalker.bluetoothmessenger.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MessageDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Message message);
    
    @Query("SELECT * FROM messages WHERE (senderAddress = :address1 AND receiverAddress = :address2) OR (senderAddress = :address2 AND receiverAddress = :address1) ORDER BY timestamp ASC")
    LiveData<List<Message>> getMessagesBetweenDevices(String address1, String address2);
    
    @Query("SELECT * FROM messages WHERE (senderAddress = :address1 AND receiverAddress = :address2) OR (senderAddress = :address2 AND receiverAddress = :address1) ORDER BY timestamp ASC")
    List<Message> getMessagesBetweenDevicesSync(String address1, String address2);
    
    @Query("SELECT * FROM messages WHERE deviceAddress = :deviceAddress ORDER BY timestamp ASC")
    List<Message> getMessagesByDevice(String deviceAddress);
    
    @Query("DELETE FROM messages WHERE (senderAddress = :address1 AND receiverAddress = :address2) OR (senderAddress = :address2 AND receiverAddress = :address1)")
    void deleteMessagesBetweenDevices(String address1, String address2);
    
    @Query("DELETE FROM messages")
    void deleteAllMessages();
} 