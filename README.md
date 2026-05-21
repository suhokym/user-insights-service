# User Insights Service

대용량 유저 행동 로그를 수집·집계하여 대시보드로 시각화하는 데이터 파이프라인 서비스입니다.

## 아키텍처

```
user-insights-service/
├── event-log-service       # Spring Boot + Apache Spark 집계 API 및 대시보드 (Port: 8080)
└── userweblog-generator    # 테스트용 유저 행동 이벤트 생성기
```

## 핵심 기술 스택

| 구분 | 기술 |
|------|------|
| Language | Java 17 / 21 |
| Framework | Spring Boot 3.5 |
| Event Storage | Elasticsearch 8.13 |
| Aggregation Engine | Apache Spark 3.5.1 |
| Database | MySQL 8.0 |
| Dashboard | Chart.js 4.4 (내장 정적 파일) |
| Build | Gradle |

## 모듈 설명

### event-log-service (Port: 8080)

Elasticsearch에 쌓인 유저 행동 로그를 Apache Spark로 집계하고, 결과를 MySQL에 저장한 뒤 내장 대시보드에서 시각화합니다.

- Elasticsearch Scroll API로 대용량 로그 조회
- Spark DataFrame으로 봇 IP 탐지 및 다차원 집계
- 집계 결과를 MySQL에 upsert
- `localhost:8080` 접속 시 Chart.js 기반 대시보드 즉시 확인

### userweblog-generator

실제 유저처럼 동작하는 합성 이벤트를 생성해 Elasticsearch에 인덱싱합니다. 개발·테스트 환경에서 데이터 시딩에 사용합니다.

- 애플리케이션 시작 시 과거 7일치 이벤트 일괄 생성 (기본 100,000건)
- 세션 단위로 PAGE_VIEW → CLICK → PURCHASE 흐름을 현실적으로 모델링
- 유입 채널(utmMedium): `ad`, `social_media`, `web_search`, `email`, `banner_ad`, `null`(직접유입)

## 전체 데이터 흐름

```
userweblog-generator
  │  ① 세션별 이벤트 생성 (PAGE_VIEW / PAGE_LEAVE / CLICK / PURCHASE / LOGIN / SIGN_UP)
  │  ② Elasticsearch 인덱스 userweblog-events 에 벌크 인덱싱
  ▼
Elasticsearch
  │
  ▼
event-log-service (집계 API)
  │  ① POST /api/aggregation/week 요청 수신
  │  ② EsReaderService — Scroll API로 날짜별 문서 전체 조회
  │  ③ AggregationJob (Spark)
  │     - NULL utmMedium → "직접유입" 변환
  │     - 봇 IP 탐지: PAGE_VIEW ≥ 1,000 AND PURCHASE ≥ 50 인 IP 제거
  │     - 유입 채널별 트래픽 / 구매 / 체류시간 / 바운스 / 전환율 집계
  │     - 시간대별 트래픽 집계
  │     - 클릭 패턴 집계
  │  ④ 집계 결과 MySQL 저장
  ▼
MySQL (insights DB)
  │
  ▼
대시보드 (localhost:8080)
  └── GET /api/aggregation/dashboard?date=YYYY-MM-DD
```

## 이벤트 유형

| 이벤트 | 설명 |
|--------|------|
| PAGE_VIEW | 페이지 진입 |
| PAGE_LEAVE | 페이지 이탈 (durationSeconds 포함) |
| CLICK | 요소 클릭 (targetId 포함) |
| PURCHASE | 구매 완료 (amount 포함) |
| LOGIN | 로그인 |
| SIGN_UP | 회원가입 |

## 봇 탐지 로직

Spark 집계 전 단계에서 비정상 IP를 제거합니다.

```
IP별 PAGE_VIEW ≥ 1,000  AND  IP별 PURCHASE ≥ 50
→ 해당 IP의 모든 이벤트를 집계에서 제외
```

