package com.Jhon.myempty.blogdenotasjava;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ChangelogAdapter extends RecyclerView.Adapter<ChangelogAdapter.ViewHolder> {

    private List<Cambio> listaCambios;

    public ChangelogAdapter(List<Cambio> listaCambios) {
        this.listaCambios = listaCambios;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_changelog, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Cambio cambio = listaCambios.get(position);
        holder.txtVersion.setText(cambio.getVersion());
        holder.txtFecha.setText(cambio.getFecha());
        holder.txtDescripcion.setText(cambio.getDescripcion());
    }

    @Override
    public int getItemCount() {
        return listaCambios.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtVersion, txtFecha, txtDescripcion;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtVersion = itemView.findViewById(R.id.txtVersion);
            txtFecha = itemView.findViewById(R.id.txtFecha);
            txtDescripcion = itemView.findViewById(R.id.txtDescripcion);
        }
    }
}