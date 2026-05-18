package com.study.userweblog.event;

import com.study.userweblog.domain.UserWebLogType;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * 세션 단위 이벤트 시퀀스 생성기
 *
 *   BOUNCE        (20%) : 진입 → 이탈
 *   BROWSE        (25%) : 홈 → 상품목록 → 상품상세 → 이탈
 *   CART_ABANDON  (20%) : BROWSE + 장바구니 → 이탈
 *   PURCHASE      (10%) : CART + 결제 → 구매 → 이탈
 *   LOGIN         (13%) : 홈 → 로그인 → 홈 → 이탈
 *   SIGN_UP       ( 7%) : 홈 → 회원가입 → 완료 → 이탈
 *   BOT           (15%) : 초단위 과도 접속 + 비정상 구매
 */
@Component
public class UserSessionFactory {

    private static final Random RANDOM = new Random();

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

    private static final List<String> PRODUCT_PAGES = List.of(
            "/products/101", "/products/202", "/products/303", "/products/404", "/products/505"
    );

    private static final List<String> PRODUCT_IDS = List.of(
            "PROD-101", "PROD-202", "PROD-303", "PROD-404", "PROD-505"
    );

    // ── public API ────────────────────────────────────────────────────────────

    public List<UserWebLogEvent> createSession(LocalDateTime startTime) {
        boolean isBot = RANDOM.nextInt(100) < 15;

        String userId    = "user-"    + UUID.randomUUID().toString().substring(0, 8);
        String sessionId = "session-" + UUID.randomUUID().toString().substring(0, 12);
        String ip        = isBot ? randomFrom(BOT_IPS) : randomFrom(NORMAL_IPS);
        String utmMedium = RANDOM.nextInt(100) < 30 ? null : randomFrom(UTM_MEDIUMS);

        if (isBot) return buildBotSession(userId, sessionId, ip, utmMedium, startTime);

        int roll = RANDOM.nextInt(100);
        if (roll < 20) return buildBounceSession     (userId, sessionId, ip, utmMedium, startTime);
        if (roll < 45) return buildBrowseSession     (userId, sessionId, ip, utmMedium, startTime);
        if (roll < 65) return buildCartAbandonSession(userId, sessionId, ip, utmMedium, startTime);
        if (roll < 75) return buildPurchaseSession   (userId, sessionId, ip, utmMedium, startTime);
        if (roll < 88) return buildLoginSession      (userId, sessionId, ip, utmMedium, startTime);
        return              buildSignUpSession       (userId, sessionId, ip, utmMedium, startTime);
    }

    // ── 세션 빌더 ─────────────────────────────────────────────────────────────

    /** 진입 → 이탈 */
    private List<UserWebLogEvent> buildBounceSession(
            String userId, String sessionId, String ip, String utm, LocalDateTime start) {
        Seq seq = new Seq(start);
        String page = RANDOM.nextBoolean() ? "/home" : "/products";
        return List.of(
                pageView (userId, sessionId, ip, utm, page, seq.next(0, 0)),
                pageLeave(userId, sessionId, ip, utm, page, rand(5, 30), seq.next(5, 30))
        );
    }

    /** 홈 → 상품목록 → 상품상세 → 이탈 */
    private List<UserWebLogEvent> buildBrowseSession(
            String userId, String sessionId, String ip, String utm, LocalDateTime start) {
        Seq seq = new Seq(start);
        String productPage = randomFrom(PRODUCT_PAGES);
        return List.of(
                pageView (userId, sessionId, ip, utm, "/home",     seq.next(0, 0)),
                click    (userId, sessionId, ip, utm, "/home",     "nav-link-products", seq.next(10, 40)),
                pageView (userId, sessionId, ip, utm, "/products", seq.next(1, 3)),
                click    (userId, sessionId, ip, utm, "/products", "product-thumbnail", seq.next(15, 60)),
                pageView (userId, sessionId, ip, utm, productPage, seq.next(1, 3)),
                pageLeave(userId, sessionId, ip, utm, productPage, rand(30, 120), seq.next(30, 120))
        );
    }

