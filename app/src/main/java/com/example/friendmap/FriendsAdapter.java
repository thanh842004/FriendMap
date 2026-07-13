package com.example.friendmap;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.ViewHolder> {

    private List<User> friendsList;

    public FriendsAdapter(List<User> friendsList) {
        this.friendsList = friendsList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (friendsList == null || position >= friendsList.size()) return;

        User friend = friendsList.get(position);
        if (friend == null) return;

        // Xác định tên hiển thị
        String finalName = "Người dùng FriendMap";
        if (friend.getHoTen() != null && !friend.getHoTen().trim().isEmpty()) {
            finalName = friend.getHoTen();
        } else if (friend.getDisplayName() != null && !friend.getDisplayName().trim().isEmpty()) {
            finalName = friend.getDisplayName();
        } else if (friend.getUsername() != null && !friend.getUsername().trim().isEmpty()) {
            finalName = friend.getUsername();
        }

        holder.txtDisplayName.setText(finalName);

        // Hiển thị số điện thoại
        if (friend.getPhone() != null && !friend.getPhone().trim().isEmpty()) {
            holder.txtPhone.setText(friend.getPhone());
        } else {
            holder.txtPhone.setText("Chưa cập nhật SĐT");
        }

        // Hiển thị avatar
        loadAvatar(holder.imgAvatar, friend.getAvatarBase64());

        // Đếm tin nhắn chưa đọc
        String currentUid = com.google.firebase.auth.FirebaseAuth.getInstance()
                .getCurrentUser() != null ?
                com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
        loadUnreadCount(holder, friend.getUid(), currentUid);

        // Biến final để dùng trong lambda
        final String partnerName = finalName;

        // Mở màn hình chat
        holder.itemView.setOnClickListener(v -> {
            Context context = v.getContext();
            Intent intent = new Intent(context, ChatActivity.class);
            intent.putExtra("PARTNER_ID", friend.getUid());
            intent.putExtra("PARTNER_NAME", partnerName);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return friendsList == null ? 0 : friendsList.size();
    }

    private void loadAvatar(ImageView imageView, String base64) {
        if (imageView == null) return;
        if (base64 == null || base64.isEmpty()) {
            imageView.setImageResource(android.R.drawable.sym_def_app_icon);
            return;
        }
        try {
            byte[] decoded = Base64.decode(base64, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
            imageView.setImageBitmap(bitmap);
        } catch (Exception e) {
            imageView.setImageResource(android.R.drawable.sym_def_app_icon);
        }
    }

    private void loadUnreadCount(ViewHolder holder, String friendUid, String currentUid) {
        if (friendUid == null || currentUid == null || currentUid.isEmpty()) return;

        List<String> ids = new java.util.ArrayList<>();
        ids.add(currentUid);
        ids.add(friendUid);
        java.util.Collections.sort(ids);
        String chatRoomId = ids.get(0) + "_" + ids.get(1);

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("messages")
                .whereEqualTo("chatRoomId", chatRoomId)
                .whereEqualTo("isRead", false)
                .whereEqualTo("senderId", friendUid)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;
                    int count = snapshots.size();
                    if (holder.txtUnreadCount == null) return;
                    if (count > 0) {
                        holder.txtUnreadCount.setVisibility(View.VISIBLE);
                        holder.txtUnreadCount.setText(String.valueOf(count));
                    } else {
                        holder.txtUnreadCount.setVisibility(View.GONE);
                        holder.txtUnreadCount.setText("");
                    }
                });
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar;
        TextView txtDisplayName;
        TextView txtPhone;
        TextView txtUnreadCount;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgFriendAvatar);
            txtDisplayName = itemView.findViewById(R.id.txtFriendDisplayName);
            txtPhone = itemView.findViewById(R.id.txtFriendPhone);
            txtUnreadCount = itemView.findViewById(R.id.txtUnreadCount);
        }
    }
}