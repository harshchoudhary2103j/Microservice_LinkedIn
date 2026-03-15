package com.harsh.linkedin.Post_Service.service;

import com.harsh.linkedin.Post_Service.dto.PostCreateRequestDto;
import com.harsh.linkedin.Post_Service.dto.PostDto;
import com.harsh.linkedin.Post_Service.entity.Post;
import com.harsh.linkedin.Post_Service.exception.ResourceNotFoundException;
import com.harsh.linkedin.Post_Service.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {
    private final PostRepository postRepository;
    private final ModelMapper modelMapper;
    public PostDto createPost(PostCreateRequestDto requestDto, Long userId) {
        Post post = modelMapper.map(requestDto,Post.class);
        post.setUserId(userId);
        Post savedPost = postRepository.save(post);
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
