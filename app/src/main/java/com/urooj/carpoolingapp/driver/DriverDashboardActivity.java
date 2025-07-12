package com.urooj.carpoolingapp.driver;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;
import com.urooj.carpoolingapp.R;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;

public class DriverDashboardActivity extends AppCompatActivity implements NavigationBarView.OnItemSelectedListener {

    private BottomNavigationView bottomNavigationView;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_dashboard);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize the bottom navigation view
        bottomNavigationView = findViewById(R.id.nav_view_driver);
        bottomNavigationView.setOnItemSelectedListener(this);

        // Set default fragment
        if (savedInstanceState == null) {
            // Set the default fragment
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container_driver, new DriverHomeFragment())
                    .commit();
            bottomNavigationView.setSelectedItemId(R.id.navigation_driver_home);
        }

        // Handle back button press (newer API)
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
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

        if (itemId == R.id.navigation_driver_home) {
            setTitle(R.string.nav_home);
            selectedFragment = new DriverHomeFragment();
        } else if (itemId == R.id.navigation_driver_location) {
            setTitle(R.string.nav_location);
            selectedFragment = new DriverLocationFragment();
        } else if (itemId == R.id.navigation_driver_notifications) {
            setTitle(R.string.nav_notifications);
            refreshAuthToken();
            selectedFragment = new DriverNotificationsFragment();
        } else if (itemId == R.id.navigation_driver_profile) {
            setTitle(R.string.nav_profile);
            selectedFragment = new DriverProfileFragment();
        }

        if (selectedFragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container_driver, selectedFragment)
                    .commit();
            return true;
        }

        return false;
    }

    /**
     * Refreshes the auth token to ensure database access permissions are current
     */
    private void refreshAuthToken() {
        if (mAuth.getCurrentUser() != null) {
            mAuth.getCurrentUser().getIdToken(true)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // Token refreshed successfully
                        } else {
                            Toast.makeText(DriverDashboardActivity.this,
                                    "Failed to refresh authentication. You may need to sign in again.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}