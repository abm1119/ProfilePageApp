package com.urooj.carpoolingapp.passenger;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.urooj.carpoolingapp.R;
import com.urooj.carpoolingapp.passenger.PassengerDashboardActivity;

import java.util.HashMap;
import java.util.Map;

/**
 * Activity to configure ride parameters for passengers.
 * This activity allows users to:
 * 1. Input their current location and destination
 * 2. Select the number of seats required (for shared rides)
 * 3. Preview the route (simulated in this version)
 * 4. Search for available drivers
 * <p>
 * The ride preferences are stored in Firebase under "ride_preferences/{userId}"
 * and used by PassengerSearchFragment to filter drivers.
 */
public class RideConfigActivity extends AppCompatActivity {

    public static final String EXTRA_RIDE_TYPE = "ride_type";
    public static final String RIDE_TYPE_SHARED = "shared";
    public static final String RIDE_TYPE_PERSONAL = "personal";

    private String rideType;
    private TextInputEditText currentLocationInput;
    private TextInputEditText destinationInput;
    private LinearLayout seatsContainer;
    private RadioGroup seatsRadioGroup;
    private Button searchButton;
    private ImageView routePreviewImage;
    private TextView routePreviewText;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ride_config);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Initialize views
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        TextView rideTypeTitle = findViewById(R.id.ride_type_title);
        currentLocationInput = findViewById(R.id.current_location_input);
        destinationInput = findViewById(R.id.destination_input);
        seatsContainer = findViewById(R.id.seats_container);
        seatsRadioGroup = findViewById(R.id.seats_radio_group);
        Button selectOnMapButton = findViewById(R.id.select_on_map_button);
        searchButton = findViewById(R.id.search_ride_button);
        routePreviewImage = findViewById(R.id.route_preview_image);
        routePreviewText = findViewById(R.id.route_preview_text);

        // Update button text from resources
        selectOnMapButton.setText(R.string.select_on_map);
        searchButton.setText(R.string.search_ride);

        // Set initial visibility for route preview elements
        routePreviewImage.setVisibility(View.GONE);
        routePreviewText.setVisibility(View.GONE);

        // Get ride type from intent
        rideType = getIntent().getStringExtra(EXTRA_RIDE_TYPE);
        if (rideType == null) {
            rideType = RIDE_TYPE_SHARED; // Default to shared ride
        }

        // Set activity title based on ride type
        setTitle(R.string.ride_config_title);

        // Set up UI based on ride type
        if (RIDE_TYPE_SHARED.equals(rideType)) {
            rideTypeTitle.setText(R.string.shared_ride_title);
            seatsContainer.setVisibility(View.VISIBLE);
        } else {
            rideTypeTitle.setText(R.string.personal_ride_title);
            seatsContainer.setVisibility(View.GONE);
        }

        // Select first radio button by default
        if (seatsRadioGroup.getChildCount() > 0) {
            ((RadioButton) seatsRadioGroup.getChildAt(0)).setChecked(true);
        }

        // Set up button click listeners
        selectOnMapButton.setOnClickListener(v -> {
            Toast.makeText(RideConfigActivity.this,
                    "Map selection not implemented in this demo",
                    Toast.LENGTH_SHORT).show();
        });

        searchButton.setOnClickListener(v -> {
            searchRide();
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void searchRide() {
        String currentLocation = currentLocationInput.getText() != null ?
                currentLocationInput.getText().toString().trim() : "";
        String destination = destinationInput.getText() != null ?
                destinationInput.getText().toString().trim() : "";

        if (currentLocation.isEmpty()) {
            Toast.makeText(this, "Please enter your current location", Toast.LENGTH_SHORT).show();
            return;
        }

        if (destination.isEmpty()) {
            Toast.makeText(this, "Please enter your destination", Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedSeats = 1;
        if (RIDE_TYPE_SHARED.equals(rideType)) {
            int radioButtonId = seatsRadioGroup.getCheckedRadioButtonId();
            if (radioButtonId == R.id.radio_1_seat) {
                selectedSeats = 1;
            } else if (radioButtonId == R.id.radio_2_seats) {
                selectedSeats = 2;
            } else if (radioButtonId == R.id.radio_3_seats) {
                selectedSeats = 3;
            } else if (radioButtonId == R.id.radio_4_seats) {
                selectedSeats = 4;
            }
        }

        // Change the view to simulate route preview
        routePreviewText.setVisibility(View.VISIBLE);
        routePreviewText.setText("Route from " + currentLocation + " to " + destination);
        routePreviewImage.setVisibility(View.VISIBLE);

        // Save the ride preferences to Firebase
        if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();

            Map<String, Object> ridePreferences = new HashMap<>();
            ridePreferences.put("rideType", rideType);
            ridePreferences.put("currentLocation", currentLocation);
            ridePreferences.put("destination", destination);
            ridePreferences.put("seats", selectedSeats);
            ridePreferences.put("timestamp", System.currentTimeMillis());

            // Store the ride preferences
            mDatabase.child("ride_preferences").child(userId).setValue(ridePreferences)
                    .addOnSuccessListener(aVoid -> {
                        // Navigate to the passenger dashboard with the search tab selected
                        Intent intent = new Intent(RideConfigActivity.this, PassengerDashboardActivity.class);
                        intent.putExtra("open_search_tab", true);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this,
                                "Failed to save ride preferences: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        } else {
            // User is not authenticated, show error
            Toast.makeText(this, "You must be logged in to search for rides",
                    Toast.LENGTH_SHORT).show();
        }

        Toast.makeText(this, "Searching for a " + rideType + " ride with " +
                        (RIDE_TYPE_SHARED.equals(rideType) ? selectedSeats + " seats" : "personal car"),
                Toast.LENGTH_LONG).show();
    }
}
