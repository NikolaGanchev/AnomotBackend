package com.anomot.anomotbackend.services

import com.anomot.anomotbackend.entities.User
import com.anomot.anomotbackend.repositories.*
import com.anomot.anomotbackend.security.CustomUserDetails
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import javax.transaction.Transactional

@Service
class UserDeletionService @Autowired constructor(
        private val authenticationService: AuthenticationService,
        private val userRepository: UserRepository,
        private val emailVerificationTokenRepository: EmailVerificationTokenRepository,
        private val mfaTotpSecretRepository: MfaTotpSecretRepository,
        private val mfaRecoveryCodeRepository: MfaRecoveryCodeRepository,
        private val passwordResetTokenRepository: PasswordResetTokenRepository,
        private val rememberMeTokenRepository: RememberMeTokenRepository,
        private val successfulLoginRepository: SuccessfulLoginRepository,
        private val urlRepository: UrlRepository,
        private val voteRepository: VoteRepository,
        private val followCodeRepository: FollowCodeRepository,
        private val followRepository: FollowRepository,
        private val notificationRepository: NotificationRepository,
        private val commentLikeRepository: CommentLikeRepository,
        private val previousCommentVersionRepository: PreviousCommentVersionRepository,
        private val commentRepository: CommentRepository,
        private val likeRepository: LikeRepository,
        private val battleRepository: BattleRepository,
        private val battleQueueRepository: BattleQueueRepository,
        private val postRepository: PostRepository,
        private val nsfwScanRepository: NsfwScanRepository,
        private val mediaService: MediaService
) {

    @Transactional
    fun deleteUser(password: String) {
        val userDetails = SecurityContextHolder.getContext().authentication
                ?: throw AccessDeniedException("No authentication present")

        if (authenticationService.verifyAuthenticationWithoutMfa(userDetails, password) == null) {
            throw BadCredentialsException("Bad credentials")
        }

        val user = userRepository.getReferenceById((userDetails.principal as CustomUserDetails).id!!)

        deleteUser(user)
    }

    fun deleteUser(user: User) {
        emailVerificationTokenRepository.deleteByUser(user)
        mfaTotpSecretRepository.deleteByUser(user)
        mfaRecoveryCodeRepository.deleteByUser(user)
        passwordResetTokenRepository.deleteByUser(user)
        rememberMeTokenRepository.deleteByUser(user)
        notificationRepository.deleteByUser(user)
        successfulLoginRepository.deleteByUser(user)
        urlRepository.deleteByPublisher(user)
        voteRepository.deleteByUser(user)
        followCodeRepository.deleteByUser(user)
        followRepository.deleteByUser(user)
        commentLikeRepository.deleteByUser(user)
        previousCommentVersionRepository.deleteByUser(user)
        commentRepository.setDeletedByUser(user)
        commentRepository.deleteByUser(user)
        likeRepository.deleteByUser(user)
        battleRepository.setPostsByUserToNull(user)
        battleQueueRepository.deleteByUser(user)
        postRepository.deleteByUser(user)
        nsfwScanRepository.deleteByUser(user)
        mediaService.deleteMediaByUserWithoutNsfwScans(user)
        mediaService.deleteFilesByUser(user)

        userRepository.deleteById(user.id!!)
    }
}