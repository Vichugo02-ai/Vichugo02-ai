package edu.poligran.proyecto;

import edu.poligran.proyecto.model.Producto;
import edu.poligran.proyecto.model.Vendedor;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * ENTREGa 1 (Semana 3):
 * Genera archivos planos CSV de prueba:
 *  - data/productos.csv              (ID;Nombre;Precio)
 *  - data/vendedores.csv             (TipoDocumento;NumeroDocumento;Nombres;Apellidos)
 *  - data/ventas/<TipoDoc>_<Num>.csv
 *      * Línea 1: TipoDocumentoVendedor;NumeroDocumentoVendedor
 *      * Luego N ventas, una por línea: IDProducto;CantidadVendida;    (con ';' final)
 *
 * No solicita datos al usuario. Imprime mensaje de éxito o error.
 */
public class GenerateInfoFiles {

    // --- Configuración de salida ---
    private static final Path OUT_DIR = Paths.get("data");
    private static final Path VENTAS_DIR = OUT_DIR.resolve("ventas");
    private static final Path PRODUCTOS_FILE = OUT_DIR.resolve("productos.csv");
    private static final Path VENDEDORES_FILE = OUT_DIR.resolve("vendedores.csv");

    // --- RNG reproducible (misma semilla) ---
    private static final Random RNG = new Random(20250307L);

    // --- Listas base para nombres ---
    private static final List<String> NOMBRES = Arrays.asList(
            "Camila", "Juan", "Valentina", "Andres", "Luisa", "Mateo", "Sofia", "Daniel", "Mariana", "Nicolas"
    );
    private static final List<String> APELLIDOS = Arrays.asList(
            "Garcia", "Rodriguez", "Martinez", "Lopez", "Hernandez", "Gomez", "Diaz", "Ramirez", "Torres", "Castro"
    );
    private static final List<String> TIPOS_DOC = Arrays.asList("CC", "CE", "TI");

    private static final List<String> BASE_NOMBRES_PRODUCTOS = Arrays.asList(
            "Teclado", "Mouse", "Monitor", "Laptop", "Auriculares", "Impresora", "Webcam", "Microfono", "Router", "SSD"
    );

