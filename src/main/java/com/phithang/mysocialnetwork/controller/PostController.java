package com.phithang.mysocialnetwork.controller;

import com.phithang.mysocialnetwork.dto.*;
import com.phithang.mysocialnetwork.dto.request.PostRequestDto;
import com.phithang.mysocialnetwork.dto.request.PostUpdateDto;
import com.phithang.mysocialnetwork.dto.response.CommentResponseDto;
import com.phithang.mysocialnetwork.dto.response.ResponseDto;
import com.phithang.mysocialnetwork.entity.CommentEntity;
import com.phithang.mysocialnetwork.entity.PostEntity;
import com.phithang.mysocialnetwork.service.ICommentService;
import com.phithang.mysocialnetwork.service.IPostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping
public class PostController {
    @Autowired
    private IPostService postService;

    @Autowired
    private ICommentService commentService;

    @GetMapping("/userpost/{id}")
    public ResponseDto<Iterable<PostDto>> getUserPosts(@PathVariable Long id) {
        // Lấy tất cả bài viết
        List<PostEntity> posts = postService.getUserPosts(id);
        List<PostDto> list = new ArrayList<>();

        // Lấy người dùng hiện tại từ SecurityContextHolder
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        for (PostEntity postEntity : posts) {
            PostDto postDto = new PostDto();

            // Kiểm tra xem bài viết có ai đã like không, tránh lỗi khi likedBy rỗng
            boolean isLiked = false;
            if (!postEntity.getLikedBy().isEmpty()) {
                // Kiểm tra xem người dùng hiện tại có trong danh sách thích không
                isLiked = postEntity.getLikedBy().stream()
                        .anyMatch(user -> user.getEmail().equals(currentUserEmail));
            }


            postDto = postDto.toPostDto(postEntity);
            postDto.setLiked(isLiked);
            // Chuyển đổi PostEntity sang PostDto
            list.add(postDto);
            // Cập nhật trường liked trong PostDto

        }
        Collections.reverse(list);

        // Trả về ResponseDto với trạng thái thành công và danh sách bài viết
        return new ResponseDto<>(200, list, "Get posts successful!");
    }

    @GetMapping("/myposts")
    public ResponseDto<Iterable<PostDto>> getMyPosts() {
        // Lấy tất cả bài viết
        List<PostEntity> posts = postService.getMyPost();
        List<PostDto> list = new ArrayList<>();

        // Lấy người dùng hiện tại từ SecurityContextHolder
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        for (PostEntity postEntity : posts) {
            PostDto postDto = new PostDto();

            // Kiểm tra xem bài viết có ai đã like không, tránh lỗi khi likedBy rỗng
            boolean isLiked = false;
            if (!postEntity.getLikedBy().isEmpty()) {
                // Kiểm tra xem người dùng hiện tại có trong danh sách thích không
                isLiked = postEntity.getLikedBy().stream()
                        .anyMatch(user -> user.getEmail().equals(currentUserEmail));
            }


            postDto = postDto.toPostDto(postEntity);
            postDto.setLiked(isLiked);
            // Chuyển đổi PostEntity sang PostDto
            list.add(postDto);
            // Cập nhật trường liked trong PostDto

        }
        Collections.reverse(list);

        // Trả về ResponseDto với trạng thái thành công và danh sách bài viết
        return new ResponseDto<>(200, list, "Get posts successful!");
    }
    @GetMapping("/posts")
    public ResponseDto<Iterable<PostDto>> getPosts() {
        // Lấy tất cả bài viết
        List<PostEntity> posts = postService.getAllPost();
        List<PostDto> list = new ArrayList<>();

        // Lấy người dùng hiện tại từ SecurityContextHolder
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        for (PostEntity postEntity : posts) {
            PostDto postDto = new PostDto();

            // Kiểm tra xem bài viết có ai đã like không, tránh lỗi khi likedBy rỗng
            boolean isLiked = false;
            if (!postEntity.getLikedBy().isEmpty()) {
                // Kiểm tra xem người dùng hiện tại có trong danh sách thích không
                isLiked = postEntity.getLikedBy().stream()
                        .anyMatch(user -> user.getEmail().equals(currentUserEmail));
            }


            postDto = postDto.toPostDto(postEntity);
            postDto.setLiked(isLiked);
            // Chuyển đổi PostEntity sang PostDto
            list.add(postDto);
        }
        Collections.reverse(list);
        // Trả về ResponseDto với trạng thái thành công và danh sách bài viết
        return new ResponseDto<>(200, list, "Get posts successful!");
    }


