package com.example.voy.activities;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager2.widget.ViewPager2;

import com.example.voy.R;
import com.example.voy.adapters.ViewPagerAdapter;
import com.google.android.material.tabs.TabLayout;

public class LoginActivity extends AppCompatActivity {
private TabLayout tabLayout;
private ViewPager2 viewPager2;
private ViewPagerAdapter adapter;


@Override
protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_login);
    tabLayout = findViewById(R.id.tab_layout);
    viewPager2 = findViewById(R.id.view_pager);
    tabLayout.addTab(tabLayout.newTab().setText("Log In"));
    tabLayout.addTab(tabLayout.newTab().setText("Sign Up"));

    FragmentManager fragmentManager = getSupportFragmentManager();
    adapter = new ViewPagerAdapter(fragmentManager, getLifecycle());
    viewPager2.setAdapter(adapter);

    tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            viewPager2.setCurrentItem(tab.getPosition());
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {

        }

        @Override
        public void onTabReselected(TabLayout.Tab tab) {

        }
    });

    viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
        @Override
        public void onPageSelected(int position) {
            tabLayout.selectTab(tabLayout.getTabAt(position));
        }
    });
}


}