    /** BROWSE + 장바구니 담기 → 장바구니 → 이탈 */
    private List<UserWebLogEvent> buildCartAbandonSession(
            String userId, String sessionId, String ip, String utm, LocalDateTime start) {
        Seq seq = new Seq(start);
        String productPage = randomFrom(PRODUCT_PAGES);
        return List.of(
                pageView (userId, sessionId, ip, utm, "/home",     seq.next(0, 0)),
                click    (userId, sessionId, ip, utm, "/home",     "nav-link-products", seq.next(10, 40)),
                pageView (userId, sessionId, ip, utm, "/products", seq.next(1, 3)),
                click    (userId, sessionId, ip, utm, "/products", "product-thumbnail", seq.next(15, 60)),
                pageView (userId, sessionId, ip, utm, productPage, seq.next(1, 3)),
                click    (userId, sessionId, ip, utm, productPage, "btn-add-to-cart",   seq.next(20, 90)),
                pageView (userId, sessionId, ip, utm, "/cart",     seq.next(1, 3)),
                pageLeave(userId, sessionId, ip, utm, "/cart",     rand(30, 180), seq.next(30, 180))
        );
    }

    /** CART + 결제 → 구매 → 이탈 */
    private List<UserWebLogEvent> buildPurchaseSession(
            String userId, String sessionId, String ip, String utm, LocalDateTime start) {
        Seq seq = new Seq(start);
        int idx = RANDOM.nextInt(PRODUCT_PAGES.size());
        String productPage = PRODUCT_PAGES.get(idx);
        String productId   = PRODUCT_IDS.get(idx);
        long amount = (RANDOM.nextInt(20) + 1) * 5000L;
        return List.of(
                pageView (userId, sessionId, ip, utm, "/home",     seq.next(0, 0)),
                click    (userId, sessionId, ip, utm, "/home",     "nav-link-products", seq.next(10, 40)),
                pageView (userId, sessionId, ip, utm, "/products", seq.next(1, 3)),
                click    (userId, sessionId, ip, utm, "/products", "product-thumbnail", seq.next(15, 60)),
                pageView (userId, sessionId, ip, utm, productPage, seq.next(1, 3)),
                click    (userId, sessionId, ip, utm, productPage, "btn-add-to-cart",   seq.next(20, 90)),
                pageView (userId, sessionId, ip, utm, "/cart",     seq.next(1, 3)),
                pageView (userId, sessionId, ip, utm, "/checkout", seq.next(10, 30)),
                purchase (userId, sessionId, ip, utm, productId, amount, seq.next(30, 120)),
                pageLeave(userId, sessionId, ip, utm, "/checkout", rand(10, 30), seq.next(10, 30))
        );
    }

    /** 홈 → 로그인 페이지 → LOGIN → 홈 → 이탈 */
    private List<UserWebLogEvent> buildLoginSession(
            String userId, String sessionId, String ip, String utm, LocalDateTime start) {
        Seq seq = new Seq(start);
        return List.of(
                pageView(userId, sessionId, ip, utm, "/home",   seq.next(0, 0)),
                click   (userId, sessionId, ip, utm, "/home",   "btn-login", seq.next(5, 20)),
                pageView(userId, sessionId, ip, utm, "/login",  seq.next(1, 2)),
                login   (userId, sessionId, ip, utm,            seq.next(10, 40)),
                pageView(userId, sessionId, ip, utm, "/home",   seq.next(1, 2)),
                pageLeave(userId, sessionId, ip, utm, "/home",  rand(10, 60), seq.next(10, 60))
        );
    }

    /** 홈 → 회원가입 페이지 → SIGN_UP → 완료 페이지 → 이탈 */
    private List<UserWebLogEvent> buildSignUpSession(
            String userId, String sessionId, String ip, String utm, LocalDateTime start) {
        Seq seq = new Seq(start);
        return List.of(
                pageView(userId, sessionId, ip, utm, "/home",      seq.next(0, 0)),
                click   (userId, sessionId, ip, utm, "/home",      "btn-signup", seq.next(5, 20)),
                pageView(userId, sessionId, ip, utm, "/signup",    seq.next(1, 2)),
                signUp  (userId, sessionId, ip, utm,               seq.next(30, 120)),
                pageView(userId, sessionId, ip, utm, "/signup/done", seq.next(1, 2)),
                pageLeave(userId, sessionId, ip, utm, "/signup/done", rand(5, 20), seq.next(5, 20))
        );
    }