    @PostMapping("/post")
    public ResponseDto<PostEntity> post(@RequestBody PostRequestDto post) throws IOException {
        PostEntity postEntity = postService.createPost(post);
        if (postEntity != null) {
            return new ResponseDto<>(200, postEntity, "Success");
        }
        return new ResponseDto<>(400,null,"Fail");
    }

    @PutMapping("/post/{id}")
    public ResponseDto<PostEntity> update(@RequestBody PostUpdateDto post) throws IOException {
        PostEntity postEntity = postService.updatePost(post);
        if (postEntity != null) {
            return new ResponseDto<>(200, postEntity, "Success");
        }
        return new ResponseDto<>(400,null,"Fail");
    }

    @DeleteMapping("/post/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        if (postService.deletePost(id)) {
            return new ResponseEntity<>("Success", HttpStatus.OK);
        }
        return new ResponseEntity<>("Fail", HttpStatus.BAD_REQUEST);
    }


    @PostMapping("/like/{id}")
    public ResponseDto<String> like(@PathVariable Long id)
    {
        if(postService.likePost(id))
        {
            return new ResponseDto<>(200,"null","Like successful!");
        }
        return new ResponseDto<>(400,null,"Fail");
    }



    @PostMapping("/comment/{postId}")
    public ResponseDto<CommentDto> comment(@PathVariable Long postId, @RequestBody CommentDto commentDto) {
        if (postService.commentPost(postId, commentDto)) {
            return new ResponseDto<>(200, commentDto, "Comment successful!");
        }
        return new ResponseDto<>(400, null, "Comment failed!");
    }


    @GetMapping("/comment/{id}")
    public ResponseDto<List<CommentResponseDto>> getComments(@PathVariable Long id) {
        // Tìm bài viết theo ID
        PostEntity postEntity = postService.findById(id);

        if (postEntity != null) {
            List<CommentEntity> comments = postEntity.getComments();
            List<CommentResponseDto> list = new ArrayList<>();
            for (CommentEntity commentEntity : comments) {
                CommentResponseDto commentResponseDto = new CommentResponseDto();
                commentResponseDto.setId(commentEntity.getId());
                commentResponseDto.setAuthorId(commentEntity.getAuthor().getId());
                commentResponseDto.setContent(commentEntity.getContent());
                commentResponseDto.setAuthorName(commentEntity.getAuthor().getFirstname() + " " + commentEntity.getAuthor().getLastname());
                commentResponseDto.setImageUrl(commentEntity.getAuthor().getImageUrl());
                commentResponseDto.setTimestamp(commentEntity.getTimestamp());
                if(commentEntity.getParentComment() != null)
                {
                    commentResponseDto.setReplyAuthorName(commentEntity.getParentComment().getAuthor().getFirstname() + " " + commentEntity.getParentComment().getAuthor().getLastname());
                    commentResponseDto.setReplyId(commentEntity.getParentComment().getId());
                    commentResponseDto.setReplyAuthorId(commentEntity.getParentComment().getAuthor().getId());
                }
                list.add(commentResponseDto);
            }

            // Trả về danh sách bình luận của bài viết
            return new ResponseDto<>(200,list,"Success");
        }

        // Nếu bài viết không tồn tại, trả về lỗi
        return new ResponseDto<>(400,null,"Fail");
    }

    @DeleteMapping("/comment/{id}")
    public ResponseDto<String> deleteComment(@PathVariable Long id) {
        CommentEntity commentEntity = commentService.findById(id);
        if (commentEntity != null) {

            if(commentService.delete(commentEntity)) {
                return new ResponseDto<>(200, "Success", "Comment successful!");
            }
        }
        return new ResponseDto<>(400,null,"Fail");
    }



}
