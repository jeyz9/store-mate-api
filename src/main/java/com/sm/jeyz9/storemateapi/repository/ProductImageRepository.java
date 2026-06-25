package com.sm.jeyz9.storemateapi.repository;

import com.sm.jeyz9.storemateapi.models.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    @Query(value = """
        SELECT * FROM product_images pi WHERE pi.product_id = :productId;
    """, nativeQuery = true)
    List<ProductImage> findAllByProductId(@Param("productId") Long productId);
    
    @Query(value = """
        SELECT * FROM product_images WHERE id = :imageId AND product_id = :productId
    """, nativeQuery = true)
    Optional<ProductImage> findProductImageByIdAndProductId(@Param("imageId") Long imageId, @Param("productId") Long productId);
}
