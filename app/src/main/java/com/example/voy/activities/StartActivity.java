package com.example.voy.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class StartActivity extends AppCompatActivity {
    @Override
    public void onCreate(Bundle savedInstanceState){

        super.onCreate(savedInstanceState);
        SharedPreferences pref = getSharedPreferences("auth",MODE_PRIVATE);
        boolean loggedIn = pref.getBoolean("logged_in",false);
        if(loggedIn){
            startActivity(new Intent(this, MainActivity.class));
        }else{
            startActivity(new Intent(this,LoginActivity.class));
        }
        finish();
    }
}
