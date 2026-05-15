package com.sm.jeyz9.storemateapi.services.impl;

import com.sm.jeyz9.storemateapi.dto.OrderAddressDTO;
import com.sm.jeyz9.storemateapi.dto.OrderDTO;
import com.sm.jeyz9.storemateapi.dto.OrderDetailsDTO;
import com.sm.jeyz9.storemateapi.dto.OrderItemDTO;
import com.sm.jeyz9.storemateapi.dto.OrderRecipientDTO;
import com.sm.jeyz9.storemateapi.exceptions.WebException;
import com.sm.jeyz9.storemateapi.models.Order;
import com.sm.jeyz9.storemateapi.models.OrderStatusHistory;
import com.sm.jeyz9.storemateapi.models.OrderStatusName;
import com.sm.jeyz9.storemateapi.models.ProductImage;
import com.sm.jeyz9.storemateapi.models.User;
import com.sm.jeyz9.storemateapi.repository.OrderRepository;
import com.sm.jeyz9.storemateapi.repository.OrderStatusHistoryRepository;
import com.sm.jeyz9.storemateapi.repository.UserRepository;
import com.sm.jeyz9.storemateapi.services.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

@Service
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Autowired
    public OrderServiceImpl(OrderRepository orderRepository, UserRepository userRepository, OrderStatusHistoryRepository orderStatusHistoryRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.orderStatusHistoryRepository = orderStatusHistoryRepository;
    }

    @Override
    public List<OrderDTO> getUserOrders(OrderStatusName status, String email) {
        try {
            User user = userRepository.findUserByEmail(email)
                    .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "User not found"));

            List<Order> orders = orderRepository.findAllByUser(user);
            Stream<Order> filtered = orders.stream();
            if(!status.equals(OrderStatusName.ALL)){
                filtered = filtered.filter(o -> o.getStatus().equals(status));
            }

            return mapToDTO(filtered.toList());

        } catch (Exception e) {
            throw new WebException(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error: " + e.getMessage());
        }
    }
    
    @Override
    public String getOrderStatus(String orderNo) {
        return orderRepository.findOrderStatusByOrderNo(orderNo);
    }

    @Override
    public OrderDetailsDTO getOrderDetails(String email, String orderNo) {
        try {
            User user = userRepository.findUserByEmail(email).orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "User not found"));
            Order order = orderRepository.findOrderByOrderNoAndUserId(orderNo, user.getId()).orElseThrow(() -> new WebException(HttpStatus.FORBIDDEN, "This order does not belong to you"));
            return mapToOrderDetailsDTO(order);
        }catch (WebException e) {
            throw e;
        }catch (Exception e) {
            throw new WebException(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error: " + e.getMessage());
        }
    }

    @Override
    public void getAllOrders() {

    }

    @Override
    public void getOrderDetailsByModerator() {

    }

    @Override
    @Transactional
    public String changeOrderStatus(String orderNo, String status, String email) {
        User user = userRepository.findUserByEmail(email).orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "User not found"));
        Order order = orderRepository.findOneByOrderNo(orderNo).orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "Order not found"));
        OrderStatusName statusName = OrderStatusName.valueOf(status);
        order.setStatus(statusName);
        orderRepository.save(order);

        OrderStatusHistory orderStatusHistory = OrderStatusHistory.builder()
                .id(null)
                .user(user)
                .order(order)
                .status(statusName)
                .createdAt(LocalDateTime.now())
                .build();
        
        orderStatusHistoryRepository.save(orderStatusHistory);
        return "Update status successfully";
    }

    @Override
    public void printShippingLabel() {

    }
    
    private OrderDetailsDTO mapToOrderDetailsDTO(Order order) {
        List<OrderItemDTO> orderItemDTO = order.getOrderItems().stream().map(
                oi -> OrderItemDTO.builder()
                        .id(oi.getId())
                        .productName(oi.getProduct().getName())
                        .imageUrl(oi.getProduct().getProductImage().stream().findFirst().map(ProductImage::getImageUrl).orElse(null))
                        .price(oi.getProduct().getPrice())
                        .quantity(oi.getQuantity())
                        .subTotal(oi.getProduct().getPrice() * oi.getQuantity())
                        .build()
        ).toList();
        OrderRecipientDTO orderRecipientDTO = order.getOrderAddresses().stream().findFirst().map(oa ->
                OrderRecipientDTO.builder()
                        .recipientName(order.getUser().getName())
                        .phone(order.getUser().getPhone())
                        .streetAddress(oa.getStreetAddress())
                        .subdistrict(oa.getZipcode().getSubdistrict().getName())
                        .district(oa.getZipcode().getDistrict().getName())
                        .province(oa.getZipcode().getProvince().getName())
                        .zipcode(oa.getZipcode().getZipcode())
                        .build()
        ).orElse(null);
        return OrderDetailsDTO.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .status(order.getStatus().name())
                .orderItems(orderItemDTO)
                .orderRecipient(orderRecipientDTO)
                .total(order.getOrderItems().stream().mapToDouble(oi -> oi.getQuantity() * oi.getProduct().getPrice()).sum())
                .checkoutType(order.getCheckoutType() != null ? order.getCheckoutType().name() : null)
                .build();
    }
    
    private List<OrderDTO> mapToDTO(List<Order> order) {
        return order.stream().map(
                o -> OrderDTO.builder()
                        .id(o.getId())
                        .orderNo(o.getOrderNo())
                        .orderItems(o.getOrderItems().stream().map(oi -> OrderItemDTO.builder()
                                        .id(oi.getId())
                                        .imageUrl(oi.getProduct().getProductImage().stream().findFirst().map(ProductImage::getImageUrl).orElse(null))
                                        .productName(oi.getProduct().getName())
                                        .price(oi.getProduct().getPrice())
                                        .quantity(oi.getQuantity())
                                        .subTotal(oi.getProduct().getPrice() * oi.getQuantity())
                                        .build()
                                ).toList()
                        )
                        .orderAddress(o.getOrderAddresses().stream().map(oa -> OrderAddressDTO.builder()
                                .id(oa.getId())
                                .streetAddress(oa.getStreetAddress())
                                .subdistrict(oa.getZipcode().getSubdistrict().getName())
                                .district(oa.getZipcode().getDistrict().getName())
                                .province(oa.getZipcode().getProvince().getName())
                                .zipcode(oa.getZipcode().getZipcode())
                                .build()).toList())
                        .status(o.getStatus().toString())
                        .checkoutType(o.getCheckoutType() != null ? o.getCheckoutType().name() : null)
                        .total(o.getOrderItems().stream().mapToDouble(oi -> oi.getQuantity() * oi.getProduct().getPrice()).sum())
                        .createdAt(o.getCreatedAt())
                        .build()
        ).toList();
    }
}
