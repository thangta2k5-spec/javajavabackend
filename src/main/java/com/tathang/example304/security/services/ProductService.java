package com.tathang.example304.security.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.tathang.example304.model.Category;
import com.tathang.example304.model.Product;
import com.tathang.example304.payload.request.ProductRequest;
import com.tathang.example304.repository.CategoryRepository;
import com.tathang.example304.repository.ProductRepository;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final FileStorageService fileStorageService;

    public ProductService(ProductRepository productRepository,
            CategoryRepository categoryRepository,
            FileStorageService fileStorageService) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.fileStorageService = fileStorageService;
    }

    // === CẬP NHẬT PRODUCT TỪ FORM-DATA ===
    public Product updateProductFromForm(Long id, String name, String description, BigDecimal price,
            Long categoryId, Integer stockQuantity, String imageUrl, Boolean active, // THÊM stockQuantity
            MultipartFile imageFile) throws IOException {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy product với ID: " + id));

        System.out.println("=== SERVICE UPDATE DEBUG ===");
        System.out.println("Updating product: " + product.getName());
        System.out.println("New imageFile: " + (imageFile != null ? imageFile.getOriginalFilename() : "null"));
        System.out.println("New stockQuantity: " + stockQuantity); // DEBUG

        // Lưu file mới nếu có
        String savedImageUrl = product.getImageUrl();
        if (imageFile != null && !imageFile.isEmpty()) {
            // Xóa file cũ nếu có
            if (savedImageUrl != null && !savedImageUrl.isEmpty()) {
                fileStorageService.deleteFile(savedImageUrl);
            }
            String fileName = fileStorageService.storeFile(imageFile);
            savedImageUrl = fileName;
            System.out.println("✅ Đã lưu ảnh mới: " + savedImageUrl);
        } else if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            savedImageUrl = imageUrl;
        }

        // Cập nhật các field
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setImageUrl(savedImageUrl);

        // Cập nhật stockQuantity
        if (stockQuantity != null) {
            product.setStockQuantity(stockQuantity);
        }

        if (active != null) {
            product.setActive(active);
        }

        // Cập nhật category nếu có
        if (categoryId != null) {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy category với ID: " + categoryId));
            product.setCategory(category);
        }

        System.out.println("💾 Saving updated product...");
        return productRepository.save(product);
    }

    // === XÓA VĨNH VIỄN PRODUCT ===
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy product với ID: " + id));

        // Xóa ảnh nếu có
        if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
            try {
                fileStorageService.deleteFile(product.getImageUrl());
            } catch (Exception e) {
                System.out.println("⚠️ Không thể xóa file ảnh: " + e.getMessage());
            }
        }

        productRepository.delete(product);
    }

    // === LẤY TẤT CẢ PRODUCTS ===
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    // === LẤY PRODUCTS ĐANG ACTIVE (không bị Active) ===
    public List<Product> getActiveProducts() {
        return productRepository.findByActiveTrue();
    }

    // === LẤY PRODUCT THEO ID ===
    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    // === LẤY PRODUCTS THEO CATEGORY ===
    public List<Product> getProductsByCategory(Long categoryId) {
        return productRepository.findByCategoryId(categoryId);
    }

    // === TÌM KIẾM PRODUCT THEO TÊN ===
    public List<Product> searchProducts(String keyword) {
        return productRepository.findByNameContainingIgnoreCase(keyword);
    }

    // === LẤY PRODUCTS HẾT HÀNG ===
    public List<Product> getOutOfStockProducts() {
        return productRepository.findByStockQuantityLessThan(1);
    }

    // === CẬP NHẬT SỐ LƯỢNG TỒN KHO ===
    public Product updateStockQuantity(Long id, Integer quantity) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy product với ID: " + id));

        product.setStockQuantity(quantity);
        return productRepository.save(product);
    }

    // === TOGGLE ACTIVE STATUS ===
    public Product toggleProductStatus(Long id, Boolean active) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy product"));

        product.setActive(active);
        return productRepository.save(product);
    }

    public Product createProduct(ProductRequest request) throws IOException {

        String savedImageUrl = request.getImageUrl();
        MultipartFile imageFile = request.getImage();

        if (imageFile != null && !imageFile.isEmpty()) {
            String fileName = fileStorageService.storeFile(imageFile);
            savedImageUrl = fileName;
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy category"));

        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setCategory(category);
        product.setImageUrl(savedImageUrl);
        product.setStockQuantity(
                request.getStockQuantity() != null ? request.getStockQuantity() : 0);
        product.setActive(
                request.getActive() != null ? request.getActive() : true);

        return productRepository.save(product);
    }

}