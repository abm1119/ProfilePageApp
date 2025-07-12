package com.urooj.carpoolingapp.passenger.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.urooj.carpoolingapp.R;
import com.urooj.carpoolingapp.model.PassengerNotification;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PassengerNotificationAdapter extends RecyclerView.Adapter<PassengerNotificationAdapter.NotificationViewHolder> {

    public interface NotificationListener {
        void onDismiss(String notificationId);
    }

    private final Context context;
    private final List<PassengerNotification> notifications;
    private final NotificationListener listener;

    public PassengerNotificationAdapter(Context context, NotificationListener listener) {
        this.context = context;
        this.notifications = new ArrayList<>();
        this.listener = listener;
    }

    public void setNotifications(List<PassengerNotification> notifications) {
        this.notifications.clear();
        this.notifications.addAll(notifications);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_passenger_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        PassengerNotification notification = notifications.get(position);
        holder.bind(notification);

        holder.dismissButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDismiss(notification.getId());
            }
        });
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    public class NotificationViewHolder extends RecyclerView.ViewHolder {
        private final ImageView driverImage;
        private final TextView notificationTitle;
        private final TextView notificationMessage;
        private final ImageView dismissButton;
        private final View statusIndicator;
        private final LinearLayout rejectionContainer;
        private final TextView rejectionReasonText;
        private final TextView notificationTime;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            driverImage = itemView.findViewById(R.id.driver_image);
            notificationTitle = itemView.findViewById(R.id.notification_title);
            notificationMessage = itemView.findViewById(R.id.notification_message);
            dismissButton = itemView.findViewById(R.id.dismiss_button);
            statusIndicator = itemView.findViewById(R.id.status_indicator);
            rejectionContainer = itemView.findViewById(R.id.rejection_container);
            rejectionReasonText = itemView.findViewById(R.id.rejection_reason_text);
            notificationTime = itemView.findViewById(R.id.notification_time);
        }

        public void bind(PassengerNotification notification) {
            // Set the driver image
            if (!notification.getDriverPhoto().isEmpty()) {
                Glide.with(context)
                        .load(notification.getDriverPhoto())
                        .placeholder(R.drawable.circular_background)
                        .into(driverImage);
            } else {
                driverImage.setImageResource(R.drawable.circular_background);
            }

            // Set notification title based on type
            if ("ride_accepted".equals(notification.getType())) {
                notificationTitle.setText(R.string.ride_accepted);
                statusIndicator.setBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_green_dark));
                rejectionContainer.setVisibility(View.GONE);
            } else {
                notificationTitle.setText(R.string.ride_rejected);
                statusIndicator.setBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_red_dark));

                // Show rejection reason if available
                if (!notification.getRejectionReason().isEmpty()) {
                    rejectionContainer.setVisibility(View.VISIBLE);
                    rejectionReasonText.setText(notification.getRejectionReason());
                } else {
                    rejectionContainer.setVisibility(View.GONE);
                }
            }

            // Set notification message
            notificationMessage.setText(notification.getMessage());

            // Format and set time
            notificationTime.setText(formatTimeFromNow(notification.getTimestamp()));
        }

        private String formatTimeFromNow(String timestamp) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                Date date = sdf.parse(timestamp);
                if (date == null) return timestamp;

                Date now = Calendar.getInstance().getTime();
                long diffInMillis = now.getTime() - date.getTime();
                long diffInMinutes = diffInMillis / (60 * 1000);
                long diffInHours = diffInMillis / (60 * 60 * 1000);
                long diffInDays = diffInMillis / (24 * 60 * 60 * 1000);

                if (diffInMinutes < 60) {
                    return diffInMinutes + " min ago";
                } else if (diffInHours < 24) {
                    return diffInHours + " hr ago";
                } else {
                    return diffInDays + " days ago";
                }
            } catch (ParseException e) {
                return timestamp;
            }
        }
    }
}