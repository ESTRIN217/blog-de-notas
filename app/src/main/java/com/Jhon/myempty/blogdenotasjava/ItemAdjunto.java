package com.Jhon.myempty.blogdenotasjava;

public class ItemAdjunto {
    // Constantes para identificar el tipo
    public static final int TIPO_IMAGEN = 0;
    public static final int TIPO_DIBUJO = 1; // Se comporta casi igual que imagen
    public static final int TIPO_AUDIO  = 2;
    public static final int TIPO_CHECK  = 3;

    private int tipo;
    private String contenido; // URI de la imagen/audio o Texto del Check
    private boolean isChecked; // Solo para checks

    // Constructor general (Audio, Imagen, Dibujo)
    public ItemAdjunto(int tipo, String uri) {
        this.tipo = tipo;
        this.contenido = uri;
        this.isChecked = false;
    }

    // Constructor para Checkbox
    public ItemAdjunto(String texto, boolean isChecked) {
        this.tipo = TIPO_CHECK;
        this.contenido = texto;
        this.isChecked = isChecked;
    }

    public int getTipo() { return tipo; }
    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }
    public boolean isChecked() { return isChecked; }
    public void setChecked(boolean checked) { isChecked = checked; }
}