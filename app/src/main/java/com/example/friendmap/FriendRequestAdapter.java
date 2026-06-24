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
        FriendRequest request = requestList.get(position);
        holder.txtSenderName.setText(request.getSenderName());

        holder.btnAccept.setOnClickListener(v -> {
            holder.btnAccept.setEnabled(false);
            // Cập nhật trạng thái 'status' thành 'accepted' trên Firestore
            FirebaseFirestore.getInstance().collection("friendRequests")
                    .document(request.getRequestId())
                    .update("status", "accepted")
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(v.getContext(), "Đã đồng ý kết bạn!", Toast.LENGTH_SHORT).show();
                        // Xóa phần tử khỏi danh sách hiển thị hiện tại
                        requestList.remove(position);
                        notifyItemRemoved(position);
                        notifyItemRangeChanged(position, requestList.size());
                    })
                    .addOnFailureListener(e -> holder.btnAccept.setEnabled(true));
        });
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtSenderName;
        Button btnAccept;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtSenderName = itemView.findViewById(R.id.txtRequestSenderName);
            btnAccept = itemView.findViewById(R.id.btnAcceptRequest);
        }
    }
}