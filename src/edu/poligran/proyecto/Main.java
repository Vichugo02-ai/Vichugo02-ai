package edu.poligran.proyecto;

import edu.poligran.proyecto.model.Producto;
import edu.poligran.proyecto.model.Vendedor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ENTREGa 2 (Semana 5):
 * Lee los CSV de entrada y genera dos reportes:
 *  - data/reporte_vendedores.csv     (NombreCompleto;TotalRecaudado)  orden desc por total
 *  - data/reporte_productos.csv      (NombreProducto;Precio;Cantidad)  orden desc por cantidad
 *
 * Formatos de entrada esperados:
 *  - data/productos.csv:     ID;Nombre;Precio
 *  - data/vendedores.csv:    TipoDoc;NumeroDoc;Nombres;Apellidos
 *  - data/ventas/*.csv:
 *        Línea 1: TipoDocVendedor;NumeroDocVendedor
 *        Líneas siguientes: IDProducto;Cantidad;
 *
 * No solicita datos al usuario. Muestra mensajes de éxito o error.
 * Java 8.
 */
public class Main {

    // --- Rutas de trabajo (idénticas a Entrega 1) ---
    private static final Path BASE_DIR = Paths.get("data");
    private static final Path PRODUCTOS_FILE = BASE_DIR.resolve("productos.csv");
    private static final Path VENDEDORES_FILE = BASE_DIR.resolve("vendedores.csv");
    private static final Path VENTAS_DIR = BASE_DIR.resolve("ventas");

    private static final Path REPORTE_VENDEDORES = BASE_DIR.resolve("reporte_vendedores.csv");
    private static final Path REPORTE_PRODUCTOS = BASE_DIR.resolve("reporte_productos.csv");

    // --- Separador y números ---
    private static final String SEP = ";";
    private static final RoundingMode MONEY_RM = RoundingMode.HALF_UP;
    private static final int MONEY_SCALE = 2;

