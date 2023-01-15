package com.anomot.anomotbackend.services

import com.anomot.anomotbackend.dto.CommentDto
import com.anomot.anomotbackend.entities.*
import com.anomot.anomotbackend.repositories.CommentRepository
import com.anomot.anomotbackend.repositories.PreviousCommentVersionRepository
import com.anomot.anomotbackend.repositories.VoteRepository
import com.anomot.anomotbackend.utils.Constants
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.lang.NumberFormatException
import javax.transaction.Transactional

@Service
class CommentService @Autowired constructor(
        private val commentRepository: CommentRepository,
        private val userDetailsServiceImpl: UserDetailsServiceImpl,
        private val voteRepository: VoteRepository,
        private val postService: PostService,
        private val previousCommentVersionRepository: PreviousCommentVersionRepository,
        private val battleService: BattleService
) {
    @Transactional
    fun addCommentToPost(text: String, user: User, postId: String): CommentDto? {
        val post = postService.getPostReferenceFromIdUnsafe(postId) ?: return null

        if (!postService.canSeeUserAndPost(user, post)) return null

        return addComment(text, user, null, post, null)
    }

    @Transactional
    fun addCommentToBattle(text: String, user: User, battleId: String): CommentDto? {
        val battle = battleService.getBattleReferenceFromIdUnsafe(battleId) ?: return null

        if (!voteRepository.existsByBattleAndVoter(battle, user)) return null

        return addComment(text, user, battle, null, null)
    }

    @Transactional
    fun addCommentToComment(text: String, user: User, commentId: String): CommentDto? {
        val comment = getCommentFromIdUnsafe(commentId) ?: return null

        if (!canSeeComment(user, comment)) return null

        return addComment(text, user, null, null, comment)
    }

    private fun addComment(text: String, user: User, parentBattle: Battle?, parentPost: Post?, parentComment: Comment?): CommentDto {
        val comment = Comment(text, parentBattle, parentPost, parentComment, user)
        val savedComment = commentRepository.save(comment)
        return getAsDto(savedComment)
    }

    fun getPostComments(user: User, postId: String, page: Int): List<CommentDto>? {
        val post = postService.getPostReferenceFromIdUnsafe(postId) ?: return null

        if (!postService.canSeeUserAndPost(user, post)) return null

        return commentRepository.getAllByParentPost(post, PageRequest.of(page, Constants.COMMENTS_PAGE)).map {
            getAsDto(it)
        }
    }

    fun getBattleComments(user: User, battleId: String, page: Int): List<CommentDto>? {
        val battle = battleService.getBattleReferenceFromIdUnsafe(battleId) ?: return null

        if (!voteRepository.existsByBattleAndVoter(battle, user)) return null

        return commentRepository.getAllByParentBattle(battle, PageRequest.of(page, Constants.COMMENTS_PAGE)).map {
            getAsDto(it)
        }
    }

    @Transactional
    fun getCommentComments(user: User, commentId: String, page: Int): List<CommentDto>? {
        val comment = getCommentFromIdUnsafe(commentId) ?: return null

        if (!canSeeComment(user, comment)) return null

        return commentRepository.getAllByParentComment(comment, PageRequest.of(page, Constants.COMMENTS_PAGE)).map {
            getAsDto(it)
        }
    }

    private fun canSeeComment(user: User, comment: Comment): Boolean {
        // Check if comment is not a child already
        if (comment.parentComment != null) {
            return false
        }

        // Check if comment is on post and if can user can access it
        if (comment.parentPost != null) {
            if (!postService.canSeeUserAndPost(user, comment.parentPost!!)) return false
        }

        // Check if comment is on battle and if user can access it
        if (comment.parentBattle != null) {
            if (!voteRepository.existsByBattleAndVoter(comment.parentBattle!!, user)) return false
        }

        return true
    }

    fun deleteComment(user: User, commentId: String): Boolean {
        val comment = getCommentFromIdUnsafe(commentId) ?: return false

        if (commentRepository.existsByParentComment(comment)) {
            previousCommentVersionRepository.deleteAllByComment(comment)
            commentRepository.setDeleted(user, comment.id!!)
        } else {
            previousCommentVersionRepository.deleteAllByComment(comment)
            commentRepository.delete(comment)
        }

        return true
    }

    @Transactional
    fun editComment(user: User, newText: String, commentId: String): Boolean {
        val comment = getCommentFromIdUnsafe(commentId) ?: return false

        if (comment.commenter?.id != user.id) return false

        val oldText = comment.text

        commentRepository.edit(newText, comment.id!!)

        previousCommentVersionRepository.save(PreviousCommentVersion(oldText, comment))

        return true
    }

    fun getCommentHistory(user: User, commentId: String, page: Int): List<CommentDto>? {
        val comment = getCommentReferenceFromIdUnsafe(commentId) ?: return null

        if (!canSeeComment(user, comment)) return null

        return previousCommentVersionRepository.findByComment(comment,
                PageRequest.of(page,
                        Constants.COMMENTS_PAGE, Sort.by("creationDate"))).map {
            return@map getAsDto(it.comment)
        }
    }

    private fun getCommentFromIdUnsafe(commentId: String): Comment? {
        return try {
            val comment = commentRepository.findById(commentId.toLong())

            if (comment.isEmpty) return null

            comment.get()
        } catch(numberFormatException: NumberFormatException) {
            null
        }
    }

    private fun getCommentReferenceFromIdUnsafe(commentId: String): Comment? {
        return try {
            return if (commentRepository.existsById(commentId.toLong())) {
                commentRepository.getReferenceById(commentId.toLong())
            } else null
        } catch(numberFormatException: NumberFormatException) {
            null
        }
    }

    private fun getAsDto(comment: Comment): CommentDto {
        return CommentDto(
                comment.text,
                if (comment.isDeleted || comment.commenter == null) {
                    null
                } else userDetailsServiceImpl.getAsDto(comment.commenter!!),
                comment.isEdited
        )
    }
}