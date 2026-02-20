package com.example.voy.adapters;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voy.R;
import com.example.voy.data.entities.TripEntity;

import java.util.ArrayList;
import java.util.List;

public class TripAdapter extends RecyclerView.Adapter<TripViewHolder> {
    private List<TripEntity> trips = new ArrayList<>();
    public interface OnTripActionListener {
        void onDeleteTrip(TripEntity trip);
        void onTripClicked(TripEntity trip); // optional for opening details later
    }

    private final OnTripActionListener listener;

    public TripAdapter(OnTripActionListener listener) {
        this.listener = listener;
    }
    public void setTrips(List<TripEntity> trips){
        this.trips = trips;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TripViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.trip_item, parent, false);
        return new TripViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TripViewHolder holder, int position) {
        TripEntity trip = trips.get(position);
        holder.bind(trip, listener);
    }

    @Override
    public int getItemCount() {
        return trips.size();
    }
}
