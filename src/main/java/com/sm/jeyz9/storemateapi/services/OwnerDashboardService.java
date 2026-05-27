package com.sm.jeyz9.storemateapi.services;

import com.sm.jeyz9.storemateapi.dto.OwnerDashboardDTO;
import com.sm.jeyz9.storemateapi.dto.SalesAnalyticsDashboardDTO;
import com.sm.jeyz9.storemateapi.dto.ProductImportDTO;
import com.sm.jeyz9.storemateapi.dto.ShippingImportDTO;
import com.sm.jeyz9.storemateapi.exceptions.WebException;
import com.sm.jeyz9.storemateapi.models.Order;
import com.sm.jeyz9.storemateapi.models.OrderAddress;
import com.sm.jeyz9.storemateapi.models.OrderChannelName;
import com.sm.jeyz9.storemateapi.models.OrderItem;
import com.sm.jeyz9.storemateapi.models.OrderStatusName;
import com.sm.jeyz9.storemateapi.models.Product;
import com.sm.jeyz9.storemateapi.repository.OrderAddressRepository;
import com.sm.jeyz9.storemateapi.repository.OrderItemRepository;
import com.sm.jeyz9.storemateapi.repository.OrderRepository;
import com.sm.jeyz9.storemateapi.repository.OwnerDashboardRepository;
import com.sm.jeyz9.storemateapi.repository.ProductRepository;
import com.sm.jeyz9.storemateapi.repository.ZipcodeRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class OwnerDashboardService {
    private final OwnerDashboardRepository ownerDashboardRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final ZipcodeRepository zipcodeRepository;

    @Autowired
    public OwnerDashboardService(OwnerDashboardRepository ownerDashboardRepository, ProductRepository productRepository, OrderRepository orderRepository, ZipcodeRepository zipcodeRepository) {
        this.ownerDashboardRepository = ownerDashboardRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.zipcodeRepository = zipcodeRepository;
    }
    
    public OwnerDashboardDTO getAdminDashboard() {
        return ownerDashboardRepository.findAdminDashboard().orElse(new OwnerDashboardDTO());
    }
    
    public SalesAnalyticsDashboardDTO salesAnalyticsDashboard() {
        return ownerDashboardRepository.findSalesAnalyticsDashboard("").orElse(new SalesAnalyticsDashboardDTO());
    }

    @Transactional
    public String importExcel(MultipartFile file) {

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            Sheet sheet = workbook.getSheetAt(0);

            List<ShippingImportDTO> results = new ArrayList<>();

            for (int i = 2; i <= sheet.getLastRowNum(); i++) {

                Row row = sheet.getRow(i);

                if (row == null) continue;

                Cell cell = row.getCell(2);

                if (cell == null || cell.toString().trim().isBlank()) {
                    break;
                }

                ShippingImportDTO dto = ShippingImportDTO.builder()
                        .receiverName(getString(row.getCell(2)))
                        .phoneNumber(getString(row.getCell(4)))
                        .address(getString(row.getCell(5)))
                        .zipcode(getString(row.getCell(9)))
                        .products(buildProducts(row, evaluator))
                        .returned(parseReturned(getString(row.getCell(31))))
                        .totalPrice(sumPrice(row, evaluator))
                        .build();

                results.add(dto);
            }

            createOrder(results);
            return "Import order successfully";

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<ProductImportDTO> buildProducts(Row row, FormulaEvaluator evaluator) {

        List<ProductImportDTO> products = new ArrayList<>();

        addProduct(products, row, 10, 29L, evaluator);
        addProduct(products, row, 11, 28L, evaluator);
        addProduct(products, row, 12, 27L, evaluator);
        addProduct(products, row, 13, 30L, evaluator);
        addProduct(products, row, 14, 26L, evaluator);
        addProduct(products, row, 15, 25L, evaluator);

        return products;
    }

    private void addProduct(
            List<ProductImportDTO> products,
            Row row,
            int column,
            Long productId,
            FormulaEvaluator evaluator
    ) {
        Integer quantity = getIntegerCellValue(row.getCell(column), evaluator);

        if (quantity == null || quantity <= 0) {
            return;
        }

        Product product = productRepository
                .findById(productId)
                .orElseThrow(() -> new RuntimeException(
                        "Product not found: " + productId
                ));
        products.add(
                ProductImportDTO.builder()
                        .productId(product.getId())
                        .productName(product.getName())
                        .quantity(quantity)
                        .build()
        );
    }

    private String getString(Cell cell) {

        if (cell == null) return null;

        return new DataFormatter()
                .formatCellValue(cell)
                .trim();
    }

    private Boolean parseReturned(String value) {

        if (value == null) return false;

        return value.equalsIgnoreCase("true")
                || value.equals("ใช่");
    }

    private Integer getIntegerCellValue(Cell cell, FormulaEvaluator evaluator) {
        if (cell == null) {
            return 0;
        }

        CellValue cellValue = evaluator.evaluate(cell);
        if(cellValue == null) return 0;

        return switch (cellValue.getCellType()) {
            case NUMERIC -> (int) cellValue.getNumberValue();

            case STRING -> Integer.parseInt(cellValue.getStringValue());

            default -> 0;
        };
    }

    private void createOrder(List<ShippingImportDTO> results) {

        for (ShippingImportDTO shipping : results) {

            Order order = Order.builder()
                    .status(!shipping.getReturned()
                            ? OrderStatusName.COMPLETED
                            : OrderStatusName.CANCELLED)
                    .createdAt(LocalDateTime.now())
                    .orderChannel(OrderChannelName.OTHER)
                    .orderItems(new HashSet<>())
                    .orderAddresses(new HashSet<>())
                    .build();

            String phone = shipping.getPhoneNumber() == null
                    ? ""
                    : shipping.getPhoneNumber()
                    .trim()
                    .replace("-", "")
                    .replaceAll("\\s+", "");

            if (phone.length() > 10) {
                phone = phone.substring(0, 10);
            }

            OrderAddress orderAddress = OrderAddress.builder()
                    .recipientName(shipping.getReceiverName())
                    .phone(phone)
                    .streetAddress(shipping.getAddress())
                    .zipcode(
                            zipcodeRepository
                                    .findByZipcode(shipping.getZipcode())
                                    .orElse(null)
                    )
                    .createdAt(LocalDateTime.now())
                    .order(order)
                    .build();

            order.getOrderAddresses().add(orderAddress);
            order.setTotalPrice(shipping.getTotalPrice());

            Set<OrderItem> orderItems = shipping.getProducts()
                    .stream()
                    .filter(p -> p.getQuantity() != null && p.getQuantity() > 0)
                    .map(product -> {

                        Product pd = productRepository.findById(product.getProductId())
                                .orElseThrow(() ->
                                        new WebException(
                                                HttpStatus.NOT_FOUND,
                                                "Product not found"
                                        )
                                );

                        return OrderItem.builder()
                                .order(order)
                                .product(pd)
                                .quantity(product.getQuantity())
                                .build();
                    })
                    .collect(Collectors.toSet());

            order.getOrderItems().addAll(orderItems);

            orderRepository.save(order);
        }
    }
    
    private double sumPrice(Row row, FormulaEvaluator evaluator) {
        return getIntegerCellValue(row.getCell(23), evaluator) + getIntegerCellValue(row.getCell(24), evaluator);
    }
}
