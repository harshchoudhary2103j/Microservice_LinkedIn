package com.harsh.linkedin.Post_Service.service;

import com.harsh.linkedin.Post_Service.auth.UserContextHolder;
import com.harsh.linkedin.Post_Service.clients.ConnectionsClient;
import com.harsh.linkedin.Post_Service.dto.PersonDto;
import com.harsh.linkedin.Post_Service.dto.PostCreateRequestDto;
import com.harsh.linkedin.Post_Service.dto.PostDto;
import com.harsh.linkedin.Post_Service.entity.Post;
import com.harsh.linkedin.Post_Service.event.PostCreatedEvent;
import com.harsh.linkedin.Post_Service.exception.ResourceNotFoundException;
import com.harsh.linkedin.Post_Service.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {
    private final PostRepository postRepository;
    private final ModelMapper modelMapper;
    private final ConnectionsClient connectionsClient;
    private final KafkaTemplate<Long, PostCreatedEvent>kafkaTemplate;
    public PostDto createPost(PostCreateRequestDto requestDto) {
        Long userId = UserContextHolder.getCurrentUserId();
        Post post = modelMapper.map(requestDto,Post.class);
        post.setUserId(userId);
        Post savedPost = postRepository.save(post);

        PostCreatedEvent postCreatedEvent = PostCreatedEvent.builder()
                .postId(savedPost.getId())
                .creatorId(userId)
                .content(savedPost.getContent())
                .build();

        kafkaTemplate.send("post-created-topic",postCreatedEvent);
        return modelMapper.map(savedPost,PostDto.class);

    }


    public PostDto getPostById(Long postId) {
        log.debug("Retrieving post with ID: {}", postId);

        Post post = postRepository.findById(postId).orElseThrow(() ->
                new ResourceNotFoundException("Post not found with id: "+postId));
        return modelMapper.map(post, PostDto.class);
    }
    public List<PostDto> getAllPostsOfUser(Long userId) {
        List<Post> posts = postRepository.findByUserId(userId);

        return posts
                .stream()
                .map((element) -> modelMapper.map(element, PostDto.class))
                .collect(Collectors.toList());
    }
}