    public static void main(String[] args) {
        try {
            prepararDirectorios();

            // 1) Generar productos.csv
            List<Producto> catalogo = createProductsFile(20); // e.g., 20 productos

            // 2) Generar vendedores.csv
            List<Vendedor> vendedores = createSalesManInfoFile(8); // e.g., 8 vendedores

            // 3) Un archivo de ventas por vendedor, con N ventas aleatorias
            for (Vendedor v : vendedores) {
                int ventas = 10 + RNG.nextInt(16); // 10..25
                // Llamada CORRECTA a la sobrecarga con Vendedor + catálogo
                createSalesMenFile(ventas, v, catalogo);
            }

            System.out.println("✅ Archivos generados correctamente en: " + OUT_DIR.toAbsolutePath());
        } catch (Exception e) {
            System.err.println("❌ Error durante la generación de archivos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // =========================================================================================
    // MÉTODOS REQUERIDOS POR LA GUÍA
    // =========================================================================================

    /**
     * a) createSalesMenFile(int randomSalesCount, String name, long id)
     *    Dada una cantidad, un nombre y un id, crea un archivo pseudoaleatorio de ventas
     *    de un vendedor con el nombre y el id dados.
     *
     *    NOTA: el enunciado exige en el archivo de ventas la PRIMERA LÍNEA:
     *    TipoDocumentoVendedor;NumeroDocumentoVendedor
     *    Como esta firma no trae tipo de documento, usamos "CC" por defecto en esta variante.
     *    Para generar ventas con IDs válidos, usa la sobrecarga privada con Vendedor + catálogo.
     */
    public static void createSalesMenFile(int randomSalesCount, String name, long id) throws IOException {
        Vendedor ficticio = new Vendedor("CC", String.valueOf(id), name, "");
        createSalesMenFile(randomSalesCount, ficticio, Collections.<Producto>emptyList());
    }

    /**
     * b) createProductsFile(int productsCount)
     *    Crea productos pseudoaleatorios en productos.csv:
     *    IDProducto;NombreProducto;PrecioPorUnidadProducto
     *    Devuelve la lista para reutilizarla al generar ventas.
     */
    public static List<Producto> createProductsFile(int productsCount) throws IOException {
        if (productsCount <= 0) throw new IllegalArgumentException("productsCount debe ser > 0");

        List<Producto> productos = new ArrayList<>(productsCount);

        try (BufferedWriter bw = Files.newBufferedWriter(PRODUCTOS_FILE, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

            for (int i = 1; i <= productsCount; i++) {
                String id = String.format("P%04d", i);
                String nombreBase = BASE_NOMBRES_PRODUCTOS.get(RNG.nextInt(BASE_NOMBRES_PRODUCTOS.size()));
                String nombre = nombreBase + " " + i;

                BigDecimal precio = BigDecimal.valueOf(20_000 + RNG.nextInt(1_980_001))
                        .setScale(2, RoundingMode.HALF_UP);

                Producto p = new Producto(id, nombre, precio);
                productos.add(p);
                // ID;Nombre;Precio
                bw.write(p.toString());
                bw.newLine();
            }
        }
        return productos;
    }

    /**
     * c) createSalesManInfoFile(int salesmanCount)
     *    Crea vendedores pseudoaleatorios en vendedores.csv:
     *    TipoDocumento;NúmeroDocumento;Nombres;Apellidos
     *    Devuelve la lista para generar ventas por-vendedor.
     */
    public static List<Vendedor> createSalesManInfoFile(int salesmanCount) throws IOException {
        if (salesmanCount <= 0) throw new IllegalArgumentException("salesmanCount debe ser > 0");

        List<Vendedor> vendedores = new ArrayList<>(salesmanCount);
        Set<String> usados = new HashSet<>();

        try (BufferedWriter bw = Files.newBufferedWriter(VENDEDORES_FILE, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

            for (int i = 0; i < salesmanCount; i++) {
                String tipo = TIPOS_DOC.get(RNG.nextInt(TIPOS_DOC.size()));
                String numero = generarNumeroDocumento(usados);
                String nombres = NOMBRES.get(RNG.nextInt(NOMBRES.size()));
                String apellidos = APELLIDOS.get(RNG.nextInt(APELLIDOS.size()));

                Vendedor v = new Vendedor(tipo, numero, nombres, apellidos);
                vendedores.add(v);

                // TipoDocumento;NúmeroDocumento;Nombres;Apellidos
                bw.write(v.toString());
                bw.newLine();
            }
        }
        return vendedores;
    }

    // =========================================================================================
    // SOBRECARGAS / UTILIDADES INTERNAS
    // =========================================================================================

    /**
     * Sobrecarga coherente con el enunciado y con vendedores existentes:
     *  - 1ª línea: TipoDocumentoVendedor;NumeroDocumentoVendedor
     *  - N líneas: IDProducto;CantidadVendida;   (con ';' final)
     */
    private static void createSalesMenFile(int randomSalesCount, Vendedor vendedor, List<Producto> catalogo) throws IOException {
        if (randomSalesCount < 0) throw new IllegalArgumentException("randomSalesCount no puede ser negativo");

        String nombreArchivo = vendedor.getTipoDocumento() + "_" + vendedor.getNumeroDocumento() + ".csv";
        Path archivo = VENTAS_DIR.resolve(nombreArchivo);

        try (BufferedWriter bw = Files.newBufferedWriter(archivo, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

            // Encabezado requerido por la guía:
            // TipoDocumentoVendedor;NumeroDocumentoVendedor
            bw.write(vendedor.getTipoDocumento() + ";" + vendedor.getNumeroDocumento());
            bw.newLine();

            // Si no hay catálogo, sólo escribimos el encabezado (cumple el formato mínimo)
            if (catalogo == null || catalogo.isEmpty()) return;

            // Ventas: una por línea -> IDProducto;CantidadVendida;   (con ';' al final)
            for (int i = 0; i < randomSalesCount; i++) {
                Producto p = catalogo.get(RNG.nextInt(catalogo.size()));
                int cantidad = 1 + RNG.nextInt(10); // 1..10
                bw.write(p.getId() + ";" + cantidad + ";");
                bw.newLine();
            }
        }
    }

    private static void prepararDirectorios() throws IOException {
        if (!Files.exists(OUT_DIR)) Files.createDirectories(OUT_DIR);
        if (!Files.exists(VENTAS_DIR)) Files.createDirectories(VENTAS_DIR);
    }

    private static String generarNumeroDocumento(Set<String> usados) {
        while (true) {
            int len = 8 + RNG.nextInt(3); // 8..10 dígitos
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < len; i++) sb.append(RNG.nextInt(10));
            String doc = sb.toString();
            if (usados.add(doc)) return doc;
        }
    }
}
