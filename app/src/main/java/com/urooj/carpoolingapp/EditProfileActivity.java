package com.urooj.carpoolingapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.urooj.carpoolingapp.auth.LoginActivity;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private static final String TAG = "EditProfileActivity";

    private TextInputEditText fullNameEditText, phoneEditText, addressEditText, profileImageUrlEditText;
    private TextInputEditText driverPhotoUrlEditText, licensePhotoUrlEditText, carNumberEditText;
    private ShapeableImageView profileImageView;
    private CardView driverInfoCard;
    private Button saveButton;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private String userId;
    private String profileImageUrl;
    private String driverPhotoUrl;
    private String licensePhotoUrl;
    private String email;
    private String userType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // Initialize views
        initializeViews();

        // Get data from intent
        loadIntentData();
    }

    private void initializeViews() {
        fullNameEditText = findViewById(R.id.full_name_edit_text);
        phoneEditText = findViewById(R.id.phone_edit_text);
        addressEditText = findViewById(R.id.address_edit_text);
        profileImageUrlEditText = findViewById(R.id.profile_image_url_edit_text);
        profileImageView = findViewById(R.id.profile_image_card);
        saveButton = findViewById(R.id.save_button);
        progressBar = findViewById(R.id.progress_bar);

        // Driver specific fields
        driverInfoCard = findViewById(R.id.driver_info_card);
        driverPhotoUrlEditText = findViewById(R.id.driver_photo_url_edit_text);
        licensePhotoUrlEditText = findViewById(R.id.license_photo_url_edit_text);
        carNumberEditText = findViewById(R.id.car_number_edit_text);

        // Set up URL image preview
        setupImageUrlListeners();

        // Save button click listener
        saveButton.setOnClickListener(v -> saveUserData());
    }

    private void loadIntentData() {
        // Get data from intent
        Intent intent = getIntent();
        if (intent != null) {
            userId = intent.getStringExtra("userId");
            String fullName = intent.getStringExtra("fullName");
            String phoneNumber = intent.getStringExtra("phoneNumber");
            String address = intent.getStringExtra("address");
            profileImageUrl = intent.getStringExtra("profileImageUrl");
            driverPhotoUrl = intent.getStringExtra("driverPhotoUrl");
            licensePhotoUrl = intent.getStringExtra("licensePhotoUrl");
            String carNumber = intent.getStringExtra("carNumber");
            email = intent.getStringExtra("email");
            userType = intent.getStringExtra("userType");

            if (userId == null) {
                Toast.makeText(this, getString(R.string.error_loading_profile), Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // Initialize Firebase Database
            mDatabase = FirebaseDatabase.getInstance().getReference("users").child(userId);
            mAuth = FirebaseAuth.getInstance();

            // Set existing data to views
            if (fullName != null) fullNameEditText.setText(fullName);
            if (phoneNumber != null) phoneEditText.setText(phoneNumber);
            if (address != null) addressEditText.setText(address);
            if (profileImageUrl != null) {
                profileImageUrlEditText.setText(profileImageUrl);
                loadImageWithGlide(profileImageUrl, profileImageView);
            }

            // Handle driver-specific fields
            if ("driver".equals(userType)) {
                driverInfoCard.setVisibility(View.VISIBLE);
                if (driverPhotoUrl != null) driverPhotoUrlEditText.setText(driverPhotoUrl);
                if (licensePhotoUrl != null) licensePhotoUrlEditText.setText(licensePhotoUrl);
                if (carNumber != null) carNumberEditText.setText(carNumber);
            } else {
                driverInfoCard.setVisibility(View.GONE);
            }
        } else {
            Toast.makeText(this, getString(R.string.error_loading_profile), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void saveUserData() {
        String fullName = fullNameEditText.getText().toString().trim();
        String phoneNumber = phoneEditText.getText().toString().trim();
        String address = addressEditText.getText().toString().trim();
        String profileImageUrl = profileImageUrlEditText.getText().toString().trim();

        // Validate input
        if (fullName.isEmpty()) {
            fullNameEditText.setError(getString(R.string.error_name_required));
            return;
        }

        if (phoneNumber.isEmpty()) {
            phoneEditText.setError(getString(R.string.error_phone_required));
            return;
        }

        // Validate profile image URL if provided
        if (!profileImageUrl.isEmpty() && !Patterns.WEB_URL.matcher(profileImageUrl).matches()) {
            profileImageUrlEditText.setError(getString(R.string.error_invalid_url));
            return;
        }

        // Show progress bar
        progressBar.setVisibility(View.VISIBLE);
        saveButton.setEnabled(false);

        try {
            // Update user data in Firebase using a Map for partial updates
            Map<String, Object> updates = new HashMap<>();
            updates.put("fullName", fullName);
            updates.put("phoneNumber", phoneNumber);
            updates.put("address", address);

            // Only update image URL if it was changed
            if (!profileImageUrl.isEmpty()) {
                updates.put("profileImageUrl", profileImageUrl);
            }

            // Add driver-specific fields if applicable
            if ("driver".equals(userType) && driverInfoCard.getVisibility() == View.VISIBLE) {
                String driverPhotoUrl = driverPhotoUrlEditText.getText().toString().trim();
                String licensePhotoUrl = licensePhotoUrlEditText.getText().toString().trim();
                String carNumber = carNumberEditText.getText().toString().trim();

                // Validate URLs
                if (!driverPhotoUrl.isEmpty()) {
                    if (!Patterns.WEB_URL.matcher(driverPhotoUrl).matches()) {
                        driverPhotoUrlEditText.setError(getString(R.string.error_invalid_url));
                        progressBar.setVisibility(View.GONE);
                        saveButton.setEnabled(true);
                        return;
                    }
                    updates.put("driverPhotoUrl", driverPhotoUrl);
                }

                if (!licensePhotoUrl.isEmpty()) {
                    if (!Patterns.WEB_URL.matcher(licensePhotoUrl).matches()) {
                        licensePhotoUrlEditText.setError(getString(R.string.error_invalid_url));
                        progressBar.setVisibility(View.GONE);
                        saveButton.setEnabled(true);
                        return;
                    }
                    updates.put("licensePhotoUrl", licensePhotoUrl);
                }

                if (!carNumber.isEmpty()) {
                    updates.put("carNumber", carNumber);
                }
            }

            mDatabase.updateChildren(updates)
                    .addOnCompleteListener(task -> {
                        progressBar.setVisibility(View.GONE);
                        saveButton.setEnabled(true);

                        if (task.isSuccessful()) {
                            Toast.makeText(EditProfileActivity.this, getString(R.string.success_profile_update),
                                    Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            handleDatabaseError(task.getException());
                        }
                    });
        } catch (Exception e) {
            progressBar.setVisibility(View.GONE);
            saveButton.setEnabled(true);
            Log.e(TAG, "Error saving profile", e);
            Toast.makeText(this, getString(R.string.error_update_profile), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupImageUrlListeners() {
        // Preview profile image when URL is entered
        profileImageUrlEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String url = s.toString().trim();
                if (!url.isEmpty() && Patterns.WEB_URL.matcher(url).matches()) {
                    loadImageWithGlide(url, profileImageView);
                }
            }
        });

        // Only set up driver specific listeners if they exist
        if (driverPhotoUrlEditText != null && "driver".equals(userType)) {
            driverPhotoUrlEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    // No need for visual preview here as we'll see it in the profile
                }
            });

            licensePhotoUrlEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    // No need for visual preview here as we'll see it in the profile
                }
            });
        }
    }

    private void loadImageWithGlide(String imageUrl, ImageView imageView) {
        Glide.with(this)
                .load(imageUrl)
                .transition(DrawableTransitionOptions.withCrossFade())
                .centerCrop()
                .into(imageView);
    }

    private void handleDatabaseError(Exception exception) {
        String errorMessage = getString(R.string.error_update_profile);

        if (exception != null) {
            Log.e(TAG, "Error updating profile", exception);

            // Check if it might be a permission error (we can't directly cast to DatabaseError)
            if (exception.getMessage() != null &&
                    exception.getMessage().contains("Permission denied")) {
                showPermissionDeniedError();
                return;
            }

            if (exception.getMessage() != null) {
                errorMessage += ": " + exception.getMessage();
            }
        }

        Toast.makeText(EditProfileActivity.this, errorMessage, Toast.LENGTH_LONG).show();
    }

    private void showPermissionDeniedError() {
        // Alert dialog for permission denied error with troubleshooting steps
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.error_permission_denied_title))
                .setMessage(getString(R.string.error_permission_denied_message))
                .setPositiveButton(getString(R.string.btn_sign_out), (dialog, which) -> {
                    // Initialize Auth if needed
                    if (mAuth == null) {
                        mAuth = FirebaseAuth.getInstance();
                    }
                    mAuth.signOut();
                    startActivity(new Intent(EditProfileActivity.this, com.urooj.carpoolingapp.auth.LoginActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                    finish();
                })
                .setNegativeButton(getString(R.string.btn_retry), (dialog, which) -> saveUserData())
                .setCancelable(false)
                .show();
    }
}