package com.example.voy.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voy.R;
import com.example.voy.model.TravelEntry;

import java.util.List;

public class TravelEntryAdapter extends RecyclerView.Adapter<EntryViewHolder>{
    private List<TravelEntry> entries;
    private Context context;
    public TravelEntryAdapter(Context context, List<TravelEntry> entries){
        this.entries = entries;
        this.context = context;
    }
    public void SetEntries(Context context, List<TravelEntry> entries){
        this.entries = entries;
    }

    @NonNull
    @Override
    public EntryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
        View view = LayoutInflater.from(context).inflate(R.layout.travel_entry_item, parent, false);
        return new EntryViewHolder(view);
    }

    public void onBindViewHolder(@NonNull EntryViewHolder holder, int position){
        TravelEntry entry = entries.get(position);

        holder.title.setText(entry.title());
        holder.date.setText((int) entry.dateMillis());

        holder.itemView.setOnClickListener(v->{
            Intent intent = new Intent(context, TravelEntry.class);
            intent.putExtra("entry id", entry.id());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }
}
