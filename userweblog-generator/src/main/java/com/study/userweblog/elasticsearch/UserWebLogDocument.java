package com.study.userweblog.elasticsearch;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;

@Document(indexName = "userweblog-events")
@Getter
@Builder
public class UserWebLogDocument {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String userId;

    @Field(type = FieldType.Keyword)
    private String sessionId;

    @Field(type = FieldType.Ip)
    private String ip;

    @Field(type = FieldType.Keyword)
    private String eventType;

    @Field(type = FieldType.Keyword)
    private String uri;

    @Field(type = FieldType.Keyword)
    private String targetId;

    @Field(type = FieldType.Long)
    private Long amount;

    @Field(type = FieldType.Keyword)
    private String utmMedium;

    @Field(type = FieldType.Long)
    private Long durationSeconds;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime occurredAt;
}
