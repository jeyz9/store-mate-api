package com.sm.jeyz9.storemateapi;

import com.sm.jeyz9.storemateapi.dto.PaginationDTO;
import com.sm.jeyz9.storemateapi.dto.ProductDTO;
import com.sm.jeyz9.storemateapi.models.Category;
import com.sm.jeyz9.storemateapi.models.Product;
import com.sm.jeyz9.storemateapi.models.ProductImage;
import com.sm.jeyz9.storemateapi.models.ProductStatus;
import com.sm.jeyz9.storemateapi.repository.CategoryRepository;
import com.sm.jeyz9.storemateapi.repository.ProductImageRepository;
import com.sm.jeyz9.storemateapi.repository.ProductRepository;
import com.sm.jeyz9.storemateapi.repository.ProductStatusRepository;
import com.sm.jeyz9.storemateapi.repository.ReviewRepository;
import com.sm.jeyz9.storemateapi.services.SupabaseService;
import com.sm.jeyz9.storemateapi.services.impl.ProductServiceImpl;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductServiceTest {

    @Test
    void searchProducts_filtersProductsAndReturnsRequestedPage() {
        Category soap = Category.builder().id(7L).name("Soap").build();
        Category otherCategory = Category.builder().id(8L).name("Drinks").build();
        ProductStatus active = new ProductStatus(1L, "ACTIVE");
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 0, 0);

        List<Product> products = List.of(
                product(1L, "Soap Alpha", soap, 12.0, createdAt, active, false),
                product(2L, "Soap Beta", soap, 18.0, createdAt.plusDays(1), active, false),
                product(3L, "Soap Cheap", soap, 5.0, createdAt.plusDays(2), active, false),
                product(4L, "Soap Deleted", soap, 15.0, createdAt.plusDays(3), active, true),
                product(5L, "Soap Drink", otherCategory, 15.0, createdAt.plusDays(4), active, false)
        );
        ProductRepository productRepository = proxy(ProductRepository.class, (proxy, method, args) ->
                method.getName().equals("findAll") ? products : null
        );
        ProductServiceImpl service = new ProductServiceImpl(
                productRepository,
                proxy(ProductStatusRepository.class, (proxy, method, args) -> null),
                proxy(CategoryRepository.class, (proxy, method, args) -> null),
                null,
                proxy(ReviewRepository.class, (proxy, method, args) -> null),
                new ModelMapper(),
                proxy(ProductImageRepository.class, (proxy, method, args) -> null)
        );

        PaginationDTO<ProductDTO> result = service.searchProducts(
                "SOAP", 7L, 10.0, 20.0, 1, 1
        );

        assertEquals(2, result.getTotal());
        assertEquals(1, result.getPage());
        assertEquals(1, result.getSize());
        assertEquals(1, result.getData().size());
        assertEquals(2L, result.getData().getFirst().getId());
        assertEquals("Soap Beta", result.getData().getFirst().getProductName());
    }

    private static Product product(
            Long id,
            String name,
            Category category,
            Double price,
            LocalDateTime createdAt,
            ProductStatus status,
            boolean deleted
    ) {
        return Product.builder()
                .id(id)
                .name(name)
                .category(category)
                .price(price)
                .productStatus(status)
                .productImage(List.<ProductImage>of())
                .createdAt(createdAt)
                .deleted(deleted)
                .build();
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }
}
