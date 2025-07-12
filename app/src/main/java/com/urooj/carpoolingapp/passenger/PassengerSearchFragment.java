package com.urooj.carpoolingapp.passenger;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.urooj.carpoolingapp.R;
import com.urooj.carpoolingapp.model.User;
import com.urooj.carpoolingapp.passenger.adapters.DriverAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PassengerSearchFragment extends Fragment {

    private TextInputEditText searchEditText;
    private MaterialButton searchButton;
    private RecyclerView driversRecyclerView;
    private ProgressBar progressBar;
    private TextView emptyView;

    private DriverAdapter driverAdapter;
    private DatabaseReference databaseRef;
    private FirebaseAuth mAuth;
    private Map<String, Object> ridePreferences;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_passenger_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase
        databaseRef = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        searchEditText = view.findViewById(R.id.search_edit_text);
        searchButton = view.findViewById(R.id.search_button);
        driversRecyclerView = view.findViewById(R.id.drivers_recycler_view);
        progressBar = view.findViewById(R.id.progress_bar);
        emptyView = view.findViewById(R.id.empty_view);

        // Setup RecyclerView
        driversRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        driverAdapter = new DriverAdapter(getContext());
        driversRecyclerView.setAdapter(driverAdapter);

        // Load ride preferences if available
        loadRidePreferences();

        // Load all drivers initially
        loadDrivers("");

        // Setup search functionality
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Filter results as user types
                if (s.length() >= 3) {
                    driverAdapter.filterDrivers(s.toString());
                    updateEmptyView();
                } else if (s.length() == 0) {
                    driverAdapter.filterDrivers("");
                    updateEmptyView();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        searchButton.setOnClickListener(v -> {
            String query = searchEditText.getText() != null ? searchEditText.getText().toString().trim() : "";
            if (!query.isEmpty()) {
                loadDrivers(query);
            } else {
                loadDrivers("");
            }
        });
    }

    private void loadDrivers(String searchQuery) {
        showLoading(true);

        // Query all users who are drivers
        Query query = databaseRef.child("users").orderByChild("userType").equalTo("driver");

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<User> drivers = new ArrayList<>();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        // Make sure to set the userId from the Firebase key
                        if (user.getUserId() == null) {
                            user.setUserId(snapshot.getKey());
                        }
                        drivers.add(user);
                    }
                }

                // Update the UI with found drivers
                if (getContext() != null) {
                    Toast.makeText(getContext(),
                            String.format("Found %d drivers", drivers.size()),
                            Toast.LENGTH_SHORT).show();
                }

                driverAdapter.setDrivers(drivers);

                if (!searchQuery.isEmpty()) {
                    driverAdapter.filterDrivers(searchQuery);
                }

                showLoading(false);
                updateEmptyView();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                showLoading(false);
                Toast.makeText(getContext(), "Failed to load drivers: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        driversRecyclerView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
    }

    private void updateEmptyView() {
        if (driverAdapter.getItemCount() == 0) {
            emptyView.setVisibility(View.VISIBLE);
            driversRecyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            driversRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Loads ride preferences from Firebase to filter drivers
     */
    private void loadRidePreferences() {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();
        databaseRef.child("ride_preferences").child(userId).get()
                .addOnSuccessListener(dataSnapshot -> {
                    if (dataSnapshot.exists()) {
                        ridePreferences = (Map<String, Object>) dataSnapshot.getValue();

                        // Update search text with locations from preferences
                        if (ridePreferences != null && ridePreferences.containsKey("currentLocation") &&
                                ridePreferences.containsKey("destination")) {
                            String currentLocation = (String) ridePreferences.get("currentLocation");
                            String destination = (String) ridePreferences.get("destination");
                            searchEditText.setText(currentLocation + " to " + destination);

                            // Auto-search using these preferences
                            loadDrivers(currentLocation + " " + destination);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to load ride preferences", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
