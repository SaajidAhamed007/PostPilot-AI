package com.hasa.linkedIn.Post.Generator.kafka;

import com.hasa.linkedIn.Post.Generator.event.PostPublishEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaPostProducer {

    private final KafkaTemplate<String, Object>
            kafkaTemplate;

    private static final String TOPIC =
            "linkedin-post-topic";

    public void publish(Long postId) {

        log.info(
                "Sending Kafka event for postId={}",
                postId
        );

        kafkaTemplate.send(
                TOPIC,
                postId.toString(),
                new PostPublishEvent(postId)
        );
    }
}
