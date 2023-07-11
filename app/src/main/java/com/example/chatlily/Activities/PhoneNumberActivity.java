package com.example.chatlily.Activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.chatlily.databinding.ActivityPhoneNumberBinding;
import com.google.firebase.auth.FirebaseAuth;

public class PhoneNumberActivity extends AppCompatActivity {

    FirebaseAuth auth;

    ActivityPhoneNumberBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPhoneNumberBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getSupportActionBar().hide();

        auth = FirebaseAuth.getInstance();

        if(auth.getCurrentUser()!=null){
            Intent intent = new Intent(PhoneNumberActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }

        binding.btnContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(binding.phoneBox.getText().toString().isEmpty()){
                    binding.phoneBox.setError("Please enter your phone number first");
                    return;
                }
                    Intent intent = new Intent(PhoneNumberActivity.this, OTPActivity.class);
                    intent.putExtra("phoneNumber", binding.phoneBox.getText().toString());
                    startActivity(intent);

            }
        });
    }
}