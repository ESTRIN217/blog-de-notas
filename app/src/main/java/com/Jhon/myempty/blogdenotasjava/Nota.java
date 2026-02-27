package com.Jhon.myempty.blogdenotasjava;

public class Nota {
    private String titulo;
    private String contenido;
    private String fecha;
    private String uri;
    private int color;

    public Nota(String titulo, String contenido, String fecha, int color, String uri ) {
        this.titulo = titulo;
        this.contenido = contenido;
        this.fecha = fecha;
        this.uri = uri;
        this.color = color;
    }

    // Getters
    public String getTitulo() { return titulo; }
    public String getContenido() { return contenido; }
    public String getFecha() { return fecha; }
    
    public String getUri() { return uri; } 
    public int getColor() { return color; }
}