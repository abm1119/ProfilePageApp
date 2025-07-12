# Carpooling App

This application provides separate interfaces for drivers and passengers to manage carpooling.

## Features

### Passenger Dashboard

The passenger dashboard includes:

- **Home**: View available rides and suggestions
- **Search**: Search for rides
- **Location**: Map view showing nearby drivers and pickup points
- **Profile**: View and edit personal profile

### Driver Dashboard

The driver dashboard includes:

- **Home**: View upcoming rides and statistics
- **Location**: Map view showing routes and pickup points
- **Notifications**: Important updates and ride requests
- **Profile**: View and edit personal and vehicle profile

## Authentication

- Users can register as either a driver or a passenger
- Login will redirect to the appropriate dashboard based on user type

## Profile Management

Both user types can:

- View and edit their profile information
- Upload profile pictures
- Update contact information

## Getting Started

1. Register as either a driver or passenger
2. Log in with your credentials
3. Navigate the dashboard using the bottom navigation tabs
4. Use the profile section to update your information

## Recent Changes

- Fixed login flow to redirect users to appropriate dashboards based on user type
- Added proper back button handling to prevent navigation issues
- Optimized dashboard layouts with proper fragment loading
- Fixed error handling to provide fallback navigation when database issues occur

## Note
This app is currently in development. Some features may be incomplete or placeholder functionality.