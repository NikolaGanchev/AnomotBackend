package com.anomot.anomotbackend.controllers

import com.anomot.anomotbackend.dto.CommentDto
import com.anomot.anomotbackend.dto.CommentEditDto
import com.anomot.anomotbackend.dto.CommentUploadDto
import com.anomot.anomotbackend.security.CustomUserDetails
import com.anomot.anomotbackend.services.CommentService
import com.anomot.anomotbackend.services.UserDetailsServiceImpl
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

@RestController
@RequestMapping
class CommentController@Autowired constructor(
        private val commentService: CommentService,
        private val userDetailsServiceImpl: UserDetailsServiceImpl
) {
    @PostMapping("/post/comment")
    fun commentPost(@RequestBody @Valid commentUploadDto: CommentUploadDto,
                    @RequestParam("id") postId: String,
                    authentication: Authentication): ResponseEntity<CommentDto> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)
        val comment = commentService.addCommentToPost(commentUploadDto.text, user, postId)

        return if (comment == null) {
            ResponseEntity(HttpStatus.BAD_REQUEST)
        } else {
            ResponseEntity(comment, HttpStatus.CREATED)
        }
    }

    @PostMapping("/battle/comment")
    fun commentBattle(@RequestBody @Valid commentUploadDto: CommentUploadDto,
                    @RequestParam("id") battleId: String,
                    authentication: Authentication): ResponseEntity<CommentDto> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)
        val comment = commentService.addCommentToBattle(commentUploadDto.text, user, battleId)

        return if (comment == null) {
            ResponseEntity(HttpStatus.BAD_REQUEST)
        } else {
            ResponseEntity(comment, HttpStatus.CREATED)
        }
    }

    @PostMapping("/comment/comment")
    fun commentComment(@RequestBody @Valid commentUploadDto: CommentUploadDto,
                      @RequestParam("id") commentId: String,
                      authentication: Authentication): ResponseEntity<CommentDto> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)
        val comment = commentService.addCommentToComment(commentUploadDto.text, user, commentId)

        return if (comment == null) {
            ResponseEntity(HttpStatus.BAD_REQUEST)
        } else {
            ResponseEntity(comment, HttpStatus.CREATED)
        }
    }

    @GetMapping("/post/comment")
    fun getCommentPost(@RequestParam("id") postId: String,
                       @RequestParam("page") page: Int,
                    authentication: Authentication): ResponseEntity<List<CommentDto>> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)
        val comments = commentService.getPostComments(user, postId, page)

        return if (comments == null) {
            ResponseEntity(HttpStatus.BAD_REQUEST)
        } else {
            ResponseEntity(comments, HttpStatus.OK)
        }
    }

    @GetMapping("/battle/comment")
    fun getCommentBattle(@RequestParam("id") battleId: String,
                         @RequestParam("page") page: Int,
                      authentication: Authentication): ResponseEntity<List<CommentDto>> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)
        val comments = commentService.getBattleComments(user, battleId, page)

        return if (comments == null) {
            ResponseEntity(HttpStatus.BAD_REQUEST)
        } else {
            ResponseEntity(comments, HttpStatus.OK)
        }
    }

    @GetMapping("/comment/comment")
    fun getCommentComment(@RequestParam("id") commentId: String,
                          @RequestParam("page") page: Int,
                       authentication: Authentication): ResponseEntity<List<CommentDto>> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)
        val comments = commentService.getCommentComments(user, commentId, page)

        return if (comments == null) {
            ResponseEntity(HttpStatus.BAD_REQUEST)
        } else {
            ResponseEntity(comments, HttpStatus.OK)
        }
    }

    @PostMapping("/comment/edit")
    fun editComment(@RequestBody @Valid commentUploadDto: CommentUploadDto,
                    @RequestParam("id") commentId: String,
                    authentication: Authentication): ResponseEntity<CommentDto> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)
        val comment = commentService.editComment(user, commentUploadDto.text, commentId)

        return if (comment) {
            ResponseEntity(HttpStatus.CREATED)
        } else {
            ResponseEntity(HttpStatus.BAD_REQUEST)
        }
    }

    @GetMapping("/comment/history")
    fun getCommentHistory(@RequestParam("id") commentId: String,
                          @RequestParam("page") page: Int,
                    authentication: Authentication): ResponseEntity<List<CommentEditDto>> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)
        val comments = commentService.getCommentHistory(user, commentId, page)

        return if (comments == null) {
            ResponseEntity(HttpStatus.BAD_REQUEST)
        } else {
            ResponseEntity(comments, HttpStatus.OK)
        }
    }

    @DeleteMapping("/comment")
    fun getCommentComment(@RequestParam("id") commentId: String,
                          authentication: Authentication): ResponseEntity<List<CommentDto>> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)
        val comments = commentService.deleteComment(user, commentId)

        return if (comments) {
            ResponseEntity(HttpStatus.OK)
        } else {
            ResponseEntity(HttpStatus.BAD_REQUEST)
        }
    }
}