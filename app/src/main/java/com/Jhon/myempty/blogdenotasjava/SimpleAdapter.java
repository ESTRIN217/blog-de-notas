package com.Jhon.myempty.blogdenotasjava;

import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import java.util.List;
import java.util.ArrayList;

public class SimpleAdapter extends RecyclerView.Adapter<SimpleAdapter.ViewHolder> {
    private List<View> views = new ArrayList<>();
    
    // --- CORRECCIÓN 1: Método sobrecargado para añadir al final ---
    public void addView(View view) {
        addView(view, views.size()); 
    }

    // Método original
    public void addView(View view, int position) {
        views.add(position, view);
        notifyItemInserted(position);
    }
    
    public void removeView(View view) {
        int position = views.indexOf(view);
        if (position != -1) {
            views.remove(position);
            notifyItemRemoved(position);
        }
    }

    // --- CORRECCIÓN 2: Método para mover items (Drag & Drop) ---
    public void moveItem(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                java.util.Collections.swap(views, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                java.util.Collections.swap(views, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
    }
    
    // Getter para ayudar a encontrar posiciones
    public List<View> getViews() {
        return views;
    }
    
    @Override
    public SimpleAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Usamos FrameLayout para asegurar que la vista tenga contenedor
        FrameLayout container = new FrameLayout(parent.getContext());
        container.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT));
        return new ViewHolder(container);
    }
    
    @Override
    public void onBindViewHolder(SimpleAdapter.ViewHolder holder, int position) {
        holder.container.removeAllViews();
        View view = views.get(position);
        
        // Importante: Si la vista ya tiene padre, hay que quitarla antes de añadirla
        if (view.getParent() != null) {
            ((ViewGroup)view.getParent()).removeView(view);
        }
        holder.container.addView(view);
    }
    
    @Override
    public int getItemCount() {
        return views.size();
    }
    
    class ViewHolder extends RecyclerView.ViewHolder {
        FrameLayout container;
        ViewHolder(FrameLayout container) {
            super(container);
            this.container = container;
        }
    }
}