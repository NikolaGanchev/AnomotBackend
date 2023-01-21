package com.anomot.anomotbackend.controllers

import com.anomot.anomotbackend.dto.*
import com.anomot.anomotbackend.security.CustomUserDetails
import com.anomot.anomotbackend.security.EmailVerified
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
class CommentController @Autowired constructor(
        private val commentService: CommentService,
        private val userDetailsServiceImpl: UserDetailsServiceImpl
) {
    @PostMapping("/post/comment")
    @EmailVerified
    fun commentPost(@RequestBody @Valid commentUploadDto: CommentUploadDto,
                    authentication: Authentication): ResponseEntity<CommentDto> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)
        val comment = commentService.addCommentToPost(commentUploadDto.text, user, commentUploadDto.id)

        return if (comment == null) {
            ResponseEntity(HttpStatus.BAD_REQUEST)
        } else {
            ResponseEntity(comment, HttpStatus.CREATED)
        }
    }

    @PostMapping("/battle/comment")
    @EmailVerified
    fun commentBattle(@RequestBody @Valid commentUploadDto: CommentUploadDto,
                    authentication: Authentication): ResponseEntity<CommentDto> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)
        val comment = commentService.addCommentToBattle(commentUploadDto.text, user, commentUploadDto.id)

        return if (comment == null) {
            ResponseEntity(HttpStatus.BAD_REQUEST)
        } else {
            ResponseEntity(comment, HttpStatus.CREATED)
        }
    }

    @PostMapping("/comment/comment")
    @EmailVerified
    fun commentComment(@RequestBody @Valid commentUploadDto: CommentUploadDto,
                      authentication: Authentication): ResponseEntity<CommentDto> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)
        val comment = commentService.addCommentToComment(commentUploadDto.text, user, commentUploadDto.id)

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
    @EmailVerified
    fun editComment(@RequestBody @Valid commentUploadDto: CommentUploadDto,
                    authentication: Authentication): ResponseEntity<String> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)
        val result = commentService.editComment(user, commentUploadDto.text, commentUploadDto.id)

        return if (result) {
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
    fun deleteComment(@RequestParam("id") commentId: String,
                          authentication: Authentication): ResponseEntity<String> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)
        val comments = commentService.deleteComment(user, commentId)

        return if (comments) {
            ResponseEntity(HttpStatus.OK)
        } else {
            ResponseEntity(HttpStatus.BAD_REQUEST)
        }
    }

    @PostMapping("/comment/like")
    @EmailVerified
    fun like(@RequestParam("id") commentId: String, authentication: Authentication): ResponseEntity<String> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)

        val result = commentService.like(user, commentId)
        return ResponseEntity(if (result) HttpStatus.OK else HttpStatus.BAD_REQUEST)
    }

    @PostMapping("/comment/unlike")
    @EmailVerified
    fun unlike(@RequestParam("id") commentId: String, authentication: Authentication): ResponseEntity<String> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)

        val result = commentService.unlike(user, commentId)
        return ResponseEntity(if (result) HttpStatus.OK else HttpStatus.BAD_REQUEST)
    }

    @GetMapping("/comment/likes")
    fun getLikes(@RequestParam("id") commentId: String, @RequestParam("page") page: Int, authentication: Authentication): ResponseEntity<List<UserDto>> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)

        val result = commentService.getLikedBy(user, commentId, page)
        return ResponseEntity(result, if (result != null) HttpStatus.OK else HttpStatus.BAD_REQUEST)
    }

    @PostMapping("/comment/report")
    fun reportComment(@RequestBody @Valid commentReportDto: CommentReportDto, authentication: Authentication): ResponseEntity<String> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)

        val result = commentService.report(commentReportDto, user)

        return ResponseEntity(if (result) HttpStatus.OK else HttpStatus.NOT_FOUND)
    }

    @GetMapping("/comment/report")
    fun getCommentReport(@RequestParam("id") commentId: String, authentication: Authentication): ResponseEntity<ReportDto> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)

        val result = commentService.getReport(user, commentId)

        return if (result != null) {
            ResponseEntity(result, HttpStatus.OK)
        } else {
            ResponseEntity(HttpStatus.BAD_REQUEST)
        }
    }
}