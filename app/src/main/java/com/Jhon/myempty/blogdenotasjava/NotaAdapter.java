package com.Jhon.myempty.blogdenotasjava;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import com.google.android.material.card.MaterialCardView;

public class NotaAdapter extends RecyclerView.Adapter<NotaAdapter.NotaViewHolder> {

    private List<Nota> listaNotas;
    private final OnNotaClickListener listenerClick;
    private final OnNotaLongClickListener listenerLongClick; // NUEVO

    // Interfaz para click normal
    public interface OnNotaClickListener {
        void onNotaClick(Nota nota);
    }

    // NUEVO: Interfaz para click largo (pasamos la vista para anclar el menú popup)
    public interface OnNotaLongClickListener {
        void onNotaLongClick(View v, Nota nota);
    }

    // Constructor actualizado
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
        holder.bind(nota, listenerClick, listenerLongClick);
    }

    @Override
    public int getItemCount() {
        return listaNotas.size();
    }

    public void actualizarLista(List<Nota> nuevaLista) {
        this.listaNotas = nuevaLista;
        notifyDataSetChanged();
    }

    static class NotaViewHolder extends RecyclerView.ViewHolder {
        TextView txtTitulo, txtContenido, txtFecha;
        View background;

        public NotaViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTitulo = itemView.findViewById(R.id.txtTituloNota);
            txtContenido = itemView.findViewById(R.id.txtContenidoNota);
            txtFecha = itemView.findViewById(R.id.txtFechaNota);
            background = itemView.findViewById(R.id.nota_background);
        }

        public void bind(final Nota nota, final OnNotaClickListener listener, final OnNotaLongClickListener longListener) {
            txtTitulo.setText(nota.getTitulo());
            txtContenido.setText(nota.getContenido());
            txtFecha.setText(nota.getFecha());
            background.setBackgroundColor(nota.getColor());

            // Click Normal
            itemView.setOnClickListener(v -> listener.onNotaClick(nota));

            // NUEVO: Click Largo
            itemView.setOnLongClickListener(v -> {
                longListener.onNotaLongClick(v, nota);
                return true; // "true" significa que consumimos el evento (no hace click normal después)
            });
        }
    }
}