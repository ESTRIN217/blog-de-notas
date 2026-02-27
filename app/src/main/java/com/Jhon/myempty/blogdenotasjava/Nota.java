package com.Jhon.myempty.blogdenotasjava;

public class Nota {
    private String titulo;
    private String contenido;
    private String path;
    private int color;

    public Nota(String titulo, String contenido, int color, String path ) {
        this.titulo = titulo;
        this.contenido = contenido;
        this.path = path;
        this.color = color;
    }

    // Getters
    public String getTitulo() { return titulo; }
    public String getContenido() { return contenido; }
    
    public String getPath() { return path; } 
    public int getColor() { return color; }
}