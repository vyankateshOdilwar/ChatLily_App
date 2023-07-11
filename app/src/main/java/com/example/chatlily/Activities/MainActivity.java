package com.example.chatlily.Activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.chatlily.Adapters.TopStatusAdapter;
import com.example.chatlily.Models.Status;
import com.example.chatlily.Models.UserStatus;
import com.example.chatlily.R;
import com.example.chatlily.Models.User;
import com.example.chatlily.Adapters.UsersAdapter;
import com.example.chatlily.databinding.ActivityMainBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;
    FirebaseDatabase database;
    ArrayList<User> users;

    UsersAdapter usersAdapter;
    TopStatusAdapter statusAdapter;
    ArrayList<UserStatus> userStatuses;

    ProgressDialog dialog;
    User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //Remote configuration through firebase
        FirebaseRemoteConfig mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(0) //change it later on.
                .build();
        mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings);

        //change background through firebase.
        String backgroundImage = mFirebaseRemoteConfig.getString("backgroundImage");
        Glide.with(MainActivity.this).load(backgroundImage)
                .into(binding.bgImageView);


        //sample functionality to change toolbar from firebase remote config
        mFirebaseRemoteConfig.fetchAndActivate().addOnSuccessListener(new OnSuccessListener<Boolean>() {
            @Override
            public void onSuccess(Boolean aBoolean) {
                String toolBarImage = mFirebaseRemoteConfig.getString("toolbarImage");
                Glide.with(MainActivity.this)
                                .load(toolBarImage)
                                        .into(new CustomTarget<Drawable>() {
                                            @Override
                                            public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                                                getSupportActionBar().setBackgroundDrawable(resource);
                                            }

                                            @Override
                                            public void onLoadCleared(@Nullable Drawable placeholder) {

                                            }
                                        });

            }
  });


        database = FirebaseDatabase.getInstance();

        FirebaseMessaging.getInstance()
                .getToken()
                .addOnSuccessListener(new OnSuccessListener<String>() {
                    @Override
                    public void onSuccess(String token) {
                        HashMap<String, Object> map = new HashMap<>();
                        map.put("token", token);
                        database.getReference().
                                child("users")
                                .child(FirebaseAuth.getInstance().getUid())
                                .updateChildren(map);
                        //Toast.makeText(MainActivity.this, token, Toast.LENGTH_SHORT).show();
                    }
                });

        dialog = new ProgressDialog(this);
        dialog.setMessage("Uploading status");
        dialog.setCancelable(false);


        users = new ArrayList<>();
        userStatuses = new ArrayList<>();

        database.getReference().child("users").child(FirebaseAuth.getInstance().getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        user = snapshot.getValue(User.class);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });


        usersAdapter = new UsersAdapter(this, users);
        statusAdapter = new TopStatusAdapter(this, userStatuses);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(RecyclerView.HORIZONTAL);
        binding.statusList.setLayoutManager(layoutManager);

        binding.recyclerView.setAdapter(usersAdapter);
        binding.shimmer.startShimmer();
        binding.statusList.setAdapter(statusAdapter);
        binding.shimmer2.startShimmer();


//        get data from firebase
        database.getReference().child("users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    users.clear();
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        User user = dataSnapshot.getValue(User.class);
                        if (!user.getUid().equals(FirebaseAuth.getInstance().getUid())) {
                            users.add(user);
                        }
                        binding.shimmer.stopShimmer();
                        binding.shimmer.setVisibility(View.GONE);
                        usersAdapter.notifyDataSetChanged();

                    }
                } else {
                    binding.shimmer.stopShimmer();
                    binding.shimmer.setVisibility(View.GONE);
                }


            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        //show statuses on UI
        database.getReference().child("stories").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    userStatuses.clear();
                    for (DataSnapshot storySnapshot : snapshot.getChildren()) {
                        UserStatus status = new UserStatus();
                        status.setName(storySnapshot.child("name").getValue(String.class));
                        status.setProfileImage(storySnapshot.child("profileImage").getValue(String.class));
                        status.setLastUpdated(storySnapshot.child("lastUpdated").getValue(Long.class));

                        ArrayList<Status> statuses = new ArrayList<>();

                        for (DataSnapshot statusSnapshot : storySnapshot.child("statuses").getChildren()) {
                            Status refStatus = statusSnapshot.getValue(Status.class);
                            statuses.add(refStatus);
                        }
                        status.setStatuses(statuses);
                        ;


                        userStatuses.add(status);
                    }
                    binding.shimmer2.stopShimmer();
                    binding.shimmer2.setVisibility(View.GONE);

                    statusAdapter.notifyDataSetChanged();

                } else {
                    binding.shimmer2.stopShimmer();
                    binding.shimmer2.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


        //get image when clicked on status icon on botton navigation options
        binding.bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.status:
                        Intent intent = new Intent();
                        intent.setType("image/*");
                        intent.setAction(Intent.ACTION_GET_CONTENT);
                        startActivityForResult(intent, 19);
                        break;
                }
                return false;
            }
        });
    }

    //upload selected status to firebase
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null) {
            if (data.getData() != null) {
                dialog.show();
                FirebaseStorage storage = FirebaseStorage.getInstance();

                Date date = new Date();

                StorageReference reference = storage.getReference()
                        .child("status")
                        .child(date.getTime() + "");

                reference.putFile(data.getData()).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        if (task.isSuccessful()) {
                            reference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    UserStatus userStatus = new UserStatus();
                                    userStatus.setName(user.getName());
                                    userStatus.setProfileImage(user.getProfileImage());
                                    userStatus.setLastUpdated(date.getTime());

                                    HashMap<String, Object> obj = new HashMap<>();

                                    obj.put("name", userStatus.getName());
                                    obj.put("profileImage", userStatus.getProfileImage());
                                    obj.put("lastUpdated", userStatus.getLastUpdated());

                                    String imageUrl = uri.toString();
                                    Status status = new Status(imageUrl, userStatus.getLastUpdated());


                                    database.getReference()
                                            .child("stories")
                                            .child(FirebaseAuth.getInstance().getUid())
                                            .updateChildren(obj);

                                    database.getReference().child("stories")
                                            .child(FirebaseAuth.getInstance().getUid())
                                            .child("statuses")
                                            .push()
                                            .setValue(status);

                                    dialog.dismiss();
                                }
                            });
                        }
                    }
                });
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        String currentId = FirebaseAuth.getInstance().getUid();
        database.getReference().child("presence").child(currentId).setValue("Online");
    }

    @Override
    protected void onPause() {
        String currentId = FirebaseAuth.getInstance().getUid();
        database.getReference().child("presence").child(currentId).setValue("Offline");
        super.onPause();
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch ((item.getItemId())) {
            case R.id.search:
                Toast.makeText(this, "Clicked on Search", Toast.LENGTH_SHORT).show();
                break;

            case R.id.settings:
                Intent it = new Intent(MainActivity.this, SettingActivity.class);
                startActivity(it);
                break;

            case R.id.invite:
                Toast.makeText(this, "Clicked on Invite", Toast.LENGTH_SHORT).show();
                break;

            case R.id.group:
                Toast.makeText(this, "Clicked on Group", Toast.LENGTH_SHORT).show();
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.topmenu, menu);
        return super.onCreateOptionsMenu(menu);
    }
}