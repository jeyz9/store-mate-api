package com.sm.jeyz9.storemateapi.services.impl;

import com.sm.jeyz9.storemateapi.dto.OrderAddressDTO;
import com.sm.jeyz9.storemateapi.dto.OrderDTO;
import com.sm.jeyz9.storemateapi.dto.OrderDetailsDTO;
import com.sm.jeyz9.storemateapi.dto.OrderItemDTO;
import com.sm.jeyz9.storemateapi.dto.OrderModDTO;
import com.sm.jeyz9.storemateapi.dto.OrderModDetailsDTO;
import com.sm.jeyz9.storemateapi.dto.OrderRecipientDTO;
import com.sm.jeyz9.storemateapi.dto.OrderStatusHistoryDTO;
import com.sm.jeyz9.storemateapi.dto.PersonInfoDTO;
import com.sm.jeyz9.storemateapi.dto.RefundDTO;
import com.sm.jeyz9.storemateapi.dto.RefundDetailsDTO;
import com.sm.jeyz9.storemateapi.dto.RefundPaginationDTO;
import com.sm.jeyz9.storemateapi.dto.ShippingDTO;
import com.sm.jeyz9.storemateapi.exceptions.WebException;
import com.sm.jeyz9.storemateapi.models.Order;
import com.sm.jeyz9.storemateapi.models.OrderAddress;
import com.sm.jeyz9.storemateapi.models.OrderStatusHistory;
import com.sm.jeyz9.storemateapi.models.OrderStatusName;
import com.sm.jeyz9.storemateapi.models.Product;
import com.sm.jeyz9.storemateapi.models.ProductImage;
import com.sm.jeyz9.storemateapi.models.RefundRequest;
import com.sm.jeyz9.storemateapi.models.RefundStatusName;
import com.sm.jeyz9.storemateapi.models.ShippingItemsDTO;
import com.sm.jeyz9.storemateapi.models.StoreInfo;
import com.sm.jeyz9.storemateapi.models.User;
import com.sm.jeyz9.storemateapi.models.Zipcode;
import com.sm.jeyz9.storemateapi.repository.OrderRepository;
import com.sm.jeyz9.storemateapi.repository.OrderStatusHistoryRepository;
import com.sm.jeyz9.storemateapi.repository.RefundRequestRepository;
import com.sm.jeyz9.storemateapi.repository.StoreInfoRepository;
import com.sm.jeyz9.storemateapi.repository.UserRepository;
import com.sm.jeyz9.storemateapi.services.OrderService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final ModelMapper modelMapper;
    private final StoreInfoRepository storeInfoRepository;
    private final RefundRequestRepository refundRequestRepository;

    @Autowired
    public OrderServiceImpl(OrderRepository orderRepository, UserRepository userRepository, OrderStatusHistoryRepository orderStatusHistoryRepository, ModelMapper modelMapper, StoreInfoRepository storeInfoRepository, RefundRequestRepository refundRequestRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.orderStatusHistoryRepository = orderStatusHistoryRepository;
        this.modelMapper = modelMapper;
        this.storeInfoRepository = storeInfoRepository;
        this.refundRequestRepository = refundRequestRepository;
    }

    @Override
    public List<OrderDTO> getUserOrders(OrderStatusName status, String email) {
        try {
            User user = userRepository.findUserByEmail(email)
                    .orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "User not found"));

            List<Order> orders;
            if(status.equals(OrderStatusName.ALL)) {
                orders = orderRepository.findAllByUser(user);
            }else {
                orders = orderRepository.findAllByUserAndStatus(user, status);
            }
            
            return mapToDTO(orders);
        } catch (Exception e) {
            throw new WebException(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error: " + e.getMessage());
        }
    }
    
    @Override
    public String getOrderStatus(String orderNo) {
        return orderRepository.findOrderStatusByOrderNo(orderNo);
    }

    @Override
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
    public Page<OrderModDTO> getAllOrders(String keyword, LocalDate startDate, LocalDate endDate, String period, Integer page, Integer size) {
        LocalDateTime start = null;
        LocalDateTime end = null;

        if(startDate != null) {
            start = startDate.atStartOfDay();
        }

        if(endDate != null) {
            end = endDate.atTime(LocalTime.MAX);
        }
        
        if(period != null && !period.trim().isBlank()) {

            LocalDate today = LocalDate.now();

            switch (period.toUpperCase()) {

                case "TODAY" -> {
                    start = today.atStartOfDay();
                    end = today.atTime(LocalTime.MAX);
                }

                case "WEEK" -> {
                    start = today.with(DayOfWeek.MONDAY).atStartOfDay();
                    end = LocalDateTime.now();
                }

                case "MONTH" -> {
                    start = today.withDayOfMonth(1).atStartOfDay();
                    end = LocalDateTime.now();
                }

                default -> throw new WebException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid period"
                );
            }
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return orderRepository.findOrders(
                keyword,
                start,
                end,
                pageable
        ).map(this::mapToOrderModDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderModDetailsDTO getOrderDetailsByModerator(String orderNo) {
        try {
            Order order = orderRepository.findOneByOrderNo(orderNo).orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "Order not found"));
            List<OrderStatusHistory> orderHistory = orderStatusHistoryRepository.findByOrderId(order.getId());
            OrderModDetailsDTO orderModDetails = modelMapper.map(mapToOrderDetailsDTO(order), OrderModDetailsDTO.class);
            if(orderModDetails != null) {
                orderModDetails.setOrderStatusHistory(orderHistory.stream().map(oh -> OrderStatusHistoryDTO.builder()
                        .updatedBy(oh.getUser().getName())
                        .status(oh.getStatus().name())
                        .updatedAt(oh.getCreatedAt())
                        .build()).toList());
            }
            
            return orderModDetails;
        }catch (Exception e) {
            e.printStackTrace();
            throw new WebException(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public String changeOrderStatus(String orderNo, String status, String email) {
        User user = userRepository.findUserByEmail(email).orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "User not found"));
        Order order = orderRepository.findOneByOrderNo(orderNo).orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "Order not found"));
        
        try {
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
        }catch (Exception e) {
            e.printStackTrace();
        }
        return "Update status successfully";
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShippingDTO> printShippingLabel(List<Long> ids) {
        try {
            StoreInfo storeInfo = storeInfoRepository.findAll().getFirst();
            return ids.stream().map(id -> {
                Order order = orderRepository.findById(id).orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "Order not found"));
                if (!order.getStatus().equals(OrderStatusName.PROCESSING)) {
                    throw new WebException(HttpStatus.BAD_REQUEST, "The order status must be processing");
                }
                
                OrderAddress address = order.getOrderAddresses().stream().findFirst().orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "Order address not found"));
                
                return ShippingDTO.builder()
                        .orderNo(order.getOrderNo())
                        .shippingItems(order.getOrderItems().stream().map(oi -> ShippingItemsDTO.builder()
                                .productName(oi.getProduct().getName())
                                .quantity(oi.getQuantity())
                                .price(oi.getProduct().getPrice())
                                .build()).toList()
                        )
                        .total(order.getOrderItems().stream().mapToDouble(o -> o.getProduct().getPrice() * o.getQuantity()).sum())
                        .checkoutType(order.getCheckoutType().name())
                        .senderInfo(PersonInfoDTO.builder()
                                .name(storeInfo.getStoreName())
                                .phone(storeInfo.getPhone())
                                .address(storeInfo.getStreetAddress())
                                .build()
                        )
                        .receiverInfo(PersonInfoDTO.builder()
                                .name(address.getRecipientName())
                                .phone(address.getPhone())
                                .address(formatAddress(address.getStreetAddress(), address.getZipcode()))
                                .build()
                        )
                        .build();
            }).toList();
        }catch (Exception e) {
            throw new WebException(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error: " + e.getMessage());
        }
    }
    
    @Override
    public RefundPaginationDTO getAllOrdersRefund(int page, int size) {
        List<RefundRequest> refundRequests = refundRequestRepository.findAll();
        List<RefundDTO> refunds = refundRequests.stream().map(r -> RefundDTO.builder()
                .refundNo(r.getRefundNo())
                .receiverName(r.getUser().getName())
                .orderNo(r.getOrder().getOrderNo())
                .total(r.getOrder().getOrderItems().stream().mapToDouble(o -> o.getProduct().getPrice() * o.getQuantity()).sum())
                .reason(r.getReason())
                .requestedAt(r.getRequestedAt())
                .status(r.getStatus().name())
                .build()).toList();

        Pageable pageable = PageRequest.of(page, size);
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), refunds.size());
        int total = refunds.size();
        
        List<RefundDTO> refundList = refunds.subList(start, end);
        Page<RefundDTO> refundPage = new PageImpl<>(refundList, pageable, refunds.size());
        
        return RefundPaginationDTO.builder()
                .refunds(refundPage.getContent())
                .page(page)
                .size(size)
                .pendingCount((int) refundRequests.stream().filter(r -> r.getStatus().equals(RefundStatusName.PENDING)).count())
                .total(total)
                .build();
    }
    
    @Override
    public RefundDetailsDTO getOrderRefundDetails(String refundNo) {
        RefundRequest refund = refundRequestRepository.findOneByRefundNo(refundNo).orElseThrow(() -> new WebException(HttpStatus.NOT_FOUND, "Refund not found"));
        return RefundDetailsDTO.builder()
                .refundNo(refund.getRefundNo())
                .orderNo(refund.getOrder().getOrderNo())
                .receiverName(refund.getUser().getName())
                .total(refund.getOrder().getOrderItems().stream().mapToDouble(o -> o.getProduct().getPrice() * o.getQuantity()).sum())
                .reason(refund.getReason())
                .requestedAt(refund.getRequestedAt())
                .status(refund.getStatus().name())
                .build();
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
                        .recipientName(oa.getRecipientName())
                        .phone(oa.getPhone())
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
    
    private List<OrderDTO> mapToDTO(List<Order> orders) {
        return orders.stream().map(
                o -> {
                    Set<OrderItemDTO> orderItems = o.getOrderItems().stream().map(item -> {
                        Product product = item.getProduct();
                        
                        String imageUrl = product.getProductImage().stream().findFirst().map(ProductImage::getImageUrl).orElse(null);
                        
                        return OrderItemDTO.builder()
                                .id(item.getId())
                                .imageUrl(imageUrl)
                                .productName(product.getName())
                                .price(product.getPrice())
                                .quantity(item.getQuantity())
                                .subTotal(product.getPrice() * item.getQuantity())
                                .build();
                    }).collect(Collectors.toSet());
                    
                    Set<OrderAddressDTO> orderAddress = o.getOrderAddresses().stream().map(
                            a -> OrderAddressDTO.builder()
                                    .id(a.getId())
                                    .streetAddress(a.getStreetAddress())
                                    .subdistrict(a.getZipcode().getSubdistrict().getName())
                                    .district(a.getZipcode().getDistrict().getName())
                                    .province(a.getZipcode().getProvince().getName())
                                    .zipcode(a.getZipcode().getZipcode())
                                    .build()
                    ).collect(Collectors.toSet());
                    
                    double total = o.getOrderItems().stream().mapToDouble(
                            i -> i.getProduct().getPrice() * i.getQuantity()
                    ).sum();
                    
                    return OrderDTO.builder()
                            .id(o.getId())
                            .orderNo(o.getOrderNo())
                            .orderItems(orderItems)
                            .orderAddress(orderAddress)
                            .status(o.getStatus().toString())
                            .checkoutType(o.getCheckoutType() != null ? o.getCheckoutType().name() : null)
                            .total(total)
                            .createdAt(o.getCreatedAt())
                            .build();
                }
        ).toList();
    }

    private OrderModDTO mapToOrderModDTO(Order o) {

        Double total = o.getOrderItems()
                .stream()
                .mapToDouble(oi -> oi.getProduct().getPrice() * oi.getQuantity())
                .sum();

        OrderAddress address = o.getOrderAddresses()
                .stream()
                .findFirst()
                .orElse(null);

        return OrderModDTO.builder()
                .id(o.getId())
                .orderNo(o.getOrderNo())
                .recipientName(address != null ? address.getRecipientName() : null)
                .phone(address != null ? address.getPhone() : null)
                .createdAt(o.getCreatedAt())
                .total(total)
                .shippingFrom(
                        o.getOrderChannel() != null
                                ? o.getOrderChannel().name()
                                : null
                )
                .status(
                        o.getStatus() != null
                                ? o.getStatus().name()
                                : null
                )
                .build();
    }
    
    private String formatAddress(String address, Zipcode zipcode) {
        return String.format(
                "%s %s %s %s %s",
                address,
                zipcode.getSubdistrict().getName(),
                zipcode.getDistrict().getName(),
                zipcode.getProvince().getName(),
                zipcode.getZipcode()
        );
    }
}
