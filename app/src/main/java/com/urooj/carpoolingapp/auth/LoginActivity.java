package com.urooj.carpoolingapp.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.urooj.carpoolingapp.ProfileActivity;
import com.urooj.carpoolingapp.R;
import com.urooj.carpoolingapp.driver.DriverDashboardActivity;
import com.urooj.carpoolingapp.passenger.PassengerDashboardActivity;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private EditText emailEditText, passwordEditText;
    private Button loginButton;
    private TextView signUpTextView, forgotPasswordTextView;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        
        // Initialize views
        initializeViews();
        setupClickListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is already signed in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            navigateToProfile();
        }
    }

    private void initializeViews() {
        emailEditText = findViewById(R.id.email_edit_text);
        passwordEditText = findViewById(R.id.password_edit_text);
        loginButton = findViewById(R.id.login_button);
        signUpTextView = findViewById(R.id.signup_text_view);
        forgotPasswordTextView = findViewById(R.id.forgot_password_text_view);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void setupClickListeners() {
        // Login button click listener
        loginButton.setOnClickListener(v -> loginUser());
        
        // Sign up text click listener
        signUpTextView.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, com.urooj.carpoolingapp.auth.RegisterTypeActivity.class));
            // Don't finish, allow back navigation
        });
        
        // Forgot password text click listener
        forgotPasswordTextView.setOnClickListener(v -> handleForgotPassword());
    }

    private void handleForgotPassword() {
        String email = emailEditText.getText().toString().trim();
        
        if (TextUtils.isEmpty(email)) {
            emailEditText.setError(getString(R.string.error_email_required));
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this,
                                "Password reset email sent. Check your inbox.",
                                Toast.LENGTH_LONG).show();
                    } else {
                        String errorMessage = getString(R.string.error_login_failed);
                        if (task.getException() != null) {
                            errorMessage = task.getException().getMessage();
                        }
                        Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void loginUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // Validate input
        if (!validateLoginInput(email, password)) {
            return;
        }

        // Show progress bar and disable login button
        progressBar.setVisibility(View.VISIBLE);
        loginButton.setEnabled(false);
        
        // Sign in with email and password
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    progressBar.setVisibility(View.GONE);
                    loginButton.setEnabled(true);

                    if (task.isSuccessful()) {
                        // Sign in success
                        Log.d(TAG, "signInWithEmail:success");
                        Toast.makeText(LoginActivity.this, "Login successful.",
                                Toast.LENGTH_SHORT).show();

                        navigateToProfile();
                    } else {
                        // If sign in fails, display a message to the user
                        handleLoginError(task.getException());
                    }
                });
    }

    private boolean validateLoginInput(String email, String password) {
        boolean isValid = true;

        if (TextUtils.isEmpty(email)) {
            emailEditText.setError(getString(R.string.error_email_required));
            isValid = false;
        }

        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError(getString(R.string.error_password_required));
            isValid = false;
        }

        return isValid;
    }

    private void handleLoginError(Exception exception) {
        Log.w(TAG, "signInWithEmail:failure", exception);

        String errorMessage = getString(R.string.error_login_failed);

        if (exception != null) {
            if (exception instanceof FirebaseAuthInvalidUserException) {
                errorMessage = getString(R.string.error_email_required);
            } else if (exception instanceof FirebaseAuthInvalidCredentialsException) {
                errorMessage = getString(R.string.error_login_failed);
            } else {
                errorMessage = exception.getMessage();
            }
        }

        Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
    }

    private void navigateToProfile() {
        // Check user type and navigate to the appropriate dashboard
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser != null) {
            String userId = firebaseUser.getUid();
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);

            progressBar.setVisibility(View.VISIBLE);

            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    progressBar.setVisibility(View.GONE);

                    if (dataSnapshot.exists()) {
                        try {
                            String userType = dataSnapshot.child("userType").getValue(String.class);
                            Intent intent;

                            if ("driver".equals(userType)) {
                                // Navigate to driver dashboard
                                intent = new Intent(LoginActivity.this, DriverDashboardActivity.class);
                            } else {
                                // Navigate to passenger dashboard
                                intent = new Intent(LoginActivity.this, PassengerDashboardActivity.class);
                            }

                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        } catch (Exception e) {
                            Log.e(TAG, "Error determining user type: " + e.getMessage());
                            // Fallback to profile on error
                            navigateToProfileActivity();
                        }
                    } else {
                        // User doesn't exist in database
                        Log.e(TAG, "User doesn't exist in database");
                        navigateToProfileActivity();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Database error: " + databaseError.getMessage());
                    // Fallback to profile on error
                    navigateToProfileActivity();
                }
            });
        } else {
            // No user is signed in
            Toast.makeText(LoginActivity.this, getString(R.string.error_login_failed), Toast.LENGTH_SHORT).show();
        }
    }

    // Fallback method to navigate to profile if dashboard navigation fails
    private void navigateToProfileActivity() {
        Intent intent = new Intent(LoginActivity.this, ProfileActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}