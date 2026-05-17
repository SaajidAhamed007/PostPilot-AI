package com.hasa.linkedIn.Post.Generator.kafka;

import com.hasa.linkedIn.Post.Generator.event.PostPublishEvent;
import com.hasa.linkedIn.Post.Generator.service.PostService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaPostConsumer {

    private final PostService postService;

    @KafkaListener(
            topics = "linkedin-post-topic",
            groupId = "linkedin-group"
    )
    public void consume(
            PostPublishEvent event
    ) {

        postService.processPostPublishing(
                event.postId()
        );
    }
}
