package com.sm.jeyz9.storemateapi.services.impl;

import com.sm.jeyz9.storemateapi.dto.OrderAddressDTO;
import com.sm.jeyz9.storemateapi.dto.OrderDTO;
import com.sm.jeyz9.storemateapi.dto.OrderItemDTO;
import com.sm.jeyz9.storemateapi.exceptions.WebException;
import com.sm.jeyz9.storemateapi.models.Order;
import com.sm.jeyz9.storemateapi.models.OrderStatusName;
import com.sm.jeyz9.storemateapi.models.ProductImage;
import com.sm.jeyz9.storemateapi.models.User;
import com.sm.jeyz9.storemateapi.repository.OrderRepository;
import com.sm.jeyz9.storemateapi.repository.UserRepository;
import com.sm.jeyz9.storemateapi.services.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Stream;

@Service
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Autowired
    public OrderServiceImpl(OrderRepository orderRepository, UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
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
    public void getOrderDetails() {

    }

    @Override
    public void getAllOrders() {

    }

    @Override
    public void getOrderDetailsByModerator() {

    }

    @Override
    public void changeOrderStatus() {

    }

    @Override
    public void printShippingLabel() {

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
                        .checkoutType(o.getCheckoutType().toString())
                        .total(o.getOrderItems().stream().mapToDouble(oi -> oi.getQuantity() * oi.getProduct().getPrice()).sum())
                        .paidAt(o.getPaidAt())
                        .build()
        ).toList();
    }
}