## 집계 항목

| 집계 테이블 | 집계 단위 | 주요 컬럼 |
|------------|---------|---------|
| aggregation_result | 날짜 × utmMedium | totalCount, purchaseCount, conversionRate, totalAmount, avgAmount, avgDurationSeconds, bounceCount |
| aggregation_traffic_result | 날짜 × 시간(0–23) | totalCount |
| aggregation_click_result | 날짜 × utmMedium × targetId | clickCount |

## 대시보드 화면 구성

`localhost:8080` 접속 시 바로 확인할 수 있는 내장 대시보드입니다.

| 위치 | 차트 | 설명 |
|------|------|------|
| 상단 KPI | 총 이벤트 / 구매 건수 / 평균 전환율 / 총 구매액 | 선택 날짜 전체 합산 |
| 전체 폭 | 시간대별 트래픽 (line) | 0–23시 이벤트 수 |
| 좌 | 유입 채널별 트래픽 (bar) | utmMedium별 총 이벤트 |
| 우 | 유입 채널별 전환율 (bar) | utmMedium별 구매/유입 비율 |
| 좌 | 유입 채널별 구매액 (bar) | utmMedium별 totalAmount |
| 우 | 유입 채널별 평균 체류시간 (bar) | utmMedium별 avgDurationSeconds |
| 전체 폭 | TOP 클릭 요소 상위 10 (horizontal bar) | utmMedium / targetId별 클릭 수 |

## API 명세

### 집계 실행

```
POST /api/aggregation/week
```

어제 기준 7일치(D-6 ~ D-0)를 Spark로 집계하고 MySQL에 저장합니다.

### 날짜 목록 조회

```
GET /api/aggregation/dates
```

집계가 완료된 날짜 목록을 최신순으로 반환합니다 (최대 30건).

```json
["2026-05-20", "2026-05-19", "2026-05-18"]
```

### 대시보드 데이터 조회

```
GET /api/aggregation/dashboard?date=2026-05-20
```

```json
{
  "aggregation": [
    {
      "aggDate": "2026-05-20",
      "utmMedium": "ad",
      "totalCount": 5000,
      "purchaseCount": 150,
      "conversionRate": 3.00,
      "totalAmount": 450000,
      "avgAmount": 3000.00,
      "avgDurationSeconds": 120,
      "bounceCount": 800
    }
  ],
  "traffic": [
    { "aggDate": "2026-05-20", "hour": 0, "totalCount": 150 }
  ],
  "clicks": [
    { "aggDate": "2026-05-20", "utmMedium": "ad", "targetId": "btn-buy", "clickCount": 450 }
  ]
}
```

## 도메인 모델

```
Elasticsearch Index: userweblog-events
  └── UserWebLogDocument
        userId, sessionId, ip, eventType, uri
        targetId, amount, utmMedium, durationSeconds, occurredAt

MySQL: insights DB
  ├── aggregation_result          (날짜 × utmMedium)
  ├── aggregation_traffic_result  (날짜 × hour)
  └── aggregation_click_result    (날짜 × utmMedium × targetId)
```

## 인프라 설정

`docker-compose.yml`로 전체 인프라를 한 번에 실행합니다.

```bash
docker-compose up -d
```

| 서비스 | 포트 |
|--------|------|
| event-log-service | 8080 |
| Elasticsearch | 9200 |
| Kibana | 5601 |
| MySQL | 3306 |

## 실행 방법

```bash
# 전체 실행 (빌드 포함)
docker-compose up --build

# event-log-service만 재빌드
docker-compose up --build event-log-service
```

실행 후 `http://localhost:8080` 접속 → **1주일 집계 실행** 버튼 클릭 → 날짜 선택 → 대시보드 확인

## DB 접속 정보 (로컬)

| 항목 | 값 |
|------|----|
| Host | localhost:3306 |
| Database | insights |
| Username | root |
| Password | 1234 |
