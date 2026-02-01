package com.Jhon.myempty.blogdenotasjava;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import java.util.ArrayList;
import java.util.List;

public class NotaAdapter extends RecyclerView.Adapter<NotaAdapter.NotaViewHolder> {

    private List<Nota> listaNotas;
    // 1. Lista para guardar las posiciones seleccionadas
    private List<Nota> itemsSeleccionados = new ArrayList<>(); 
    
    private final OnNotaClickListener listenerClick;
    private final OnNotaLongClickListener listenerLongClick;

    public interface OnNotaClickListener {
        void onNotaClick(Nota nota);
    }

    public interface OnNotaLongClickListener {
        void onNotaLongClick(View v, Nota nota, int position); // Agregamos position
    }

    public NotaAdapter(List<Nota> listaNotas, OnNotaClickListener listenerClick, OnNotaLongClickListener listenerLongClick) {
        this.listaNotas = listaNotas;
        this.listenerClick = listenerClick;
        this.listenerLongClick = listenerLongClick;
    }

    @NonNull
    @Override
    public NotaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_nota, parent, false);
        return new NotaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotaViewHolder holder, int position) {
        Nota nota = listaNotas.get(position);
        
        // 2. Comprobamos si esta nota está seleccionada
        boolean estaSeleccionada = itemsSeleccionados.contains(nota);
        
        // 3. Pasamos el estado de selección al bind
        holder.bind(nota, listenerClick, listenerLongClick, position, estaSeleccionada);
    }

    @Override
    public int getItemCount() {
        return listaNotas.size();
    }

    public void actualizarLista(List<Nota> nuevaLista) {
        this.listaNotas = nuevaLista;
        notifyDataSetChanged();
    }

    // --- MÉTODOS NUEVOS PARA GESTIONAR LA SELECCIÓN ---
    
    public void toggleSeleccion(Nota nota) {
        if (itemsSeleccionados.contains(nota)) {
            itemsSeleccionados.remove(nota);
        } else {
            itemsSeleccionados.add(nota);
        }
        notifyDataSetChanged(); // Refresca la vista para mostrar el borde
    }

    public void limpiarSeleccion() {
        itemsSeleccionados.clear();
        notifyDataSetChanged();
    }
    
    public boolean haySeleccion() {
        return !itemsSeleccionados.isEmpty();
    }

    static class NotaViewHolder extends RecyclerView.ViewHolder {
        TextView txtTitulo, txtContenido, txtFecha;
        MaterialCardView background; // Asegúrate de usar MaterialCardView en el XML

        public NotaViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTitulo = itemView.findViewById(R.id.txtTituloNota);
            txtContenido = itemView.findViewById(R.id.txtContenidoNota);
            txtFecha = itemView.findViewById(R.id.txtFechaNota);
            background = itemView.findViewById(R.id.nota_background);
        }

        public void bind(final Nota nota, final OnNotaClickListener listener, final OnNotaLongClickListener longListener, int position, boolean isSelected) {
            txtTitulo.setText(nota.getTitulo());
            txtContenido.setText(nota.getContenido());
            txtFecha.setText(nota.getFecha());
            background.setCardBackgroundColor(nota.getColor());

            // 4. CAMBIO VISUAL AL SELECCIONAR
            if (isSelected) {
                // Borde grueso y de color (ej. Azul o el acento del sistema)
                background.setStrokeWidth(4); 
                background.setStrokeColor(Color.parseColor("#FF6200EE")); // O usa ContextCompat.getColor(...)
                background.setAlpha(0.8f); // Opcional: un poco transparente
            } else {
                // (estado normal)
                background.setStrokeWidth(1);
                background.setAlpha(1.0f);
            }

            // Click Normal
            itemView.setOnClickListener(v -> listener.onNotaClick(nota));

            // Click Largo
            itemView.setOnLongClickListener(v -> {
                // Pasamos la posición también
                longListener.onNotaLongClick(v, nota, position);
                return true;
            });
        }
    }
}