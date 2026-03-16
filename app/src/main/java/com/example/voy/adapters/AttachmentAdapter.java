package com.example.voy.adapters;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.voy.R;

import java.util.List;

public class AttachmentAdapter extends RecyclerView.Adapter<AttachmentAdapter.ViewHolder> {

    public interface OnRemoveListener {
        void onRemove(String uri);
    }

    private final List<String> attachments;
    private boolean editMode;
    private final OnRemoveListener removeListener;

    public AttachmentAdapter(List<String> attachments, boolean editMode,
                             OnRemoveListener removeListener) {
        this.attachments    = attachments;
        this.editMode       = editMode;
        this.removeListener = removeListener;
    }

    public void setAttachments(List<String> newList) {
        attachments.clear();
        attachments.addAll(newList);
        notifyDataSetChanged();
    }

    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_attachment, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String uriStr = attachments.get(position);
        Uri uri = Uri.parse(uriStr);

        boolean isPdf = uriStr.endsWith(".pdf")
                || "application/pdf".equals(
                holder.itemView.getContext()
                        .getContentResolver().getType(uri));

        if (isPdf) {
            holder.thumbnail.setImageResource(android.R.drawable.ic_menu_agenda);
        } else {
            Glide.with(holder.itemView.getContext())
                    .load(uri)
                    .centerCrop()
                    .into(holder.thumbnail);
        }

        // Tap to open
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            v.getContext().startActivity(intent);
        });

        // Remove button only in edit mode
        holder.btnRemove.setVisibility(editMode ? View.VISIBLE : View.GONE);
        holder.btnRemove.setOnClickListener(v -> {
            if (removeListener != null) removeListener.onRemove(uriStr);
        });
    }

    @Override
    public int getItemCount() {
        return attachments.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView thumbnail;
        final ImageButton btnRemove;

        ViewHolder(View itemView) {
            super(itemView);
            thumbnail = itemView.findViewById(R.id.imgAttachment);
            btnRemove = itemView.findViewById(R.id.btnRemoveAttachment);
        }
    }
}