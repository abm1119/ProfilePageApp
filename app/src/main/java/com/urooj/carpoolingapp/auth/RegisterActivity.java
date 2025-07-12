package com.urooj.carpoolingapp.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.urooj.carpoolingapp.ProfileActivity;
import com.urooj.carpoolingapp.R;
import com.urooj.carpoolingapp.model.User;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    private EditText emailEditText, passwordEditText, confirmPasswordEditText, fullNameEditText, phoneEditText;
    private RadioGroup userTypeRadioGroup;
    private Button registerButton;
    private TextView signInTextView;
    private ProgressBar progressBar;
    
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        
        // Initialize views
        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        emailEditText = findViewById(R.id.email_edit_text);
        passwordEditText = findViewById(R.id.password_edit_text);
        confirmPasswordEditText = findViewById(R.id.confirm_password_edit_text);
        fullNameEditText = findViewById(R.id.full_name_edit_text);
        phoneEditText = findViewById(R.id.phone_edit_text);
        userTypeRadioGroup = findViewById(R.id.user_type_radio_group);
        registerButton = findViewById(R.id.register_button);
        signInTextView = findViewById(R.id.signin_text_view);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void setupClickListeners() {
        // Register button click listener
        registerButton.setOnClickListener(v -> registerUser());
        
        // Sign in text click listener
        signInTextView.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            // Don't finish to allow back navigation
        });
    }
    
    private void registerUser() {
        // Get user input
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();
        String fullName = fullNameEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();

        // Get selected user type
        String userType = getUserType();
        if (userType == null) {
            Toast.makeText(this, getString(R.string.error_user_type_required), Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Validate input
        if (!validateRegistrationInput(email, password, confirmPassword, fullName, phone)) {
            return;
        }

        // Show progress bar and disable register button
        progressBar.setVisibility(View.VISIBLE);
        registerButton.setEnabled(false);

        // Create user with email and password
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success
                        Log.d(TAG, "createUserWithEmail:success");
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();

                        if (firebaseUser != null) {
                            // Create User object
                            saveUserToDatabase(firebaseUser.getUid(), email, fullName, phone, userType);
                        } else {
                            registrationFailed("Failed to get user after registration");
                        }
                    } else {
                        // If sign up fails, handle errors
                        handleRegistrationError(task.getException());
                    }
                });
    }

    private String getUserType() {
        int selectedId = userTypeRadioGroup.getCheckedRadioButtonId();
        if (selectedId == -1) {
            return null;
        }
        RadioButton radioButton = findViewById(selectedId);
        return radioButton.getText().toString().toLowerCase();
    }

    private boolean validateRegistrationInput(String email, String password, String confirmPassword,
                                              String fullName, String phone) {
        boolean isValid = true;

        // Validate email
        if (TextUtils.isEmpty(email)) {
            emailEditText.setError(getString(R.string.error_email_required));
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError(getString(R.string.error_invalid_email));
            isValid = false;
        }

        // Validate password
        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError(getString(R.string.error_password_required));
            isValid = false;
        } else if (password.length() < 6) {
            passwordEditText.setError(getString(R.string.error_password_length));
            isValid = false;
        }

        // Validate password confirmation
        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordEditText.setError(getString(R.string.error_password_required));
            isValid = false;
        } else if (!password.equals(confirmPassword)) {
            confirmPasswordEditText.setError(getString(R.string.error_passwords_not_match));
            isValid = false;
        }

        // Validate full name
        if (TextUtils.isEmpty(fullName)) {
            fullNameEditText.setError(getString(R.string.error_name_required));
            isValid = false;
        }

        // Validate phone
        if (TextUtils.isEmpty(phone)) {
            phoneEditText.setError(getString(R.string.error_phone_required));
            isValid = false;
        } else if (phone.length() < 10) {
            phoneEditText.setError(getString(R.string.error_invalid_phone));
            isValid = false;
        }

        return isValid;
    }

    private void handleRegistrationError(Exception exception) {
        progressBar.setVisibility(View.GONE);
        registerButton.setEnabled(true);

        Log.w(TAG, "createUserWithEmail:failure", exception);

        String errorMessage = getString(R.string.error_registration_failed);

        if (exception != null) {
            if (exception instanceof FirebaseAuthUserCollisionException) {
                errorMessage = getString(R.string.error_email_used);
            } else if (exception instanceof FirebaseAuthWeakPasswordException) {
                errorMessage = getString(R.string.error_password_length);
            } else if (exception.getMessage() != null) {
                errorMessage = exception.getMessage();
            }
        }

        Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_LONG).show();
    }

    private void saveUserToDatabase(String userId, String email, String fullName, String phone, String userType) {
        try {
            // Create User object
            User user;
            // Use appropriate constructor based on user type
            if (userType.equals("driver")) {
                // For drivers, we need more info that we don't have here, 
                // so we'll use default values for the additional fields
                String defaultValue = "";
                user = new User(userId, email, fullName, phone,
                        defaultValue, // caste
                        defaultValue, // cnic
                        "male", // default gender
                        defaultValue, // religion
                        defaultValue, // nationality
                        defaultValue, // car number
                        defaultValue); // home address
            } else {
                // For passengers
                String defaultValue = "";
                user = new User(userId, email, fullName, phone,
                        defaultValue, // caste
                        defaultValue, // cnic
                        "male"); // default gender
            }

            // Make sure the user type is set correctly
            user.setUserType(userType);

            // Save user to database
            mDatabase.child("users").child(userId).setValue(user)
                    .addOnCompleteListener(dbTask -> {
                        progressBar.setVisibility(View.GONE);
                        registerButton.setEnabled(true);

                        if (dbTask.isSuccessful()) {
                            registrationSuccessful();
                        } else {
                            String errorMessage = "Failed to save user data";
                            if (dbTask.getException() != null && dbTask.getException().getMessage() != null) {
                                errorMessage += ": " + dbTask.getException().getMessage();
                            }
                            registrationFailed(errorMessage);
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error saving user data", e);
            registrationFailed("Error creating user profile: " + e.getMessage());
        }
    }

    private void registrationSuccessful() {
        Toast.makeText(RegisterActivity.this, getString(R.string.success_registration), Toast.LENGTH_SHORT).show();

        // Navigate to profile activity
        Intent intent = new Intent(RegisterActivity.this, ProfileActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void registrationFailed(String errorMessage) {
        progressBar.setVisibility(View.GONE);
        registerButton.setEnabled(true);
        Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_LONG).show();

        // Sign out the user since registration wasn't completed properly
        if (mAuth.getCurrentUser() != null) {
            mAuth.signOut();
        }
    }
}