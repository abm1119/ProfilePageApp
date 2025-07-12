package com.urooj.carpoolingapp.passenger;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.urooj.carpoolingapp.R;

public class PassengerDashboardActivity extends AppCompatActivity implements NavigationBarView.OnItemSelectedListener {

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passenger_dashboard);

        // Initialize the bottom navigation view
        bottomNavigationView = findViewById(R.id.nav_view_passenger);
        bottomNavigationView.setOnItemSelectedListener(this);

        // Set default fragment
        if (savedInstanceState == null) {
            // Check if we should open the search tab directly
            if (getIntent().getBooleanExtra("open_search_tab", false)) {
                loadFragment(new PassengerSearchFragment());
                bottomNavigationView.setSelectedItemId(R.id.navigation_passenger_search);
                setTitle(R.string.nav_search);
            } else {
                // Set the default fragment (home)
                loadFragment(new PassengerHomeFragment());
                bottomNavigationView.setSelectedItemId(R.id.navigation_passenger_home);
            }
        }

        // Handle back button press (newer API)
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Just finish the activity which will exit the app if this is the last activity
                finish();
            }
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment selectedFragment = null;
        int itemId = item.getItemId();

        if (itemId == R.id.navigation_passenger_home) {
            setTitle(R.string.nav_home);
            selectedFragment = new PassengerHomeFragment();
        } else if (itemId == R.id.navigation_passenger_search) {
            setTitle(R.string.nav_search);
            selectedFragment = new PassengerSearchFragment();
        } else if (itemId == R.id.navigation_passenger_location) {
            setTitle(R.string.nav_location);
            selectedFragment = new PassengerLocationFragment();
        } else if (itemId == R.id.navigation_passenger_profile) {
            setTitle(R.string.nav_profile);
            selectedFragment = new PassengerProfileFragment();
        }

        if (selectedFragment != null) {
            loadFragment(selectedFragment);
            return true;
        }

        return false;
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container_passenger, fragment)
                .commit();
    }
}
