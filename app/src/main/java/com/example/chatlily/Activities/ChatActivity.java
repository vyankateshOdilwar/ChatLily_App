package com.example.chatlily.Activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.example.chatlily.Adapters.MessagesAdapter;
import com.example.chatlily.Models.Message;
import com.example.chatlily.R;
import com.example.chatlily.databinding.ActivityChatBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {


    ActivityChatBinding binding;

    FirebaseDatabase database;

    FirebaseAuth auth;

    FirebaseStorage storage;

    ProgressDialog dialog;

    String senderUid;

    String receiverUid;

    String senderRoom;

    String receiverRoom;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getSupportActionBar().hide();

        dialog = new ProgressDialog(this);
        dialog.setMessage("Uploading files...");
        dialog.setCancelable(false);


        database = FirebaseDatabase.getInstance();
        auth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();


        String name = getIntent().getStringExtra("name");
        String profile = getIntent().getStringExtra("image");
        receiverUid = getIntent().getStringExtra("uid");
        String token = getIntent().getStringExtra("token");
        senderUid = auth.getUid();


        database.getReference().child("presence").child(receiverUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    String status = snapshot.getValue(String.class);
                    if(!status.isEmpty()){
                        if(status.equals("Offline")){
                            binding.status.setVisibility(View.GONE);
                        }else{
                            binding.status.setText(status);
                            binding.status.setVisibility(View.VISIBLE);
                        }

                    }
                    binding.status.setText(status);
                }


            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


        binding.name.setText(name);
        Glide.with(ChatActivity.this).load(profile)
                .placeholder(R.drawable.avatar)
                .into(binding.profile);


        binding.leftBackArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ChatActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

        senderRoom = senderUid + receiverUid;
        receiverRoom = receiverUid + senderUid;

        final ArrayList<Message> messages = new ArrayList<>();
        final MessagesAdapter adapter = new MessagesAdapter(messages, this, senderRoom, receiverRoom);
        binding.recyclerView.setAdapter(adapter);


        binding.sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String messageTxt = binding.messageBox.getText().toString();

                Date date = new Date();
                Message message = new Message(messageTxt, senderUid, date.getTime());
                binding.messageBox.setText("");


                String randomKey = database.getReference().push().getKey();

                HashMap<String, Object> lastMsgObj = new HashMap<>();
                lastMsgObj.put("lastMsg", message.getMessage());
                lastMsgObj.put("lastMsgTime", date.getTime());

                //save last message of sender into database
                database.getReference().child("chats").child(senderRoom).updateChildren(lastMsgObj);
                database.getReference().child("chats").child(receiverRoom).updateChildren(lastMsgObj);

                //for sender message
                database.getReference().child("chats")
                        .child(senderRoom)
                        .child("messages")
                        .child(randomKey)
                        .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                database.getReference().child("chats")
                                        .child(receiverRoom)
                                        .child("messages")
                                        .child(randomKey)
                                        .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                sendNotification(name, message.getMessage(), token);
                                            }
                                        });


                            }
                        });


            }
        });
        //show message on UI
        database.getReference().child("chats")
                .child(senderRoom)
                .child("messages")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        messages.clear();

                        for (DataSnapshot snapshot5 : snapshot.getChildren()) {
                            Message message = snapshot5.getValue(Message.class);
                            message.setMessageId(snapshot5.getKey());
                            messages.add(message);
                        }
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

        //open gallary when clicked on attachment
        binding.attachment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent, 21);
            }
        });

        final Handler handler = new Handler();
        binding.messageBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                database.getReference().child("presence").child(senderUid).setValue("typing...");
                handler.removeCallbacksAndMessages(null);
                handler.postDelayed(userStoppedTyping,1000);
            }

            Runnable userStoppedTyping = new Runnable() {
                @Override
                public void run() {
                    database.getReference().child("presence").child(senderUid).setValue("Online");

                }
            };

        });



    } // end oncreate

    //for notification
    void  sendNotification(String name, String message, String token){
        try {
            RequestQueue queue = Volley.newRequestQueue(this);

            String url = "https://fcm.googleapis.com/fcm/send";

            JSONObject data = new JSONObject();
            data.put("title", name);
            data.put("body", message);
            JSONObject notificationData = new JSONObject();
            notificationData.put("notification", data);
            notificationData.put("to", token);

            JsonObjectRequest request = new JsonObjectRequest(url, notificationData, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    //Toast.makeText(ChatActivity.this, "Success", Toast.LENGTH_SHORT).show();
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Toast.makeText(ChatActivity.this, error.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                }
            }){

                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    HashMap<String, String> map = new HashMap<>();
                    String key = "key=AAAAez_wFyA:APA91bEJQDHRUGjSgcEtbarravJtrqkjMIUzg6sE4P-cc6ZS_0imbvbTNErC49-MXUdZn83gI2vpoY6kvmHQ477BZ4auay2O4xs6_JRuP-3FilwnHufC-x4OLJZ4-qYMfi4QZh-A_sHi";
                    map.put("Authorization", key);
                    map.put("Content-Type", "application/json");

                    return map;
                }
            };

            queue.add(request);

        }catch (Exception ex){

        }

    }

    //after selecting attachment do this...
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == 21){
           if(data!=null){
               if(data.getData()!=null){

                   Uri selectedImage = data.getData();
                   Calendar calendar = Calendar.getInstance();

                   //upload attachment image
                   StorageReference reference = storage.getReference()
                           .child("chats")
                           .child(calendar.getTimeInMillis()+"");

                   dialog.show(); // showing dialogue
                   reference.putFile(selectedImage).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                       @Override
                       public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                           dialog.dismiss();
                           if(task.isSuccessful()){
                              reference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                  @Override
                                  public void onSuccess(Uri uri) {
                                      String filePath = uri.toString();

                                      //get attachment from firebase and show on UI recycler view
                                      String messageTxt = binding.messageBox.getText().toString();

                                      Date date = new Date();
                                      Message message = new Message(messageTxt, senderUid, date.getTime());
                                      message.setMessage("photo");
                                      message.setImageUrl(filePath);
                                      binding.messageBox.setText("");


                                      String randomKey = database.getReference().push().getKey();

                                      HashMap<String, Object> lastMsgObj = new HashMap<>();
                                      lastMsgObj.put("lastMsg", message.getMessage());
                                      lastMsgObj.put("lastMsgTime", date.getTime());

                                      //save last message of sender into database
                                      database.getReference().child("chats").child(senderRoom).updateChildren(lastMsgObj);
                                      database.getReference().child("chats").child(receiverRoom).updateChildren(lastMsgObj);

                                      //for sender message
                                      database.getReference().child("chats")
                                              .child(senderRoom)
                                              .child("messages")
                                              .child(randomKey)
                                              .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                  @Override
                                                  public void onSuccess(Void aVoid) {
                                                      database.getReference().child("chats")
                                                              .child(receiverRoom)
                                                              .child("messages")
                                                              .child(randomKey)
                                                              .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                  @Override
                                                                  public void onSuccess(Void aVoid) {

                                                                  }
                                                              });


                                                  }
                                              });


                                  }
                              });
                           }
                       }
                   });


               }
           }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        String currentId = FirebaseAuth.getInstance().getUid();
        database.getReference().child("presence").child(currentId).setValue("Online");
    }

    protected void onPause() {
        String currentId = FirebaseAuth.getInstance().getUid();
        database.getReference().child("presence").child(currentId).setValue("Offline");
        super.onPause();
    }




} // end class