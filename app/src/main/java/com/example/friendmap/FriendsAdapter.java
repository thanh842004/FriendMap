package com.example.friendmap;

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
        // ĐÃ CẬP NHẬT: Kiểm tra phần tử trong danh sách an toàn tránh NullPointerException
        if (friendsList == null || position >= friendsList.size()) return;

        User friend = friendsList.get(position);

        if (friend != null) {
            // Kiểm tra an toàn cho TextView Tên hiển thị
            if (holder.txtDisplayName != null) {
                if (friend.getDisplayName() != null && !friend.getDisplayName().trim().isEmpty()) {
                    holder.txtDisplayName.setText(friend.getDisplayName());
                } else if (friend.getUsername() != null) {
                    holder.txtDisplayName.setText(friend.getUsername());
                } else {
                    holder.txtDisplayName.setText("Người dùng FriendMap");
                }
            }

            // Kiểm tra an toàn cho TextView Số điện thoại
            if (holder.txtPhone != null) {
                holder.txtPhone.setText(friend.getPhone() != null ? friend.getPhone() : "Chưa cập nhật SĐT");
            }
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
            // Ánh xạ View an toàn
            txtDisplayName = itemView.findViewById(R.id.txtFriendDisplayName);
            txtPhone = itemView.findViewById(R.id.txtFriendPhone);
        }
    }
}