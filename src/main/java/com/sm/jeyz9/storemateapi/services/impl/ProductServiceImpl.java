package com.sm.jeyz9.storemateapi.services.impl;

import com.sm.jeyz9.storemateapi.dto.PaginationDTO;
import com.sm.jeyz9.storemateapi.dto.ProductDTO;
import com.sm.jeyz9.storemateapi.dto.ProductDetailsDTO;
import com.sm.jeyz9.storemateapi.dto.ProductImageDTO;
import com.sm.jeyz9.storemateapi.dto.ProductRequestDTO;
import com.sm.jeyz9.storemateapi.dto.ProductWithCategoryDTO;
import com.sm.jeyz9.storemateapi.dto.ReviewDTO;
import com.sm.jeyz9.storemateapi.dto.ReviewerDTO;
import com.sm.jeyz9.storemateapi.exceptions.WebException;
import com.sm.jeyz9.storemateapi.models.Category;
import com.sm.jeyz9.storemateapi.models.Product;
import com.sm.jeyz9.storemateapi.models.ProductImage;
import com.sm.jeyz9.storemateapi.models.ProductStatus;
import com.sm.jeyz9.storemateapi.models.ProductStatusName;
import com.sm.jeyz9.storemateapi.models.Review;
import com.sm.jeyz9.storemateapi.repository.CategoryRepository;
import com.sm.jeyz9.storemateapi.repository.ProductImageRepository;
import com.sm.jeyz9.storemateapi.repository.ProductRepository;
import com.sm.jeyz9.storemateapi.repository.ProductStatusRepository;
import com.sm.jeyz9.storemateapi.repository.ReviewRepository;
import com.sm.jeyz9.storemateapi.services.ProductService;
import com.sm.jeyz9.storemateapi.services.SupabaseService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
public class ProductServiceImpl implements ProductService {
    
    private final ProductRepository productRepository;
    private final ProductStatusRepository productStatusRepository;
    private final CategoryRepository categoryRepository;
    private final SupabaseService supabaseService;
    private final ReviewRepository reviewRepository;
    private final ModelMapper modelMapper;
    private final ProductImageRepository productImageRepository;

    @Autowired
    public ProductServiceImpl(ProductRepository productRepository,
                              ProductStatusRepository productStatusRepository,
                              CategoryRepository categoryRepository,
                              SupabaseService supabaseService,
                              ReviewRepository reviewRepository,
                              ModelMapper modelMapper, ProductImageRepository productImageRepository) {
        this.productRepository = productRepository;
        this.productStatusRepository = productStatusRepository;
        this.categoryRepository = categoryRepository;
        this.supabaseService = supabaseService;
        this.reviewRepository = reviewRepository;
        this.modelMapper = modelMapper;
        this.productImageRepository = productImageRepository;
    }
    
