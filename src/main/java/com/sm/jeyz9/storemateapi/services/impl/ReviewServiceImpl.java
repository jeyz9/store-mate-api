package com.sm.jeyz9.storemateapi.services.impl;

import com.sm.jeyz9.storemateapi.dto.ReviewDTO;
import com.sm.jeyz9.storemateapi.dto.ReviewRequestDTO;
import com.sm.jeyz9.storemateapi.dto.ReviewerDTO;
import com.sm.jeyz9.storemateapi.exceptions.WebException;
import com.sm.jeyz9.storemateapi.models.OrderItem;
import com.sm.jeyz9.storemateapi.models.OrderStatusName;
import com.sm.jeyz9.storemateapi.models.Product;
import com.sm.jeyz9.storemateapi.models.Review;
import com.sm.jeyz9.storemateapi.models.User;
import com.sm.jeyz9.storemateapi.repository.OrderItemRepository;
import com.sm.jeyz9.storemateapi.repository.ProductRepository;
import com.sm.jeyz9.storemateapi.repository.ReviewRepository;
import com.sm.jeyz9.storemateapi.repository.UserRepository;
import com.sm.jeyz9.storemateapi.services.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Autowired
    public ReviewServiceImpl(ReviewRepository reviewRepository, OrderItemRepository orderItemRepository, UserRepository userRepository,ProductRepository productRepository) {
        this.reviewRepository = reviewRepository;
        this.orderItemRepository = orderItemRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    @Override
    public List<ReviewDTO> getReviewsByProductId(Long productId) {
        try{
        productRepository.findById(productId)
                .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "Product not found."));

        return reviewRepository.findAllByProductId(productId)
                .stream()
                .map(review -> ReviewDTO.builder()
                        .id(review.getId())
                        .reviewer(ReviewerDTO.builder()
                                .id(review.getReviewer().getId())
                                .name(review.getReviewer().getName())
                                .build())
                        .reviewScore(review.getReviewScore())
                        .message(review.getMessage())
                        .createdAt(review.getCreatedAt())
                        .orderNo(review.getOrderItem().getOrder().getOrderNo())
                        .build())
                .toList();
        } catch (WebException e) {
            throw e;
        }catch (Exception e) {
            throw new WebException(HttpStatus.INTERNAL_SERVER_ERROR, "Server error: " + e.getMessage());
        }
    }


    @Override
    @Transactional
    public String addReview(Long orderItemId, String userEmail, ReviewRequestDTO request) {
        try{
        User user = userRepository.findUserByEmail(userEmail)
                .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "User not found."));

        OrderItem orderItem = orderItemRepository.findByIdAndOrderUser(orderItemId, user)
                .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "Order item not found."));

        if (orderItem.getOrder().getStatus() != OrderStatusName.COMPLETED) {
            throw new WebException(HttpStatus.BAD_REQUEST, "You can only review items from completed orders.");
        }

        if (reviewRepository.existsByOrderItemId(orderItemId)) {
            throw new WebException(HttpStatus.CONFLICT, "You have already reviewed this item.");
        }

        Review review = Review.builder()
                .reviewer(user)
                .product(orderItem.getProduct())
                .orderItem(orderItem)
                .reviewScore(request.getReviewScore())
                .message(request.getMessage())
                .createdAt(LocalDateTime.now())
                .build();

        reviewRepository.save(review);
        return "Review added successfully.";
    } catch (WebException e) {
        throw e;
    }catch (Exception e) {
        throw new WebException(HttpStatus.INTERNAL_SERVER_ERROR, "Server error: " + e.getMessage());
    }
}

    @Override
    @Transactional
    public String updateReview(Long reviewId, String userEmail, ReviewRequestDTO request) {
        try{
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "Review not found."));

        // ตรวจสอบว่าเป็นเจ้าของรีวิวหรือไม่
        if (!review.getReviewer().getEmail().equals(userEmail)) {
            throw new WebException(HttpStatus.FORBIDDEN, "You do not have permission to update this review.");
        }

        review.setReviewScore(request.getReviewScore());
        review.setMessage(request.getMessage());
        // อาจจะมีการเก็บ updatedAt เพิ่มเติมถ้าใน Entity มี

        reviewRepository.save(review);
        return "Review updated successfully.";
        } catch (WebException e) {
            throw e;
        }catch (Exception e) {
            throw new WebException(HttpStatus.INTERNAL_SERVER_ERROR, "Server error: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public String deleteReview(Long reviewId, String userEmail) {
        try{
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "Review not found."));

        // ตรวจสอบว่าเป็นเจ้าของรีวิวหรือไม่ (อาจจะอนุญาตให้ Role ADMIN ลบได้ด้วยก็ได้)
        if (!review.getReviewer().getEmail().equals(userEmail)) {
            throw new WebException(HttpStatus.FORBIDDEN, "You do not have permission to delete this review.");
        }

        reviewRepository.delete(review);
        return "Review deleted successfully.";
        } catch (WebException e) {
            throw e;
        }catch (Exception e) {
            throw new WebException(HttpStatus.INTERNAL_SERVER_ERROR, "Server error: " + e.getMessage());
        }
    }
}