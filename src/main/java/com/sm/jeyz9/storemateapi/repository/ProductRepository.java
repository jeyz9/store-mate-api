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
        WITH ranked_products AS (
            SELECT
                p.id AS id,
                p.name AS "productName",
                c.name AS "categoryName",
                p.description AS description,
                pi.image_url AS "imageUrl",
                p.price AS price,
                p.created_at AS "createdAt",
                ps.status AS "productStatus",
                p.is_deleted AS "deleted",
                ROW_NUMBER() OVER (
                    PARTITION BY LOWER(c.name)
                    ORDER BY p.created_at ASC, p.id ASC
                ) AS category_rank
            FROM products p
            JOIN categories c ON c.id = p.category_id
            LEFT JOIN product_status ps ON ps.id = p.status_id
            LEFT JOIN LATERAL (
                SELECT image_url
                FROM product_images
                WHERE product_id = p.id
                ORDER BY id
                LIMIT 1
            ) pi ON TRUE
            WHERE p.is_deleted = FALSE
              AND LOWER(c.name) IN ('promotion', 'soap', 'drinks', 'shampoo')
        )
        SELECT id, "productName", "categoryName", description, "imageUrl", price,
               "createdAt", "productStatus", "deleted"
        FROM ranked_products
        WHERE category_rank <= 4
        ORDER BY "createdAt" ASC, id ASC
        """, nativeQuery = true)
    List<ProductCategoryProjection> findProductsForCategoryLandingPage();

    @Query(value = """
        SELECT p.id AS id,
               p.name AS "productName",
               c.name AS "categoryName",
               p.description AS description,
               pi.image_url AS "imageUrl",
               p.price AS price,
               p.created_at AS "createdAt",
               ps.status AS "productStatus",
               p.is_deleted AS "deleted"
        FROM products p
        LEFT JOIN categories c ON c.id = p.category_id
        LEFT JOIN product_status ps ON ps.id = p.status_id
        LEFT JOIN LATERAL (
            SELECT image_url
            FROM product_images
            WHERE product_id = p.id
            ORDER BY id
            LIMIT 1
        ) pi ON TRUE
        WHERE p.is_deleted = FALSE
          AND (:keyword = '' OR LOWER(p.name) LIKE CONCAT('%', LOWER(:keyword), '%'))
          AND (:categoryId IS NULL OR p.category_id = :categoryId)
          AND (:minPrice IS NULL OR p.price >= :minPrice)
          AND (:maxPrice IS NULL OR p.price <= :maxPrice)
        ORDER BY p.created_at ASC, p.id ASC
        LIMIT :limit OFFSET :offset
        """, nativeQuery = true)
    List<ProductCategoryProjection> searchProducts(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice,
            @Param("limit") int limit,
            @Param("offset") long offset);

    @Query(value = """
        SELECT COUNT(*)
        FROM products p
        WHERE p.is_deleted = FALSE
          AND (:keyword = '' OR LOWER(p.name) LIKE CONCAT('%', LOWER(:keyword), '%'))
          AND (:categoryId IS NULL OR p.category_id = :categoryId)
          AND (:minPrice IS NULL OR p.price >= :minPrice)
          AND (:maxPrice IS NULL OR p.price <= :maxPrice)
        """, nativeQuery = true)
    long countSearchProducts(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice);

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
