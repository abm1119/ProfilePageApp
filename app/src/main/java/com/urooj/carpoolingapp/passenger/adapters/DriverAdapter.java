package com.urooj.carpoolingapp.passenger.adapters;

import android.content.Context;
import android.content.Intent;
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
import com.urooj.carpoolingapp.model.User;
import com.urooj.carpoolingapp.passenger.DriverProfileActivity;

import java.util.ArrayList;
import java.util.List;

public class DriverAdapter extends RecyclerView.Adapter<DriverAdapter.DriverViewHolder> {

    private Context context;
    private List<User> driverList;
    private List<User> filteredDriverList;

    public DriverAdapter(Context context) {
        this.context = context;
        this.driverList = new ArrayList<>();
        this.filteredDriverList = new ArrayList<>();
    }

    public void setDrivers(List<User> driverList) {
        this.driverList = driverList;
        this.filteredDriverList = new ArrayList<>(driverList);
        notifyDataSetChanged();
    }

    public void filterDrivers(String query) {
        filteredDriverList.clear();
        if (query.isEmpty()) {
            filteredDriverList.addAll(driverList);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (User driver : driverList) {
                if (driver.getFullName().toLowerCase().contains(lowerCaseQuery) ||
                        driver.getCarNumber() != null && driver.getCarNumber().toLowerCase().contains(lowerCaseQuery)) {
                    filteredDriverList.add(driver);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DriverViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_driver, parent, false);
        return new DriverViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DriverViewHolder holder, int position) {
        User driver = filteredDriverList.get(position);
        holder.bind(driver);

        holder.carIcon.setOnClickListener(v -> {
            Intent intent = new Intent(context, DriverProfileActivity.class);
            intent.putExtra("DRIVER_ID", driver.getUserId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return filteredDriverList.size();
    }

    public static class DriverViewHolder extends RecyclerView.ViewHolder {
        private ImageView driverImage;
        private TextView driverName;
        private TextView carNumber;
        private ImageView carIcon;

        public DriverViewHolder(@NonNull View itemView) {
            super(itemView);
            driverImage = itemView.findViewById(R.id.driver_image);
            driverName = itemView.findViewById(R.id.driver_name);
            carNumber = itemView.findViewById(R.id.car_number);
            carIcon = itemView.findViewById(R.id.car_icon);
        }

        public void bind(User driver) {
            driverName.setText(driver.getFullName());
            carNumber.setText(driver.getCarNumber());

            // Load driver profile image
            if (!driver.getProfileImageUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(driver.getProfileImageUrl())
                        .placeholder(R.drawable.circular_background)
                        .into(driverImage);
            }
        }
    }
}