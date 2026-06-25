package com.sm.jeyz9.storemateapi.services;

public interface LinePaymentService {

    /**
     * สร้าง PromptPay PaymentIntent + Order จาก cart ของ Line user
     * คืนค่า QR code URL, orderNo และยอดรวม
     */
    LineCheckoutResult createPromptPayCheckout(Long userId);

    /**
     * ดึง QR code URL จาก Stripe PaymentIntent ที่มีอยู่แล้ว (สำหรับ order ที่ PENDING)
     */
    String getQrCodeUrl(String stripePaymentIntentId);

    /**
     * สร้าง QR code ใหม่สำหรับ order ที่ PENDING แต่ PI เดิมหมดอายุแล้ว
     * อัปเดต stripePaymentIntent ใน order และคืน QR URL ใหม่
     */
    String renewQrCode(String oldPaymentIntentId);

    record LineCheckoutResult(String orderNo, String qrCodeUrl, double totalAmount) {}
}
