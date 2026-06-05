package com.sm.jeyz9.storemateapi.services;

import com.sm.jeyz9.storemateapi.dto.PaginationDTO;
import com.sm.jeyz9.storemateapi.dto.ReviewDTO;
import com.sm.jeyz9.storemateapi.dto.ReviewRequestDTO;


public interface ReviewService {
    PaginationDTO<ReviewDTO> getReviewsByProductId(Long productId, int page, int size);
    String addReview(Long productId, String userEmail, ReviewRequestDTO request);
    String updateReview(Long reviewId, String userEmail, ReviewRequestDTO request);
    String deleteReview(Long reviewId, String userEmail);
}