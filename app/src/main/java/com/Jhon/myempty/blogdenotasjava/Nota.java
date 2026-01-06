package com.Jhon.myempty.blogdenotasjava;

public class Nota {
    private String titulo;
    private String contenido;
    private String fecha;
    private String uriArchivo; // Antes era nombreArchivo, ahora es la URI completa de SAF

    public Nota(String titulo, String contenido, String fecha, String uriArchivo) {
        this.titulo = titulo;
        this.contenido = contenido;
        this.fecha = fecha;
        this.uriArchivo = uriArchivo;
    }

    // Getters
    public String getTitulo() { return titulo; }
    public String getContenido() { return contenido; }
    public String getFecha() { return fecha; }
    
    /**
     * @return La URI de SAF como String (ej: content://com.android...)
     */
    public String getNombreArchivo() { return uriArchivo; } 
}