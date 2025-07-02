package com.lonewalker.bluetoothmessenger.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "messages")
public class Message {
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    private String content;
    private String senderAddress;
    private String receiverAddress;
    private String deviceAddress; // For storing the device address for filtering
    private long timestamp;
    private boolean isSent;
    
    // Default constructor for Room
    public Message() {
    }
    
    public Message(String content, String senderAddress, String receiverAddress, long timestamp, boolean isSent) {
        this.content = content;
        this.senderAddress = senderAddress;
        this.receiverAddress = receiverAddress;
        this.timestamp = timestamp;
        this.isSent = isSent;
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getSenderAddress() {
        return senderAddress;
    }
    
    public void setSenderAddress(String senderAddress) {
        this.senderAddress = senderAddress;
    }
    
    public String getReceiverAddress() {
        return receiverAddress;
    }
    
    public void setReceiverAddress(String receiverAddress) {
        this.receiverAddress = receiverAddress;
    }
    
    public String getDeviceAddress() {
        return deviceAddress;
    }
    
    public void setDeviceAddress(String deviceAddress) {
        this.deviceAddress = deviceAddress;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public boolean isSent() {
        return isSent;
    }
    
    public void setSent(boolean sent) {
        isSent = sent;
    }
} 