    /** 봇: 초단위 과도 접속 + 비정상 구매 */
    private List<UserWebLogEvent> buildBotSession(
            String userId, String sessionId, String ip, String utm, LocalDateTime start) {
        Seq seq = new Seq(start);
        List<UserWebLogEvent> events = new ArrayList<>();
        List<String> pages = List.of("/home", "/products", "/search", "/event", "/coupon");

        for (int i = 0; i < 60; i++) {
            events.add(pageView(userId, sessionId, ip, utm, randomFrom(pages), seq.next(1, 2)));
        }

        int purchaseCount = RANDOM.nextInt(5) + 3;
        for (int i = 0; i < purchaseCount; i++) {
            int idx = RANDOM.nextInt(PRODUCT_IDS.size());
            events.add(purchase(userId, sessionId, ip, utm, PRODUCT_IDS.get(idx),
                    (RANDOM.nextInt(20) + 1) * 5000L, seq.next(1, 2)));
        }
        return events;
    }

    // ── 이벤트 생성 헬퍼 ──────────────────────────────────────────────────────

    private UserWebLogEvent pageView(String userId, String sessionId, String ip,
                                     String utm, String uri, LocalDateTime time) {
        return base(userId, sessionId, ip, utm, time).eventType(UserWebLogType.PAGE_VIEW).uri(uri).build();
    }

    private UserWebLogEvent pageLeave(String userId, String sessionId, String ip,
                                      String utm, String uri, long duration, LocalDateTime time) {
        return base(userId, sessionId, ip, utm, time)
                .eventType(UserWebLogType.PAGE_LEAVE).uri(uri).durationSeconds(duration).build();
    }

    private UserWebLogEvent click(String userId, String sessionId, String ip,
                                  String utm, String uri, String targetId, LocalDateTime time) {
        return base(userId, sessionId, ip, utm, time)
                .eventType(UserWebLogType.CLICK).uri(uri).targetId(targetId).build();
    }

    private UserWebLogEvent purchase(String userId, String sessionId, String ip,
                                     String utm, String productId, long amount, LocalDateTime time) {
        return base(userId, sessionId, ip, utm, time)
                .eventType(UserWebLogType.PURCHASE).uri("/checkout").targetId(productId).amount(amount).build();
    }

    private UserWebLogEvent login(String userId, String sessionId, String ip,
                                  String utm, LocalDateTime time) {
        return base(userId, sessionId, ip, utm, time).eventType(UserWebLogType.LOGIN).uri("/login").build();
    }

    private UserWebLogEvent signUp(String userId, String sessionId, String ip,
                                   String utm, LocalDateTime time) {
        return base(userId, sessionId, ip, utm, time).eventType(UserWebLogType.SIGN_UP).uri("/signup").build();
    }

    private UserWebLogEvent.UserWebLogEventBuilder base(String userId, String sessionId,
                                                        String ip, String utm, LocalDateTime time) {
        return UserWebLogEvent.builder()
                .userId(userId).sessionId(sessionId).ip(ip).utmMedium(utm).occurredAt(time);
    }

    // ── 유틸 ─────────────────────────────────────────────────────────────────

    private long rand(int min, int max) { return RANDOM.nextInt(max - min) + min; }

    private <T> T randomFrom(List<T> list) { return list.get(RANDOM.nextInt(list.size())); }

    private static class Seq {
        private LocalDateTime current;
        Seq(LocalDateTime start) { this.current = start; }
        LocalDateTime next(int min, int max) {
            if (max > min) current = current.plusSeconds(RANDOM.nextInt(max - min) + min);
            return current;
        }
    }
}
