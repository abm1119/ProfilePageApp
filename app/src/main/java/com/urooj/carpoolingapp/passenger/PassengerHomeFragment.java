package com.urooj.carpoolingapp.passenger;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.urooj.carpoolingapp.R;
import com.urooj.carpoolingapp.model.PassengerNotification;
import com.urooj.carpoolingapp.passenger.adapters.PassengerNotificationAdapter;

import java.util.ArrayList;
import java.util.List;

public class PassengerHomeFragment extends Fragment implements PassengerNotificationAdapter.NotificationListener {

    private MaterialCardView sharedRideCard;
    private MaterialCardView personalRideCard;
    private RecyclerView notificationsRecyclerView;
    private TextView emptyNotificationsText;
    private TextView notificationsTitle;
    private View divider;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private PassengerNotificationAdapter notificationAdapter;
    private ValueEventListener notificationsListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_passenger_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Initialize views
        TextView selectRideTypeText = view.findViewById(R.id.text_select_ride_type);
        selectRideTypeText.setText(R.string.select_ride_type);

        sharedRideCard = view.findViewById(R.id.card_shared_ride);
        personalRideCard = view.findViewById(R.id.card_personal_ride);
        notificationsRecyclerView = view.findViewById(R.id.notifications_recycler_view);
        emptyNotificationsText = view.findViewById(R.id.empty_notifications_text);
        notificationsTitle = view.findViewById(R.id.notifications_title);
        divider = view.findViewById(R.id.divider);

        // Set click listeners
        sharedRideCard.setOnClickListener(v -> {
            // Launch shared ride configuration
            Intent intent = new Intent(getActivity(), RideConfigActivity.class);
            intent.putExtra(RideConfigActivity.EXTRA_RIDE_TYPE, RideConfigActivity.RIDE_TYPE_SHARED);
            startActivity(intent);
        });

        personalRideCard.setOnClickListener(v -> {
            // Launch personal ride configuration
            Intent intent = new Intent(getActivity(), RideConfigActivity.class);
            intent.putExtra(RideConfigActivity.EXTRA_RIDE_TYPE, RideConfigActivity.RIDE_TYPE_PERSONAL);
            startActivity(intent);
        });

        // Setup notifications recycler view
        notificationAdapter = new PassengerNotificationAdapter(requireContext(), this);
        notificationsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        notificationsRecyclerView.setAdapter(notificationAdapter);

        // Load notifications
        loadNotifications();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Remove listeners to avoid memory leaks
        if (notificationsListener != null && mAuth.getCurrentUser() != null) {
            mDatabase.child("passenger_notifications").child(mAuth.getCurrentUser().getUid())
                    .removeEventListener(notificationsListener);
        }
    }

    private void loadNotifications() {
        if (mAuth.getCurrentUser() == null) {
            hideNotificationsSection();
            return;
        }

        // First test permissions by writing to a test path
        testDatabasePermissions();

        String passengerId = mAuth.getCurrentUser().getUid();
        notificationsListener = mDatabase.child("passenger_notifications").child(passengerId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        List<PassengerNotification> notifications = new ArrayList<>();

                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            try {
                                PassengerNotification notification = snapshot.getValue(PassengerNotification.class);
                                if (notification != null && !notification.isDismissed()) {
                                    // Set ID from Firebase key if not already set
                                    if (notification.getId() == null) {
                                        notification.setId(snapshot.getKey());
                                    }
                                    notifications.add(notification);
                                }
                            } catch (Exception e) {
                                // Handle any parsing errors for individual notifications
                                if (getContext() != null) {
                                    Log.e("PassengerHome", "Error parsing notification: " + e.getMessage());
                                }
                            }
                        }

                        updateNotificationUI(notifications);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        if (getContext() != null && isAdded()) {
                            if (databaseError.getCode() == DatabaseError.PERMISSION_DENIED) {
                                // Handle permission denied error specifically
                                showPermissionErrorDialog();
                            } else {
                                Toast.makeText(getContext(), "Failed to load notifications: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                            hideNotificationsSection();
                        }
                    }
                });
    }

    private void updateNotificationUI(List<PassengerNotification> notifications) {
        if (notifications.isEmpty()) {
            hideNotificationsSection();
        } else {
            showNotificationsSection();
            notificationAdapter.setNotifications(notifications);

            // Check if there are any non-dismissed notifications
            boolean hasActiveNotifications = false;
            for (PassengerNotification notification : notifications) {
                if (!notification.isDismissed()) {
                    hasActiveNotifications = true;
                    break;
                }
            }

            if (hasActiveNotifications) {
                notificationsRecyclerView.setVisibility(View.VISIBLE);
                emptyNotificationsText.setVisibility(View.GONE);
            } else {
                notificationsRecyclerView.setVisibility(View.GONE);
                emptyNotificationsText.setVisibility(View.VISIBLE);
            }
        }
    }

    private void showNotificationsSection() {
        if (notificationsTitle != null) {
            notificationsTitle.setVisibility(View.VISIBLE);
        }
        if (divider != null) {
            divider.setVisibility(View.VISIBLE);
        }
        if (notificationsRecyclerView != null) {
            notificationsRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void hideNotificationsSection() {
        if (notificationsTitle != null) {
            notificationsTitle.setVisibility(View.GONE);
        }
        if (divider != null) {
            divider.setVisibility(View.GONE);
        }
        if (notificationsRecyclerView != null) {
            notificationsRecyclerView.setVisibility(View.GONE);
        }
        if (emptyNotificationsText != null) {
            emptyNotificationsText.setVisibility(View.GONE);
        }
    }

    /**
     * Tests database permissions by writing to a temporary location
     */
    private void testDatabasePermissions() {
        if (mAuth.getCurrentUser() == null) return;

        String uid = mAuth.getCurrentUser().getUid();
        // Try to write to a test node in the passenger notifications
        mDatabase.child("passenger_notifications").child(uid).child("test_permission")
                .setValue(System.currentTimeMillis())
                .addOnSuccessListener(aVoid -> {
                    // Success means we have write permissions, clean up test node
                    mDatabase.child("passenger_notifications").child(uid).child("test_permission").removeValue();
                })
                .addOnFailureListener(e -> {
                    // If we fail, show a helpful dialog with rule information
                    showPermissionErrorDialog();
                });
    }

    /**
     * Shows a dialog explaining database permission errors
     */
    private void showPermissionErrorDialog() {
        if (getContext() != null && isAdded()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Database Permission Error");
            builder.setMessage("Unable to access notifications. This may be due to network issues or database permissions.\n\n" +
                    "Please try the following:\n" +
                    "1. Check your internet connection\n" +
                    "2. Sign out and sign back in\n" +
                    "3. Contact support if the problem persists");

            builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
            builder.show();
        }
    }

    @Override
    public void onDismiss(String notificationId) {
        if (mAuth.getCurrentUser() != null && notificationId != null) {
            // Mark notification as dismissed in Firebase
            mDatabase.child("passenger_notifications").child(mAuth.getCurrentUser().getUid())
                    .child(notificationId).child("dismissed").setValue(true)
                    .addOnSuccessListener(aVoid -> {
                        // Successfully dismissed
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(),
                            "Failed to dismiss notification", Toast.LENGTH_SHORT).show());
        }
    }
}