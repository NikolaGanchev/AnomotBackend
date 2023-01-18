package com.anomot.anomotbackend.services

import com.anomot.anomotbackend.dto.CommentDto
import com.anomot.anomotbackend.dto.CommentEditDto
import com.anomot.anomotbackend.dto.CommentIntermediate
import com.anomot.anomotbackend.dto.UserDto
import com.anomot.anomotbackend.entities.*
import com.anomot.anomotbackend.repositories.*
import com.anomot.anomotbackend.utils.Constants
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.lang.NumberFormatException
import java.util.*
import javax.transaction.Transactional

@Service
class CommentService @Autowired constructor(
        private val commentRepository: CommentRepository,
        private val userDetailsServiceImpl: UserDetailsServiceImpl,
        private val voteRepository: VoteRepository,
        private val postService: PostService,
        private val previousCommentVersionRepository: PreviousCommentVersionRepository,
        private val battleService: BattleService,
        private val commentLikeRepository: CommentLikeRepository,
        private val battleRepository: BattleRepository
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

        if (comment.parentComment != null) {
            return null
        }

        if (!canSeeComment(user, comment)) return null

        return addComment(text, user, null, null, comment)
    }

    private fun addComment(text: String, user: User, parentBattle: Battle?, parentPost: Post?, parentComment: Comment?): CommentDto {
        val comment = Comment(text, parentBattle, parentPost, parentComment, user)
        val savedComment = commentRepository.save(comment)
        return getAsDto(CommentIntermediate(savedComment, 0, 0, false, false), user)
    }

    fun getPostComments(user: User, postId: String, page: Int): List<CommentDto>? {
        val post = postService.getPostReferenceFromIdUnsafe(postId) ?: return null

        if (!postService.canSeeUserAndPost(user, post)) return null

        return commentRepository.getAllByParentPost(post, user, PageRequest.of(page, Constants.COMMENTS_PAGE)).map {
            getAsDto(it, user)
        }
    }

    fun getBattleComments(user: User, battleId: String, page: Int): List<CommentDto>? {
        val battle = battleService.getBattleReferenceFromIdUnsafe(battleId) ?: return null

        if (!battleRepository.canSeeBattle(user, battle)) return null

        return commentRepository.getAllByParentBattle(battle, user, PageRequest.of(page, Constants.COMMENTS_PAGE)).map {
            getAsDto(it, user)
        }
    }

    @Transactional
    fun getCommentComments(user: User, commentId: String, page: Int): List<CommentDto>? {
        val comment = getCommentFromIdUnsafe(commentId) ?: return null

        if (comment.parentComment != null) {
            return null
        }

        if (!canSeeComment(user, comment)) return null

        return commentRepository.getAllByParentComment(comment, user, PageRequest.of(page, Constants.COMMENTS_PAGE)).map {
            getAsDto(it, user)
        }
    }

    private fun canSeeComment(user: User, comment: Comment): Boolean {

        // If child comment, check if user can see parent
        if (comment.parentComment != null) {
            return canSeeComment(user, comment.parentComment!!)
        }

        // Check if comment is on post and if can user can access it
        if (comment.parentPost != null) {
            if (!postService.canSeeUserAndPost(user, comment.parentPost!!)) return false
        }

        // Check if comment is on battle and if user can access it
        if (comment.parentBattle != null) {
            if (!battleRepository.canSeeBattle(user, comment.parentBattle!!)) return false
        }

        return true
    }

    @Transactional
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

    fun editComment(user: User, newText: String, commentId: String): Boolean {
        val comment = getCommentFromIdUnsafe(commentId) ?: return false

        if (comment.commenter?.id != user.id) return false

        val oldText = comment.text
        comment.text = newText
        comment.isEdited = true
        comment.creationDate = Date()

        commentRepository.save(comment)

        previousCommentVersionRepository.save(PreviousCommentVersion(oldText, comment))

        return true
    }

    fun getCommentHistory(user: User, commentId: String, page: Int): List<CommentEditDto>? {
        val comment = getCommentReferenceFromIdUnsafe(commentId) ?: return null

        if (!canSeeComment(user, comment)) return null

        return previousCommentVersionRepository.findByComment(comment,
                PageRequest.of(page,
                        Constants.COMMENTS_PAGE, Sort.by("creationDate").descending())).map {
            return@map CommentEditDto(it.text, it.creationDate, it.comment.id.toString())
        }
    }

    fun like(user: User, commentId: String): Boolean {
        val comment = getCommentFromIdUnsafe(commentId) ?: return false

        if (!canSeeComment(user, comment)) return false

        if (commentLikeRepository.existsByLikedByAndComment(user, comment)) return false

        commentLikeRepository.save(CommentLike(comment, user))

        return true
    }

    @Transactional
    fun unlike(user: User, commentId: String): Boolean {
        val comment = getCommentReferenceFromIdUnsafe(commentId) ?: return false

        val result = commentLikeRepository.deleteByLikedByAndComment(user, comment)

        return result > 0
    }

    fun getLikedBy(user: User, commentId: String, page: Int): List<UserDto>? {
        val comment = getCommentReferenceFromIdUnsafe(commentId) ?: return null

        return commentLikeRepository.getLikedByByUserAndComment(user, comment,
                PageRequest.of(page, Constants.LIKED_BY_PAGE)).map {
            return@map userDetailsServiceImpl.getAsDto(it)
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

    private fun getAsDto(commentIntermediate: CommentIntermediate, user: User): CommentDto {
        val comment = commentIntermediate.comment
        return CommentDto(
                comment.text,
                if (comment.isDeleted ||
                        comment.commenter == null ||
                        (!commentIntermediate.followsCommenter && commentIntermediate.comment.commenter?.id != user.id)) {
                    null
                } else userDetailsServiceImpl.getAsDto(comment.commenter!!),
                comment.isEdited,
                // TODO
                commentIntermediate.responseCount.toInt(),
                commentIntermediate.likes,
                commentIntermediate.hasUserLiked,
                comment.creationDate,
                comment.id.toString()
        )
    }
}