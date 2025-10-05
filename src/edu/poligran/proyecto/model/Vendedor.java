package edu.poligran.proyecto.model;

import java.util.Objects;

/**
 * Representa un vendedor.
 * Formato CSV de salida (vendedores.csv):
 *   TipoDocumento;NúmeroDocumento;Nombres;Apellidos
 */
public class Vendedor {
    private final String tipoDocumento;
    private final String numeroDocumento;
    private final String nombres;
    private final String apellidos;

    public Vendedor(String tipoDocumento, String numeroDocumento, String nombres, String apellidos) {
        this.tipoDocumento = Objects.requireNonNull(tipoDocumento, "tipoDocumento no puede ser null");
        this.numeroDocumento = Objects.requireNonNull(numeroDocumento, "numeroDocumento no puede ser null");
        this.nombres = Objects.requireNonNull(nombres, "nombres no puede ser null");
        this.apellidos = Objects.requireNonNull(apellidos, "apellidos no puede ser null");
    }

    public String getTipoDocumento() { return tipoDocumento; }
    public String getNumeroDocumento() { return numeroDocumento; }
    public String getNombres() { return nombres; }
    public String getApellidos() { return apellidos; }

    /** Línea CSV para vendedores.csv */
    @Override
    public String toString() {
        // TipoDocumento;NúmeroDocumento;Nombres;Apellidos
        return tipoDocumento + ";" + numeroDocumento + ";" + nombres + ";" + apellidos;
    }
}

