package com.urooj.carpoolingapp.driver;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.urooj.carpoolingapp.R;
import com.urooj.carpoolingapp.driver.adapters.RideRequestAdapter;
import com.urooj.carpoolingapp.model.RideRequest;
import com.urooj.carpoolingapp.model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DriverNotificationsFragment extends Fragment implements RideRequestAdapter.RideRequestListener {

    private RecyclerView notificationsRecyclerView;
    private ProgressBar progressBar;
    private TextView emptyView;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private RideRequestAdapter adapter;
    private ValueEventListener notificationsListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_driver_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Initialize views
        notificationsRecyclerView = view.findViewById(R.id.notifications_recycler_view);
        progressBar = view.findViewById(R.id.progress_bar);
        emptyView = view.findViewById(R.id.empty_view);

        // Setup RecyclerView
        notificationsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new RideRequestAdapter(getContext(), this);
        notificationsRecyclerView.setAdapter(adapter);

        // Load notifications
        loadNotifications();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Remove listeners to avoid memory leaks
        if (notificationsListener != null && mAuth.getCurrentUser() != null) {
            mDatabase.child("notifications").child(mAuth.getCurrentUser().getUid())
                    .removeEventListener(notificationsListener);
        }
    }

    private void loadNotifications() {
        if (mAuth.getCurrentUser() == null) {
            showError("You must be logged in to view notifications");
            return;
        }

        showLoading(true);

        // First test database permissions
        testDatabasePermissions();

        String driverId = mAuth.getCurrentUser().getUid();

        // Listen for ride request notifications
        notificationsListener = mDatabase.child("notifications").child(driverId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        List<RideRequestAdapter.RideRequestNotification> notifications = new ArrayList<>();

                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            // Check if this is a ride request notification
                            if (snapshot.child("type").exists() &&
                                    "ride_request".equals(snapshot.child("type").getValue(String.class))) {

                                String notificationId = snapshot.getKey();
                                String requestId = snapshot.child("requestId").getValue(String.class);
                                String timestamp = snapshot.child("timestamp").getValue(String.class);
                                boolean read = snapshot.child("read").exists() &&
                                        Boolean.TRUE.equals(snapshot.child("read").getValue(Boolean.class));

                                // Get passenger details directly from the notification
                                String passengerName = snapshot.child("passengerName").exists() ?
                                        snapshot.child("passengerName").getValue(String.class) : "Passenger";
                                String currentLocation = snapshot.child("currentLocation").exists() ?
                                        snapshot.child("currentLocation").getValue(String.class) : "Not specified";
                                String destination = snapshot.child("destination").exists() ?
                                        snapshot.child("destination").getValue(String.class) : "Not specified";

                                // If we have a requestId, try to get additional details from the ride_requests node
                                if (requestId != null) {
                                    loadAdditionalRequestDetails(notificationId, requestId, passengerName,
                                            currentLocation, destination, timestamp, read, notifications);
                                } else {
                                    // Just add what we have
                                    notifications.add(new RideRequestAdapter.RideRequestNotification(
                                            notificationId,
                                            requestId != null ? requestId : "",
                                            passengerName,
                                            "",  // Caste info not available
                                            "",  // Phone not available
                                            "",  // Photo URL not available
                                            currentLocation,
                                            destination,
                                            timestamp,
                                            read
                                    ));
                                    updateUI(notifications);
                                }
                            }
                        }

                        if (notifications.isEmpty()) {
                            showEmpty(true);
                        } else {
                            updateUI(notifications);
                        }

                        showLoading(false);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        showLoading(false);
                        // Check if the error is permission denied
                        if (databaseError.getCode() == DatabaseError.PERMISSION_DENIED) {
                            handlePermissionDeniedError();
                        } else {
                            showError("Failed to load notifications: " + databaseError.getMessage());
                        }
                    }
                });
    }

    private void loadAdditionalRequestDetails(String notificationId, String requestId, String defaultName,
                                              String defaultLocation, String defaultDestination,
                                              String timestamp, boolean read,
                                              List<RideRequestAdapter.RideRequestNotification> notifications) {
        mDatabase.child("ride_requests").child(requestId).addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            RideRequest request = dataSnapshot.getValue(RideRequest.class);

                            if (request != null) {
                                // Check if we already have passenger details cached in the request
                                String passengerName = !request.getPassengerName().isEmpty() ?
                                        request.getPassengerName() : defaultName;
                                String passengerCaste = request.getPassengerCaste();
                                String passengerPhone = request.getPassengerPhone();
                                String passengerPhotoUrl = request.getPassengerPhotoUrl();
                                String currentLocation = !request.getCurrentLocation().isEmpty() ?
                                        request.getCurrentLocation() : defaultLocation;
                                String destination = !request.getDestination().isEmpty() ?
                                        request.getDestination() : defaultDestination;

                                // If we don't have passenger details in the request, load them from the user profile
                                if (passengerName.isEmpty() || passengerCaste.isEmpty() || passengerPhone.isEmpty()) {
                                    loadPassengerDetails(notificationId, requestId, request.getPassengerId(),
                                            passengerName, passengerCaste, passengerPhone, passengerPhotoUrl,
                                            currentLocation, destination, timestamp, read, notifications);
                                } else {
                                    notifications.add(new RideRequestAdapter.RideRequestNotification(
                                            notificationId, requestId, passengerName, passengerCaste,
                                            passengerPhone, passengerPhotoUrl, currentLocation,
                                            destination, timestamp, read));
                                    updateUI(notifications);
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        // Still add notification with default values
                        notifications.add(new RideRequestAdapter.RideRequestNotification(
                                notificationId, requestId, defaultName, "", "", "",
                                defaultLocation, defaultDestination, timestamp, read));
                        updateUI(notifications);
                    }
                });
    }

    private void loadPassengerDetails(String notificationId, String requestId, String passengerId,
                                      String defaultName, String defaultCaste, String defaultPhone,
                                      String defaultPhotoUrl, String currentLocation, String destination,
                                      String timestamp, boolean read,
                                      List<RideRequestAdapter.RideRequestNotification> notifications) {
        mDatabase.child("users").child(passengerId).addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            User passenger = dataSnapshot.getValue(User.class);

                            String name = passenger != null ? passenger.getFullName() : defaultName;
                            String caste = passenger != null ? passenger.getCaste() : defaultCaste;
                            String phone = passenger != null ? passenger.getPhoneNumber() : defaultPhone;
                            String photoUrl = passenger != null ? passenger.getProfileImageUrl() : defaultPhotoUrl;

                            notifications.add(new RideRequestAdapter.RideRequestNotification(
                                    notificationId, requestId, name, caste, phone, photoUrl,
                                    currentLocation, destination, timestamp, read));
                        } else {
                            notifications.add(new RideRequestAdapter.RideRequestNotification(
                                    notificationId, requestId, defaultName, defaultCaste, defaultPhone,
                                    defaultPhotoUrl, currentLocation, destination, timestamp, read));
                        }
                        updateUI(notifications);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        notifications.add(new RideRequestAdapter.RideRequestNotification(
                                notificationId, requestId, defaultName, defaultCaste, defaultPhone,
                                defaultPhotoUrl, currentLocation, destination, timestamp, read));
                        updateUI(notifications);
                    }
                });
    }

    private void updateUI(List<RideRequestAdapter.RideRequestNotification> notifications) {
        if (isAdded() && getContext() != null) {
            adapter.setNotifications(notifications);
            showEmpty(notifications.isEmpty());
        }
    }

    private void showLoading(boolean isLoading) {
        if (progressBar != null && notificationsRecyclerView != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            if (isLoading) {
                notificationsRecyclerView.setVisibility(View.GONE);
                emptyView.setVisibility(View.GONE);
            } else {
                notificationsRecyclerView.setVisibility(View.VISIBLE);
            }
        }
    }

    private void showEmpty(boolean isEmpty) {
        if (emptyView != null && notificationsRecyclerView != null) {
            emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            notificationsRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        }
    }

    private void showError(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    private void handlePermissionDeniedError() {
        // Set the empty view to show a meaningful message
        if (emptyView != null && getContext() != null) {
            emptyView.setText(R.string.permission_denied_message);
            emptyView.setVisibility(View.VISIBLE);
            notificationsRecyclerView.setVisibility(View.GONE);

            // Show an informative dialog with database rules
            showDatabaseRulesDialog();

            // Add a retry button
            View view = getView();
            if (view != null) {
                FloatingActionButton retryButton = view.findViewById(R.id.retry_button);
                if (retryButton == null) {
                    // If retry button doesn't exist in layout, we'll use Toast with instructions
                    Toast.makeText(getContext(),
                            "Database permission denied. Please check your account permissions or try signing out and signing back in.",
                            Toast.LENGTH_LONG).show();
                } else {
                    retryButton.setVisibility(View.VISIBLE);
                    retryButton.setOnClickListener(v -> {
                        retryButton.setVisibility(View.GONE);
                        loadNotifications();
                    });
                }
            }
        }
    }

    private void showDatabaseRulesDialog() {
        if (getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Database Permission Denied");
        builder.setMessage("Your app is experiencing a permission denied error when trying to access notifications.\n\n" +
                "This typically happens when Firebase database rules are restrictive.\n\n" +
                "Required rules for this feature:\n" +
                "- Authenticated users should be able to read user profiles\n" +
                "- Users should be able to read their own notifications\n" +
                "- Users should be able to read and write ride requests\n\n" +
                "Please check the README_DATABASE_RULES.md file in assets folder for details on updating your rules.");

        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.setNeutralButton("Retry", (dialog, which) -> {
            dialog.dismiss();
            loadNotifications();
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void testDatabasePermissions() {
        if (mAuth.getCurrentUser() == null) return;

        String uid = mAuth.getCurrentUser().getUid();
        // Try to write to a test node in the user's notifications
        mDatabase.child("notifications").child(uid).child("test_permission")
                .setValue(System.currentTimeMillis())
                .addOnSuccessListener(aVoid -> {
                    // Success means we have write permissions, clean up test node
                    mDatabase.child("notifications").child(uid).child("test_permission").removeValue();
                })
                .addOnFailureListener(e -> {
                    // If we fail, show a helpful dialog with rule information
                    if (getContext() != null) {
                        new AlertDialog.Builder(getContext())
                                .setTitle("Database Permission Test Failed")
                                .setMessage("Could not write to your notifications node. " +
                                        "Please make sure your Firebase rules allow users to read " +
                                        "and write their own notifications.")
                                .setPositiveButton("Show Required Rules", (dialog, which) -> {
                                    showDatabaseRulesDialog();
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                    }
                });
    }

    @Override
    public void onAccept(String notificationId, String requestId) {
        // Update ride request status
        if (requestId != null && !requestId.isEmpty()) {
            // First get the ride request details to notify passenger
            mDatabase.child("ride_requests").child(requestId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            RideRequest request = snapshot.getValue(RideRequest.class);
                            if (request != null) {
                                // Remove the notification immediately from UI to provide feedback
                                removeFromAdapter(notificationId);

                                // Update status to accepted
                                mDatabase.child("ride_requests").child(requestId).child("status")
                                        .setValue("accepted")
                                        .addOnSuccessListener(aVoid -> {
                                            showMessage("Ride request accepted");

                                            // Delete notification completely instead of just marking as read
                                            mDatabase.child("notifications").child(mAuth.getCurrentUser().getUid())
                                                    .child(notificationId).removeValue();

                                            // Create notification for passenger
                                            createPassengerAcceptNotification(request);
                                        })
                                        .addOnFailureListener(e ->
                                                showError("Failed to accept request: " + e.getMessage()));
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            showError("Error getting request details: " + error.getMessage());
                        }
                    });
        }
    }

    @Override
    public void onDecline(String notificationId, String requestId) {
        // Show dialog to get rejection reason
        if (getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(getString(R.string.enter_rejection_reason));

        // Add an EditText to the dialog
        final EditText input = new EditText(getContext());
        input.setHint(R.string.rejection_hint);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton(R.string.send, (dialog, which) -> {
            String reason = input.getText().toString().trim();
            if (reason.isEmpty()) {
                reason = "No reason provided";
            }
            processRideRejection(notificationId, requestId, reason);
        });

        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void processRideRejection(String notificationId, String requestId, String rejectionReason) {
        if (requestId != null && !requestId.isEmpty()) {
            // Remove the notification immediately from UI to provide feedback
            removeFromAdapter(notificationId);

            // First get the ride request details to notify passenger
            mDatabase.child("ride_requests").child(requestId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            RideRequest request = snapshot.getValue(RideRequest.class);
                            if (request != null) {
                                // Update status to rejected and add rejection reason
                                Map<String, Object> updates = new HashMap<>();
                                updates.put("status", "rejected");
                                updates.put("rejectionReason", rejectionReason);

                                mDatabase.child("ride_requests").child(requestId)
                                        .updateChildren(updates)
                                        .addOnSuccessListener(aVoid -> {
                                            showMessage("Ride request declined");

                                            // Delete notification completely instead of just marking as read
                                            mDatabase.child("notifications").child(mAuth.getCurrentUser().getUid())
                                                    .child(notificationId).removeValue();

                                            // Create notification for passenger
                                            createPassengerRejectNotification(request, rejectionReason);
                                        })
                                        .addOnFailureListener(e ->
                                                showError("Failed to decline request: " + e.getMessage()));
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            showError("Error getting request details: " + error.getMessage());
                        }
                    });
        }
    }

    private void createPassengerAcceptNotification(RideRequest request) {
        if (mAuth.getCurrentUser() == null) return;

        // Get driver details
        mDatabase.child("users").child(mAuth.getCurrentUser().getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        User driver = snapshot.getValue(User.class);
                        if (driver == null) return;

                        // Create a notification for the passenger
                        String notificationId = mDatabase.child("passenger_notifications")
                                .child(request.getPassengerId()).push().getKey();

                        if (notificationId != null) {
                            String message = "Your ride request has been accepted by " + driver.getFullName();

                            // Create notification
                            Map<String, Object> notification = new HashMap<>();
                            notification.put("id", notificationId);
                            notification.put("type", "ride_accepted");
                            notification.put("requestId", request.getRequestId());
                            notification.put("driverId", mAuth.getCurrentUser().getUid());
                            notification.put("driverName", driver.getFullName());
                            notification.put("driverPhoto", driver.getProfileImageUrl());
                            notification.put("message", message);
                            notification.put("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                                    Locale.getDefault()).format(new Date()));
                            notification.put("read", false);
                            notification.put("dismissed", false);

                            // Save to database
                            mDatabase.child("passenger_notifications").child(request.getPassengerId())
                                    .child(notificationId).setValue(notification);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Continue even if there's an error
                    }
                });
    }

    private void createPassengerRejectNotification(RideRequest request, String rejectionReason) {
        if (mAuth.getCurrentUser() == null) return;

        // Get driver details
        mDatabase.child("users").child(mAuth.getCurrentUser().getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        User driver = snapshot.getValue(User.class);
                        if (driver == null) return;

                        // Create a notification for the passenger
                        String notificationId = mDatabase.child("passenger_notifications")
                                .child(request.getPassengerId()).push().getKey();

                        if (notificationId != null) {
                            String message = "Your ride request has been rejected by " + driver.getFullName();

                            // Create notification
                            Map<String, Object> notification = new HashMap<>();
                            notification.put("id", notificationId);
                            notification.put("type", "ride_rejected");
                            notification.put("requestId", request.getRequestId());
                            notification.put("driverId", mAuth.getCurrentUser().getUid());
                            notification.put("driverName", driver.getFullName());
                            notification.put("driverPhoto", driver.getProfileImageUrl());
                            notification.put("message", message);
                            notification.put("rejectionReason", rejectionReason);
                            notification.put("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                                    Locale.getDefault()).format(new Date()));
                            notification.put("read", false);
                            notification.put("dismissed", false);

                            // Save to database
                            mDatabase.child("passenger_notifications").child(request.getPassengerId())
                                    .child(notificationId).setValue(notification);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Continue even if there's an error
                    }
                });
    }

    private void showMessage(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Removes a notification from the adapter by ID
     * This provides immediate visual feedback to the driver
     */
    private void removeFromAdapter(String notificationId) {
        if (adapter != null) {
            adapter.removeNotification(notificationId);
        }
    }
}