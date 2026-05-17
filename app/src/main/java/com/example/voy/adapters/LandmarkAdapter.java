package com.example.voy.adapters;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voy.R;
import com.example.voy.data.entities.LandmarkEntity;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class LandmarkAdapter extends RecyclerView.Adapter<LandmarkAdapter.ViewHolder> {
    public interface OnDeleteListener {
        void onDelete(LandmarkEntity landmark);
    }
    private List<LandmarkEntity> items = new ArrayList<>();
    private final OnDeleteListener onDelete;
    public LandmarkAdapter(OnDeleteListener onDelete) {
        this.onDelete = onDelete;
    }
    public void setItems(List<LandmarkEntity> items) {
        this.items = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_landmark, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LandmarkEntity landmark = items.get(position);
        holder.tvName.setText(landmark.name);
        holder.checkbox.setChecked(landmark.isVisited);
        holder.checkbox.setEnabled(false);
        if (landmark.isVisited){
            holder.tvName.setPaintFlags(
                    holder.tvName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        }else{
            holder.tvName.setPaintFlags(
                    holder.tvName.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
        }
        holder.btnDelete.setOnClickListener(v -> onDelete.onDelete(landmark));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        CheckBox checkbox;
        MaterialButton btnDelete;

        ViewHolder(View v) {
            super(v);
            tvName    = v.findViewById(R.id.tvLandmarkName);
            checkbox  = v.findViewById(R.id.checkboxLandmark);
            btnDelete = v.findViewById(R.id.btnDeleteLandmark);
        }
}
}
