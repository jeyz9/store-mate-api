package com.sm.jeyz9.storemateapi.services.impl;

import com.sm.jeyz9.storemateapi.dto.OrderAddressDTO;
import com.sm.jeyz9.storemateapi.dto.OrderDTO;
import com.sm.jeyz9.storemateapi.dto.OrderDetailsDTO;
import com.sm.jeyz9.storemateapi.dto.OrderItemDTO;
import com.sm.jeyz9.storemateapi.dto.OrderModDTO;
import com.sm.jeyz9.storemateapi.dto.OrderModDetailsDTO;
import com.sm.jeyz9.storemateapi.dto.OrderRecipientDTO;
import com.sm.jeyz9.storemateapi.dto.OrderStatusHistoryDTO;
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
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final ModelMapper modelMapper;

    @Autowired
    public OrderServiceImpl(OrderRepository orderRepository, UserRepository userRepository, OrderStatusHistoryRepository orderStatusHistoryRepository, ModelMapper modelMapper) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.orderStatusHistoryRepository = orderStatusHistoryRepository;
        this.modelMapper = modelMapper;
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
    public List<OrderModDTO> getAllOrders(String keyword, LocalDate startDate, LocalDate endDate, String period) {
        List<Order> orders = orderRepository.findAll();
        Stream<Order> stream = orders.stream();
        if(keyword != null && !keyword.trim().isBlank()) {
            stream = stream.filter(o -> o.getUser().getName().toLowerCase().contains(keyword.toLowerCase()) || o.getUser().getPhone().equals(keyword));
        }
        
        if(startDate != null) {
            stream = stream.filter(o -> !o.getCreatedAt().isBefore(startDate.atStartOfDay()));
        }
        
        if(endDate != null) {
            stream = stream.filter(o -> !o.getCreatedAt().isAfter(endDate.atTime(LocalTime.MAX)));
        }
        
        if(period != null && !period.trim().isBlank()) {
            switch (period.toUpperCase()) {
                case "TODAY" -> {
                    LocalDate today = LocalDate.now();
                    stream = stream.filter(o -> o.getCreatedAt().toLocalDate().isEqual(today));
                }
                
                case "WEEK" -> {
                    LocalDateTime startOfWeek = LocalDate.now().with(DayOfWeek.MONDAY).atStartOfDay();
                    stream = stream.filter(o -> !o.getCreatedAt().isBefore(startOfWeek));
                }
                
                case "MONTH" -> {
                    LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
                    stream = stream.filter(o -> !o.getCreatedAt().isBefore(startOfMonth));
                }
                
                default -> throw new WebException(HttpStatus.BAD_REQUEST, "Invalid period");
            }
        }
        
        return mapToOrderModDTO(stream.sorted(Comparator.comparing(Order::getCreatedAt).reversed()).toList());
    }

    @Override
    public OrderModDetailsDTO getOrderDetailsByModerator(String orderNo) {
        Order order = orderRepository.findOneByOrderNo(orderNo).orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "Order not found"));
        List<OrderStatusHistory> orderHistory = orderStatusHistoryRepository.findByOrderId(order.getId());
        OrderModDetailsDTO orderModDetails = modelMapper.map(mapToOrderDetailsDTO(order), OrderModDetailsDTO.class);
        orderModDetails.setOrderStatusHistory(orderHistory.stream().map(oh -> OrderStatusHistoryDTO.builder()
                .updatedBy(oh.getUser().getName())
                .status(oh.getStatus().name())
                .updatedAt(oh.getCreatedAt())
                .build()).toList());
        
        return orderModDetails;
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
    
    private List<OrderModDTO> mapToOrderModDTO(List<Order> orders) {
        return orders.stream().map(o -> {
            Double total = o.getOrderItems().stream().mapToDouble(oi -> oi.getProduct().getPrice() * oi.getQuantity()).sum();
            return OrderModDTO.builder()
                    .orderNo(o.getOrderNo())
                    .recipientName(o.getUser().getName())
                    .phone(o.getUser().getPhone())
                    .createdAt(o.getCreatedAt())
                    .total(total)
                    .shippingFrom(o.getOrderChannel() != null ? o.getOrderChannel().name() : null)
                    .status(o.getStatus() != null ? o.getStatus().name() : null)
                    .build();
        }).toList();
    }
}
