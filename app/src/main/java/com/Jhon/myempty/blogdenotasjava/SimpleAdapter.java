package com.Jhon.myempty.blogdenotasjava;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.button.MaterialButton;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import android.graphics.Paint;
import android.graphics.Color;
import androidx.annotation.NonNull;
import android.text.TextWatcher;

public class SimpleAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<ItemAdjunto> listaDatos;
    private Context context;
    private MediaPlayer mediaPlayer;
    private Handler handlerAudio = new Handler(Looper.getMainLooper());

    public SimpleAdapter(Context context) {
        this.context = context;
        this.listaDatos = new ArrayList<>();
    }

    public void agregarItem(ItemAdjunto item) {
        listaDatos.add(item);
        notifyItemInserted(listaDatos.size() - 1);
    }

    // Eliminar un ítem de la lista y notificar
    public void removeView(int position) {
        if (position >= 0 && position < listaDatos.size()) {
            ItemAdjunto item = listaDatos.get(position);
            eliminarArchivoFisico(item.getContenido());
            listaDatos.remove(position);
            notifyItemRemoved(position);
        }
    }
    
    public List<ItemAdjunto> getListaDatos() {
    return listaDatos;
    }

    public void moverItem(int from, int to) {
    Collections.swap(listaDatos, from, to);
    notifyItemMoved(from, to);
    }

    @Override
    public int getItemViewType(int position) {
        return listaDatos.get(position).getTipo();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        if (viewType == ItemAdjunto.TIPO_AUDIO) {
            return new AudioViewHolder(inflater.inflate(R.layout.item_audio_adjunto, parent, false));
        } else if (viewType == ItemAdjunto.TIPO_CHECK) {
        return new CheckViewHolder(inflater.inflate(R.layout.item_check, parent, false));
        }else {
            return new ImagenViewHolder(inflater.inflate(R.layout.item_adjunto, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
    ItemAdjunto item = listaDatos.get(position);

    if (holder instanceof AudioViewHolder) {
        ((AudioViewHolder) holder).bind(item);
    } 
    else if (holder instanceof CheckViewHolder) { // <-- Aquí faltaba la condición
        ((CheckViewHolder) holder).bind(item);
    } 
    else if (holder instanceof ImagenViewHolder) {
        ((ImagenViewHolder) holder).bind(item);
    }
    }

    @Override
    public int getItemCount() { return listaDatos.size(); }

// --- VIEWHOLDER PARA CHECKBOXES ---
    class CheckViewHolder extends RecyclerView.ViewHolder {
    MaterialCheckBox checkBox;
    TextInputEditText editText;
    MaterialButton btnEliminar, handle;

    CheckViewHolder(View v) {
        super(v);
        checkBox = v.findViewById(R.id.chkEstado);
        editText = v.findViewById(R.id.txtCheckCuerpo);
        btnEliminar = v.findViewById(R.id.btnEliminarCheck);
        handle = v.findViewById(R.id.drag);
    }

    void bind(ItemAdjunto item) {
    // 1. ELIMINAR cualquier listener previo para evitar conflictos al reciclar
    if (editText.getTag() instanceof TextWatcher) {
        editText.removeTextChangedListener((TextWatcher) editText.getTag());
    }

    editText.setText(item.getContenido());
    checkBox.setChecked(item.isMarcado());
    actualizarEstiloTachado(item.isMarcado());

    // 2. CREAR el nuevo listener
    SimpleTextWatcher textWatcher = new SimpleTextWatcher() {
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            item.setContenido(s.toString());
        }
    };

    // 3. GUARDAR el listener en el Tag y activarlo
    editText.setTag(textWatcher);
    editText.addTextChangedListener(textWatcher);

    // Guardar estado del checkbox (limpiar listener anterior primero)
    checkBox.setOnCheckedChangeListener(null); 
    checkBox.setChecked(item.isMarcado());
    checkBox.setOnCheckedChangeListener((v, isChecked) -> {
        item.setMarcado(isChecked);
        actualizarEstiloTachado(isChecked);
    });

    btnEliminar.setOnClickListener(v -> removeView(getAbsoluteAdapterPosition()));
    }

    void actualizarEstiloTachado(boolean marcado) {
    int color = com.google.android.material.color.MaterialColors.getColor(editText, com.google.android.material.R.attr.colorOnSurface);
    
    if (marcado) {
        editText.setPaintFlags(editText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        editText.setTextColor(Color.GRAY); // O un color con transparencia
    } else {
        editText.setPaintFlags(editText.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        editText.setTextColor(color);
    }
    }
    }

    // --- VIEWHOLDER PARA IMÁGENES / DIBUJOS ---
    class ImagenViewHolder extends RecyclerView.ViewHolder {
        ImageView miniatura, btnEliminar, btnEditar;

        ImagenViewHolder(View v) {
            super(v);
            miniatura = v.findViewById(R.id.miniatura);
            btnEliminar = v.findViewById(R.id.btnEliminar);
            btnEditar = v.findViewById(R.id.btnEditar);
        }

        void bind(ItemAdjunto item) {
            miniatura.setImageURI(Uri.parse(item.getContenido()));
            
            btnEliminar.setOnClickListener(v -> removeView(getAbsoluteAdapterPosition()));

            btnEditar.setOnClickListener(v -> {
                // Lógica de edición enviada a la Activity mediante un Intent
                Intent intent = new Intent(context, DibujoActivity.class);
                String clave = (item.getTipo() == ItemAdjunto.TIPO_DIBUJO) ? "uri_dibujo_editar" : "uri_foto_editar";
                intent.putExtra(clave, item.getContenido());
                
                // Como necesitamos startActivityForResult, usamos el context
                if (context instanceof EditorActivity) {
                    ((EditorActivity) context).dibujoLauncher.launch(intent);
                    removeView(getAbsoluteAdapterPosition()); // Eliminar la vieja mientras se edita
                }
            });
        }
    }

    // --- VIEWHOLDER PARA AUDIO ---
    class AudioViewHolder extends RecyclerView.ViewHolder {
        ImageView btnPlay, btnEliminar;
        ProgressBar progressBar;

        AudioViewHolder(View v) {
            super(v);
            btnPlay = v.findViewById(R.id.btnPlayAudio);
            btnEliminar = v.findViewById(R.id.btnEliminarAudio);
            progressBar = v.findViewById(R.id.progressAudio);
        }

        void bind(ItemAdjunto item) {
            btnPlay.setOnClickListener(v -> gestionarAudio(item.getContenido(), btnPlay, progressBar));
            btnEliminar.setOnClickListener(v -> {
                liberarMediaPlayer();
                removeView(getAbsoluteAdapterPosition());
            });
        }
    }

    // --- LÓGICA DE AUDIO (Centralizada en el Adapter) ---
    private void gestionarAudio(String ruta, ImageView btnPlay, ProgressBar pb) {
        try {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                btnPlay.setImageResource(R.drawable.play_circle_outline);
                return;
            }

            if (mediaPlayer == null) {
                mediaPlayer = new MediaPlayer();
                if (ruta.startsWith("content://")) mediaPlayer.setDataSource(context, Uri.parse(ruta));
                else mediaPlayer.setDataSource(ruta);
                
                mediaPlayer.prepare();
                mediaPlayer.setOnCompletionListener(mp -> {
                    btnPlay.setImageResource(R.drawable.play_circle_outline);
                    pb.setProgress(0);
                    liberarMediaPlayer();
                });
            }

            mediaPlayer.start();
            btnPlay.setImageResource(R.drawable.pause_circle_outline);
            actualizarProgreso(pb);
        } catch (Exception e) {
            Toast.makeText(context, "Error de audio", Toast.LENGTH_SHORT).show();
        }
    }

    private void actualizarProgreso(ProgressBar pb) {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            pb.setProgress((mediaPlayer.getCurrentPosition() * 100) / mediaPlayer.getDuration());
            handlerAudio.postDelayed(() -> actualizarProgreso(pb), 100);
        }
    }

    private void liberarMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
    
    public void liberarRecursos() {
    if (handlerAudio != null) {
        handlerAudio.removeCallbacksAndMessages(null);
    }
    if (mediaPlayer != null) {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
        mediaPlayer.release();
        mediaPlayer = null;
    }
    }

    private void eliminarArchivoFisico(String ruta) {
        try {
            if (ruta.startsWith("content://")) {
                DocumentFile file = DocumentFile.fromSingleUri(context, Uri.parse(ruta));
                if (file != null) file.delete();
            } else {
                File f = new File(ruta);
                if (f.exists()) f.delete();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
    public abstract class SimpleTextWatcher implements TextWatcher {
    @Override 
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override 
    public void onTextChanged(CharSequence s, int start, int before, int count) {}

    @Override 
    public void afterTextChanged(android.text.Editable s) {} // <--- ESTO FALTA
    }
}