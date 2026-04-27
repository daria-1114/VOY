package com.example.voy.activities;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.voy.R;
import com.google.android.material.appbar.MaterialToolbar;

public class InfoActivity extends AppCompatActivity {
    MaterialToolbar toolbar;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);
        toolbar = findViewById(R.id.infoToolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v->{
            finish();
        });
    }
}
