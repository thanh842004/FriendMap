package com.example.friendmap;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class FriendRequestAdapter extends RecyclerView.Adapter<FriendRequestAdapter.ViewHolder> {

    private List<FriendRequest> requestList;

    public FriendRequestAdapter(List<FriendRequest> requestList) {
        this.requestList = requestList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // ĐÃ CẬP NHẬT: Kiểm tra vị trí an toàn chống crash danh sách
        if (requestList == null || position >= requestList.size()) return;

        FriendRequest request = requestList.get(position);
        if (request == null) return;

        // ĐÃ CẬP NHẬT: Chống NullPointerException khi gán Tên người gửi lời mời
        if (holder.txtSenderName != null) {
            if (request.getSenderName() != null && !request.getSenderName().trim().isEmpty()) {
                holder.txtSenderName.setText(request.getSenderName());
            } else {
                holder.txtSenderName.setText("Một người dùng FriendMap");
            }
        }

        // ĐÃ CẬP NHẬT: Xử lý sự kiện bấm nút Đồng ý kết bạn an toàn
        if (holder.btnAccept != null) {
            holder.btnAccept.setOnClickListener(v -> {
                // Lấy vị trí thực tế hiện tại của item trong danh sách (Tránh lỗi lệch index khi xóa phần tử trước đó)
                int currentPos = holder.getAdapterPosition();
                if (currentPos == RecyclerView.NO_POSITION || requestList == null || currentPos >= requestList.size()) return;

                FriendRequest currentRequest = requestList.get(currentPos);
                if (currentRequest == null || currentRequest.getRequestId() == null) {
                    Toast.makeText(v.getContext(), "Lỗi: Không tìm thấy ID lời mời!", Toast.LENGTH_SHORT).show();
                    return;
                }

                holder.btnAccept.setEnabled(false);

                // Cập nhật trạng thái 'status' thành 'accepted' trên Firestore
                FirebaseFirestore.getInstance().collection("friendRequests")
                        .document(currentRequest.getRequestId())
                        .update("status", "accepted")
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(v.getContext(), "Đã đồng ý kết bạn!", Toast.LENGTH_SHORT).show();

                            // ĐÃ CẬP NHẬT: Xóa phần tử bằng vị trí chạy thực tế (currentPos) để chống văng app
                            if (currentPos < requestList.size()) {
                                requestList.remove(currentPos);
                                notifyItemRemoved(currentPos);
                                notifyItemRangeChanged(currentPos, requestList.size());
                            }
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(v.getContext(), "Lỗi hệ thống kết nối, thử lại sau!", Toast.LENGTH_SHORT).show();
                            holder.btnAccept.setEnabled(true);
                        });
            });
        }
    }

    @Override
    public int getItemCount() {
        return requestList != null ? requestList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtSenderName;
        Button btnAccept;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Ánh xạ View an toàn từ file item_friend_request.xml
            txtSenderName = itemView.findViewById(R.id.txtRequestSenderName);
            btnAccept = itemView.findViewById(R.id.btnAcceptRequest);
        }
    }
}