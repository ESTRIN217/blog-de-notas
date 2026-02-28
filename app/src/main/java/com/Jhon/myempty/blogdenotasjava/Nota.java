package com.Jhon.myempty.blogdenotasjava;

public class Nota {
    private String titulo;
    private String contenido;
    private String uri;
    private int color;

    public Nota(String titulo, String contenido, int color, String uri) {
        this.titulo = titulo;
        this.contenido = contenido;
        this.color = color;
        this.uri = uri;
    }

    // Getters
    public String getTitulo() { return titulo; }
    public String getContenido() { return contenido; }
    public String getUri() { return uri; }
    public int getColor() { return color; }

    // Setters
    public void setUri(String uri) {
        this.uri = uri;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }
}
