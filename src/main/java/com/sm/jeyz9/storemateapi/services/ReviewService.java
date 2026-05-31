package com.sm.jeyz9.storemateapi.services;

import com.sm.jeyz9.storemateapi.dto.ReviewDTO;
import com.sm.jeyz9.storemateapi.dto.ReviewRequestDTO;

import java.util.List;

public interface ReviewService {
    List<ReviewDTO> getReviewsByProductId(Long productId);
    String addReview(Long productId, String userEmail, ReviewRequestDTO request);
    String updateReview(Long reviewId, String userEmail, ReviewRequestDTO request);
    String deleteReview(Long reviewId, String userEmail);
}