    public static void main(String[] args) {
        try {
            // 1) Cargar catálogos
            Map<String, Producto> productos = cargarProductos(PRODUCTOS_FILE);
            Map<String, Vendedor> vendedores = cargarVendedores(VENDEDORES_FILE);

            // 2) Recorrer archivos de ventas y acumular resultados
            Resultados acumulados = procesarVentas(VENTAS_DIR, productos, vendedores);

            // 3) Escribir reportes ordenados
            escribirReporteVendedores(REPORTE_VENDEDORES, acumulados.totalPorVendedor, vendedores);
            escribirReporteProductos(REPORTE_PRODUCTOS, acumulados.cantidadPorProducto, productos);

            System.out.println("✅ Reportes generados en: " + BASE_DIR.toAbsolutePath());
        } catch (Exception ex) {
            System.err.println("❌ Error al ejecutar Main: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // =========================================================================================
    // Lectura de entradas
    // =========================================================================================

    private static Map<String, Producto> cargarProductos(Path file) throws IOException {
        Map<String, Producto> out = new HashMap<>();
        try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split(";");
                if (parts.length < 3) {
                    System.err.println("⚠️  Línea inválida en productos: " + line);
                    continue;
                }
                String id = parts[0].trim();
                String nombre = parts[1].trim();
                BigDecimal precio;
                try {
                    precio = new BigDecimal(parts[2].trim()).setScale(MONEY_SCALE, MONEY_RM);
                } catch (NumberFormatException nfe) {
                    System.err.println("⚠️  Precio inválido para producto " + id + ": " + parts[2]);
                    continue;
                }
                if (precio.signum() < 0) {
                    System.err.println("⚠️  Precio negativo ignorado para producto " + id);
                    continue;
                }
                out.put(id, new Producto(id, nombre, precio));
            }
        }
        return out;
    }

    private static Map<String, Vendedor> cargarVendedores(Path file) throws IOException {
        Map<String, Vendedor> out = new HashMap<>();
        try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split(";");
                if (parts.length < 4) {
                    System.err.println("⚠️  Línea inválida en vendedores: " + line);
                    continue;
                }
                String tipo = parts[0].trim();
                String num = parts[1].trim();
                String nombres = parts[2].trim();
                String apellidos = parts[3].trim();

                Vendedor v = new Vendedor(tipo, num, nombres, apellidos);
                out.put(claveVendedor(tipo, num), v);
            }
        }
        return out;
    }

    // =========================================================================================
    // Procesamiento de ventas
    // =========================================================================================

    private static class Resultados {
        final Map<String, BigDecimal> totalPorVendedor = new HashMap<>(); // clave vendedor -> dinero
        final Map<String, Integer> cantidadPorProducto = new HashMap<>(); // id producto -> cantidad
    }

    private static Resultados procesarVentas(Path ventasDir,
                                             Map<String, Producto> productos,
                                             Map<String, Vendedor> vendedores) throws IOException {

        Resultados res = new Resultados();

        if (!Files.isDirectory(ventasDir)) {
            System.err.println("⚠️  La carpeta de ventas no existe: " + ventasDir.toAbsolutePath());
            return res;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(ventasDir, "*.csv")) {
            for (Path archivo : stream) {
                procesarArchivoVenta(archivo, productos, vendedores, res);
            }
        }
        return res;
    }

    private static void procesarArchivoVenta(Path archivo,
                                             Map<String, Producto> productos,
                                             Map<String, Vendedor> vendedores,
                                             Resultados res) throws IOException {
        List<String> lineas = Files.readAllLines(archivo, StandardCharsets.UTF_8);
        if (lineas.isEmpty()) {
            System.err.println("⚠️  Archivo de ventas vacío: " + archivo.getFileName());
            return;
        }

        // Cabecera: TipoDoc;NumeroDoc
        String cab = lineas.get(0).trim();
        String[] head = cab.split(";");
        if (head.length < 2) {
            System.err.println("⚠️  Cabecera inválida en " + archivo.getFileName() + ": " + cab);
            return;
        }
        String tipo = head[0].trim();
        String num = head[1].trim();
        String claveVend = claveVendedor(tipo, num);

        if (!vendedores.containsKey(claveVend)) {
            System.err.println("⚠️  Vendedor no encontrado para " + claveVend + " en " + archivo.getFileName());
            // Continuamos igual: registramos dinero en esa clave “desconocida”
        }

        // Ventas: ID;Cantidad;  (toleramos ; final)
        for (int i = 1; i < lineas.size(); i++) {
            String ln = lineas.get(i).trim();
            if (ln.isEmpty()) continue;

            // Si termina en ';' lo quitamos para dividir limpio
            if (ln.endsWith(SEP)) ln = ln.substring(0, ln.length() - 1);

            String[] parts = ln.split(";");
            if (parts.length < 2) {
                System.err.println("⚠️  Línea de venta inválida en " + archivo.getFileName() + ": " + lineas.get(i));
                continue;
            }
            String idProd = parts[0].trim();
            String cantStr = parts[1].trim();

            Producto p = productos.get(idProd);
            if (p == null) {
                System.err.println("⚠️  IDProducto inexistente (" + idProd + ") en " + archivo.getFileName());
                continue; // extra de validación
            }
            int cantidad;
            try {
                cantidad = Integer.parseInt(cantStr);
            } catch (NumberFormatException nfe) {
                System.err.println("⚠️  Cantidad inválida (" + cantStr + ") en " + archivo.getFileName());
                continue;
            }
            if (cantidad <= 0) {
                System.err.println("⚠️  Cantidad no positiva (" + cantidad + ") en " + archivo.getFileName());
                continue;
            }

            // Acumular dinero por vendedor
            BigDecimal subtotal = p.getPrecioUnitario().multiply(BigDecimal.valueOf(cantidad))
                    .setScale(MONEY_SCALE, MONEY_RM);
            res.totalPorVendedor.merge(claveVend, subtotal, BigDecimal::add);

            // Acumular cantidad por producto
            res.cantidadPorProducto.merge(idProd, cantidad, Integer::sum);
        }
    }

    // =========================================================================================
    // Escritura de reportes
    // =========================================================================================

    private static void escribirReporteVendedores(Path outFile,
                                                  Map<String, BigDecimal> totalPorVendedor,
                                                  Map<String, Vendedor> vendedores) throws IOException {

        List<Map.Entry<String, BigDecimal>> ordenado = totalPorVendedor.entrySet()
                .stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .collect(Collectors.toList());

        try (BufferedWriter bw = Files.newBufferedWriter(outFile, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

            // (Opcional) BOM para Excel:
            bw.write('\uFEFF');

            // Encabezado (opcional, no exigido por el enunciado):
            // bw.write("NombreCompleto;TotalRecaudado"); bw.newLine();

            for (Map.Entry<String, BigDecimal> e : ordenado) {
                String clave = e.getKey();
                BigDecimal total = e.getValue().setScale(MONEY_SCALE, MONEY_RM);

                Vendedor v = vendedores.get(clave);
                String nombreCompleto = (v == null)
                        ? clave // fallback: "CC;123..."
                        : (v.getNombres() + " " + v.getApellidos());

                bw.write(nombreCompleto + SEP + total.toPlainString());
                bw.newLine();
            }
        }
    }

    private static void escribirReporteProductos(Path outFile,
                                                 Map<String, Integer> cantidadPorProducto,
                                                 Map<String, Producto> productos) throws IOException {

        List<Map.Entry<String, Integer>> ordenado = cantidadPorProducto.entrySet()
                .stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .collect(Collectors.toList());

        try (BufferedWriter bw = Files.newBufferedWriter(outFile, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

            // (Opcional) BOM para Excel:
            bw.write('\uFEFF');

            // Encabezado (opcional):
            // bw.write("NombreProducto;PrecioUnitario;CantidadTotal"); bw.newLine();

            for (Map.Entry<String, Integer> e : ordenado) {
                String id = e.getKey();
                int cantidad = e.getValue();

                Producto p = productos.get(id);
                if (p == null) {
                    // Esto no debería ocurrir porque validamos antes, pero por seguridad:
                    System.err.println("⚠️  Producto " + id + " sin datos en catálogo.");
                    continue;
                }
                String precio = p.getPrecioUnitario().setScale(MONEY_SCALE, MONEY_RM).toPlainString();

                bw.write(p.getNombre() + SEP + precio + SEP + cantidad);
                bw.newLine();
            }
        }
    }

    // =========================================================================================
    // Utils
    // =========================================================================================

    private static String claveVendedor(String tipo, String num) {
        return (tipo == null ? "" : tipo.trim()) + ";" + (num == null ? "" : num.trim());
    }
}
