package com.sm.jeyz9.storemateapi;

import com.sm.jeyz9.storemateapi.dto.PaginationDTO;
import com.sm.jeyz9.storemateapi.dto.ProductDTO;
import com.sm.jeyz9.storemateapi.dto.ProductWithCategoryDTO;
import com.sm.jeyz9.storemateapi.repository.CategoryRepository;
import com.sm.jeyz9.storemateapi.repository.ProductCategoryProjection;
import com.sm.jeyz9.storemateapi.repository.ProductImageRepository;
import com.sm.jeyz9.storemateapi.repository.ProductRepository;
import com.sm.jeyz9.storemateapi.repository.ProductStatusRepository;
import com.sm.jeyz9.storemateapi.repository.ReviewRepository;
import com.sm.jeyz9.storemateapi.services.impl.ProductServiceImpl;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductServiceTest {

    @Test
    void searchProductsPassesFiltersAndPaginationToRepository() {
        AtomicReference<Object[]> queryArgs = new AtomicReference<>();
        ProductCategoryProjection soapBeta = product(2L, "Soap Beta", "Soap", 18.0, "soap.png");
        ProductRepository repository = proxy(ProductRepository.class, (proxy, method, args) -> switch (method.getName()) {
            case "searchProducts" -> {
                queryArgs.set(args);
                yield List.of(soapBeta);
            }
            case "countSearchProducts" -> 2L;
            default -> null;
        });
        ProductServiceImpl service = service(repository);

        PaginationDTO<ProductDTO> result = service.searchProducts(
                " SOAP ", 7L, 10.0, 20.0, 1, 1
        );

        assertEquals("SOAP", queryArgs.get()[0]);
        assertEquals(7L, queryArgs.get()[1]);
        assertEquals(10.0, queryArgs.get()[2]);
        assertEquals(20.0, queryArgs.get()[3]);
        assertEquals(1, queryArgs.get()[4]);
        assertEquals(1L, queryArgs.get()[5]);
        assertEquals(2, result.getTotal());
        assertEquals(1, result.getPage());
        assertEquals(1, result.getSize());
        assertEquals(1, result.getData().size());
        assertEquals(2L, result.getData().getFirst().getId());
        assertEquals("Soap Beta", result.getData().getFirst().getProductName());
        assertEquals("soap.png", result.getData().getFirst().getImageUrl());
    }

    @Test
    void getProductsWithCategoryGroupsProjectionRowsIntoResponseCategories() {
        ProductRepository repository = proxy(ProductRepository.class, (proxy, method, args) ->
                method.getName().equals("findProductsForCategoryLandingPage")
                        ? List.of(
                                product(1L, "Promo", "Promotion", 10.0, "promotion.png"),
                                product(2L, "Soap", "Soap", 20.0, "soap.png"),
                                product(3L, "Drink", "Drinks", 30.0, "drinks.png"),
                                product(4L, "Shampoo", "Shampoo", 40.0, "shampoo.png"))
                        : null
        );
        ProductServiceImpl service = service(repository);

        ProductWithCategoryDTO result = service.getProductsWithCategory();

        assertEquals("Promo", result.getPromotion().getFirst().getProductName());
        assertEquals("Soap", result.getSoap().getFirst().getProductName());
        assertEquals("Drink", result.getDrinks().getFirst().getProductName());
        assertEquals("Shampoo", result.getShampoo().getFirst().getProductName());
        assertEquals("promotion.png", result.getPromotion().getFirst().getImageUrl());
    }

    private static ProductCategoryProjection product(
            Long id, String name, String category, Double price, String imageUrl) {
        return new ProductCategoryProjection() {
            @Override public Long getId() { return id; }
            @Override public String getProductName() { return name; }
            @Override public String getCategoryName() { return category; }
            @Override public String getDescription() { return "description"; }
            @Override public String getImageUrl() { return imageUrl; }
            @Override public Double getPrice() { return price; }
            @Override public LocalDateTime getCreatedAt() { return LocalDateTime.of(2026, 1, 1, 0, 0); }
            @Override public String getProductStatus() { return "ACTIVE"; }
            @Override public Boolean getDeleted() { return false; }
        };
    }

    private static ProductServiceImpl service(ProductRepository repository) {
        return new ProductServiceImpl(
                repository,
                proxy(ProductStatusRepository.class, (proxy, method, args) -> null),
                proxy(CategoryRepository.class, (proxy, method, args) -> null),
                null,
                proxy(ReviewRepository.class, (proxy, method, args) -> null),
                new ModelMapper(),
                proxy(ProductImageRepository.class, (proxy, method, args) -> null)
        );
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }
}
