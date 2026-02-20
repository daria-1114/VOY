package com.example.voy.activities;


import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.voy.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;

public class TravelActivity extends AppCompatActivity {
        @Override
        protected void onCreate (Bundle savedInstanceState){
                super.onCreate(savedInstanceState);
                setContentView(R.layout.activity_travel);
                MaterialToolbar toolbar = findViewById(R.id.headerToolbarTravel);
                toolbar.setNavigationOnClickListener(v -> finish());
                toolbar.setOnMenuItemClickListener(item -> {
                        if(item.getItemId() == R.id.accountIcon){
                                showPopupMenu(toolbar);
                                return true;
                        }
                        return false;
                });
        }


        private void showPopupMenu(MaterialToolbar toolbar) {
                PopupMenu popupMenu = new PopupMenu(this, toolbar);
                popupMenu.getMenuInflater().inflate(R.menu.popup_menu, popupMenu.getMenu());
                popupMenu.setOnMenuItemClickListener(menuItem ->{
                        if (menuItem.getItemId() == R.id.LogOut_btn) {
                                LogOut();
                                return true;
                        }
                        return false;
                });
                popupMenu.show();
        }

        private void LogOut() {
                FirebaseAuth.getInstance().signOut();

                Intent intent = new Intent(TravelActivity.this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
        }

        @Override
        public boolean onOptionsItemSelected(@NonNull MenuItem menuItem){
                return super.onOptionsItemSelected(menuItem);
        }





}
