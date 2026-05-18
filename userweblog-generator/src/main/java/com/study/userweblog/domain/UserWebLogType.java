package com.study.userweblog.domain;

/**
 * 사용자 행동 이벤트 타입
 *
 * PAGE_VIEW  - 사용자가 특정 페이지를 조회한 행동
 * PAGE_LEAVE - 사용자가 페이지를 이탈한 행동 (체류시간 추적용)
 * CLICK      - 사용자가 특정 요소(버튼, 링크 등)를 클릭한 행동
 * PURCHASE   - 사용자가 상품을 구매 완료한 행동
 */
public enum UserWebLogType {
    PAGE_VIEW,
    PAGE_LEAVE,
    CLICK,
    PURCHASE,
    LOGIN,
    SIGN_UP
}
