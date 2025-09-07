package edu.poligran.proyecto.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Representa un producto del catálogo.
 * Formato CSV de salida (productos.csv):
 *   IDProducto;NombreProducto;PrecioPorUnidadProducto
 */
public class Producto {
    private final String id;
    private final String nombre;
    private final BigDecimal precioUnitario;

    public Producto(String id, String nombre, BigDecimal precioUnitario) {
        this.id = Objects.requireNonNull(id, "id no puede ser null");
        this.nombre = Objects.requireNonNull(nombre, "nombre no puede ser null");
        this.precioUnitario = Objects.requireNonNull(precioUnitario, "precioUnitario no puede ser null");
        if (precioUnitario.signum() < 0) {
            throw new IllegalArgumentException("El precio no puede ser negativo");
        }
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public BigDecimal getPrecioUnitario() { return precioUnitario; }

    /** Línea CSV para productos.csv */
    @Override
    public String toString() {
        // IDProducto;NombreProducto;PrecioPorUnidadProducto
        return id + ";" + nombre + ";" + precioUnitario;
    }
}
