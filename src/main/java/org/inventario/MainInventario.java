package org.inventario;


import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.*;
        import org.xmldb.api.modules.*;

        import java.util.Scanner;

public class MainInventario {

    static String uriBase = "xmldb:exist://localhost:8080/exist/xmlrpc/db";
    static String usuario = "admin";
    static String password = "1234";

    public static void main(String[] args) {
        try {
            // Registrar el driver
            String driver = "org.exist.xmldb.DatabaseImpl";
            Class<?> cl = Class.forName(driver);
            Database database = (Database) cl.getDeclaredConstructor().newInstance();
            DatabaseManager.registerDatabase(database);

            System.out.println("Driver registrado correctamente\n");

            // Menú principal
            Scanner scanner = new Scanner(System.in);
            int opcion;

            do {
                System.out.println("\nGESTIÓN DE INVENTARIO");
                System.out.println("1. Crear colección inventario");
                System.out.println("2. Añadir producto");
                System.out.println("3. Ver todos los productos");
                System.out.println("4. Validar documento XML");
                System.out.println("5. Eliminar colección");
                System.out.println("6. Salir");
                System.out.print("Elige una opción: ");

                opcion = scanner.nextInt();
                scanner.nextLine(); // Limpiar buffer

                switch (opcion) {
                    case 1:
                        crearColeccion();
                        break;
                    case 2:
                        añadirProducto(scanner);
                        break;
                    case 3:
                        verProductos();
                        break;
                    case 4:
                        validarDocumento();
                        break;
                    case 5:
                        eliminarColeccion(scanner);
                        break;
                    case 6:
                        System.out.println("Adios");
                        break;
                    default:
                        System.out.println("Opción no válida");
                }
            } while (opcion != 6);

            scanner.close();

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Método para crear la colección inventario
    public static void crearColeccion() {
        try {
            Collection colRaiz = DatabaseManager.getCollection(uriBase, usuario, password);

            CollectionManagementService servicio = (CollectionManagementService)
                    colRaiz.getService("CollectionManagementService", "1.0");

            // Crear colección inventario
            servicio.createCollection("inventario");
            System.out.println("Colección 'inventario' creada correctamente");

            // Crear documento inicial vacío
            Collection colInventario = DatabaseManager.getCollection(uriBase + "/inventario", usuario, password);

            String xmlInicial = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<productos>\n" +
                    "</productos>";

            XMLResource resource = (XMLResource) colInventario.createResource("productos.xml", "XMLResource");
            resource.setContent(xmlInicial);
            colInventario.storeResource(resource);

            System.out.println("Documento 'productos.xml' creado correctamente");

            colInventario.close();
            colRaiz.close();

        } catch (Exception e) {
            System.out.println("Error al crear colección: " + e.getMessage());
        }
    }

    // Método para añadir un producto
    public static void añadirProducto(Scanner scanner) {
        try {
            System.out.print("ID del producto: ");
            String id = scanner.nextLine();
            System.out.print("Nombre del producto: ");
            String nombre = scanner.nextLine();
            System.out.print("Proveedor: ");
            String proveedor = scanner.nextLine();
            System.out.print("Unidades: ");
            int unidades = scanner.nextInt();
            System.out.print("Precio: ");
            double precio = scanner.nextDouble();
            scanner.nextLine(); // Limpiar buffer

            Collection col = DatabaseManager.getCollection(uriBase + "/inventario", usuario, password);
            XQueryService servicio = (XQueryService) col.getService("XQueryService", "1.0");

            String xquery = "let $doc := doc('/db/inventario/productos.xml')/productos " +
                    "let $nuevo := <producto>" +
                    "<id>" + id + "</id>" +
                    "<nombre>" + nombre + "</nombre>" +
                    "<proveedor>" + proveedor + "</proveedor>" +
                    "<unidades>" + unidades + "</unidades>" +
                    "<precio>" + precio + "</precio>" +
                    "</producto> " +
                    "return update insert $nuevo into $doc";

            servicio.query(xquery);
            System.out.println("¡Producto añadido correctamente!");

            col.close();

        } catch (Exception e) {
            System.out.println("Error al añadir producto: " + e.getMessage());
        }
    }

    // Método para ver todos los productos
    public static void verProductos() {
        try {
            Collection col = DatabaseManager.getCollection(uriBase + "/inventario", usuario, password);

            if (col != null) {
                XMLResource resource = (XMLResource) col.getResource("productos.xml");

                if (resource != null) {
                    System.out.println("\n LISTADO DE PRODUCTOS ");
                    System.out.println(resource.getContent().toString());
                } else {
                    System.out.println("No se encontró el documento productos.xml");
                }

                col.close();
            } else {
                System.out.println("No existe la colección inventario. Créala primero (opción 1)");
            }

        } catch (Exception e) {
            System.out.println("Error al ver productos: " + e.getMessage());
        }
    }

    // Método para validar el documento XML
    public static void validarDocumento() {
        try {
            Collection col = DatabaseManager.getCollection(uriBase + "/inventario", usuario, password);

            if (col != null) {
                XMLResource resource = (XMLResource) col.getResource("productos.xml");

                if (resource != null) {
                    String contenido = resource.getContent().toString();

                    // Validaciones básicas
                    boolean valido = true;
                    StringBuilder errores = new StringBuilder();

                    // Verificar que tiene la estructura correcta
                    if (!contenido.contains("<productos>")) {
                        valido = false;
                        errores.append("- Falta el elemento raíz <productos>\n");
                    }

                    if (!contenido.contains("</productos>")) {
                        valido = false;
                        errores.append("- Falta el cierre del elemento </productos>\n");
                    }

                    // Verificar estructura de productos
                    if (contenido.contains("<producto>")) {
                        if (!contenido.contains("<id>")) {
                            valido = false;
                            errores.append("- Algunos productos no tienen <id>\n");
                        }
                        if (!contenido.contains("<nombre>")) {
                            valido = false;
                            errores.append("- Algunos productos no tienen <nombre>\n");
                        }
                        if (!contenido.contains("<proveedor>")) {
                            valido = false;
                            errores.append("- Algunos productos no tienen <proveedor>\n");
                        }
                        if (!contenido.contains("<unidades>")) {
                            valido = false;
                            errores.append("- Algunos productos no tienen <unidades>\n");
                        }
                        if (!contenido.contains("<precio>")) {
                            valido = false;
                            errores.append("- Algunos productos no tienen <precio>\n");
                        }
                    }

                    if (valido) {
                        System.out.println("\n✓ El documento XML es válido");
                        System.out.println("Estructura correcta con los campos: id, nombre, proveedor, unidades, precio");
                    } else {
                        System.out.println("\n✗ El documento XML tiene errores:");
                        System.out.println(errores.toString());
                    }
                }

                col.close();
            } else {
                System.out.println("No existe la colección inventario. Créala primero (opción 1)");
            }

        } catch (Exception e) {
            System.out.println("Error al validar: " + e.getMessage());
        }
    }

    // Método para eliminar una colección
    public static void eliminarColeccion(Scanner scanner) {
        try {
            System.out.print("Nombre de la colección a eliminar: ");
            String nombreColeccion = scanner.nextLine();

            System.out.print("¿Estás seguro? (s/n): ");
            String confirmacion = scanner.nextLine();

            if (confirmacion.equalsIgnoreCase("s")) {
                Collection colRaiz = DatabaseManager.getCollection(uriBase, usuario, password);

                CollectionManagementService servicio = (CollectionManagementService)
                        colRaiz.getService("CollectionManagementService", "1.0");

                servicio.removeCollection(nombreColeccion);
                System.out.println("Colección '" + nombreColeccion + "' eliminada correctamente");

                colRaiz.close();
            } else {
                System.out.println("Operación cancelada");
            }

        } catch (Exception e) {
            System.out.println("Error al eliminar colección: " + e.getMessage());
        }
    }
}