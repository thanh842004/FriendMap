package com.example.friendmap;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (friendsList == null || position >= friendsList.size()) return;

        User friend = friendsList.get(position);

        if (friend != null) {
            // Xác định tên hiển thị chính xác theo trường dữ liệu thực tế hoTen hoặc displayName
            String finalName = "Người dùng FriendMap";
            if (friend.getHoTen() != null && !friend.getHoTen().trim().isEmpty()) {
                finalName = friend.getHoTen();
            } else if (friend.getDisplayName() != null && !friend.getDisplayName().trim().isEmpty()) {
                finalName = friend.getDisplayName();
            } else if (friend.getUsername() != null) {
                finalName = friend.getUsername();
            }

            if (holder.txtDisplayName != null) {
                holder.txtDisplayName.setText(finalName);
            }

            if (holder.txtPhone != null) {
                holder.txtPhone.setText(friend.getPhone() != null ? friend.getPhone() : "Chưa cập nhật SĐT");
            }

            // ĐỒNG BỘ TIN NHẮN CHÍ MẠNG: Bắt sự kiện click vào item bạn bè để mở màn hình Chat thật
            final String exactName = finalName;
            holder.itemView.setOnClickListener(v -> {
                Context context = v.getContext();
                Intent intent = new Intent(context, ChatActivity.class);
                intent.putExtra("PARTNER_ID", friend.getUid()); // Gửi ID thật của đối phương sang
                intent.putExtra("PARTNER_NAME", exactName);     // Gửi tên thật sang
                context.startActivity(intent);
            });
        }
    }

    @Override
    public int getItemCount() {
        return friendsList != null ? friendsList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtDisplayName, txtPhone;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtDisplayName = itemView.findViewById(R.id.txtFriendDisplayName);
            txtPhone = itemView.findViewById(R.id.txtFriendPhone);
        }
    }
}