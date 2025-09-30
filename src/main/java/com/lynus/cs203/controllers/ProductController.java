package com.lynus.cs203.controllers;

import com.lynus.cs203.entities.Product;
import com.lynus.cs203.repositories.ProductRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/products")
public class ProductController {
    private final ProductRepository productRepository;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @GetMapping
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @GetMapping("/search")
    public List<Product> search(@RequestParam String q) {
        return productRepository.findByDescriptionOrCode(q);
    }
}
