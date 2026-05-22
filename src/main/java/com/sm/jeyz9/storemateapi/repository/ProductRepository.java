package com.sm.jeyz9.storemateapi.repository;

import com.sm.jeyz9.storemateapi.dto.ProductModDTO;
import com.sm.jeyz9.storemateapi.models.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    @Query(value = """
        SELECT
            p.id,
            p.product_no,
            p.name AS product_name,
            c.name AS category,
            p.price,
            p.stock_quantity,
            ps.status
        FROM products p
        LEFT JOIN categories c ON c.id = p.category_id
        LEFT JOIN product_status ps ON ps.id = p.status_id
        ORDER BY p.updated_at DESC;
    """, nativeQuery = true)
    List<ProductModDTO> findAllProductModerator();
    
    @Query(value = """
        SELECT * FROM products WHERE name = :name;
    """, nativeQuery = true)
    Optional<Product> findByName(@Param("name") String name);
}
