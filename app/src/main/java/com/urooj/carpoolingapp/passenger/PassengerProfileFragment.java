package com.urooj.carpoolingapp.passenger;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.urooj.carpoolingapp.ProfileActivity;
import com.urooj.carpoolingapp.R;

/**
 * Passenger Profile Fragment to show a button to open the full profile
 */
public class PassengerProfileFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Just inflate the layout
        return inflater.inflate(R.layout.fragment_profile_simple, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Find and set up the profile button
        Button openProfileBtn = view.findViewById(R.id.btn_open_profile);
        openProfileBtn.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), ProfileActivity.class);
            startActivity(intent);
        });
    }
}