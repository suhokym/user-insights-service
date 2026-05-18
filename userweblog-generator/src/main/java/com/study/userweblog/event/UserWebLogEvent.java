package com.study.userweblog.event;

import com.study.userweblog.domain.UserWebLogType;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Kafka로 발행되는 사용자 행동 로그 이벤트
 */
@Getter
@Builder
@ToString
public class UserWebLogEvent {

    /** 사용자 식별자 */
    private String userId;

    /** 세션 식별자 */
    private String sessionId;

    /** 접속 IP 주소 */
    private String ip;

    /** 행동 타입 (PAGE_VIEW / PAGE_LEAVE / CLICK / PURCHASE) */
    private UserWebLogType eventType;

    /** 행동이 발생한 페이지 URI */
    private String uri;

    /** 행동 대상 (버튼명, 상품ID 등 이벤트 타입별 컨텍스트) */
    private String targetId;

    /** 구매 이벤트의 경우 주문 금액 (PAGE_VIEW, PAGE_LEAVE, CLICK은 null) */
    private Long amount;

    /** 유입 매체 (광고, 소셜미디어, 웹검색, 이메일, 배너광고, null=직접유입) */
    private String utmMedium;

    /** 페이지 체류시간(초) - PAGE_LEAVE 이벤트에만 존재, 나머지는 null */
    private Long durationSeconds;

    /** 이벤트 발생 시각 */
    private LocalDateTime occurredAt;
}
