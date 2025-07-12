package com.urooj.carpoolingapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.urooj.carpoolingapp.auth.LoginActivity;
import com.urooj.carpoolingapp.model.User;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    private static final int SPLASH_DURATION = 1500; // 1.5 seconds

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Start delay for splash screen
        new Handler(Looper.getMainLooper()).postDelayed(this::checkUserStatus, SPLASH_DURATION);
    }

    /**
     * Check if user is logged in and verify their data exists in the database
     */
    private void checkUserStatus() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            // User is signed in, verify their data exists in the database
            String userId = currentUser.getUid();
            mDatabase = FirebaseDatabase.getInstance().getReference("users").child(userId);

            mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        // User data exists, proceed to profile
                        Log.d(TAG, "User data found, proceeding to dashboard");
                        navigateToDashboard();
                    } else {
                        // User exists in Auth but not in database, log them out and go to login
                        Log.w(TAG, "User authenticated but no data found in database");
                        mAuth.signOut();
                        navigateToLogin();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    // Error occurred, go to login to be safe
                    Log.e(TAG, "Database error: " + databaseError.getMessage());

                    // If permission denied error, handle it specially
                    if (databaseError.getCode() == DatabaseError.PERMISSION_DENIED) {
                        Log.e(TAG, "Permission denied: likely database rules issue");
                    }

                    // Regardless of error type, log the user out and go to login
                    mAuth.signOut();
                    navigateToLogin();
                }
            });
        } else {
            // No user is signed in, go to login
            navigateToLogin();
        }
    }

    private void navigateToDashboard() {
        // Check user type from database and navigate to the appropriate dashboard
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            // No user is signed in, redirect to login
            Log.e(TAG, "No user found, redirecting to login");
            navigateToLogin();
            return;
        }

        String userId = currentUser.getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    try {
                        String userType = dataSnapshot.child("userType").getValue(String.class);
                        Intent intent;

                        if ("driver".equals(userType)) {
                            // Navigate to driver dashboard
                            intent = new Intent(SplashActivity.this, com.urooj.carpoolingapp.driver.DriverDashboardActivity.class);
                        } else {
                            // Default to passenger dashboard
                            intent = new Intent(SplashActivity.this, com.urooj.carpoolingapp.passenger.PassengerDashboardActivity.class);
                        }

                        startActivity(intent);
                        finish();
                    } catch (Exception e) {
                        Log.e(TAG, "Error determining user type: " + e.getMessage());
                        // Fallback to profile on error
                        navigateToProfileFallback();
                    }
                } else {
                    // User data doesn't exist in database
                    Log.e(TAG, "User doesn't exist in database");
                    navigateToProfileFallback();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Database error: " + databaseError.getMessage());
                navigateToProfileFallback();
            }
        });
    }

    // Fallback method to navigate to profile if dashboard navigation fails
    private void navigateToProfileFallback() {
        startActivity(new Intent(SplashActivity.this, ProfileActivity.class));
        finish();
    }

    private void navigateToLogin() {
        startActivity(new Intent(SplashActivity.this, LoginActivity.class));
        finish();
    }
}