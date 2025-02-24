package com.phithang.mysocialnetwork.service.Impl;


import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.phithang.mysocialnetwork.dto.CommentDto;
import com.phithang.mysocialnetwork.dto.MediaDto;
import com.phithang.mysocialnetwork.dto.request.PostRequestDto;
import com.phithang.mysocialnetwork.dto.request.PostUpdateDto;
import com.phithang.mysocialnetwork.entity.*;
import com.phithang.mysocialnetwork.repository.*;
import com.phithang.mysocialnetwork.service.IPostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class PostService implements IPostService {
    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private MediaService mediaService;

    @Autowired
    private MediaRepository mediaRepository;

    @Autowired
    private PostMediaRepository postMediaRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private Cloudinary cloudinary;



    @Transactional
    @Override
    public PostEntity createPost(PostRequestDto postRequestDto) throws IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        UserEntity author = userService.findUserByEmail(authentication.getName());

        // Tạo bài viết
        PostEntity postEntity = new PostEntity();
        postEntity.setContent(postRequestDto.getContent());
        postEntity.setAuthor(author);
        postEntity.setTimestamp(LocalDateTime.now());
        postRepository.save(postEntity);

        List<MediaEntity> mediaEntities = new ArrayList<>();
        for (MediaDto file : postRequestDto.getMedia()) {
            var uploadResult = cloudinary.uploader().upload(file.getUrl(), ObjectUtils.emptyMap()); // Upload base64 string
            String mediaUrl = uploadResult.get("secure_url").toString();
            String mediaType = file.getType().startsWith("image") ? "IMAGE" : "VIDEO";
            MediaEntity mediaEntity = new MediaEntity();
            mediaEntity.setUrl(mediaUrl);
            mediaEntity.setType(mediaType);
            mediaEntities.add(mediaEntity);
        }

        // Lưu media
        for (MediaEntity media : mediaEntities) {
            media = mediaRepository.save(media);

            PostMediaEntity postMedia = new PostMediaEntity();
            postMedia.setPost(postEntity);
            postMedia.setMedia(media);
            postMediaRepository.save(postMedia);
        }

        return postEntity;
    }


    @Transactional
    @Override
    public PostEntity updatePost(PostUpdateDto postRequestDto) throws IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        UserEntity author = userService.findUserByEmail(authentication.getName());

        // Tạo bài viết
        PostEntity postEntity = postRepository.findById(postRequestDto.getId()).orElse(null);
        postEntity.setContent(postRequestDto.getContent());
        postRepository.save(postEntity);

        List<MediaEntity> mediaEntities = new ArrayList<>();
//        for (MediaDto file : postRequestDto.getMedia()) {
//            var uploadResult = cloudinary.uploader().upload(file.getUrl(), ObjectUtils.emptyMap()); // Upload base64 string
//            String mediaUrl = uploadResult.get("secure_url").toString();
//            String mediaType = file.getType().startsWith("image") ? "IMAGE" : "VIDEO";
//            MediaEntity mediaEntity = new MediaEntity();
//            mediaEntity.setUrl(mediaUrl);
//            mediaEntity.setType(mediaType);
//            mediaEntities.add(mediaEntity);
//        }
//
//        // Lưu media
//        for (MediaEntity media : mediaEntities) {
//            media = mediaRepository.save(media);
//
//            PostMediaEntity postMedia = new PostMediaEntity();
//            postMedia.setPost(postEntity);
//            postMedia.setMedia(media);
//            postMediaRepository.save(postMedia);
//        }

        return postEntity;
    }


    @Override
    public boolean deletePost(Long id)
    {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        UserEntity userEntity = userService.findUserByEmail(email);
        PostEntity postEntity = postRepository.findById(id).orElse(null);
        if(postEntity!=null && postEntity.getAuthor().getId().equals(userEntity.getId()))
        {

            postRepository.delete(postEntity);
            return true;// Khi post bị xóa, các comments và postMedia sẽ bị xóa theo.
        }
        return false;
    }

    @Override
    public boolean likePost(Long id)
    {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        UserEntity userEntity = userService.findUserByEmail(email);
        PostEntity postEntity = postRepository.findById(id).orElse(null);
        if(postEntity!=null && userEntity!=null)
        {
            if(!postEntity.getLikedBy().contains(userEntity))
            {
                postEntity.getLikedBy().add(userEntity);
                if (!email.trim().equals(postEntity.getAuthor().getEmail().trim())) {
                    NotificationEntity notificationEntity = new NotificationEntity();
                    notificationEntity.setUser(postEntity.getAuthor());
                    notificationEntity.setIsread(0);
                    notificationEntity.setTimestamp(LocalDateTime.now());
                    notificationEntity.setContent(userEntity.getFirstname() +" "+userEntity.getLastname() + " đã yêu thích bài viết của bạn.");
                    notificationEntity.setPost(postEntity);
                    notificationRepository.save(notificationEntity);
                }
                postRepository.save(postEntity);
            }
            else
            {
                postEntity.getLikedBy().remove(userEntity);
                postRepository.save(postEntity);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean commentPost(Long id, CommentDto commentDto) {
        try {
            // Lấy thông tin người dùng hiện tại
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            UserEntity userEntity = userService.findUserByEmail(email);

            if (userEntity == null) {
                throw new RuntimeException("User not found.");
            }

            // Lấy bài viết theo ID
            PostEntity postEntity = postRepository.findById(id).orElse(null);

            if (postEntity == null) {
                throw new RuntimeException("Post not found.");
            }

            // Tạo comment mới
            CommentEntity commentEntity = new CommentEntity();
            commentEntity.setPost(postEntity);
            commentEntity.setAuthor(userEntity);
            commentEntity.setTimestamp(LocalDateTime.now());
            commentEntity.setContent(commentDto.getContent());

            // Xử lý parentComment nếu có
            if (commentDto.getParentCommentId() != null) {
                CommentEntity parentComment = commentRepository.findById(commentDto.getParentCommentId()).orElse(null);
                if (parentComment == null) {
                    throw new RuntimeException("Parent comment not found.");
                }
                commentEntity.setParentComment(parentComment);
            }

            // Lưu comment
            postEntity.getComments().add(commentEntity);
            postRepository.save(postEntity);

            // Thêm thông báo (nếu cần)
            if (!email.trim().equals(postEntity.getAuthor().getEmail().trim())) {
                NotificationEntity notificationEntity = new NotificationEntity();
                notificationEntity.setUser(postEntity.getAuthor());
                notificationEntity.setIsread(0); // 0 = chưa đọc
                notificationEntity.setTimestamp(LocalDateTime.now());
                notificationEntity.setContent(userEntity.getFirstname() +" "+userEntity.getLastname()+ " đã bình luận bài viết của bạn.");
                notificationEntity.setPost(postEntity);
                notificationRepository.save(notificationEntity);
            }

            return true;
        } catch (Exception e) {
            // Log lỗi (cần thêm logger ở đây)
            System.err.println("Error in commentPost: " + e.getMessage());
            return false;
        }
    }


    @Override
    public List<PostEntity> getAllPost()
    {
        return postRepository.findAll();
    }

    @Override
    public List<PostEntity> getMyPost()
    {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        UserEntity userEntity = userService.findUserByEmail(email);
        return postRepository.findAllByAuthor(userEntity);
    }
    @Override
    public List<PostEntity> getUserPosts(Long userId)
    {
        UserEntity userEntity = userService.findById(userId);
        return postRepository.findAllByAuthor(userEntity);
    }


    @Override
    public PostEntity findById(Long id)
    {
        return postRepository.findById(id).orElse(null);
    }
}
