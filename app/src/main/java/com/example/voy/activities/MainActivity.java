package com.example.voy.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voy.R;
import com.example.voy.dao.TravelEntryDao;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;


public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private TravelEntryDao travelEntryDao;
    private TextView emptyMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FloatingActionButton fab = findViewById(R.id.fabAddEntry);
        MaterialToolbar toolbar = findViewById(R.id.headerToolbar);
        toolbar.setNavigationOnClickListener(v->{
            Intent intent = new Intent(this, MapsActivity.class);
            startActivity(intent);
        });
        fab.setOnClickListener(v ->{
            Intent intent = new Intent(MainActivity.this, TravelActivity.class);
            startActivity(intent);
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return super.onOptionsItemSelected(item);

    }
}