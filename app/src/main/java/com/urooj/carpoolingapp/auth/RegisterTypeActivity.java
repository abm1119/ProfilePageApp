package com.urooj.carpoolingapp.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.urooj.carpoolingapp.R;

public class RegisterTypeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_type);

        Button passengerButton = findViewById(R.id.passenger_button);
        Button driverButton = findViewById(R.id.driver_button);
        Button loginButton = findViewById(R.id.login_button);

        passengerButton.setOnClickListener(v -> {
            // Navigate to passenger registration
            Intent intent = new Intent(RegisterTypeActivity.this, PassengerRegisterActivity.class);
            startActivity(intent);
        });

        driverButton.setOnClickListener(v -> {
            // Navigate to driver registration
            Intent intent = new Intent(RegisterTypeActivity.this, DriverRegisterActivity.class);
            startActivity(intent);
        });

        loginButton.setOnClickListener(v -> {
            // Navigate to login
            Intent intent = new Intent(RegisterTypeActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }
}