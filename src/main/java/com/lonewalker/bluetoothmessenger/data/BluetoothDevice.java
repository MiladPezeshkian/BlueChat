package com.lonewalker.bluetoothmessenger.data;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "bluetooth_devices")
public class BluetoothDevice implements Parcelable {
    @PrimaryKey
    private String address;
    private String name;
    private boolean isPaired;
    private long lastSeen;

    public BluetoothDevice() {
    }

    public BluetoothDevice(android.bluetooth.BluetoothDevice device) {
        this.address = device.getAddress();
        this.name = device.getName();
        this.isPaired = device.getBondState() == android.bluetooth.BluetoothDevice.BOND_BONDED;
        this.lastSeen = System.currentTimeMillis();
    }

    protected BluetoothDevice(Parcel in) {
        address = in.readString();
        name = in.readString();
        isPaired = in.readByte() != 0;
        lastSeen = in.readLong();
    }

    public static final Creator<BluetoothDevice> CREATOR = new Creator<BluetoothDevice>() {
        @Override
        public BluetoothDevice createFromParcel(Parcel in) {
            return new BluetoothDevice(in);
        }

        @Override
        public BluetoothDevice[] newArray(int size) {
            return new BluetoothDevice[size];
        }
    };

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isPaired() {
        return isPaired;
    }

    public void setPaired(boolean paired) {
        isPaired = paired;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BluetoothDevice that = (BluetoothDevice) o;
        return address.equals(that.address);
    }

    @Override
    public int hashCode() {
        return address.hashCode();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(address);
        dest.writeString(name);
        dest.writeByte((byte) (isPaired ? 1 : 0));
        dest.writeLong(lastSeen);
    }
} 