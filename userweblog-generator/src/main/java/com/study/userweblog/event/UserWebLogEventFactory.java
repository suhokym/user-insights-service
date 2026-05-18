package com.study.userweblog.event;

import com.study.userweblog.domain.UserWebLogType;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

/**
 * 스케줄러용 단건 랜덤 이벤트 생성 팩토리
 *
 * PAGE_VIEW 40% / CLICK 20% / PAGE_LEAVE 17% / PURCHASE 8% / LOGIN 10% / SIGN_UP 5%
 */
@Component
public class UserWebLogEventFactory {

    private static final Random RANDOM = new Random();

    private static final List<String> PAGES = List.of(
            "/home", "/products", "/products/123", "/products/456", "/products/789",
            "/cart", "/checkout", "/mypage", "/search", "/event", "/coupon"
    );

    private static final List<String> CLICK_TARGETS = List.of(
            "btn-add-to-cart", "btn-buy-now", "nav-link-products",
            "banner-promotion", "btn-search", "product-thumbnail-456",
            "coupon-apply-btn", "btn-wishlist", "tab-review", "btn-share"
    );

    private static final List<String> PRODUCT_IDS = List.of(
            "PROD-001", "PROD-002", "PROD-003", "PROD-100", "PROD-250",
            "PROD-301", "PROD-402", "PROD-550"
    );

    private static final List<String> BOT_IPS = List.of(
            "192.168.100.1", "10.0.0.254", "172.16.0.99", "203.0.113.50", "198.51.100.77"
    );

    private static final List<String> NORMAL_IPS;
    static {
        var ips = new java.util.ArrayList<String>(50);
        for (int i = 1; i <= 50; i++) ips.add("211.200.1." + i);
        NORMAL_IPS = List.copyOf(ips);
    }

    private static final List<String> UTM_MEDIUMS = List.of(
            "광고", "소셜미디어", "웹검색", "이메일", "배너광고"
    );

    public UserWebLogEvent create(String userId, String sessionId) {
        return create(userId, sessionId, LocalDateTime.now());
    }

    public UserWebLogEvent create(String userId, String sessionId, LocalDateTime occurredAt) {
        boolean isBot = RANDOM.nextInt(100) < 15;
        String ip = isBot ? randomFrom(BOT_IPS) : randomFrom(NORMAL_IPS);
        String utmMedium = pickUtmMedium();

        UserWebLogType type = isBot ? pickBotEventType() : pickNormalEventType();

        return switch (type) {
            case PAGE_VIEW  -> buildPageView(userId, sessionId, ip, utmMedium, occurredAt);
            case PAGE_LEAVE -> buildPageLeave(userId, sessionId, ip, utmMedium, occurredAt);
            case CLICK      -> buildClick(userId, sessionId, ip, utmMedium, occurredAt);
            case PURCHASE   -> buildPurchase(userId, sessionId, ip, utmMedium, occurredAt);
            case LOGIN      -> buildLogin(userId, sessionId, ip, utmMedium, occurredAt);
            case SIGN_UP    -> buildSignUp(userId, sessionId, ip, utmMedium, occurredAt);
        };
    }

    private UserWebLogEvent buildPageView(String userId, String sessionId, String ip,
                                          String utmMedium, LocalDateTime occurredAt) {
        return base(userId, sessionId, ip, utmMedium, occurredAt)
                .eventType(UserWebLogType.PAGE_VIEW).uri(randomFrom(PAGES)).build();
    }

    private UserWebLogEvent buildPageLeave(String userId, String sessionId, String ip,
                                           String utmMedium, LocalDateTime occurredAt) {
        return base(userId, sessionId, ip, utmMedium, occurredAt)
                .eventType(UserWebLogType.PAGE_LEAVE)
                .uri(randomFrom(PAGES))
                .durationSeconds(RANDOM.nextInt(300) + 1L).build();
    }

    private UserWebLogEvent buildClick(String userId, String sessionId, String ip,
                                       String utmMedium, LocalDateTime occurredAt) {
        return base(userId, sessionId, ip, utmMedium, occurredAt)
                .eventType(UserWebLogType.CLICK)
                .uri(randomFrom(PAGES))
                .targetId(randomFrom(CLICK_TARGETS)).build();
    }

    private UserWebLogEvent buildPurchase(String userId, String sessionId, String ip,
                                          String utmMedium, LocalDateTime occurredAt) {
        return base(userId, sessionId, ip, utmMedium, occurredAt)
                .eventType(UserWebLogType.PURCHASE)
                .uri("/checkout")
                .targetId(randomFrom(PRODUCT_IDS))
                .amount((RANDOM.nextInt(20) + 1) * 5000L).build();
    }

    private UserWebLogEvent buildLogin(String userId, String sessionId, String ip,
                                       String utmMedium, LocalDateTime occurredAt) {
        return base(userId, sessionId, ip, utmMedium, occurredAt)
                .eventType(UserWebLogType.LOGIN).uri("/login").build();
    }

    private UserWebLogEvent buildSignUp(String userId, String sessionId, String ip,
                                        String utmMedium, LocalDateTime occurredAt) {
        return base(userId, sessionId, ip, utmMedium, occurredAt)
                .eventType(UserWebLogType.SIGN_UP).uri("/signup").build();
    }

    private UserWebLogEvent.UserWebLogEventBuilder base(String userId, String sessionId,
                                                        String ip, String utmMedium,
                                                        LocalDateTime occurredAt) {
        return UserWebLogEvent.builder()
                .userId(userId).sessionId(sessionId).ip(ip)
                .utmMedium(utmMedium).occurredAt(occurredAt);
    }

    /** PAGE_VIEW 40% / CLICK 20% / PAGE_LEAVE 17% / PURCHASE 8% / LOGIN 10% / SIGN_UP 5% */
    private UserWebLogType pickNormalEventType() {
        int roll = RANDOM.nextInt(100);
        if (roll < 40) return UserWebLogType.PAGE_VIEW;
        if (roll < 60) return UserWebLogType.CLICK;
        if (roll < 77) return UserWebLogType.PAGE_LEAVE;
        if (roll < 85) return UserWebLogType.PURCHASE;
        if (roll < 95) return UserWebLogType.LOGIN;
        return UserWebLogType.SIGN_UP;
    }

    private UserWebLogType pickBotEventType() {
        int roll = RANDOM.nextInt(100);
        if (roll < 70) return UserWebLogType.PAGE_VIEW;
        if (roll < 95) return UserWebLogType.CLICK;
        return UserWebLogType.PAGE_LEAVE;
    }

    private String pickUtmMedium() {
        if (RANDOM.nextInt(100) < 30) return null;
        return randomFrom(UTM_MEDIUMS);
    }

    private <T> T randomFrom(List<T> list) {
        return list.get(RANDOM.nextInt(list.size()));
    }
}
