# userweblog-generator

스케줄러 기반 사용자 행동 이벤트 생성기. 1초마다 랜덤 `UserWebLogEvent`를 생성해 Kafka topic으로 발행한다.

## 이벤트 타입

| 타입 | 설명 | 발생 비율 |
|---|---|---|
| `PAGE_VIEW` | 사용자가 특정 페이지를 조회 | 60% |
| `CLICK` | 사용자가 버튼/링크 등을 클릭 | 30% |
| `PURCHASE` | 사용자가 상품을 구매 완료 | 10% |

## Kafka 메시지 예시

```json
// PAGE_VIEW
{
  "userId": "user-a1b2c3d4",
  "sessionId": "session-e5f6g7h8i9j0",
  "eventType": "PAGE_VIEW",
  "pageUrl": "/products",
  "targetId": null,
  "amount": null,
  "occurredAt": "2024-05-18T10:30:00"
}

// CLICK
{
  "userId": "user-b2c3d4e5",
  "sessionId": "session-f6g7h8i9j0k1",
  "eventType": "CLICK",
  "pageUrl": "/products/123",
  "targetId": "btn-add-to-cart",
  "amount": null,
  "occurredAt": "2024-05-18T10:30:01"
}

// PURCHASE
{
  "userId": "user-c3d4e5f6",
  "sessionId": "session-g7h8i9j0k1l2",
  "eventType": "PURCHASE",
  "pageUrl": "/checkout",
  "targetId": "PROD-001",
  "amount": 35000,
  "occurredAt": "2024-05-18T10:30:02"
}
```

## 실행 방법

### 1. Kafka 실행 (Docker Compose)

```bash
docker-compose up -d
```

Kafka UI → http://localhost:8989

### 2. 애플리케이션 실행

```bash
./gradlew bootRun
```

### 3. 메시지 확인 (CLI)

```bash
# topic의 메시지 실시간 소비
docker exec -it <kafka-container-id> \
  kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic userweblog-events \
  --from-beginning
```

## 설정 값 (application.yml)

| 키 | 기본값 | 설명 |
|---|---|---|
| `userweblog.kafka.topic` | `userweblog-events` | 발행 대상 Kafka topic |
| `userweblog.scheduler.fixed-rate-ms` | `1000` | 스케줄러 실행 주기 (ms) |
| `userweblog.scheduler.users-per-tick` | `5` | 1회 실행 시 생성 이벤트 수 |

## 패키지 구조

```
com.study.userweblog
├── UserWebLogGeneratorApplication.java   # 진입점, @EnableScheduling
├── config
│   └── KafkaProducerConfig.java          # KafkaTemplate Bean 설정
├── domain
│   └── UserWebLogType.java               # 이벤트 타입 Enum
├── event
│   ├── UserWebLogEvent.java              # Kafka payload DTO
│   └── UserWebLogEventFactory.java       # 타입별 이벤트 생성 팩토리
├── producer
│   └── UserWebLogProducer.java           # Kafka 비동기 발행
└── scheduler
    └── UserWebLogScheduler.java          # @Scheduled 스케줄러
```
