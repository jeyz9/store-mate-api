package com.sm.jeyz9.storemateapi.services.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.sm.jeyz9.storemateapi.config.LineConfig;
import com.sm.jeyz9.storemateapi.dto.ProductDTO;
import com.sm.jeyz9.storemateapi.dto.ProductWithCategoryDTO;
import com.sm.jeyz9.storemateapi.dto.CartItemDTO;
import com.sm.jeyz9.storemateapi.dto.CartItemRequestDTO;
import com.sm.jeyz9.storemateapi.exceptions.WebException;
import com.sm.jeyz9.storemateapi.models.*;
import com.sm.jeyz9.storemateapi.repository.*;
import com.sm.jeyz9.storemateapi.services.CartService;
import com.sm.jeyz9.storemateapi.services.LineMessageService;
import com.sm.jeyz9.storemateapi.services.LinePaymentService;
import com.sm.jeyz9.storemateapi.services.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LineMessageServiceImpl implements LineMessageService {

    private static final Logger log = LoggerFactory.getLogger(LineMessageServiceImpl.class);

    // ─── Theme ────────────────────────────────────────────────────────────────
    private static final String C_BG        = "#F5ECD7"; // parchment background
    private static final String C_PRIMARY   = "#3D6B2C"; // dark forest green
    private static final String C_GOLD      = "#8B6914"; // golden brown
    private static final String C_TEXT      = "#2D4A1E"; // dark green text
    private static final String C_SUBTEXT   = "#6B5B3E"; // warm brown subtext
    private static final String C_MUTED     = "#9E8A6F"; // muted/label text
    private static final String C_PRICE     = "#7B1A1A"; // deep red for price
    private static final String C_WHITE     = "#FFFFFF";
    private static final String C_DANGER    = "#8B1A1A"; // delete/cancel red

    private final LineConfig lineConfig;
    private final RestTemplate restTemplate;
    private final ProductService productService;
    private final UserRepository userRepository;
    private final CartService cartService;
    private final LinePaymentService linePaymentService;
    private final OrderRepository orderRepository;
    private final GeographyRepository geographyRepository;
    private final ProvinceRepository provinceRepository;
    private final DistrictRepository districtRepository;
    private final SubdistrictRepository subdistrictRepository;
    private final ZipcodeRepository zipcodeRepository;
    private final UserAddressRepository userAddressRepository;
    private final com.sm.jeyz9.storemateapi.services.StoreInfoService storeInfoService;
    private final com.sm.jeyz9.storemateapi.repository.ProductRepository productRepository;

    // ─── Address session state ─────────────────────────────────────────────────

    private static class AddressSession {
        final Long subdistrictId;
        final Long districtId;
        final Long provinceId;
        AddressSession(Long subdistrictId, Long districtId, Long provinceId) {
            this.subdistrictId = subdistrictId;
            this.districtId    = districtId;
            this.provinceId    = provinceId;
        }
    }
    private final Map<String, AddressSession> addressSessions = new ConcurrentHashMap<>();
    private final Set<String> phoneSessions = ConcurrentHashMap.newKeySet();

    @Autowired
    public LineMessageServiceImpl(LineConfig lineConfig, RestTemplate restTemplate,
                                  ProductService productService, UserRepository userRepository,
                                  CartService cartService,
                                  LinePaymentService linePaymentService,
                                  OrderRepository orderRepository,
                                  GeographyRepository geographyRepository,
                                  ProvinceRepository provinceRepository,
                                  DistrictRepository districtRepository,
                                  SubdistrictRepository subdistrictRepository,
                                  ZipcodeRepository zipcodeRepository,
                                  UserAddressRepository userAddressRepository,
                                  com.sm.jeyz9.storemateapi.services.StoreInfoService storeInfoService,
                                  com.sm.jeyz9.storemateapi.repository.ProductRepository productRepository) {
        this.lineConfig            = lineConfig;
        this.restTemplate          = restTemplate;
        this.productService        = productService;
        this.userRepository        = userRepository;
        this.cartService           = cartService;
        this.linePaymentService    = linePaymentService;
        this.orderRepository       = orderRepository;
        this.geographyRepository   = geographyRepository;
        this.provinceRepository    = provinceRepository;
        this.districtRepository    = districtRepository;
        this.subdistrictRepository = subdistrictRepository;
        this.zipcodeRepository     = zipcodeRepository;
        this.userAddressRepository = userAddressRepository;
        this.storeInfoService      = storeInfoService;
        this.productRepository     = productRepository;
    }

    // ─── Event Entry Point ────────────────────────────────────────────────────

    @Override
    public void handleEvent(JsonNode event) {
        String type = event.get("type").asText();
        String replyToken = event.path("replyToken").asText(null);
        String lineUserId = event.path("source").path("userId").asText(null);
        User user = resolveUser(lineUserId);

        if ("message".equals(type)) {
            JsonNode message = event.get("message");
            if ("text".equals(message.get("type").asText())) {
                handleTextMessage(replyToken, message.get("text").asText(), lineUserId, user);
            }
        } else if ("postback".equals(type)) {
            String data = event.path("postback").path("data").asText();
            handlePostback(replyToken, data, user, lineUserId);
        }
    }

    // ─── User Resolution ──────────────────────────────────────────────────────

    private User resolveUser(String lineUserId) {
        if (lineUserId == null) return null;

        return userRepository.findByLineUserId(lineUserId)
                .map(existing -> {
                    if (existing.getName() == null || existing.getName().equals("ผู้ใช้ไม่ระบุชื่อ")) {
                        String displayName = fetchLineDisplayName(lineUserId);
                        if (!displayName.equals("ผู้ใช้ไม่ระบุชื่อ")) {
                            existing.setName(displayName);
                            return userRepository.save(existing);
                        }
                    }
                    return existing;
                })
                .orElseGet(() -> {
                    String displayName = fetchLineDisplayName(lineUserId);
                    User newUser = User.builder()
                            .name(displayName)
                            .lineUserId(lineUserId)
                            .build();
                    return userRepository.save(newUser);
                });
    }

    @SuppressWarnings("unchecked")
    private String fetchLineDisplayName(String lineUserId) {
        try {
            String url = "https://api.line.me/v2/bot/profile/" + lineUserId;
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(lineConfig.getChannelToken());
            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<java.util.Map> response = restTemplate.exchange(
                    url, org.springframework.http.HttpMethod.GET, request, java.util.Map.class);
            String name = (String) response.getBody().get("displayName");
            if (name == null || name.isBlank()) {
                log.warn("[LINE Profile] displayName null/blank for userId={}", lineUserId);
                return "ผู้ใช้ไม่ระบุชื่อ";
            }
            log.info("[LINE Profile] got name={} for userId={}", name, lineUserId);
            return name;
        } catch (Exception e) {
            log.warn("[LINE Profile] error lineUserId={} error={}", lineUserId, e.getMessage(), e);
            return "ผู้ใช้ไม่ระบุชื่อ";
        }
    }

    // ─── Text Message Handler ─────────────────────────────────────────────────

    private void handleTextMessage(String replyToken, String text, String lineUserId, User user) {
        if (lineUserId != null && phoneSessions.contains(lineUserId)) {
            handlePhoneInput(replyToken, text, lineUserId, user);
            return;
        }
        if (lineUserId != null && addressSessions.containsKey(lineUserId)) {
            handleStreetInput(replyToken, text, lineUserId, user);
            return;
        }
        switch (text) {
            case "สินค้าทั้งหมด":
                sendProductsFlex(replyToken);
                break;
            case "ตระกร้าสินค้า":
            case "ชำระเงิน":
            case "สถานะสินค้า":
            case "จัดการที่อยู่":
                if (user == null) {
                    replyMessage(replyToken,
                            "กรุณาผูกบัญชีก่อนใช้งานฟีเจอร์นี้ครับ\n\nพิมพ์ \"ผูกบัญชี\" แล้วตามด้วย email ของคุณ\nเช่น: ผูกบัญชี example@email.com");
                } else {
                    handleAuthenticatedMenu(replyToken, text, user);
                }
                break;
            case "ติดต่อเรา":
                sendStoreContact(replyToken);
                break;
            default:
                if (text.startsWith("เพิ่มสินค้า ")) {
                    handleAddToCart(replyToken, text.substring(12).trim(), user);
                } else {
                    replyMessage(replyToken, "กรุณาเลือกเมนูด้านล่างครับ 😊");
                }
        }
    }

    private void handleAuthenticatedMenu(String replyToken, String text, User user) {
        switch (text) {
            case "ตระกร้าสินค้า":  sendCartFlex(replyToken, user);          break;
            case "ชำระเงิน":        sendCheckoutSummary(replyToken, user);   break;
            case "สถานะสินค้า":     sendOrderStatus(replyToken, user);       break;
            case "จัดการที่อยู่":   sendAddressManagement(replyToken, user); break;
        }
    }

    // ─── Postback ─────────────────────────────────────────────────────────────

    private void handlePostback(String replyToken, String data, User user, String lineUserId) {
        Map<String, String> params = new HashMap<>();
        for (String part : data.split("&")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2) params.put(kv[0], kv[1]);
        }
        String action = params.getOrDefault("action", "");

        switch (action) {
            case "increase", "decrease", "delete" -> {
                if (user == null) { replyMessage(replyToken, "⚠️ ไม่พบข้อมูลผู้ใช้"); return; }
                try {
                    Long productId = Long.parseLong(params.getOrDefault("productId", ""));
                    switch (action) {
                        case "increase" -> { cartService.updateQuantityByUserId(user.getId(), productId,  1); sendCartFlex(replyToken, user); }
                        case "decrease" -> { cartService.updateQuantityByUserId(user.getId(), productId, -1); sendCartFlex(replyToken, user); }
                        case "delete"   -> { cartService.removeItemByUserId(user.getId(), productId);         sendCartFlex(replyToken, user); }
                    }
                } catch (Exception e) {
                    log.error("[handlePostback] cart error: {}", e.getMessage(), e);
                    replyMessage(replyToken, "❌ เกิดข้อผิดพลาด กรุณาลองใหม่");
                }
            }
            case "add_address"   -> sendRegionSelection(replyToken);
            case "select_region" -> sendProvinceSelection(replyToken,
                    Integer.parseInt(params.getOrDefault("id", "0")));
            case "select_p"      -> sendDistrictSelection(replyToken,
                    Long.parseLong(params.getOrDefault("id", "0")));
            case "select_d"      -> sendSubdistrictSelection(replyToken,
                    Long.parseLong(params.getOrDefault("id", "0")),
                    Long.parseLong(params.getOrDefault("province_id", "0")));
            case "select_s"      -> {
                if (user == null) { replyMessage(replyToken, "⚠️ ไม่พบข้อมูลผู้ใช้"); return; }
                addressSessions.put(lineUserId, new AddressSession(
                        Long.parseLong(params.getOrDefault("id", "0")),
                        Long.parseLong(params.getOrDefault("district_id", "0")),
                        Long.parseLong(params.getOrDefault("province_id", "0"))
                ));
                replyMessage(replyToken, "📝 กรุณาพิมพ์บ้านเลขที่/ถนน/ซอย\n\nพิมพ์ \"ย้อนกลับ\" หรือ \"ยกเลิก\" ได้เลย");
            }
            case "go_back"           -> handleGoBack(replyToken, params);
            case "confirm_checkout"  -> handleConfirmCheckout(replyToken, user);
            case "cancel_checkout"   -> replyMessage(replyToken, "❌ ยกเลิกการสั่งซื้อแล้วครับ\n\nสินค้ายังอยู่ในตะกร้าของคุณ");
            case "show_qr"           -> handleShowQr(replyToken, params.getOrDefault("pi", ""), user);
            case "edit_phone"        -> {
                if (user == null) { replyMessage(replyToken, "⚠️ ไม่พบข้อมูลผู้ใช้"); return; }
                phoneSessions.add(lineUserId);
                String current = (user.getPhone() != null) ? "\n📱 เบอร์ปัจจุบัน: " + user.getPhone() : "";
                replyMessage(replyToken, "📱 กรุณาพิมพ์เบอร์โทรศัพท์ของคุณ" + current + "\n\nพิมพ์ \"ยกเลิก\" เพื่อยกเลิก");
            }
            default -> { /* ignore unknown actions */ }
        }
    }

    // ─── QR Code ──────────────────────────────────────────────────────────────

    private void handleShowQr(String replyToken, String paymentIntentId, User user) {
        if (paymentIntentId.isBlank()) {
            replyMessage(replyToken, "❌ ไม่พบข้อมูล QR Code");
            return;
        }
        try {
            String qrUrl = linePaymentService.getQrCodeUrl(paymentIntentId);

            boolean isRenewed = false;
            if (qrUrl == null) {
                qrUrl = linePaymentService.renewQrCode(paymentIntentId);
                isRenewed = true;
            }

            Map<String, Object> imageMsg = new HashMap<>();
            imageMsg.put("type", "image");
            imageMsg.put("originalContentUrl", qrUrl);
            imageMsg.put("previewImageUrl", qrUrl);

            String caption = isRenewed
                    ? "🔄 QR Code หมดอายุ — สร้างใหม่ให้แล้วครับ!\n\n📱 สแกนเพื่อชำระเงินผ่าน PromptPay\n⏰ QR Code มีอายุ 24 ชั่วโมง"
                    : "📱 สแกน QR Code เพื่อชำระเงินผ่าน PromptPay\n⏰ QR Code มีอายุ 24 ชั่วโมง";

            Map<String, Object> textMsg = new HashMap<>();
            textMsg.put("type", "text");
            textMsg.put("text", caption);

            replyMessages(replyToken, List.of(imageMsg, textMsg));
        } catch (WebException e) {
            replyMessage(replyToken, "❌ " + e.getMessage());
        } catch (Exception e) {
            log.error("[handleShowQr] error: {}", e.getMessage(), e);
            replyMessage(replyToken, "❌ เกิดข้อผิดพลาด กรุณาลองใหม่");
        }
    }

    // ─── Checkout ─────────────────────────────────────────────────────────────

    private void sendCheckoutSummary(String replyToken, User user) {
        try {
            List<CartItemDTO> items = cartService.getCartItemsByUserId(user.getId());
            if (items.isEmpty()) {
                replyMessage(replyToken, "ตะกร้าของคุณว่างเปล่าครับ\nลองพิมพ์ \"สินค้าทั้งหมด\" เพื่อเลือกสินค้า");
                return;
            }

            if (user.getPhone() == null || user.getPhone().isBlank()) {
                replyMessage(replyToken, "⚠️ กรุณาเพิ่มเบอร์โทรศัพท์ก่อนชำระเงินครับ\n\nพิมพ์ \"จัดการที่อยู่\" แล้วกดปุ่ม \"เพิ่มเบอร์โทร\"");
                return;
            }

            List<UserAddress> addresses = userAddressRepository
                    .findByUserIdOrderByIsDefaultDescCreatedAtDesc(user.getId());
            String addressDisplay;
            if (addresses.isEmpty()) {
                addressDisplay = "⚠️ ยังไม่มีที่อยู่จัดส่ง\nกรุณาพิมพ์ \"จัดการที่อยู่\" ก่อนครับ";
            } else {
                UserAddress addr = addresses.get(0);
                Zipcode zip = addr.getZipcode();
                String sub = (zip != null && zip.getSubdistrict() != null) ? zip.getSubdistrict().getName() : "";
                String dis = (zip != null && zip.getDistrict()    != null) ? zip.getDistrict().getName()    : "";
                String pro = (zip != null && zip.getProvince()    != null) ? zip.getProvince().getName()    : "";
                addressDisplay = addr.getStreetAddress() + " ต." + sub + " อ." + dis + " จ." + pro;
            }

            double total = items.stream().mapToDouble(CartItemDTO::getSubTotal).sum();

            Map<String, Object> headerText = new HashMap<>();
            headerText.put("type", "text");
            headerText.put("text", "🧾 สรุปรายการสั่งซื้อ");
            headerText.put("weight", "bold");
            headerText.put("size", "lg");
            headerText.put("color", C_WHITE);
            Map<String, Object> header = new HashMap<>();
            header.put("type", "box");
            header.put("layout", "vertical");
            header.put("backgroundColor", C_PRIMARY);
            header.put("paddingAll", "md");
            header.put("contents", List.of(headerText));

            List<Object> bodyContents = new ArrayList<>();
            for (CartItemDTO item : items) {
                Map<String, Object> itemLabel = new HashMap<>();
                itemLabel.put("type", "text");
                itemLabel.put("text", item.getProductName() + " x" + item.getQuantity());
                itemLabel.put("size", "sm");
                itemLabel.put("color", C_TEXT);
                itemLabel.put("flex", 4);
                itemLabel.put("wrap", true);

                Map<String, Object> itemPrice = new HashMap<>();
                itemPrice.put("type", "text");
                itemPrice.put("text", String.format("฿%.0f", item.getSubTotal()));
                itemPrice.put("size", "sm");
                itemPrice.put("color", C_PRICE);
                itemPrice.put("align", "end");
                itemPrice.put("flex", 2);

                Map<String, Object> itemRow = new HashMap<>();
                itemRow.put("type", "box");
                itemRow.put("layout", "horizontal");
                itemRow.put("margin", "sm");
                itemRow.put("contents", List.of(itemLabel, itemPrice));
                bodyContents.add(itemRow);
            }

            Map<String, Object> sep = new HashMap<>();
            sep.put("type", "separator");
            sep.put("margin", "md");
            bodyContents.add(sep);

            Map<String, Object> totalLabel = new HashMap<>();
            totalLabel.put("type", "text");
            totalLabel.put("text", "ยอดรวมทั้งสิ้น");
            totalLabel.put("weight", "bold");
            totalLabel.put("size", "md");
            totalLabel.put("color", C_TEXT);
            totalLabel.put("flex", 3);

            Map<String, Object> totalValue = new HashMap<>();
            totalValue.put("type", "text");
            totalValue.put("text", String.format("฿%.0f", total));
            totalValue.put("weight", "bold");
            totalValue.put("size", "md");
            totalValue.put("color", C_PRICE);
            totalValue.put("align", "end");
            totalValue.put("flex", 2);

            Map<String, Object> totalRow = new HashMap<>();
            totalRow.put("type", "box");
            totalRow.put("layout", "horizontal");
            totalRow.put("margin", "md");
            totalRow.put("contents", List.of(totalLabel, totalValue));
            bodyContents.add(totalRow);

            Map<String, Object> addrSep = new HashMap<>();
            addrSep.put("type", "separator");
            addrSep.put("margin", "md");
            bodyContents.add(addrSep);

            Map<String, Object> addrTitle = new HashMap<>();
            addrTitle.put("type", "text");
            addrTitle.put("text", "📍 ที่อยู่จัดส่ง");
            addrTitle.put("size", "xs");
            addrTitle.put("color", C_MUTED);
            addrTitle.put("margin", "md");

            Map<String, Object> addrValue = new HashMap<>();
            addrValue.put("type", "text");
            addrValue.put("text", addressDisplay);
            addrValue.put("size", "sm");
            addrValue.put("color", C_SUBTEXT);
            addrValue.put("wrap", true);
            addrValue.put("margin", "xs");

            bodyContents.add(addrTitle);
            bodyContents.add(addrValue);

            Map<String, Object> body = new HashMap<>();
            body.put("type", "box");
            body.put("layout", "vertical");
            body.put("paddingAll", "md");
            body.put("backgroundColor", C_BG);
            body.put("contents", bodyContents);

            Map<String, Object> confirmAction = new HashMap<>();
            confirmAction.put("type", "postback");
            confirmAction.put("label", "✅ ยืนยันการสั่งซื้อ");
            confirmAction.put("data", "action=confirm_checkout");
            confirmAction.put("displayText", "ยืนยันการสั่งซื้อ");

            Map<String, Object> confirmBtn = new HashMap<>();
            confirmBtn.put("type", "button");
            confirmBtn.put("style", "primary");
            confirmBtn.put("color", C_PRIMARY);
            confirmBtn.put("action", confirmAction);

            Map<String, Object> cancelAction = new HashMap<>();
            cancelAction.put("type", "postback");
            cancelAction.put("label", "❌ ยกเลิก");
            cancelAction.put("data", "action=cancel_checkout");
            cancelAction.put("displayText", "ยกเลิก");

            Map<String, Object> cancelBtn = new HashMap<>();
            cancelBtn.put("type", "button");
            cancelBtn.put("style", "secondary");
            cancelBtn.put("action", cancelAction);

            Map<String, Object> footer = new HashMap<>();
            footer.put("type", "box");
            footer.put("layout", "vertical");
            footer.put("spacing", "sm");
            footer.put("backgroundColor", C_BG);
            footer.put("contents", addresses.isEmpty() ? List.of(cancelBtn) : List.of(confirmBtn, cancelBtn));

            Map<String, Object> bubble = new HashMap<>();
            bubble.put("type", "bubble");
            bubble.put("header", header);
            bubble.put("body", body);
            bubble.put("footer", footer);

            replyFlexMessage(replyToken, "🧾 สรุปรายการสั่งซื้อ", bubble);
        } catch (Exception e) {
            log.error("[sendCheckoutSummary] error: {}", e.getMessage(), e);
            replyMessage(replyToken, "❌ เกิดข้อผิดพลาด กรุณาลองใหม่");
        }
    }

    private void handleConfirmCheckout(String replyToken, User user) {
        if (user == null) {
            replyMessage(replyToken, "⚠️ ไม่พบข้อมูลผู้ใช้");
            return;
        }
        try {
            replyMessage(replyToken, "⏳ กำลังสร้าง QR Code กรุณารอสักครู่...");
            LinePaymentService.LineCheckoutResult result =
                    linePaymentService.createPromptPayCheckout(user.getId());
            pushQrAndConfirmation(user.getLineUserId(), result);
        } catch (WebException e) {
            pushMessage(user.getLineUserId(), "❌ " + e.getMessage());
        } catch (Exception e) {
            log.error("[handleConfirmCheckout] error: {}", e.getMessage(), e);
            pushMessage(user.getLineUserId(), "❌ เกิดข้อผิดพลาด กรุณาลองใหม่");
        }
    }

    private void pushQrAndConfirmation(String lineUserId, LinePaymentService.LineCheckoutResult result) {
        Map<String, Object> imageMsg = new HashMap<>();
        imageMsg.put("type", "image");
        imageMsg.put("originalContentUrl", result.qrCodeUrl());
        imageMsg.put("previewImageUrl", result.qrCodeUrl());

        String text = "✅ สร้างคำสั่งซื้อสำเร็จ!\n\n"
                + "📋 หมายเลขออเดอร์:\n" + result.orderNo() + "\n\n"
                + String.format("💰 ยอดชำระ: ฿%.0f\n\n", result.totalAmount())
                + "📱 สแกน QR Code ด้านบนเพื่อชำระเงินผ่าน PromptPay\n"
                + "⏰ QR Code มีอายุ 24 ชั่วโมง";
        Map<String, Object> textMsg = new HashMap<>();
        textMsg.put("type", "text");
        textMsg.put("text", text);

        pushMessages(lineUserId, List.of(imageMsg, textMsg));
    }

    private void pushMessage(String lineUserId, String text) {
        pushMessages(lineUserId, List.of(Map.of("type", "text", "text", text)));
    }

    private void pushMessages(String lineUserId, List<Map<String, Object>> messages) {
        try {
            String url = "https://api.line.me/v2/bot/message/push";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(lineConfig.getChannelToken());

            Map<String, Object> body = new HashMap<>();
            body.put("to", lineUserId);
            body.put("messages", messages);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            restTemplate.postForEntity(url, request, String.class);
        } catch (Exception e) {
            log.error("[pushMessages] error: {}", e.getMessage(), e);
        }
    }

    // ─── Cart Flex ────────────────────────────────────────────────────────────

    private void sendCartFlex(String replyToken, User user) {
        try {
            List<CartItemDTO> items = cartService.getCartItemsByUserId(user.getId());

            if (items.isEmpty()) {
                replyMessage(replyToken, "ตะกร้าของคุณว่างเปล่าครับ\nลองพิมพ์ \"สินค้าทั้งหมด\" เพื่อเลือกสินค้า");
                return;
            }

            double total = items.stream().mapToDouble(CartItemDTO::getSubTotal).sum();

            Map<String, Object> headerText = new HashMap<>();
            headerText.put("type", "text");
            headerText.put("text", "🛒 ตะกร้าสินค้าของคุณ");
            headerText.put("weight", "bold");
            headerText.put("size", "lg");
            headerText.put("color", C_WHITE);

            Map<String, Object> header = new HashMap<>();
            header.put("type", "box");
            header.put("layout", "vertical");
            header.put("backgroundColor", C_PRIMARY);
            header.put("paddingAll", "md");
            header.put("contents", List.of(headerText));

            List<Object> bodyContents = new ArrayList<>();

            Map<String, Object> topSep = new HashMap<>();
            topSep.put("type", "separator");
            topSep.put("margin", "sm");
            bodyContents.add(topSep);

            for (CartItemDTO item : items) {
                bodyContents.add(buildCartItemRow(item));

                Map<String, Object> itemSep = new HashMap<>();
                itemSep.put("type", "separator");
                itemSep.put("margin", "lg");
                bodyContents.add(itemSep);
            }

            Map<String, Object> body = new HashMap<>();
            body.put("type", "box");
            body.put("layout", "vertical");
            body.put("backgroundColor", C_BG);
            body.put("contents", bodyContents);

            Map<String, Object> totalLabel = new HashMap<>();
            totalLabel.put("type", "text");
            totalLabel.put("text", "ยอดรวมทั้งสิ้น");
            totalLabel.put("size", "sm");
            totalLabel.put("color", C_MUTED);

            Map<String, Object> totalValue = new HashMap<>();
            totalValue.put("type", "text");
            totalValue.put("text", String.format("฿%.0f", total));
            totalValue.put("size", "lg");
            totalValue.put("align", "end");
            totalValue.put("weight", "bold");
            totalValue.put("color", C_PRICE);

            Map<String, Object> totalRow = new HashMap<>();
            totalRow.put("type", "box");
            totalRow.put("layout", "horizontal");
            totalRow.put("contents", List.of(totalLabel, totalValue));

            Map<String, Object> footer = new HashMap<>();
            footer.put("type", "box");
            footer.put("layout", "vertical");
            footer.put("spacing", "sm");
            footer.put("backgroundColor", C_BG);
            footer.put("contents", List.of(totalRow));

            Map<String, Object> bubble = new HashMap<>();
            bubble.put("type", "bubble");
            bubble.put("size", "mega");
            bubble.put("header", header);
            bubble.put("body", body);
            bubble.put("footer", footer);

            replyFlexMessage(replyToken, "🛒 ตะกร้าสินค้า", bubble);
        } catch (Exception e) {
            log.error("[sendCartFlex] error: {}", e.getMessage(), e);
            replyMessage(replyToken, "ไม่สามารถดึงตะกร้าสินค้าได้");
        }
    }

    private Map<String, Object> buildCartItemRow(CartItemDTO item) {
        Map<String, Object> image = new HashMap<>();
        image.put("type", "image");
        image.put("url", item.getImageUrl() != null ? item.getImageUrl() : PLACEHOLDER_IMAGE);
        image.put("size", "sm");
        image.put("aspectMode", "cover");
        image.put("aspectRatio", "1:1");

        Map<String, Object> nameText = new HashMap<>();
        nameText.put("type", "text");
        nameText.put("text", item.getProductName());
        nameText.put("weight", "bold");
        nameText.put("size", "sm");
        nameText.put("color", C_TEXT);
        nameText.put("wrap", true);

        Map<String, Object> priceText = new HashMap<>();
        priceText.put("type", "text");
        priceText.put("text", String.format("฿%.0f", item.getPrice()));
        priceText.put("size", "sm");
        priceText.put("color", C_PRICE);
        priceText.put("weight", "bold");
        priceText.put("margin", "xs");

        Map<String, Object> infoBox = new HashMap<>();
        infoBox.put("type", "box");
        infoBox.put("layout", "vertical");
        infoBox.put("margin", "md");
        infoBox.put("flex", 3);
        infoBox.put("contents", List.of(nameText, priceText));

        Map<String, Object> decAction = new HashMap<>();
        decAction.put("type", "postback");
        decAction.put("label", "−");
        decAction.put("data", "action=decrease&productId=" + item.getProductId());

        Map<String, Object> decBtn = new HashMap<>();
        decBtn.put("type", "button");
        decBtn.put("action", decAction);
        decBtn.put("style", "secondary");
        decBtn.put("height", "sm");
        decBtn.put("flex", 1);

        Map<String, Object> qtyText = new HashMap<>();
        qtyText.put("type", "text");
        qtyText.put("text", String.valueOf(item.getQuantity()));
        qtyText.put("align", "center");
        qtyText.put("weight", "bold");
        qtyText.put("size", "sm");
        qtyText.put("color", C_TEXT);
        qtyText.put("flex", 1);

        Map<String, Object> incAction = new HashMap<>();
        incAction.put("type", "postback");
        incAction.put("label", "+");
        incAction.put("data", "action=increase&productId=" + item.getProductId());

        Map<String, Object> incBtn = new HashMap<>();
        incBtn.put("type", "button");
        incBtn.put("action", incAction);
        incBtn.put("style", "primary");
        incBtn.put("color", C_PRIMARY);
        incBtn.put("height", "sm");
        incBtn.put("flex", 1);

        Map<String, Object> delAction = new HashMap<>();
        delAction.put("type", "postback");
        delAction.put("label", "ลบ");
        delAction.put("data", "action=delete&productId=" + item.getProductId());

        Map<String, Object> delBtn = new HashMap<>();
        delBtn.put("type", "button");
        delBtn.put("action", delAction);
        delBtn.put("style", "primary");
        delBtn.put("color", C_DANGER);
        delBtn.put("height", "sm");
        delBtn.put("flex", 1);

        Map<String, Object> controlBox = new HashMap<>();
        controlBox.put("type", "box");
        controlBox.put("layout", "horizontal");
        controlBox.put("flex", 2);
        controlBox.put("alignItems", "center");
        controlBox.put("justifyContent", "space-between");
        controlBox.put("contents", List.of(decBtn, qtyText, incBtn, delBtn));

        Map<String, Object> row = new HashMap<>();
        row.put("type", "box");
        row.put("layout", "horizontal");
        row.put("margin", "lg");
        row.put("alignItems", "center");
        row.put("contents", List.of(image, infoBox, controlBox));

        return row;
    }

    // ─── Add to Cart ──────────────────────────────────────────────────────────

    private void handleAddToCart(String replyToken, String productIdStr, User user) {
        if (user == null) {
            replyMessage(replyToken, "⚠️ ไม่พบข้อมูลผู้ใช้ของคุณ\nกรุณาลองใหม่อีกครั้ง");
            return;
        }
        try {
            Long productId = Long.parseLong(productIdStr);
            CartItemRequestDTO request = new CartItemRequestDTO(productId, 1);
            cartService.addProductToCartByUserId(user.getId(), request);
            replyMessage(replyToken, "✅ เพิ่มสินค้าลงตะกร้าแล้ว!\nพิมพ์ \"ตะกร้าสินค้า\" เพื่อดูรายการครับ");
        } catch (NumberFormatException e) {
            replyMessage(replyToken, "ไม่พบสินค้าที่เลือก");
        } catch (WebException e) {
            replyMessage(replyToken, e.getMessage());
        } catch (Exception e) {
            log.error("[handleAddToCart] error: {}", e.getMessage(), e);
            replyMessage(replyToken, "เกิดข้อผิดพลาด กรุณาลองใหม่");
        }
    }

    // ─── Order Status ─────────────────────────────────────────────────────────

    private void sendOrderStatus(String replyToken, User user) {
        try {
            List<Order> orders = orderRepository.findAllByUser(user);

            if (orders.isEmpty()) {
                replyMessage(replyToken, "📦 ยังไม่มีประวัติการสั่งซื้อครับ\n\nลองพิมพ์ \"สินค้าทั้งหมด\" เพื่อเลือกสินค้า");
                return;
            }

            List<Order> recent = orders.stream()
                    .filter(o -> o.getStatus() != OrderStatusName.COMPLETED
                              && o.getStatus() != OrderStatusName.CANCELLED)
                    .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                    .limit(5)
                    .toList();

            if (recent.isEmpty()) {
                replyMessage(replyToken, "📦 ไม่มีรายการสั่งซื้อที่อยู่ระหว่างดำเนินการครับ");
                return;
            }

            List<Map<String, Object>> bubbles = new ArrayList<>();
            for (Order order : recent) {
                bubbles.add(buildOrderBubble(order));
            }

            Map<String, Object> carousel = new HashMap<>();
            carousel.put("type", "carousel");
            carousel.put("contents", bubbles);

            replyFlexMessage(replyToken, "📦 สถานะการสั่งซื้อ", carousel);
        } catch (Exception e) {
            log.error("[sendOrderStatus] error: {}", e.getMessage(), e);
            replyMessage(replyToken, "❌ เกิดข้อผิดพลาด กรุณาลองใหม่");
        }
    }

    private Map<String, Object> buildOrderBubble(Order order) {
        String statusColor;
        String statusEmoji;
        switch (order.getStatus()) {
            case PENDING    -> { statusColor = "#8B6914"; statusEmoji = "⏳ รอชำระเงิน";     }
            case PROCESSING -> { statusColor = "#2C5282"; statusEmoji = "🔄 กำลังดำเนินการ"; }
            case RECEIVED   -> { statusColor = "#553C7B"; statusEmoji = "🚚 จัดส่งแล้ว";     }
            case COMPLETED  -> { statusColor = C_PRIMARY; statusEmoji = "✅ สำเร็จ";           }
            case CANCELLED  -> { statusColor = "#6B5B3E"; statusEmoji = "❌ ยกเลิกแล้ว";     }
            case REFUNDED   -> { statusColor = C_DANGER;  statusEmoji = "💸 คืนเงินแล้ว";    }
            default         -> { statusColor = C_MUTED;   statusEmoji = "❓ ไม่ทราบสถานะ";   }
        }

        Map<String, Object> statusText = new HashMap<>();
        statusText.put("type", "text");
        statusText.put("text", statusEmoji);
        statusText.put("weight", "bold");
        statusText.put("size", "md");
        statusText.put("color", C_WHITE);

        Map<String, Object> dateText = new HashMap<>();
        dateText.put("type", "text");
        dateText.put("text", order.getCreatedAt().toLocalDate().toString());
        dateText.put("size", "xs");
        dateText.put("color", "#E8DCC8");
        dateText.put("margin", "xs");

        Map<String, Object> header = new HashMap<>();
        header.put("type", "box");
        header.put("layout", "vertical");
        header.put("backgroundColor", statusColor);
        header.put("paddingAll", "md");
        header.put("contents", List.of(statusText, dateText));

        List<Object> bodyContents = new ArrayList<>();

        Map<String, Object> orderNoLabel = new HashMap<>();
        orderNoLabel.put("type", "text");
        orderNoLabel.put("text", "หมายเลขออเดอร์");
        orderNoLabel.put("size", "xs");
        orderNoLabel.put("color", C_MUTED);

        String shortOrderNo = order.getOrderNo() != null
                ? order.getOrderNo().substring(0, 8).toUpperCase() + "..."
                : "-";
        Map<String, Object> orderNoValue = new HashMap<>();
        orderNoValue.put("type", "text");
        orderNoValue.put("text", shortOrderNo);
        orderNoValue.put("size", "sm");
        orderNoValue.put("color", C_TEXT);
        orderNoValue.put("weight", "bold");
        orderNoValue.put("margin", "xs");

        bodyContents.add(orderNoLabel);
        bodyContents.add(orderNoValue);

        Map<String, Object> sep = new HashMap<>();
        sep.put("type", "separator");
        sep.put("margin", "md");
        bodyContents.add(sep);

        List<OrderItem> items = new ArrayList<>(order.getOrderItems());
        double total = 0;
        for (OrderItem item : items) {
            double subTotal = item.getProduct().getPrice() * item.getQuantity();
            total += subTotal;

            Map<String, Object> itemName = new HashMap<>();
            itemName.put("type", "text");
            itemName.put("text", item.getProduct().getName() + " x" + item.getQuantity());
            itemName.put("size", "sm");
            itemName.put("color", C_SUBTEXT);
            itemName.put("flex", 4);
            itemName.put("wrap", true);

            Map<String, Object> itemPrice = new HashMap<>();
            itemPrice.put("type", "text");
            itemPrice.put("text", String.format("฿%.0f", subTotal));
            itemPrice.put("size", "sm");
            itemPrice.put("color", C_TEXT);
            itemPrice.put("align", "end");
            itemPrice.put("flex", 2);

            Map<String, Object> itemRow = new HashMap<>();
            itemRow.put("type", "box");
            itemRow.put("layout", "horizontal");
            itemRow.put("margin", "sm");
            itemRow.put("contents", List.of(itemName, itemPrice));
            bodyContents.add(itemRow);
        }

        Map<String, Object> totalSep = new HashMap<>();
        totalSep.put("type", "separator");
        totalSep.put("margin", "md");
        bodyContents.add(totalSep);

        Map<String, Object> totalLabel = new HashMap<>();
        totalLabel.put("type", "text");
        totalLabel.put("text", "ยอดรวม");
        totalLabel.put("weight", "bold");
        totalLabel.put("size", "sm");
        totalLabel.put("color", C_TEXT);
        totalLabel.put("flex", 3);

        Map<String, Object> totalValue = new HashMap<>();
        totalValue.put("type", "text");
        totalValue.put("text", String.format("฿%.0f", total));
        totalValue.put("weight", "bold");
        totalValue.put("size", "sm");
        totalValue.put("color", C_PRICE);
        totalValue.put("align", "end");
        totalValue.put("flex", 2);

        Map<String, Object> totalRow = new HashMap<>();
        totalRow.put("type", "box");
        totalRow.put("layout", "horizontal");
        totalRow.put("margin", "md");
        totalRow.put("contents", List.of(totalLabel, totalValue));
        bodyContents.add(totalRow);

        Map<String, Object> body = new HashMap<>();
        body.put("type", "box");
        body.put("layout", "vertical");
        body.put("paddingAll", "md");
        body.put("backgroundColor", C_BG);
        body.put("contents", bodyContents);

        Map<String, Object> bubble = new HashMap<>();
        bubble.put("type", "bubble");
        bubble.put("size", "mega");
        bubble.put("header", header);
        bubble.put("body", body);

        if (order.getStatus() == OrderStatusName.PENDING
                && order.getStripePaymentIntent() != null) {
            Map<String, Object> qrAction = new HashMap<>();
            qrAction.put("type", "postback");
            qrAction.put("label", "📱 ดู QR Code");
            qrAction.put("data", "action=show_qr&pi=" + order.getStripePaymentIntent());
            qrAction.put("displayText", "ขอดู QR Code");

            Map<String, Object> qrBtn = new HashMap<>();
            qrBtn.put("type", "button");
            qrBtn.put("style", "primary");
            qrBtn.put("color", C_GOLD);
            qrBtn.put("action", qrAction);

            Map<String, Object> footer = new HashMap<>();
            footer.put("type", "box");
            footer.put("layout", "vertical");
            footer.put("backgroundColor", C_BG);
            footer.put("contents", List.of(qrBtn));
            bubble.put("footer", footer);
        }

        return bubble;
    }

    // ─── Address Management ───────────────────────────────────────────────────

    private void sendAddressManagement(String replyToken, User user) {
        List<UserAddress> addresses = userAddressRepository
                .findByUserIdOrderByIsDefaultDescCreatedAtDesc(user.getId());

        boolean hasAddress = !addresses.isEmpty();
        boolean hasPhone   = user.getPhone() != null && !user.getPhone().isBlank();
        List<Object> bodyContents = new ArrayList<>();

        if (!hasAddress) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("type", "text");
            empty.put("text", "ยังไม่มีที่อยู่จัดส่ง\nกรุณาเพิ่มที่อยู่ของคุณ");
            empty.put("color", C_MUTED);
            empty.put("size", "sm");
            empty.put("wrap", true);
            bodyContents.add(empty);
        } else {
            UserAddress addr = addresses.get(0);
            Zipcode zip = addr.getZipcode();

            String street      = addr.getStreetAddress() != null ? addr.getStreetAddress() : "-";
            String subdistrict = (zip != null && zip.getSubdistrict() != null) ? zip.getSubdistrict().getName() : "-";
            String district    = (zip != null && zip.getDistrict()    != null) ? zip.getDistrict().getName()    : "-";
            String province    = (zip != null && zip.getProvince()    != null) ? zip.getProvince().getName()    : "-";
            String zipcodeStr  = (zip != null && zip.getZipcode()     != null) ? zip.getZipcode()               : "-";

            bodyContents.add(addressRow("🏠 บ้านเลขที่/ถนน", street));
            bodyContents.add(addressRow("🏘️ ตำบล/แขวง", subdistrict));
            bodyContents.add(addressRow("🏙️ อำเภอ/เขต", district));
            bodyContents.add(addressRow("🌏 จังหวัด", province));
            bodyContents.add(addressRow("📮 รหัสไปรษณีย์", zipcodeStr));
        }

        Map<String, Object> phoneSep = new HashMap<>();
        phoneSep.put("type", "separator");
        phoneSep.put("margin", "md");
        bodyContents.add(phoneSep);
        bodyContents.add(addressRow("📱 เบอร์โทรศัพท์", hasPhone ? user.getPhone() : "⚠️ ยังไม่ได้ระบุ"));

        Map<String, Object> headerText = new HashMap<>();
        headerText.put("type", "text");
        headerText.put("text", "📍 ที่อยู่จัดส่งของคุณ");
        headerText.put("weight", "bold");
        headerText.put("size", "lg");
        headerText.put("color", C_WHITE);
        Map<String, Object> header = new HashMap<>();
        header.put("type", "box");
        header.put("layout", "vertical");
        header.put("backgroundColor", C_PRIMARY);
        header.put("paddingAll", "md");
        header.put("contents", List.of(headerText));

        Map<String, Object> body = new HashMap<>();
        body.put("type", "box");
        body.put("layout", "vertical");
        body.put("spacing", "sm");
        body.put("paddingAll", "md");
        body.put("backgroundColor", C_BG);
        body.put("contents", bodyContents);

        String addrLabel   = hasAddress ? "✏️ แก้ไขที่อยู่" : "➕ เพิ่มที่อยู่";
        String addrDisplay = hasAddress ? "แก้ไขที่อยู่"     : "เพิ่มที่อยู่";
        Map<String, Object> addrAction = new HashMap<>();
        addrAction.put("type", "postback");
        addrAction.put("label", addrLabel);
        addrAction.put("data", "action=add_address");
        addrAction.put("displayText", addrDisplay);
        Map<String, Object> addrBtn = new HashMap<>();
        addrBtn.put("type", "button");
        addrBtn.put("style", "primary");
        addrBtn.put("color", C_PRIMARY);
        addrBtn.put("action", addrAction);

        String phoneLabel   = hasPhone ? "✏️ แก้ไขเบอร์โทร" : "➕ เพิ่มเบอร์โทร";
        String phoneDisplay = hasPhone ? "แก้ไขเบอร์โทร"     : "เพิ่มเบอร์โทร";
        Map<String, Object> phoneAction = new HashMap<>();
        phoneAction.put("type", "postback");
        phoneAction.put("label", phoneLabel);
        phoneAction.put("data", "action=edit_phone");
        phoneAction.put("displayText", phoneDisplay);
        Map<String, Object> phoneBtn = new HashMap<>();
        phoneBtn.put("type", "button");
        phoneBtn.put("style", "primary");
        phoneBtn.put("color", C_GOLD);
        phoneBtn.put("action", phoneAction);

        Map<String, Object> footer = new HashMap<>();
        footer.put("type", "box");
        footer.put("layout", "vertical");
        footer.put("spacing", "sm");
        footer.put("backgroundColor", C_BG);
        footer.put("contents", List.of(addrBtn, phoneBtn));

        Map<String, Object> bubble = new HashMap<>();
        bubble.put("type", "bubble");
        bubble.put("header", header);
        bubble.put("body", body);
        bubble.put("footer", footer);

        replyFlexMessage(replyToken, "📍 ที่อยู่จัดส่ง", bubble);
    }

    private Map<String, Object> addressRow(String label, String value) {
        Map<String, Object> labelText = new HashMap<>();
        labelText.put("type", "text");
        labelText.put("text", label);
        labelText.put("size", "xs");
        labelText.put("color", C_MUTED);
        labelText.put("flex", 3);

        Map<String, Object> valueText = new HashMap<>();
        valueText.put("type", "text");
        valueText.put("text", value);
        valueText.put("size", "sm");
        valueText.put("color", C_TEXT);
        valueText.put("flex", 5);
        valueText.put("wrap", true);

        Map<String, Object> row = new HashMap<>();
        row.put("type", "box");
        row.put("layout", "horizontal");
        row.put("margin", "sm");
        row.put("contents", List.of(labelText, valueText));
        return row;
    }

    // ─── Address Flow ─────────────────────────────────────────────────────────

    private void sendRegionSelection(String replyToken) {
        List<Geography> regions = geographyRepository.findAll();
        List<Map<String, Object>> items = new ArrayList<>();
        for (Geography geo : regions) {
            items.add(Map.<String, Object>of("label", geo.getName(),
                    "data", "action=select_region&id=" + geo.getId()));
        }
        Map<String, Object> carousel = buildSelectionCarousel("🗺️ เลือกภาค", "กรุณาเลือกภาคของคุณ", items, null);
        replyFlexMessage(replyToken, "เลือกภาค", carousel);
    }

    private void sendProvinceSelection(String replyToken, Integer geoId) {
        List<Province> provinces = provinceRepository.findByGeography_Id(geoId);
        String geoName = geographyRepository.findById(geoId).map(Geography::getName).orElse("");
        List<Map<String, Object>> items = provinces.stream()
                .map(p -> Map.<String, Object>of("label", p.getName(),
                        "data", "action=select_p&id=" + p.getId()))
                .toList();
        Map<String, Object> carousel = buildSelectionCarousel(
                "📍 เลือกจังหวัด (" + geoName + ")", "กรุณาเลือกจังหวัดของคุณ",
                items, "action=go_back&to=region");
        replyFlexMessage(replyToken, "เลือกจังหวัด", carousel);
    }

    private void sendDistrictSelection(String replyToken, Long provinceId) {
        Province province = provinceRepository.findById(provinceId).orElse(null);
        if (province == null) { replyMessage(replyToken, "❌ ไม่พบข้อมูลจังหวัด"); return; }
        Integer geoId = province.getGeography() != null ? province.getGeography().getId() : 0;
        List<District> districts = districtRepository.findByProvinceId(provinceId);
        List<Map<String, Object>> items = districts.stream()
                .map(d -> Map.<String, Object>of("label", d.getName(),
                        "data", "action=select_d&id=" + d.getId() + "&province_id=" + provinceId))
                .toList();
        Map<String, Object> carousel = buildSelectionCarousel(
                "📍 เลือกอำเภอ (" + province.getName() + ")", "กรุณาเลือกอำเภอของคุณ",
                items, "action=go_back&to=province&geo_id=" + geoId);
        replyFlexMessage(replyToken, "เลือกอำเภอ", carousel);
    }

    private void sendSubdistrictSelection(String replyToken, Long districtId, Long provinceId) {
        String districtName = districtRepository.findById(districtId).map(District::getName).orElse("");
        List<Subdistrict> subdistricts = subdistrictRepository.findByDistrictId(districtId);
        List<Map<String, Object>> items = subdistricts.stream()
                .map(s -> Map.<String, Object>of("label", s.getName(),
                        "data", "action=select_s&id=" + s.getId()
                                + "&district_id=" + districtId
                                + "&province_id=" + provinceId))
                .toList();
        Map<String, Object> carousel = buildSelectionCarousel(
                "📍 เลือกตำบล (" + districtName + ")", "กรุณาเลือกตำบลของคุณ",
                items, "action=go_back&to=district&province_id=" + provinceId);
        replyFlexMessage(replyToken, "เลือกตำบล", carousel);
    }

    private void handleGoBack(String replyToken, Map<String, String> params) {
        switch (params.getOrDefault("to", "region")) {
            case "region"   -> sendRegionSelection(replyToken);
            case "province" -> sendProvinceSelection(replyToken,
                    Integer.parseInt(params.getOrDefault("geo_id", "1")));
            case "district" -> sendDistrictSelection(replyToken,
                    Long.parseLong(params.getOrDefault("province_id", "1")));
            default         -> sendRegionSelection(replyToken);
        }
    }

    @Transactional
    private void handleStreetInput(String replyToken, String text, String lineUserId, User user) {
        if ("ยกเลิก".equals(text)) {
            addressSessions.remove(lineUserId);
            replyMessage(replyToken, "❌ ยกเลิกการเพิ่มที่อยู่แล้ว");
            return;
        }
        AddressSession session = addressSessions.get(lineUserId);
        if (session == null) { replyMessage(replyToken, "❌ เกิดข้อผิดพลาด กรุณาเริ่มใหม่"); return; }

        if ("ย้อนกลับ".equals(text)) {
            addressSessions.remove(lineUserId);
            sendSubdistrictSelection(replyToken, session.districtId, session.provinceId);
            return;
        }
        try {
            Zipcode zipcode = zipcodeRepository.findBySubdistrictId(session.subdistrictId)
                    .orElseThrow(() -> new RuntimeException("ไม่พบรหัสไปรษณีย์"));

            List<UserAddress> existing = userAddressRepository
                    .findByUserIdOrderByIsDefaultDescCreatedAtDesc(user.getId());

            UserAddress address;
            boolean isEdit = !existing.isEmpty();
            if (isEdit) {
                address = existing.get(0);
                address.setStreetAddress(text);
                address.setZipcode(zipcode);
            } else {
                address = UserAddress.builder()
                        .user(user)
                        .streetAddress(text)
                        .zipcode(zipcode)
                        .isDefault(true)
                        .createdAt(LocalDateTime.now())
                        .build();
            }
            userAddressRepository.save(address);
            addressSessions.remove(lineUserId);

            String subName = zipcode.getSubdistrict() != null ? zipcode.getSubdistrict().getName() : "";
            String disName = zipcode.getDistrict()    != null ? zipcode.getDistrict().getName()    : "";
            String proName = zipcode.getProvince()    != null ? zipcode.getProvince().getName()    : "";
            String confirm = (isEdit ? "✅ แก้ไขที่อยู่สำเร็จ!" : "✅ บันทึกที่อยู่สำเร็จ!")
                    + "\n\n🏠 " + text
                    + "\n🏘️ ตำบล " + subName
                    + "\n🏙️ อำเภอ " + disName
                    + "\n🌏 จังหวัด " + proName
                    + "\n📮 " + zipcode.getZipcode();
            replyMessage(replyToken, confirm);
        } catch (Exception e) {
            log.error("[handleStreetInput] error: {}", e.getMessage(), e);
            replyMessage(replyToken, "❌ เกิดข้อผิดพลาด กรุณาลองใหม่");
        }
    }

    // ─── Selection Carousel Builder ───────────────────────────────────────────

    private Map<String, Object> buildSelectionCarousel(
            String title, String subtitle,
            List<Map<String, Object>> items, String backData) {

        List<Map<String, Object>> bubbles = new ArrayList<>();
        for (int i = 0; i < Math.max(items.size(), 1); i += 20) {
            List<Map<String, Object>> page = new ArrayList<>(
                    items.subList(i, Math.min(i + 20, items.size())));
            boolean hasMore = (i + 20) < items.size();
            String sub = hasMore ? "กรุณาเลือก หรือปัดขวาเพื่อดูเพิ่มเติม" : subtitle;
            bubbles.add(buildSelectionBubble(title, sub, page, backData));
        }
        Map<String, Object> carousel = new HashMap<>();
        carousel.put("type", "carousel");
        carousel.put("contents", bubbles);
        return carousel;
    }

    private Map<String, Object> buildSelectionBubble(
            String title, String subtitle,
            List<Map<String, Object>> pageItems, String backData) {

        Map<String, Object> titleText = new HashMap<>();
        titleText.put("type", "text");
        titleText.put("text", title);
        titleText.put("weight", "bold");
        titleText.put("size", "lg");
        titleText.put("color", C_WHITE);
        titleText.put("wrap", true);
        Map<String, Object> subText = new HashMap<>();
        subText.put("type", "text");
        subText.put("text", subtitle);
        subText.put("size", "xs");
        subText.put("color", "#C8DDB0");
        subText.put("margin", "xs");
        Map<String, Object> header = new HashMap<>();
        header.put("type", "box");
        header.put("layout", "vertical");
        header.put("backgroundColor", C_PRIMARY);
        header.put("paddingAll", "md");
        header.put("contents", List.of(titleText, subText));

        List<Object> columns = new ArrayList<>();
        if (pageItems.size() <= 10) {
            columns.add(buildButtonColumn(pageItems));
        } else {
            columns.add(buildButtonColumn(pageItems.subList(0, 10)));
            columns.add(buildButtonColumn(pageItems.subList(10, pageItems.size())));
        }
        Map<String, Object> body = new HashMap<>();
        body.put("type", "box");
        body.put("layout", "horizontal");
        body.put("spacing", "md");
        body.put("backgroundColor", C_BG);
        body.put("contents", columns);

        Map<String, Object> bubble = new HashMap<>();
        bubble.put("type", "bubble");
        bubble.put("size", "giga");
        bubble.put("header", header);
        bubble.put("body", body);

        if (backData != null) {
            Map<String, Object> backAction = new HashMap<>();
            backAction.put("type", "postback");
            backAction.put("label", "⬅️ ย้อนกลับ");
            backAction.put("data", backData);
            backAction.put("displayText", "ย้อนกลับ");
            Map<String, Object> backBtn = new HashMap<>();
            backBtn.put("type", "button");
            backBtn.put("style", "link");
            backBtn.put("height", "sm");
            backBtn.put("color", C_SUBTEXT);
            backBtn.put("action", backAction);
            Map<String, Object> footer = new HashMap<>();
            footer.put("type", "box");
            footer.put("layout", "vertical");
            footer.put("backgroundColor", C_BG);
            footer.put("contents", List.of(backBtn));
            bubble.put("footer", footer);
        }
        return bubble;
    }

    private Map<String, Object> buildButtonColumn(List<Map<String, Object>> items) {
        List<Object> buttons = new ArrayList<>();
        for (Map<String, Object> item : items) {
            Map<String, Object> action = new HashMap<>();
            action.put("type", "postback");
            action.put("label", item.get("label").toString());
            action.put("data", item.get("data").toString());
            action.put("displayText", "เลือก " + item.get("label").toString());
            Map<String, Object> btn = new HashMap<>();
            btn.put("type", "button");
            btn.put("style", "secondary");
            btn.put("height", "sm");
            btn.put("action", action);
            buttons.add(btn);
        }
        Map<String, Object> col = new HashMap<>();
        col.put("type", "box");
        col.put("layout", "vertical");
        col.put("spacing", "sm");
        col.put("flex", 1);
        col.put("contents", buttons);
        return col;
    }

    // ─── Product Flex ─────────────────────────────────────────────────────────

    private void sendProductsFlex(String replyToken) {
        try {
            ProductWithCategoryDTO products = productService.getProductsWithCategory();

            Map<Long, Integer> stockMap = productRepository.findAll().stream()
                    .collect(java.util.stream.Collectors.toMap(
                            com.sm.jeyz9.storemateapi.models.Product::getId,
                            p -> p.getStock_quantity() != null ? p.getStock_quantity() : 0));

            List<Map<String, Object>> messages = new ArrayList<>();

            buildCategoryMessage(messages, "🎁 โปรโมชัน", products.getPromotion(), stockMap);
            buildCategoryMessage(messages, "🧼 สบู่",       products.getSoap(),      stockMap);
            buildCategoryMessage(messages, "🥤 เครื่องดื่ม", products.getDrinks(),   stockMap);
            buildCategoryMessage(messages, "💆 แชมพู",      products.getShampoo(),   stockMap);

            if (messages.isEmpty()) {
                replyMessage(replyToken, "ยังไม่มีสินค้าในขณะนี้");
                return;
            }

            replyMessages(replyToken, messages);
        } catch (Exception e) {
            log.error("[sendProductsFlex] failed — replyToken={}, cause={}", replyToken, e.getMessage(), e);
            try {
                replyMessage(replyToken, "ไม่สามารถดึงข้อมูลสินค้าได้ในขณะนี้");
            } catch (Exception ex) {
                log.error("[sendProductsFlex] fallback replyMessage also failed: {}", ex.getMessage());
            }
        }
    }

    private void buildCategoryMessage(List<Map<String, Object>> messages,
                                      String categoryLabel,
                                      List<ProductDTO> items,
                                      Map<Long, Integer> stockMap) {
        if (items == null || items.isEmpty()) return;

        List<ProductDTO> available = items.stream()
                .filter(p -> "ACTIVE".equalsIgnoreCase(p.getProductStatus()))
                .filter(p -> stockMap.getOrDefault(p.getId(), 0) > 0)
                .toList();

        if (available.isEmpty()) return;

        List<Map<String, Object>> bubbles = new ArrayList<>();
        for (ProductDTO p : available) {
            bubbles.add(buildProductBubble(p));
        }

        Map<String, Object> carousel = new HashMap<>();
        carousel.put("type", "carousel");
        carousel.put("contents", bubbles);

        Map<String, Object> flexMessage = new HashMap<>();
        flexMessage.put("type", "flex");
        flexMessage.put("altText", categoryLabel);
        flexMessage.put("contents", carousel);

        messages.add(flexMessage);
    }

    private static final String PLACEHOLDER_IMAGE = "https://placehold.co/600x390/F5ECD7/9E8A6F/png?text=No+Image";

    private Map<String, Object> buildProductBubble(ProductDTO p) {
        String imageUrl = (p.getImageUrl() != null && !p.getImageUrl().isBlank())
                ? p.getImageUrl()
                : PLACEHOLDER_IMAGE;

        Map<String, Object> hero = new HashMap<>();
        hero.put("type", "image");
        hero.put("url", imageUrl);
        hero.put("size", "full");
        hero.put("aspectRatio", "20:13");
        hero.put("aspectMode", "cover");

        Map<String, Object> nameText = new HashMap<>();
        nameText.put("type", "text");
        nameText.put("text", p.getProductName());
        nameText.put("size", "lg");
        nameText.put("weight", "bold");
        nameText.put("color", C_TEXT);
        nameText.put("wrap", true);

        Map<String, Object> priceText = new HashMap<>();
        priceText.put("type", "text");
        priceText.put("text", String.format("฿%.0f", p.getPrice()));
        priceText.put("weight", "bold");
        priceText.put("size", "xl");
        priceText.put("color", C_PRICE);
        priceText.put("flex", 0);

        Map<String, Object> categoryText = new HashMap<>();
        categoryText.put("type", "text");
        categoryText.put("text", p.getCategoryName() != null ? p.getCategoryName() : "");
        categoryText.put("size", "xs");
        categoryText.put("align", "end");
        categoryText.put("color", C_MUTED);

        Map<String, Object> priceRow = new HashMap<>();
        priceRow.put("type", "box");
        priceRow.put("layout", "baseline");
        priceRow.put("contents", List.of(priceText, categoryText));

        Map<String, Object> infoBox = new HashMap<>();
        infoBox.put("type", "box");
        infoBox.put("layout", "vertical");
        infoBox.put("spacing", "sm");
        infoBox.put("contents", List.of(priceRow));

        Map<String, Object> descText = new HashMap<>();
        descText.put("type", "text");
        descText.put("text", p.getDescription() != null ? p.getDescription() : "");
        descText.put("wrap", true);
        descText.put("color", C_MUTED);
        descText.put("size", "xs");

        Map<String, Object> body = new HashMap<>();
        body.put("type", "box");
        body.put("layout", "vertical");
        body.put("spacing", "sm");
        body.put("paddingAll", "md");
        body.put("backgroundColor", C_BG);
        body.put("contents", List.of(nameText, infoBox, descText));

        Map<String, Object> btnAction = new HashMap<>();
        btnAction.put("type", "message");
        btnAction.put("label", "🛒 เพิ่มลงตะกร้า");
        btnAction.put("text", "เพิ่มสินค้า " + p.getId());

        Map<String, Object> button = new HashMap<>();
        button.put("type", "button");
        button.put("style", "primary");
        button.put("color", C_PRIMARY);
        button.put("margin", "md");
        button.put("action", btnAction);

        Map<String, Object> footer = new HashMap<>();
        footer.put("type", "box");
        footer.put("layout", "vertical");
        footer.put("backgroundColor", C_BG);
        footer.put("contents", List.of(button));

        Map<String, Object> bubble = new HashMap<>();
        bubble.put("type", "bubble");
        bubble.put("hero", hero);
        bubble.put("body", body);
        bubble.put("footer", footer);

        return bubble;
    }

    // ─── Payment Notification ─────────────────────────────────────────────────

    @Override
    public void pushPaymentSuccess(String lineUserId, String orderNo, double total) {
        String msg = "✅ ชำระเงินสำเร็จ!\n\n"
                + "📋 หมายเลขออเดอร์: " + orderNo + "\n"
                + String.format("💰 ยอด: ฿%.0f\n\n", total)
                + "📦 ร้านกำลังเตรียมสินค้าให้คุณครับ\n"
                + "พิมพ์ \"สถานะสินค้า\" เพื่อติดตามออเดอร์";
        pushMessage(lineUserId, msg);
    }

    // ─── Phone Number ─────────────────────────────────────────────────────────

    private void handlePhoneInput(String replyToken, String text, String lineUserId, User user) {
        if ("ยกเลิก".equals(text)) {
            phoneSessions.remove(lineUserId);
            replyMessage(replyToken, "❌ ยกเลิกการแจ้งเบอร์แล้ว");
            return;
        }
        if (user == null) {
            phoneSessions.remove(lineUserId);
            replyMessage(replyToken, "⚠️ ไม่พบข้อมูลผู้ใช้ กรุณาลองใหม่");
            return;
        }
        String digits = text.replaceAll("[\\s\\-]", "");
        if (!digits.matches("0[6-9]\\d{8}")) {
            replyMessage(replyToken, "⚠️ รูปแบบเบอร์ไม่ถูกต้อง\nกรุณาพิมพ์เบอร์ 10 หลัก เช่น 0812345678\n\nพิมพ์ \"ยกเลิก\" เพื่อยกเลิก");
            return;
        }
        user.setPhone(digits);
        userRepository.save(user);
        phoneSessions.remove(lineUserId);
        replyMessage(replyToken, "✅ บันทึกเบอร์โทรศัพท์สำเร็จ!\n📱 " + digits);
    }

    // ─── Store Contact ────────────────────────────────────────────────────────

    private void sendStoreContact(String replyToken) {
        try {
            com.sm.jeyz9.storemateapi.dto.StoreInfoDTO store = storeInfoService.getStoreDetails();
            StringBuilder sb = new StringBuilder();
            sb.append("📞 ติดต่อเรา\n");
            if (store.getStoreName()     != null) sb.append("🏪 ").append(store.getStoreName()).append("\n");
            if (store.getPhone()         != null) sb.append("📱 โทร: ").append(store.getPhone()).append("\n");
            if (store.getEmail()         != null) sb.append("📧 อีเมล: ").append(store.getEmail()).append("\n");
            if (store.getStreetAddress() != null || store.getSubdistrict() != null) {
                sb.append("📍 ที่อยู่: ");
                if (store.getStreetAddress() != null) sb.append(store.getStreetAddress()).append(" ");
                if (store.getSubdistrict()   != null) sb.append(store.getSubdistrict()).append(" ");
                if (store.getDistrict()      != null) sb.append(store.getDistrict()).append(" ");
                if (store.getProvince()      != null) sb.append(store.getProvince()).append(" ");
                if (store.getZipcode()       != null) sb.append(store.getZipcode());
            }
            replyMessage(replyToken, sb.toString().trim());
        } catch (Exception e) {
            log.warn("ดึงข้อมูลร้านค้าไม่ได้: {}", e.getMessage());
            replyMessage(replyToken, "ขออภัย ไม่สามารถดึงข้อมูลร้านค้าได้ในขณะนี้");
        }
    }

    // ─── LINE API Helpers ─────────────────────────────────────────────────────

    private void replyMessage(String replyToken, String text) {
        String url = "https://api.line.me/v2/bot/message/reply";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(lineConfig.getChannelToken());

        Map<String, Object> body = new HashMap<>();
        body.put("replyToken", replyToken);
        body.put("messages", List.of(Map.of("type", "text", "text", text)));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        restTemplate.postForEntity(url, request, String.class);
    }

    private void replyMessages(String replyToken, List<Map<String, Object>> messages) {
        String url = "https://api.line.me/v2/bot/message/reply";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(lineConfig.getChannelToken());

        Map<String, Object> body = new HashMap<>();
        body.put("replyToken", replyToken);
        body.put("messages", messages);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        restTemplate.postForEntity(url, request, String.class);
    }

    private void replyFlexMessage(String replyToken, String altText, Map<String, Object> contents) {
        String url = "https://api.line.me/v2/bot/message/reply";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(lineConfig.getChannelToken());

        Map<String, Object> flexMessage = new HashMap<>();
        flexMessage.put("type", "flex");
        flexMessage.put("altText", altText);
        flexMessage.put("contents", contents);

        Map<String, Object> body = new HashMap<>();
        body.put("replyToken", replyToken);
        body.put("messages", List.of(flexMessage));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        restTemplate.postForEntity(url, request, String.class);
    }
}
