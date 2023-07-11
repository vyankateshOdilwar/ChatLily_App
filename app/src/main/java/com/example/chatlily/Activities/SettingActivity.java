package com.example.chatlily.Activities;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.example.chatlily.R;
import com.example.chatlily.databinding.ActivitySettingBinding;

public class SettingActivity extends AppCompatActivity {

    ActivitySettingBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        binding = ActivitySettingBinding.inflate(getLayoutInflater());
        super.onCreate(savedInstanceState);
        setContentView(binding.getRoot());


    }
}