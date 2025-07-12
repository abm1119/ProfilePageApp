package com.urooj.carpoolingapp.driver.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.urooj.carpoolingapp.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RideRequestAdapter extends RecyclerView.Adapter<RideRequestAdapter.RideRequestViewHolder> {

    public interface RideRequestListener {
        void onAccept(String notificationId, String requestId);

        void onDecline(String notificationId, String requestId);
    }

    private Context context;
    private List<RideRequestNotification> notifications;
    private RideRequestListener listener;

    public RideRequestAdapter(Context context, RideRequestListener listener) {
        this.context = context;
        this.notifications = new ArrayList<>();
        this.listener = listener;
    }

    public void setNotifications(List<RideRequestNotification> notifications) {
        this.notifications = notifications;
        notifyDataSetChanged();
    }

    /**
     * Removes a notification from the list by ID
     *
     * @param notificationId The ID of the notification to remove
     */
    public void removeNotification(String notificationId) {
        if (notificationId == null) return;

        for (int i = 0; i < notifications.size(); i++) {
            if (notificationId.equals(notifications.get(i).getId())) {
                notifications.remove(i);
                notifyItemRemoved(i);
                break;
            }
        }
    }

    @NonNull
    @Override
    public RideRequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(
                R.layout.item_ride_request_notification, parent, false);
        return new RideRequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RideRequestViewHolder holder, int position) {
        RideRequestNotification notification = notifications.get(position);
        holder.bind(notification);

        holder.acceptButton.setOnClickListener(v ->
                listener.onAccept(notification.getId(), notification.getRequestId()));

        holder.declineButton.setOnClickListener(v ->
                listener.onDecline(notification.getId(), notification.getRequestId()));
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    public static class RideRequestViewHolder extends RecyclerView.ViewHolder {
        private ImageView passengerImage;
        private TextView passengerName;
        private TextView passengerCaste;
        private TextView passengerPhone;
        private TextView currentLocationText;
        private TextView destinationText;
        private TextView timestamp;
        private MaterialButton acceptButton;
        private MaterialButton declineButton;

        public RideRequestViewHolder(@NonNull View itemView) {
            super(itemView);
            passengerImage = itemView.findViewById(R.id.passenger_image);
            passengerName = itemView.findViewById(R.id.passenger_name);
            passengerCaste = itemView.findViewById(R.id.passenger_caste);
            passengerPhone = itemView.findViewById(R.id.passenger_phone);
            currentLocationText = itemView.findViewById(R.id.current_location_text);
            destinationText = itemView.findViewById(R.id.destination_text);
            timestamp = itemView.findViewById(R.id.timestamp);
            acceptButton = itemView.findViewById(R.id.accept_button);
            declineButton = itemView.findViewById(R.id.decline_button);
        }

        public void bind(RideRequestNotification notification) {
            passengerName.setText(notification.getPassengerName());
            passengerCaste.setText(notification.getPassengerCaste());
            passengerPhone.setText(notification.getPassengerPhone());
            currentLocationText.setText(notification.getCurrentLocation());
            destinationText.setText(notification.getDestination());

            // Format timestamp
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
                Date date = inputFormat.parse(notification.getTimestamp());
                if (date != null) {
                    timestamp.setText(outputFormat.format(date));
                } else {
                    timestamp.setText(notification.getTimestamp());
                }
            } catch (ParseException e) {
                timestamp.setText(notification.getTimestamp());
            }

            // Load passenger image if available
            if (notification.getPassengerPhotoUrl() != null && !notification.getPassengerPhotoUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(notification.getPassengerPhotoUrl())
                        .placeholder(R.drawable.circular_background)
                        .into(passengerImage);
            } else {
                passengerImage.setImageResource(R.drawable.circular_background);
            }
        }
    }

    // Inner class to represent the notification data
    public static class RideRequestNotification {
        private String id;
        private String requestId;
        private String passengerName;
        private String passengerCaste;
        private String passengerPhone;
        private String passengerPhotoUrl;
        private String currentLocation;
        private String destination;
        private String timestamp;
        private boolean read;

        public RideRequestNotification(String id, String requestId, String passengerName,
                                       String passengerCaste, String passengerPhone,
                                       String passengerPhotoUrl, String currentLocation,
                                       String destination, String timestamp, boolean read) {
            this.id = id;
            this.requestId = requestId;
            this.passengerName = passengerName;
            this.passengerCaste = passengerCaste;
            this.passengerPhone = passengerPhone;
            this.passengerPhotoUrl = passengerPhotoUrl;
            this.currentLocation = currentLocation;
            this.destination = destination;
            this.timestamp = timestamp;
            this.read = read;
        }

        public String getId() {
            return id;
        }

        public String getRequestId() {
            return requestId;
        }

        public String getPassengerName() {
            return passengerName;
        }

        public String getPassengerCaste() {
            return passengerCaste;
        }

        public String getPassengerPhone() {
            return passengerPhone;
        }

        public String getPassengerPhotoUrl() {
            return passengerPhotoUrl;
        }

        public String getCurrentLocation() {
            return currentLocation;
        }

        public String getDestination() {
            return destination;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public boolean isRead() {
            return read;
        }

        public void setRead(boolean read) {
            this.read = read;
        }
    }
}