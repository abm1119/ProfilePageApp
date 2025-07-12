package com.urooj.carpoolingapp.model;

import androidx.annotation.Keep;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
@Keep
public class PassengerNotification {
    private String id;
    private String type; // "ride_accepted" or "ride_rejected"
    private String requestId;
    private String driverId;
    private String driverName;
    private String driverPhoto;
    private String message;
    private String timestamp;
    private String rejectionReason; // Only used when type is "ride_rejected"
    private boolean read;
    private boolean dismissed;

    // Required empty constructor for Firebase
    public PassengerNotification() {
    }

    // Constructor for accepted rides
    public PassengerNotification(String id, String requestId, String driverId, String driverName,
                                 String driverPhoto, String message, String timestamp) {
        this.id = id;
        this.type = "ride_accepted";
        this.requestId = requestId;
        this.driverId = driverId;
        this.driverName = driverName;
        this.driverPhoto = driverPhoto;
        this.message = message;
        this.timestamp = timestamp;
        this.read = false;
        this.dismissed = false;
    }

    // Constructor for rejected rides
    public PassengerNotification(String id, String requestId, String driverId, String driverName,
                                 String driverPhoto, String message, String rejectionReason, String timestamp) {
        this.id = id;
        this.type = "ride_rejected";
        this.requestId = requestId;
        this.driverId = driverId;
        this.driverName = driverName;
        this.driverPhoto = driverPhoto;
        this.message = message;
        this.rejectionReason = rejectionReason;
        this.timestamp = timestamp;
        this.read = false;
        this.dismissed = false;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getDriverId() {
        return driverId;
    }

    public void setDriverId(String driverId) {
        this.driverId = driverId;
    }

    public String getDriverName() {
        return driverName != null ? driverName : "";
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }

    public String getDriverPhoto() {
        return driverPhoto != null ? driverPhoto : "";
    }

    public void setDriverPhoto(String driverPhoto) {
        this.driverPhoto = driverPhoto;
    }

    public String getMessage() {
        return message != null ? message : "";
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTimestamp() {
        return timestamp != null ? timestamp : "";
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getRejectionReason() {
        return rejectionReason != null ? rejectionReason : "";
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public boolean isDismissed() {
        return dismissed;
    }

    public void setDismissed(boolean dismissed) {
        this.dismissed = dismissed;
    }
}