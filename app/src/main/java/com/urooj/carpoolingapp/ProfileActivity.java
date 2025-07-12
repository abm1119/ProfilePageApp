package com.urooj.carpoolingapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.urooj.carpoolingapp.auth.LoginActivity;
import com.urooj.carpoolingapp.model.User;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";

    // Common profile views
    private TextView fullNameTextView, userTypeTextView, emailTextView, phoneTextView, addressTextView;

    // Additional user details
    private TextView cnicTextView, genderTextView, casteTextView;

    // Driver specific views
    private TextView religionTextView, nationalityTextView, carNumberTextView;
    private ImageView profileImageView, driverImagePreview, licenseImagePreview;
    private View driverDetailsCard;

    private Button editProfileButton, logoutButton;
    private ProgressBar progressBar;
    
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private String userId;
    private User currentUser;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Check if coming from a dashboard
        boolean fromDashboard = getIntent().getBooleanExtra("fromDashboard", false);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        
        if (firebaseUser == null) {
            // User not logged in, go to login
            startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
            finish();
            return;
        }
        
        userId = firebaseUser.getUid();
        // Important: Ensure we're accessing the correct database path with proper permissions
        mDatabase = FirebaseDatabase.getInstance().getReference("users").child(userId);
        
        // Initialize views
        initializeViews();
        
        // Load user profile data
        loadUserData();
        
        // Edit profile button click listener
        editProfileButton.setOnClickListener(v -> {
            if (currentUser != null) {
                Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
                intent.putExtra("userId", userId);
                intent.putExtra("fullName", currentUser.getFullName());
                intent.putExtra("phoneNumber", currentUser.getPhoneNumber());
                intent.putExtra("address", currentUser.getAddress());
                intent.putExtra("profileImageUrl", currentUser.getProfileImageUrl());
                intent.putExtra("email", currentUser.getEmail());
                intent.putExtra("userType", currentUser.getUserType());

                // Pass driver-specific fields if this is a driver
                if ("driver".equals(currentUser.getUserType())) {
                    intent.putExtra("driverPhotoUrl", currentUser.getDriverPhotoUrl());
                    intent.putExtra("licensePhotoUrl", currentUser.getLicensePhotoUrl());
                    intent.putExtra("carNumber", currentUser.getCarNumber());
                }

                startActivity(intent);
            } else {
                Toast.makeText(ProfileActivity.this, getString(R.string.info_waiting_data), Toast.LENGTH_SHORT).show();
            }
        });
        
        // Logout button click listener
        logoutButton.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(ProfileActivity.this, LoginActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
            finish();
        });

        // If opened from dashboard, show back button
        if (fromDashboard) {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        }
    }

    private void initializeViews() {
        // Common profile views
        fullNameTextView = findViewById(R.id.full_name_text_view);
        userTypeTextView = findViewById(R.id.user_type_text_view);
        emailTextView = findViewById(R.id.email_text_view);
        phoneTextView = findViewById(R.id.phone_text_view);
        addressTextView = findViewById(R.id.address_text_view);
        profileImageView = findViewById(R.id.profile_image_view);

        // Additional user details
        cnicTextView = findViewById(R.id.cnic_text_view);
        genderTextView = findViewById(R.id.gender_text_view);
        casteTextView = findViewById(R.id.caste_text_view);

        // Driver specific views
        driverDetailsCard = findViewById(R.id.driver_details_card);
        religionTextView = findViewById(R.id.religion_text_view);
        nationalityTextView = findViewById(R.id.nationality_text_view);
        carNumberTextView = findViewById(R.id.car_number_text_view);
        driverImagePreview = findViewById(R.id.driver_image_preview);
        licenseImagePreview = findViewById(R.id.license_image_preview);

        editProfileButton = findViewById(R.id.edit_profile_button);
        logoutButton = findViewById(R.id.logout_button);
        progressBar = findViewById(R.id.progress_bar);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload user data when returning from EditProfileActivity
        if (mDatabase != null) {
            loadUserData();
        }
    }
    
    private void loadUserData() {
        progressBar.setVisibility(View.VISIBLE);

        // Check network connectivity first
        if (!isNetworkConnected()) {
            progressBar.setVisibility(View.GONE);
            showNetworkErrorDialog();
            return;
        }

        // Use ValueEventListener to keep data in sync with Firebase
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                progressBar.setVisibility(View.GONE);
                
                if (dataSnapshot.exists()) {
                    try {
                        // Parse user data from snapshot
                        currentUser = dataSnapshot.getValue(User.class);

                        if (currentUser != null) {
                            // Display user data
                            displayUserData();
                        } else {
                            Log.e(TAG, "User data is null");
                            showErrorMessage(getString(R.string.error_loading_profile));
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing user data", e);
                        showErrorMessage(getString(R.string.error_loading_profile));
                    }
                } else {
                    Log.e(TAG, "User data doesn't exist");
                    showErrorMessage(getString(R.string.error_loading_profile));
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                progressBar.setVisibility(View.GONE);
                Log.e(TAG, "Database error: " + databaseError.getMessage(), databaseError.toException());

                // Handle specific permissions error
                if (databaseError.getCode() == DatabaseError.PERMISSION_DENIED) {
                    showPermissionDeniedError();
                } else {
                    showErrorMessage("Failed to load profile: " + databaseError.getMessage());
                }
            }
        });
    }

    private void displayUserData() {
        if (currentUser == null) return;

        // Display user data safely with debug logging to help troubleshooting
        Log.d(TAG, "Displaying user data: " + currentUser);

        // Set full name
        if (currentUser.getFullName() != null && !currentUser.getFullName().isEmpty()) {
            fullNameTextView.setText(currentUser.getFullName());
            Log.d(TAG, "Set full name: " + currentUser.getFullName());
        } else {
            fullNameTextView.setText(getString(R.string.data_placeholder));
            Log.w(TAG, "Full name is empty or null");
        }

        // Set user type with proper capitalization
        if (currentUser.getUserType() != null && !currentUser.getUserType().isEmpty()) {
            String userType = currentUser.getUserType();
            String capitalizedUserType = userType.substring(0, 1).toUpperCase() + userType.substring(1);
            userTypeTextView.setText(capitalizedUserType);
            Log.d(TAG, "Set user type: " + capitalizedUserType);

            // Show or hide driver-specific information
            if ("driver".equalsIgnoreCase(userType)) {
                driverDetailsCard.setVisibility(View.VISIBLE);
                displayDriverDetails();
            } else {
                driverDetailsCard.setVisibility(View.GONE);
            }
        } else {
            userTypeTextView.setText(getString(R.string.data_placeholder));
            Log.w(TAG, "User type is empty or null");
            driverDetailsCard.setVisibility(View.GONE);
        }

        // Set email
        if (currentUser.getEmail() != null && !currentUser.getEmail().isEmpty()) {
            emailTextView.setText(currentUser.getEmail());
            Log.d(TAG, "Set email: " + currentUser.getEmail());
        } else {
            emailTextView.setText(getString(R.string.data_placeholder));
            Log.w(TAG, "Email is empty or null");
        }

        // Set phone number
        if (currentUser.getPhoneNumber() != null && !currentUser.getPhoneNumber().isEmpty()) {
            phoneTextView.setText(currentUser.getPhoneNumber());
            Log.d(TAG, "Set phone number: " + currentUser.getPhoneNumber());
        } else {
            phoneTextView.setText(getString(R.string.data_placeholder));
            Log.w(TAG, "Phone number is empty or null");
        }

        // Set address
        if (currentUser.getAddress() != null && !currentUser.getAddress().isEmpty()) {
            addressTextView.setText(currentUser.getAddress());
            Log.d(TAG, "Set address: " + currentUser.getAddress());
        } else {
            addressTextView.setText(getString(R.string.no_address));
            Log.w(TAG, "Address is empty or null");
        }

        // Set CNIC
        if (currentUser.getCnic() != null && !currentUser.getCnic().isEmpty()) {
            cnicTextView.setText(currentUser.getCnic());
            Log.d(TAG, "Set CNIC: " + currentUser.getCnic());
        } else {
            cnicTextView.setText(getString(R.string.data_placeholder));
            Log.w(TAG, "CNIC is empty or null");
        }

        // Set gender with capitalization
        if (currentUser.getGender() != null && !currentUser.getGender().isEmpty()) {
            String gender = currentUser.getGender();
            String capitalizedGender = gender.substring(0, 1).toUpperCase() + gender.substring(1);
            genderTextView.setText(capitalizedGender);
            Log.d(TAG, "Set gender: " + capitalizedGender);
        } else {
            genderTextView.setText(getString(R.string.data_placeholder));
            Log.w(TAG, "Gender is empty or null");
        }

        // Set caste
        if (currentUser.getCaste() != null && !currentUser.getCaste().isEmpty()) {
            casteTextView.setText(currentUser.getCaste());
            Log.d(TAG, "Set caste: " + currentUser.getCaste());
        } else {
            casteTextView.setText(getString(R.string.data_placeholder));
            Log.w(TAG, "Caste is empty or null");
        }

        // Load profile image if exists
        if (currentUser.getProfileImageUrl() != null && !currentUser.getProfileImageUrl().isEmpty()) {
            Log.d(TAG, "Profile image URL exists: " + currentUser.getProfileImageUrl());

            // Use Glide to load image from URL
            Glide.with(this)
                    .load(currentUser.getProfileImageUrl())
                    .apply(new RequestOptions()
                            .placeholder(android.R.drawable.ic_menu_gallery)
                            .error(android.R.drawable.ic_menu_report_image))
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(profileImageView);
        } else {
            Log.w(TAG, "Profile image URL is empty or null");
            // Set default profile image
            profileImageView.setImageResource(android.R.drawable.ic_menu_gallery);
        }
    }

    /**
     * Display driver-specific details
     */
    private void displayDriverDetails() {
        if (currentUser == null) return;

        // Set religion
        if (currentUser.getReligion() != null && !currentUser.getReligion().isEmpty()) {
            religionTextView.setText(currentUser.getReligion());
            Log.d(TAG, "Set religion: " + currentUser.getReligion());
        } else {
            religionTextView.setText(getString(R.string.data_placeholder));
            Log.w(TAG, "Religion is empty or null");
        }

        // Set nationality
        if (currentUser.getNationality() != null && !currentUser.getNationality().isEmpty()) {
            nationalityTextView.setText(currentUser.getNationality());
            Log.d(TAG, "Set nationality: " + currentUser.getNationality());
        } else {
            nationalityTextView.setText(getString(R.string.data_placeholder));
            Log.w(TAG, "Nationality is empty or null");
        }

        // Set car number
        if (currentUser.getCarNumber() != null && !currentUser.getCarNumber().isEmpty()) {
            carNumberTextView.setText(currentUser.getCarNumber());
            Log.d(TAG, "Set car number: " + currentUser.getCarNumber());
        } else {
            carNumberTextView.setText(getString(R.string.data_placeholder));
            Log.w(TAG, "Car number is empty or null");
        }

        // Load driver photo if exists
        if (currentUser.getDriverPhotoUrl() != null && !currentUser.getDriverPhotoUrl().isEmpty()) {
            Log.d(TAG, "Driver photo URL exists: " + currentUser.getDriverPhotoUrl());

            // Use Glide to load driver photo
            Glide.with(this)
                    .load(currentUser.getDriverPhotoUrl())
                    .apply(new RequestOptions()
                            .placeholder(android.R.drawable.ic_menu_report_image)
                            .error(android.R.drawable.ic_menu_report_image))
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(driverImagePreview);
        }

        // Load license photo if exists
        if (currentUser.getLicensePhotoUrl() != null && !currentUser.getLicensePhotoUrl().isEmpty()) {
            Log.d(TAG, "License photo URL exists: " + currentUser.getLicensePhotoUrl());

            // Use Glide to load license photo
            Glide.with(this)
                    .load(currentUser.getLicensePhotoUrl())
                    .apply(new RequestOptions()
                            .placeholder(android.R.drawable.ic_menu_report_image)
                            .error(android.R.drawable.ic_menu_report_image))
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(licenseImagePreview);
        }
    }

    private void showPermissionDeniedError() {
        // Alert dialog for permission denied error with troubleshooting steps
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.error_permission_denied_title))
                .setMessage(getString(R.string.error_permission_denied_message))
                .setPositiveButton(getString(R.string.btn_sign_out), (dialog, which) -> {
                    mAuth.signOut();
                    startActivity(new Intent(ProfileActivity.this, LoginActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                    finish();
                })
                .setNegativeButton(getString(R.string.btn_retry), (dialog, which) -> loadUserData())
                .setCancelable(false)
                .show();

        // Also report the error to Analytics (would be implemented in a production app)
        // FirebaseAnalytics.getInstance(this).logEvent("permission_denied_error", null);
    }

    private void showErrorMessage(String message) {
        Toast.makeText(ProfileActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    private boolean isNetworkConnected() {
        android.net.ConnectivityManager connectivityManager =
                (android.net.ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            android.net.Network network = connectivityManager.getActiveNetwork();
            android.net.NetworkCapabilities capabilities =
                    connectivityManager.getNetworkCapabilities(network);

            return capabilities != null &&
                    (capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR));
        }
        return false;
    }

    private void showNetworkErrorDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.error_network_title))
                .setMessage(getString(R.string.error_network_message))
                .setPositiveButton(getString(R.string.btn_retry), (dialog, which) -> loadUserData())
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // Go back when back button is pressed
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}