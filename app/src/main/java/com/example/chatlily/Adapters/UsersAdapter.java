package com.example.chatlily.Adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.chatlily.Activities.ChatActivity;
import com.example.chatlily.R;
import com.example.chatlily.Models.User;
import com.example.chatlily.databinding.RowConversationBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class UsersAdapter extends  RecyclerView.Adapter<UsersAdapter.UsersViewHolder>{

    Context context;
    ArrayList<User> users;

    public  UsersAdapter(Context context, ArrayList<User> users){
        this.context = context;
        this.users = users;
    }
    @NonNull
    @Override
    public UsersViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_conversation, parent, false);
        return new UsersViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UsersViewHolder holder, int position) {
        User user = users.get(position);

        String senderId = FirebaseAuth.getInstance().getUid();
        String senderRoom = senderId + user.getUid();

        FirebaseDatabase.getInstance().getReference().child("chats")
                        .child(senderRoom)
                                .addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        if(snapshot.exists()){
                                            String lastMsg = snapshot.child("lastMsg").getValue(String.class);
                                            long time = snapshot.child("lastMsgTime").getValue(Long.class);

                                            holder.binding.lastMsg.setText(lastMsg);
                                        }else{
                                            holder.binding.lastMsg.setText("Tap to chat");
                                        }

                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {

                                    }
                                });

        holder.binding.username.setText(user.getName());

        Glide.with(context).load(user.getProfileImage())
                .placeholder(R.drawable.avatar)
                .into(holder.binding.profile);


        //To open particular users chat
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, ChatActivity.class);
                intent.putExtra("name", user.getName());
                intent.putExtra("image", user.getProfileImage());
                intent.putExtra("uid", user.getUid());
                intent.putExtra("token", user.getToken());
                context.startActivity(intent);

            }
        });
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public class UsersViewHolder extends RecyclerView.ViewHolder{

        RowConversationBinding binding;

        public UsersViewHolder(@NonNull View itemView) {
            super(itemView);
            binding = RowConversationBinding.bind(itemView);
        }
    }
}
