package com.example.friendmap;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_SENT = 1;
    private static final int TYPE_RECEIVED = 2;

    private List<Message> messageList;
    private String currentUserId;

    // ĐÃ SỬA: Ép truyền currentUserId thật từ Activity sang để đồng bộ tuyệt đối
    public MessageAdapter(List<Message> messageList, String currentUserId) {
        this.messageList = messageList;
        this.currentUserId = currentUserId;
    }

    @Override
    public int getItemViewType(int position) {
        // Tránh lỗi crash nếu danh sách rỗng hoặc phần tử bị rỗng ngầm
        if (messageList == null || messageList.get(position) == null) return TYPE_RECEIVED;

        String senderId = messageList.get(position).getSenderId();
        if (senderId != null && senderId.equals(currentUserId)) {
            return TYPE_SENT;
        } else {
            return TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_SENT) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_sent, parent, false);
            return new SentViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_received, parent, false);
            return new ReceivedViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (messageList == null || position >= messageList.size()) return;

        Message message = messageList.get(position);
        if (message == null) return;

        // Kiểm tra xem tin nhắn là text thường hay là emoji trêu chọc
        String displayContent = message.getText();
        if (message.getEmojiTease() != null && !message.getEmojiTease().isEmpty()) {
            displayContent = "Đã thả trêu bạn: " + message.getEmojiTease();
        }

        // Nếu cả text và emoji đều trống (tránh lỗi giao diện trống)
        if (displayContent == null || displayContent.isEmpty()) {
            displayContent = "...";
        }

        if (holder instanceof SentViewHolder) {
            SentViewHolder sentHolder = (SentViewHolder) holder;
            if (sentHolder.txtMessage != null) {
                sentHolder.txtMessage.setText(displayContent);
            }
        } else if (holder instanceof ReceivedViewHolder) {
            ReceivedViewHolder receivedHolder = (ReceivedViewHolder) holder;
            if (receivedHolder.txtMessage != null) {
                receivedHolder.txtMessage.setText(displayContent);
            }
        }
    }

    @Override
    public int getItemCount() {
        return messageList != null ? messageList.size() : 0;
    }

    static class SentViewHolder extends RecyclerView.ViewHolder {
        TextView txtMessage;
        SentViewHolder(View itemView) {
            super(itemView);
            txtMessage = itemView.findViewById(R.id.txtMessageSent);
        }
    }

    static class ReceivedViewHolder extends RecyclerView.ViewHolder {
        TextView txtMessage;
        ReceivedViewHolder(View itemView) {
            super(itemView);
            txtMessage = itemView.findViewById(R.id.txtMessageReceived);
        }
    }
}