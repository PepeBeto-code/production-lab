package com.example.demo;

import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    // Lista en memoria para simular una base de datos temporal
    private final List<Producto> listaProductos = new ArrayList<>();

    public ProductoController() {
        // Datos de prueba iniciales
        listaProductos.add(new Producto(1L, "Laptop", 1200.00));
        listaProductos.add(new Producto(2L, "Mouse Gamer", 35.50));
    }

    // GET: Obtener todos los productos -> http://localhost:8080/api/productos
    @GetMapping
    public List<Producto> obtenerTodos() {
        return listaProductos;
    }

    // GET por ID: Obtener un producto específico -> http://localhost:8080/api/productos/1
    @GetMapping("/{id}")
    public Producto obtenerPorId(@PathVariable Long id) {
        return listaProductos.stream()
                .filter(p -> p.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    // POST: Agregar un producto -> http://localhost:8080/api/productos
    @PostMapping
    public String guardarProducto(@RequestBody Producto nuevoProducto) {
        listaProductos.add(nuevoProducto);
        return "¡Producto '" + nuevoProducto.getNombre() + "' guardado exitosamente!";
    }
}
