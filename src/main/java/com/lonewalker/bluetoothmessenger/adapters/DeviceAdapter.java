package com.lonewalker.bluetoothmessenger.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.lonewalker.bluetoothmessenger.R;

import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {

    private List<com.lonewalker.bluetoothmessenger.data.BluetoothDevice> devices;
    private OnDeviceClickListener listener;

    public interface OnDeviceClickListener {
        void onDeviceClick(com.lonewalker.bluetoothmessenger.data.BluetoothDevice device);
        void onVoiceClick(com.lonewalker.bluetoothmessenger.data.BluetoothDevice device);
        void onChatClick(com.lonewalker.bluetoothmessenger.data.BluetoothDevice device);
    }

    public DeviceAdapter(List<com.lonewalker.bluetoothmessenger.data.BluetoothDevice> devices, OnDeviceClickListener listener) {
        this.devices = devices;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_device, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        com.lonewalker.bluetoothmessenger.data.BluetoothDevice device = devices.get(position);
        holder.bind(device);
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    public void updateDevices(List<com.lonewalker.bluetoothmessenger.data.BluetoothDevice> newDevices) {
        this.devices.clear();
        this.devices.addAll(newDevices);
        notifyDataSetChanged();
    }

    class DeviceViewHolder extends RecyclerView.ViewHolder {
        private TextView deviceName;
        private TextView deviceAddress;
        private TextView deviceStatus;
        private ImageButton btnVoice;
        private ImageButton btnChat;

        public DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            deviceName = itemView.findViewById(R.id.deviceName);
            deviceAddress = itemView.findViewById(R.id.deviceAddress);
            deviceStatus = itemView.findViewById(R.id.deviceStatus);
            btnVoice = itemView.findViewById(R.id.btnVoice);
            btnChat = itemView.findViewById(R.id.btnChat);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onDeviceClick(devices.get(position));
                }
            });

            btnVoice.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onVoiceClick(devices.get(position));
                }
            });

            btnChat.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onChatClick(devices.get(position));
                }
            });
        }

        public void bind(com.lonewalker.bluetoothmessenger.data.BluetoothDevice device) {
            String name = device.getName();
            if (name == null || name.isEmpty()) {
                deviceName.setText("دستگاه ناشناس");
            } else {
                deviceName.setText(name);
            }
            
            deviceAddress.setText(device.getAddress());
            
            // نمایش وضعیت pair شدن
            if (device.isPaired()) {
                deviceStatus.setText("جفت شده");
                deviceStatus.setTextColor(itemView.getContext().getColor(R.color.status_connected));
            } else {
                deviceStatus.setText("جفت نشده");
                deviceStatus.setTextColor(itemView.getContext().getColor(R.color.status_disconnected));
            }
        }
    }
} 