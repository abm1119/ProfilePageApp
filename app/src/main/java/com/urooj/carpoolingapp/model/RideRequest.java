package com.urooj.carpoolingapp.model;

import androidx.annotation.Keep;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
@Keep
public class RideRequest {
    private String requestId;
    private String passengerId;
    private String driverId;
    private String status; // "pending", "accepted", "rejected", "completed"
    private String timestamp;
    private String currentLocation;
    private String destination;

    // Passenger details cache
    private String passengerName;
    private String passengerCaste;
    private String passengerPhone;
    private String passengerPhotoUrl;

    public RideRequest() {
        // Required empty constructor for Firebase
    }

    public RideRequest(String requestId, String passengerId, String driverId, String status,
                       String timestamp, String currentLocation, String destination) {
        this.requestId = requestId;
        this.passengerId = passengerId;
        this.driverId = driverId;
        this.status = status;
        this.timestamp = timestamp;
        this.currentLocation = currentLocation;
        this.destination = destination;
    }

    // Getters and setters
    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getPassengerId() {
        return passengerId;
    }

    public void setPassengerId(String passengerId) {
        this.passengerId = passengerId;
    }

    public String getDriverId() {
        return driverId;
    }

    public void setDriverId(String driverId) {
        this.driverId = driverId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getCurrentLocation() {
        return currentLocation != null ? currentLocation : "";
    }

    public void setCurrentLocation(String currentLocation) {
        this.currentLocation = currentLocation;
    }

    public String getDestination() {
        return destination != null ? destination : "";
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getPassengerName() {
        return passengerName != null ? passengerName : "";
    }

    public void setPassengerName(String passengerName) {
        this.passengerName = passengerName;
    }

    public String getPassengerCaste() {
        return passengerCaste != null ? passengerCaste : "";
    }

    public void setPassengerCaste(String passengerCaste) {
        this.passengerCaste = passengerCaste;
    }

    public String getPassengerPhone() {
        return passengerPhone != null ? passengerPhone : "";
    }

    public void setPassengerPhone(String passengerPhone) {
        this.passengerPhone = passengerPhone;
    }

    public String getPassengerPhotoUrl() {
        return passengerPhotoUrl != null ? passengerPhotoUrl : "";
    }

    public void setPassengerPhotoUrl(String passengerPhotoUrl) {
        this.passengerPhotoUrl = passengerPhotoUrl;
    }
}