package com.urooj.carpoolingapp.passenger;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.urooj.carpoolingapp.R;
import com.urooj.carpoolingapp.model.RideRequest;
import com.urooj.carpoolingapp.model.User;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DriverProfileActivity extends AppCompatActivity {

    private ImageView backButton;
    private TextView titleTextView;
    private ImageView driverProfileImage;
    private TextView driverNameText;
    private TextView carNumberView;
    private TextView phoneValue;
    private TextView genderValue;
    private TextView religionValue;
    private TextView addressValue;
    private TextInputEditText currentLocationInput;
    private TextInputEditText destinationInput;
    private MaterialButton requestRideButton;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private String driverId;
    private User driverData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_profile_view);
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        driverId = getIntent().getStringExtra("DRIVER_ID");
        if (driverId == null) {
            Toast.makeText(this, getString(R.string.driver_info_not_found), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        initViews();

        // Load driver data
        loadDriverData();

        // Set click listeners
        backButton.setOnClickListener(v -> finish());

        requestRideButton.setOnClickListener(v -> {
            requestRide();
        });
    }

    private void initViews() {
        backButton = findViewById(R.id.back_button);
        titleTextView = findViewById(R.id.profile_title);
        driverProfileImage = findViewById(R.id.driver_profile_image);
        driverNameText = findViewById(R.id.driver_name_text);
        carNumberView = findViewById(R.id.car_number_view);
        phoneValue = findViewById(R.id.phone_value);
        genderValue = findViewById(R.id.gender_value);
        religionValue = findViewById(R.id.religion_value);
        addressValue = findViewById(R.id.address_value);
        currentLocationInput = findViewById(R.id.current_location_input);
        destinationInput = findViewById(R.id.destination_input);
        requestRideButton = findViewById(R.id.request_ride_button);
    }

    private void loadDriverData() {
        requestRideButton.setEnabled(false);
        mDatabase.child("users").child(driverId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    driverData = snapshot.getValue(User.class);

                    if (driverData != null) {
                        populateUI(driverData);
                        requestRideButton.setEnabled(true);
                    } else {
                        showError(getString(R.string.failed_to_load_driver_info));
                    }
                } else {
                    showError(getString(R.string.driver_not_found));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showError(getString(R.string.error) + ": " + error.getMessage());
            }
        });
    }

    private void populateUI(User driver) {
        // Set driver name and profile image
        driverNameText.setText(driver.getFullName());

        if (!driver.getProfileImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(driver.getProfileImageUrl())
                    .placeholder(R.drawable.circular_background)
                    .into(driverProfileImage);
        }

        // Set car details
        carNumberView.setText(driver.getCarNumber());

        // Set personal information
        phoneValue.setText(driver.getPhoneNumber());
        genderValue.setText(driver.getGender());
        religionValue.setText(driver.getReligion());
        addressValue.setText(driver.getHomeAddress());
    }

    private void requestRide() {
        if (mAuth.getCurrentUser() == null) {
            showError(getString(R.string.login_required));
            return;
        }
        String currentLocation = currentLocationInput.getText() != null ?
                currentLocationInput.getText().toString().trim() : "";
        String destination = destinationInput.getText() != null ?
                destinationInput.getText().toString().trim() : "";
        if (currentLocation.isEmpty()) {
            currentLocationInput.setError(getString(R.string.location_required));
            requestRideButton.setEnabled(true);
            return;
        }
        if (destination.isEmpty()) {
            destinationInput.setError(getString(R.string.destination_required));
            requestRideButton.setEnabled(true);
            return;
        }

        // Disable button to prevent multiple requests
        requestRideButton.setEnabled(false);

        String passengerId = mAuth.getCurrentUser().getUid();
        String requestId = mDatabase.child("ride_requests").push().getKey();

        if (requestId != null) {
            // Get passenger data to include in the request
            mDatabase.child("users").child(passengerId).addListenerForSingleValueEvent(
                    new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            User passengerData = snapshot.getValue(User.class);

                    // Create an enhanced ride request
                    RideRequest rideRequest = new RideRequest(
                            requestId,
                            passengerId,
                            driverId,
                            "pending",
                            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()),
                            currentLocation,
                            destination
                    );

                    // Add passenger details if available
                    if (passengerData != null) {
                        rideRequest.setPassengerName(passengerData.getFullName());
                        rideRequest.setPassengerCaste(passengerData.getCaste());
                        rideRequest.setPassengerPhone(passengerData.getPhoneNumber());
                        rideRequest.setPassengerPhotoUrl(passengerData.getProfileImageUrl());
                    }

                    // Save to database
                    mDatabase.child("ride_requests").child(requestId).setValue(rideRequest)
                            .addOnSuccessListener(aVoid -> {
                                // Create notification for driver
                                createDriverNotification(requestId, currentLocation, destination);

                                // Show success message
                                Snackbar.make(requestRideButton, getString(R.string.ride_request_success), Snackbar.LENGTH_LONG)
                                        .setBackgroundTint(ContextCompat.getColor(DriverProfileActivity.this,
                                                android.R.color.holo_green_dark))
                                        .show();

                                // Close activity after a delay
                                requestRideButton.postDelayed(() -> finish(), 2000);
                            })
                            .addOnFailureListener(e -> {
                                showError(getString(R.string.failed_to_send_request) + ": " + e.getMessage());
                                requestRideButton.setEnabled(true);
                            });
                }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            showError(getString(R.string.error) + ": " + error.getMessage());
                            requestRideButton.setEnabled(true);
                        }
                    });
        } else {
            showError(getString(R.string.failed_to_create_request_id));
            requestRideButton.setEnabled(true);
        }
    }

    private void createDriverNotification(String requestId, String currentLocation, String destination) {
        // Create a detailed notification for the driver
        mDatabase.child("users").child(mAuth.getCurrentUser().getUid()).addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        User passengerData = snapshot.getValue(User.class);
                        String passengerName = passengerData != null ? passengerData.getFullName() :
                                mAuth.getCurrentUser().getEmail();

                Map<String, Object> notification = new HashMap<>();
                notification.put("type", "ride_request");
                notification.put("requestId", requestId);
                notification.put("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                        Locale.getDefault()).format(new Date()));
                notification.put("message", getString(R.string.new_ride_request) + " " + passengerName);
                notification.put("passengerName", passengerName);
                notification.put("currentLocation", currentLocation);
                notification.put("destination", destination);
                notification.put("read", false);

                mDatabase.child("notifications").child(driverId).push().setValue(notification);
            }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Even if this fails, the ride request has been created
                        // Just create a basic notification
                        Map<String, Object> notification = new HashMap<>();
                        notification.put("type", "ride_request");
                        notification.put("requestId", requestId);
                        notification.put("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                                Locale.getDefault()).format(new Date()));
                        notification.put("message", getString(R.string.new_ride_request) + " " +
                                mAuth.getCurrentUser().getEmail());
                        notification.put("read", false);

                        mDatabase.child("notifications").child(driverId).push().setValue(notification);
                    }
                });
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}