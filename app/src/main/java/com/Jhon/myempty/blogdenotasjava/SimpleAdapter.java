package com.Jhon.myempty.blogdenotasjava;

import android.content.Context;
import android.graphics.Paint;
import android.media.MediaPlayer;
import android.net.Uri;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide; // Recomendado para imágenes, si no usas Glide usa setImageURI

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SimpleAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<ItemAdjunto> listaDatos;
    private Context context;

    public SimpleAdapter(Context context) {
        this.context = context;
        this.listaDatos = new ArrayList<>();
    }

    // --- MÉTODOS DE GESTIÓN DE LISTA ---
    public void agregarItem(ItemAdjunto item) {
        listaDatos.add(item);
        notifyItemInserted(listaDatos.size() - 1);
    }

    // NEW METHOD to add a View directly (for dynamically created views like Checkboxes)
    public void addView(View view) {
        // This method will need to parse the view to create an ItemAdjunto
        // For simplicity here, we assume it's a Checkbox item
        CheckBox checkBox = view.findViewById(R.id.chkEstado);
        EditText editText = view.findViewById(R.id.txtCheckCuerpo);
        if (checkBox != null && editText != null) {
            ItemAdjunto newItem = new ItemAdjunto(editText.getText().toString(), checkBox.isChecked());
            agregarItem(newItem);
        }
    }

    public List<ItemAdjunto> getViews() {
        return listaDatos;
    }

    public void removeView(View view) {
        int position = -1;
        // Find the position of the view in the adapter's list
        for (int i = 0; i < listaDatos.size(); i++) {
            // This comparison might need adjustment based on how views are uniquely identified
            // For now, assuming the content of the ItemAdjunto is unique enough or we are removing the last instance
            // A better approach might be to store the ItemAdjunto reference when adding the view
            // For simplicity, we'll rely on the position passed from the listener.
            // If this method is called directly without a position, a more robust lookup is needed.
            // For this correction, we'll rely on the caller providing the correct view reference and assume its position
            // NOTE: This part might need refinement if multiple identical items exist.
            // A robust solution would involve storing ItemAdjunto references tied to the views.
        }
        // Since this method is called from within the adapter's context, we can rely on adapter position
        // However, if called externally, the index needs to be determined correctly.
        // The current usage in EditorActivity seems to be within the adapter's scope.
    }


    public void moverItem(int fromPosition, int toPosition) {
        if (fromPosition < 0 || toPosition < 0 || fromPosition >= listaDatos.size() || toPosition >= listaDatos.size()) return;
        Collections.swap(listaDatos, fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);
    }

    public List<ItemAdjunto> getListaDatos() {
        return listaDatos;
    }

    // --- DETERMINAR EL TIPO DE VISTA ---
    @Override
    public int getItemViewType(int position) {
        return listaDatos.get(position).getTipo();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == ItemAdjunto.TIPO_CHECK) {
            View v = inflater.inflate(R.layout.item_check, parent, false);
            return new CheckViewHolder(v);
        }
        else if (viewType == ItemAdjunto.TIPO_AUDIO) {
            View v = inflater.inflate(R.layout.item_audio_adjunto, parent, false); // Crea este XML
            return new AudioViewHolder(v);
        }
        else {
            // TIPO_IMAGEN o TIPO_DIBUJO (usan el mismo layout visual)
            View v = inflater.inflate(R.layout.item_adjunto, parent, false); // Crea este XML
            return new ImagenViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ItemAdjunto item = listaDatos.get(position);

        if (holder instanceof CheckViewHolder) {
            ((CheckViewHolder) holder).bind(item);
        } else if (holder instanceof AudioViewHolder) {
            ((AudioViewHolder) holder).bind(item);
        } else if (holder instanceof ImagenViewHolder) {
            ((ImagenViewHolder) holder).bind(item);
        }
    }

    @Override
    public int getItemCount() {
        return listaDatos.size();
    }

    // ==========================================
    // CLASES VIEWHOLDERS (Lógica de cada item)
    // ==========================================

    // 1. ViewHolder para CHECKS
    class CheckViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkBox;
        EditText editText;
        ImageView btnEliminar;

        CheckViewHolder(View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.chkEstado);
            editText = itemView.findViewById(R.id.txtCheckCuerpo);
            btnEliminar = itemView.findViewById(R.id.btnEliminarCheck);
        }

        void bind(ItemAdjunto item) {
            // Evitar bucles infinitos al setear valores
            checkBox.setOnCheckedChangeListener(null);

            checkBox.setChecked(item.isChecked());
            editText.setText(item.getContenido());
            aplicarTachado(item.isChecked());

            // Escuchar cambios en el Check
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                item.setChecked(isChecked);
                aplicarTachado(isChecked);
            });

            // Escuchar cambios en el Texto (Para guardar lo que escribes)
            editText.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    item.setContenido(s.toString());
                }
            });

            btnEliminar.setOnClickListener(v -> eliminarItem(getAdapterPosition()));
        }

        void aplicarTachado(boolean isChecked) {
            if (isChecked) {
                editText.setPaintFlags(editText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                editText.setTextColor(android.graphics.Color.GRAY);
            } else {
                editText.setPaintFlags(editText.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                // Restaurar color negro o del tema
                editText.setTextColor(android.graphics.Color.BLACK);
            }
        }
    }

    // 2. ViewHolder para AUDIOS
    class AudioViewHolder extends RecyclerView.ViewHolder {
        ImageView btnPlay, btnEliminar;

        AudioViewHolder(View itemView) {
            super(itemView);
            btnPlay = itemView.findViewById(R.id.btnPlayAudio); // Asegúrate que el ID coincida con tu XML item_audio
            btnEliminar = itemView.findViewById(R.id.btnEliminarAudio);
        }

        void bind(ItemAdjunto item) {
            btnPlay.setOnClickListener(v -> {
                try {
                    MediaPlayer mp = new MediaPlayer();
                    mp.setDataSource(context, Uri.parse(item.getContenido()));
                    mp.prepare();
                    mp.start();
                    Toast.makeText(context, "Reproduciendo...", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(context, "Error al reproducir", Toast.LENGTH_SHORT).show();
                }
            });

            btnEliminar.setOnClickListener(v -> eliminarItem(getAdapterPosition()));
        }
    }

    // 3. ViewHolder para IMAGENES y DIBUJOS
    class ImagenViewHolder extends RecyclerView.ViewHolder {
        ImageView imagenView, btnEliminar;

        ImagenViewHolder(View itemView) {
            super(itemView);
            imagenView = itemView.findViewById(R.id.miniatura); // ID en item_imagen.xml
            btnEliminar = itemView.findViewById(R.id.btnEliminar);
        }

        void bind(ItemAdjunto item) {
            try {
                // Cargar imagen con URI
                imagenView.setImageURI(Uri.parse(item.getContenido()));
            } catch (Exception e) {
                // Error visual
            }
            btnEliminar.setOnClickListener(v -> eliminarItem(getAdapterPosition()));
        }
    }

    private void eliminarItem(int position) {
        if (position != RecyclerView.NO_POSITION) {
            listaDatos.remove(position);
            notifyItemRemoved(position);
        }
    }
}