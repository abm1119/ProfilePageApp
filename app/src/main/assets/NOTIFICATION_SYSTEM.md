# Notification System Documentation

This document explains how the notification system works in the Carpooling App.

## Overview

The app features two types of notifications:

1. **Driver Notifications**: Ride requests from passengers appear in the driver's notification tab
2. **Passenger Notifications**: Updates about ride requests (accepted/rejected) appear in the
   passenger's home screen

## Notification Flow

1. Passenger searches for drivers and requests a ride
2. Driver receives a notification with passenger details and request info
3. Driver can accept or reject the ride request
    - If rejecting, driver must provide a reason
4. Passenger receives a notification about acceptance or rejection
5. Passenger can dismiss notifications using the cross (x) button

## Database Structure

### Driver Notifications

```
/notifications/{driverId}/{notificationId}
  - type: "ride_request"
  - requestId: "{ride_request_id}" 
  - message: "New ride request from {passenger_name}"
  - timestamp: "YYYY-MM-DD HH:MM:SS"
  - read: boolean
```

### Passenger Notifications

```
/passenger_notifications/{passengerId}/{notificationId}
  - type: "ride_accepted" or "ride_rejected" 
  - requestId: "{ride_request_id}"
  - driverId: "{driver_id}"
  - driverName: "{driver_name}" 
  - driverPhoto: "{driver_photo_url}"
  - message: "Your ride request has been accepted/rejected by {driver_name}"
  - rejectionReason: "{reason}" (only for rejected rides)
  - timestamp: "YYYY-MM-DD HH:MM:SS"
  - read: boolean
  - dismissed: boolean
```

## Notification Lifecycle

### Driver Notifications

- Created when a passenger requests a ride
- Disappear immediately when driver accepts or rejects the request
- The notification is deleted from the database after response

### Passenger Notifications

- Created when a driver accepts or rejects a ride request
- Remain visible until explicitly dismissed by the passenger
- Marked as "dismissed" in the database when passenger dismisses them

## Troubleshooting

If notifications fail to load, try the following:

1. Check your internet connection
2. Verify that you're signed in to the app
3. Try signing out and signing back in to refresh authentication tokens
4. Ensure the Firebase database rules allow the correct permissions:
   ```json
   {
     "rules": {
       "notifications": {
         "$uid": {
           ".read": "$uid === auth.uid",
           ".write": "auth != null"
         }
       },
       "passenger_notifications": {
         "$uid": {
           ".read": "$uid === auth.uid", 
           ".write": "auth != null"
         }
       }
     }
   }
   ```

## Important Notes

- Notifications require an active database connection
- If database permissions are denied, notifications will not appear
- The app attempts to test permissions by writing to a test path before loading notifications