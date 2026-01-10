package com.Jhon.myempty.blogdenotasjava;

public class Cambio {
    private String version;
    private String fecha;
    private String descripcion;

    public Cambio(String version, String fecha, String descripcion) {
        this.version = version;
        this.fecha = fecha;
        this.descripcion = descripcion;
    }

    // Getters
    public String getVersion() { return version; }
    public String getFecha() { return fecha; }
    public String getDescripcion() { return descripcion; }
}