    @Override
    @Transactional
    public String addProduct(ProductRequestDTO request, List<MultipartFile> files) {
        try{
            ProductStatus status = productStatusRepository.findById(request.getStatusId()).orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "Product Status not found."));
            Category category = categoryRepository.findById(request.getCategoryId()).orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "Category not found."));
            Product product = Product.builder()
                    .id(null)
                    .name(request.getProductName())
                    .description(request.getDescription())
                    .productStatus(status)
                    .category(category)
                    .price(request.getPrice())
                    .stock_quantity(request.getStockQuantity())
                    .updatedAt(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .build();
            productRepository.save(product);
            
            supabaseService.saveProductImages(product.getId(), files);
            
            return "Add product success.";
        }catch(WebException e) {
            throw e;
        }
        catch (Exception e) {
            throw new WebException(HttpStatus.INTERNAL_SERVER_ERROR, "Server error " + e.getMessage());
        }
    }
    
    @Override
    @Transactional
    public ProductWithCategoryDTO getProductsWithCategory(){
        try {
            List<Product> productList = productRepository.findAll();
            List<ProductDTO> products = mapToProductDTO(productList);
            products = products.stream().filter(a -> a.getStatus().equalsIgnoreCase(ProductStatusName.ACTIVE.toString())).toList();
            return ProductWithCategoryDTO.builder()
                    .promotion(
                            products.stream().filter(pp -> pp.getCategoryName().equalsIgnoreCase("Promotion")).limit(4).toList()
                    )
                    .soap(
                            products.stream().filter(pp -> pp.getCategoryName().equalsIgnoreCase("Soap")).limit(4).toList()
                    )
                    .drinks(
                            products.stream().filter(pp -> pp.getCategoryName().equalsIgnoreCase("Drinks")).limit(4).toList()
                    )
                    .shampoo(
                            products.stream().filter(pp -> pp.getCategoryName().equalsIgnoreCase("Shampoo")).limit(4).toList()
                    )
                    .build();
        }catch (Exception e) {
            throw new WebException(HttpStatus.INTERNAL_SERVER_ERROR, "Server error: " + e.getMessage());
        }
    }

    @Override
    public PaginationDTO<ProductDTO> searchProducts(String keyword, Long categoryId, Double minPrice, Double maxPrice, int page, int size) {
        try{
            List<Product> productList = productRepository.findAll();
            Stream<Product> stream = productList.stream();
            
            if(keyword != null && !keyword.trim().isEmpty()){
                stream = stream.filter(p -> p.getName().toLowerCase().contains(keyword.toLowerCase()));
            }

            if(categoryId != null){
                stream = stream.filter(p -> p.getCategory().getId().equals(categoryId));
            }
            
            if(minPrice != null) {
                stream = stream.filter(p -> p.getPrice() >= minPrice);
            }

            if(maxPrice != null) {
                stream = stream.filter(p -> p.getPrice() <= maxPrice);
            }
            
            List<ProductDTO> products = mapToProductDTO(stream.toList());
            Pageable pageable = PageRequest.of(page, size);
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), products.size());
            int total = products.size();
            
            List<ProductDTO> paginatedList = products.subList(start, end);
            Page<ProductDTO> productsPage = new PageImpl<>(paginatedList, pageable, products.size());
            
            PaginationDTO<ProductDTO> productPagination = new PaginationDTO<>();
            productPagination.setData(productsPage.getContent());
            productPagination.setPage(page);
            productPagination.setSize(size);
            productPagination.setTotal(total);
            return productPagination;
        } catch (Exception e) {
            throw new WebException(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error: " + e.getMessage());
        }
    }
    
    @Override
    public ProductDetailsDTO getProductDetails(Long id){
        try{
            Product product = productRepository.findById(id).orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "Product not found."));
            List<Review> reviews = reviewRepository.findAllByProductId(product.getId());
            List<ProductImage> productImages = productImageRepository.findAllByProductId(product.getId());
            Float ratingScore = reviewRepository.findRatingByProductId(product.getId());
            
            return ProductDetailsDTO.builder()
                    .id(product.getId())
                    .productImages(mapToProductImageDTO(productImages))
                    .productName(product.getName())
                    .description(product.getDescription())
                    .quantity(product.getStock_quantity())
                    .price(product.getPrice())
                    .RatingScore(ratingScore)
                    .reviews(mapToReviewDTO(reviews))
                    .build();
        } catch (Exception e) {
            throw new WebException(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error: " + e.getMessage());
        }
    }

    private List<ProductDTO> mapToProductDTO(List<Product> products) {
        return products.stream().map(p -> 
                    ProductDTO.builder()
                            .id(p.getId())
                            .productName(p.getName())
                            .categoryName(p.getCategory().getName())
                            .description(p.getDescription())
                            .imageUrl(p.getProductImage().stream().findFirst().map(ProductImage::getImageUrl).orElse(null))
                            .status(p.getProductStatus().getStatus())
                            .price(p.getPrice())
                            .createdAt(p.getCreatedAt())
                            .build()
        ).sorted(Comparator.comparing(ProductDTO::getCreatedAt)).toList();
    }
    
    private List<ReviewDTO> mapToReviewDTO(List<Review> reviews) {
        return reviews.stream().map(r -> 
                    ReviewDTO.builder()
                            .id(r.getId())
                            .reviewer(modelMapper.map(r.getReviewer(), ReviewerDTO.class))
                            .reviewScore(r.getReviewScore())
                            .message(r.getMessage())
                            .createdAt(r.getCreatedAt())
                            .build()
                ).toList();
    }
    
    private List<ProductImageDTO> mapToProductImageDTO(List<ProductImage> productImages) {
        return productImages.stream().map(pi -> modelMapper.map(pi, ProductImageDTO.class)).toList();
    }
}
