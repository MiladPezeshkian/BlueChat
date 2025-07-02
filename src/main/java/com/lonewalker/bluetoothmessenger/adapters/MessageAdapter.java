package com.lonewalker.bluetoothmessenger.adapters;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.lonewalker.bluetoothmessenger.R;
import com.lonewalker.bluetoothmessenger.data.Message;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private List<Message> messages;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", new Locale("fa"));

    public MessageAdapter(List<Message> messages) {
        this.messages = messages;
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);
        return message.isSent() ? VIEW_TYPE_SENT : VIEW_TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SENT) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_sent, parent, false);
            return new SentMessageViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_received, parent, false);
            return new ReceivedMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messages.get(position);
        if (holder instanceof SentMessageViewHolder) {
            ((SentMessageViewHolder) holder).bind(message);
        } else if (holder instanceof ReceivedMessageViewHolder) {
            ((ReceivedMessageViewHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    class SentMessageViewHolder extends RecyclerView.ViewHolder {
        private TextView messageText;
        private TextView messageTime;

        public SentMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
            messageTime = itemView.findViewById(R.id.messageTime);
            
            // Add long click listener for copy functionality
            itemView.setOnLongClickListener(v -> {
                copyMessageToClipboard(messageText.getText().toString(), itemView.getContext());
                return true;
            });
        }

        public void bind(Message message) {
            messageText.setText(message.getContent());
            messageTime.setText(timeFormat.format(new Date(message.getTimestamp())));
        }
    }

    class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        private TextView messageText;
        private TextView messageTime;

        public ReceivedMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
            messageTime = itemView.findViewById(R.id.messageTime);
            
            // Add long click listener for copy functionality
            itemView.setOnLongClickListener(v -> {
                copyMessageToClipboard(messageText.getText().toString(), itemView.getContext());
                return true;
            });
        }

        public void bind(Message message) {
            messageText.setText(message.getContent());
            messageTime.setText(timeFormat.format(new Date(message.getTimestamp())));
        }
    }
    
    private void copyMessageToClipboard(String message, Context context) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("پیام کپی شده", message);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(context, "پیام در کلیپ‌بورد کپی شد", Toast.LENGTH_SHORT).show();
    }
} 