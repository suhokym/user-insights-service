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

**필요한 도구**

| 도구 | 버전 | 설치 |
|------|------|------|
| Docker Desktop | 최신 버전 | https://www.docker.com/products/docker-desktop |

Docker Desktop 설치 후 Settings → Resources → Memory에서 **4GB 이상** 할당을 권장합니다.

**실행 순서**

```bash
# 1. 저장소 클론
git clone <repo-url>
cd user-insights-service

# 2. 전체 서비스 빌드 및 실행 (첫 실행 시 5~10분 소요)
docker-compose up --build

# 3. 브라우저에서 대시보드 접속
# http://localhost:8080

# 4. 우측 상단 [1주일 집계 실행] 버튼 클릭 → 날짜 선택 → 차트 확인
```

코드 수정 후 재빌드가 필요한 경우:

```bash
docker-compose up --build event-log-service
```

## 스키마 설명

원시 로그는 Elasticsearch에, 집계 결과만 MySQL에 분리 저장했습니다. Elasticsearch는 비정형 이벤트를 대량으로 빠르게 적재하는 데 적합하고, MySQL은 날짜·유입채널 단위로 집계된 소량의 결과를 API로 조회하는 데 적합하기 때문입니다.

MySQL 집계 테이블은 집계 단위에 따라 3개로 분리했습니다. `aggregation_result`는 날짜 × utmMedium 단위로 전환율·구매액·체류시간을 한 행에 담고, `aggregation_traffic_result`는 시간대별 트래픽을 별도 테이블로 분리해 시계열 조회를 단순하게 유지했으며, `aggregation_click_result`는 targetId가 추가되는 클릭 패턴의 카디널리티 차이를 고려해 독립 테이블로 구성했습니다.

## 구현하면서 고민한 점

**Spark를 Spring Boot에 내장하는 방식 선택**

별도 Spark 클러스터를 두지 않고 `local[*]` 모드로 Spring Boot에 내장했습니다. 인프라를 단순하게 유지하면서 `docker-compose up` 한 줄로 실행 가능하게 하려는 목적이었습니다. 다만 Java 21과 Spark 3.5의 모듈 시스템 충돌(`InaccessibleObjectException`) 문제가 있었고, Dockerfile ENTRYPOINT에 `--add-opens` 플래그를 13개 추가해 해결했습니다.

**봇 탐지 임계값 결정**

처음엔 PAGE_VIEW 60회 / PURCHASE 3회로 설정했는데, 실제 생성된 유저 데이터도 봇으로 오분류되는 문제가 생겼습니다. 봇 생성기가 세션당 60 PV를 만들지만 하루에 수십 세션을 반복해 일 4,000 PV 이상을 찍는다는 것을 확인하고, 임계값을 PAGE_VIEW 1,000 / PURCHASE 50으로 상향해 실제 유저(일 최대 139 PV)와 명확히 구분했습니다.

**NULL utmMedium의 Spark JOIN 처리**

직접유입(utmMedium = null)이 집계 결과에 나타나지 않는 문제가 있었습니다. Spark SQL에서 `NULL = NULL`은 `false`이므로 JOIN 키로 사용할 수 없기 때문이었습니다. DataFrame 생성 직후 `when(col("utmMedium").isNull(), "직접유입")`으로 null을 문자열로 변환해 모든 하위 집계에서 정상적으로 그룹핑되도록 수정했습니다.

## DB 접속 정보 (로컬)

| 항목 | 값 |
|------|----|
| Host | localhost:3306 |
| Database | insights |
| Username | root |
| Password | 1234 |

## AWS 아키텍처 설계

```
                        Internet
                           │
                     ┌─────▼─────┐
                     │    ALB    │  (Application Load Balancer)
                     └─────┬─────┘
                           │ :80 / :443
              ┌────────────▼────────────┐
              │         VPC              │
              │  ┌──────────────────┐   │
              │  │  Public Subnet   │   │
              │  └──────────────────┘   │
              │  ┌──────────────────┐   │
              │  │  Private Subnet  │   │
              │  │                  │   │
              │  │  ┌────────────┐  │   │
              │  │  │    EKS     │  │   │
              │  │  │  (Node)    │  │   │
              │  │  │            │  │   │
              │  │  │ event-log  │  │   │
              │  │  │  -service  │  │   │
              │  │  │  Pod × 2   │  │   │
              │  │  └─────┬──────┘  │   │
              │  │        │         │   │
              │  │   ┌────┴────┐    │   │
              │  │   │         │    │   │
              │  │   ▼         ▼    │   │
              │  │ Amazon    Amazon │   │
              │  │ OpenSearch  RDS  │   │
              │  │ Service  (MySQL) │   │
              │  └──────────────────┘   │
              └─────────────────────────┘
                           │
                     ┌─────▼─────┐
                     │  Amazon   │
                     │    ECR    │  (컨테이너 이미지 저장소)
                     └───────────┘
```

| 구성 요소 | AWS 서비스 | 역할 |
|----------|-----------|------|
| 컨테이너 오케스트레이션 | Amazon EKS | event-log-service Pod 실행 |
| 로드 밸런서 | Application Load Balancer | 외부 트래픽 → EKS 분산 |
| 이벤트 로그 저장소 | Amazon OpenSearch Service | Elasticsearch 대체 (관리형) |
| 집계 결과 DB | Amazon RDS (MySQL 8.0) | 가용성·백업 자동 관리 |
| 컨테이너 이미지 | Amazon ECR | Docker 이미지 저장 및 배포 |
| 네트워크 | VPC + Private Subnet | EKS·RDS·OpenSearch 외부 노출 차단 |

## Kubernetes 리소스

`k8s/` 디렉토리에 EKS 배포용 리소스 파일이 있습니다.

```
k8s/
├── deployment.yaml   # Pod 스펙 및 환경변수
├── service.yaml      # 클러스터 내부 네트워크 노출
└── configmap.yaml    # 비민감 환경변수
```

```bash
# 배포
kubectl apply -f k8s/

# 상태 확인
kubectl get pods
kubectl get svc
```
