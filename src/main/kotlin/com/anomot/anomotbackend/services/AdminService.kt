package com.anomot.anomotbackend.services

import com.anomot.anomotbackend.dto.*
import com.anomot.anomotbackend.entities.*
import com.anomot.anomotbackend.repositories.*
import com.anomot.anomotbackend.utils.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.security.access.annotation.Secured
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import javax.transaction.Transactional

@Service
class AdminService @Autowired constructor(
        private val reportRepository: ReportRepository,
        private val reportTicketRepository: ReportTicketRepository,
        private val reportDecisionRepository: ReportDecisionRepository,
        private val mediaService: MediaService,
        private val mediaRepository: MediaRepository,
        private val appealRepository: AppealRepository,
        private val userDetailsServiceImpl: UserDetailsServiceImpl,
        private val appealDecisionRepository: AppealDecisionRepository,
        private val userDeletionService: UserDeletionService,
        private val battleRepository: BattleRepository,
        private val postRepository: PostRepository,
        private val commentRepository: CommentRepository,
        private val userRepository: UserRepository,
        private val banRepository: BanRepository,
        private val authenticationService: AuthenticationService,
        private val battleService: BattleService,
        private val notificationService: NotificationService,
        private val postService: PostService,
        private val followRepository: FollowRepository,
        private val voteService: VoteService,
        private val loginInfoExtractorService: LoginInfoExtractorService,
        private val commentLikeRepository: CommentLikeRepository,
        private val likeRepository: LikeRepository,
        private val successfulLoginRepository: SuccessfulLoginRepository,
        private val voteRepository: VoteRepository,
        private val battleQueueRepository: BattleQueueRepository,
        private val urlRepository: UrlRepository
) {
    @Secured("ROLE_ADMIN")
    fun getReports(page: Int): List<ReportTicketDto> {
        return reportTicketRepository.getAll(PageRequest.of(page, 10, Sort.by("creation_date").descending())).map {
            getAsReportTicketDto(it)
        }
    }

    private fun getAsReportTicketDto(it: ReportTicketIntermediary): ReportTicketDto {
        val post = if (it.post != null) postRepository.getReferenceById(it.post!!) else null
        val battle = if (it.battle != null) battleRepository.getReferenceById(it.battle!!) else null
        val comment = if (it.comment != null) commentRepository.getReferenceById(it.comment!!) else null
        val user = if (it.user != null) userRepository.getReferenceById(it.user!!) else null

        return ReportTicketDto(ReportType.values().first {i -> i.ordinal == it.reportType},
                if (post != null) asPostDto(post, it.postLikes) else null,
                if (battle != null) AdminBattleDto(
                        if (battle.goldPost != null) asPostDto(battle.goldPost!!, null) else null,
                        if (battle.redPost != null) asPostDto(battle.redPost!!, null) else null,
                        it.goldVotes,
                        it.redVotes,
                        battle.finished,
                        battle.finishDate!!,
                battle.id.toString()) else null,
                if (comment != null) getAsCommentDto(comment, it.commentLikes, it.commentResponseCount.toInt()) else null,
                if (user != null) userDetailsServiceImpl.getAsDto(user) else null,
                it.isDecided,
                it.decisions.toInt(),
                it.creationDate,
                it.id.toString())
    }

    private fun getAsCommentDto(comment: Comment, commentLikes: Long, responseCount: Int): CommentDto {
        return CommentDto(
                comment.text,
                if (comment.commenter != null) userDetailsServiceImpl.getAsDto(comment.commenter!!) else null,
                comment.isEdited,
                responseCount,
                commentLikes,
                null,
                comment.creationDate,
                comment.id.toString()
        )
    }

    private fun asPostDto(post: Post, likes: Long?): PostDto {
        return PostDto(post.type,
                    post.text,
                    if (post.media != null) MediaDto(post.media!!.mediaType, post.media!!.name.toString()) else null,
                    userDetailsServiceImpl.getAsDto(post.poster),
                    likes,
                    hasUserLiked = null,
                    post.creationDate,
                    post.id.toString())
    }

    @Secured("ROLE_ADMIN")
    fun getUndecidedReports(page: Int): List<ReportTicketDto> {
        return reportTicketRepository.getAllByDecidedIsFalse(PageRequest.of(page, 10, Sort.by("creationDate").ascending())).map {
            getAsReportTicketDto(it)
        }
    }

    @Secured("ROLE_ADMIN")
    @Transactional
    fun decideReport(user: User, reportTicketId: String, decision: String): Boolean {
        val report = getReportTicketReferenceByIdUnsafe(reportTicketId) ?: return false

        reportDecisionRepository.save(ReportDecision(report, decision, user))
        return true
    }

    private fun getReportTicketReferenceByIdUnsafe(reportTicketId: String): ReportTicket? {
        val ticketId = reportTicketId.toLongOrNull() ?: return null
        if (!reportTicketRepository.existsById(ticketId)) return null

        return reportTicketRepository.getReferenceById(ticketId)
    }

    @Secured("ROLE_ADMIN")
    fun getDecisions(reportTicketId: String, page: Int): List<TicketDecisionDto>? {
        val report = getReportTicketReferenceByIdUnsafe(reportTicketId) ?: return null

        return reportDecisionRepository.getAllByReportTicket(report, PageRequest.of(page, 10, Sort.by("creationDate").descending())).map {
            TicketDecisionDto(
                    it.decision,
                    if (it.decidedBy != null) userDetailsServiceImpl.getAsDto(it.decidedBy!!) else null,
                    it.creationDate
            )
        }
    }

    @Secured("ROLE_ADMIN")
    fun getReports(reportTicketId: String, page: Int): List<AdminReportDto>? {
        val report = getReportTicketReferenceByIdUnsafe(reportTicketId) ?: return null

        return reportRepository.getAllByReportTicket(report,  PageRequest.of(page, 10, Sort.by("creationDate").descending())).map {
            AdminReportDto(it.reportReason, it.other, if (it.reporter != null) userDetailsServiceImpl.getAsDto(it.reporter!!) else null, it.reportTicket.id.toString())
        }
    }

    @Secured("ROLE_ADMIN")
    fun getAppeals(page: Int): List<AdminAppealDto> {
        return appealRepository.getAll(PageRequest.of(page, 10, Sort.by("creationDate").descending())).map {
            appealToAdminAppealDto(it)
        }
    }

    @Secured("ROLE_ADMIN")
    fun getUndecidedAppeals(page: Int): List<AdminAppealDto> {
        return appealRepository.getAllByDecidedIsFalse(PageRequest.of(page, 10, Sort.by("creationDate").descending())).map {
            appealToAdminAppealDto(it)
        }
    }

    @Secured("ROLE_ADMIN")
    @Transactional
    fun decideAppeal(user: User, appealId: String, decision: AppealAction, explanation: String): Boolean {
        val appealIdLong = appealId.toLongOrNull() ?: return false
        val savedDecision = appealDecisionRepository.save(AppealDecision(user, decision, explanation))
        appealDecisionRepository.flush()
        if (!appealRepository.existsById(appealIdLong)) return false
        val appeal = appealRepository.getReferenceById(appealIdLong)

        appeal.decision = savedDecision
        appeal.decided = true

        return true
    }


    @Secured("ROLE_ADMIN")
    @Transactional
    fun deleteMedia(mediaId: String): Boolean {
        val media = mediaRepository.getByName(UUID.fromString(mediaId)) ?: return false
        return mediaService.deleteMedia(media)
    }

    @Secured("ROLE_ADMIN")
    @Transactional
    fun banUser(user: User, userToBan: User, reason: String, until: Date): Boolean {
        if (user.id == userToBan.id) return false
        banRepository.save(Ban(userToBan, until, user, reason))
        userDetailsServiceImpl.expireUserSessions(userToBan)
        return true
    }

    @Secured("ROLE_ADMIN")
    @Transactional
    fun getBans(user: User, page: Int): List<BanDto> {
        return banRepository.getAllByUser(user, PageRequest.of(page, 12, Sort.by("creationDate").descending())).map {
            BanDto(it.creationDate,
                    it.until,
                    if (it.bannedBy != null) userDetailsServiceImpl.getAsDto(it.bannedBy!!) else null,
                    it.reason)
        }
    }

    @Secured("ROLE_ADMIN")
    @Transactional
    fun deleteUser(userToDelete: User, admin: Authentication, adminPassword: String) {
        if (authenticationService.verifyAuthenticationWithoutMfa(admin, adminPassword) == null) {
            throw BadCredentialsException("Bad credentials")
        }

        userDetailsServiceImpl.expireUserSessions(userToDelete)
        userDeletionService.deleteUser(userToDelete)
    }

    @Secured("ROLE_ADMIN")
    fun promote(appealId: String): Boolean {
        val appeal = getAppealReferenceByIdStringUnsafe(appealId) ?: return false

        when(appeal.objective) {
            AppealObjective.POST -> promoteToPost(appeal)
            AppealObjective.BATTLE -> promoteToBattle(appeal)
            AppealObjective.AVATAR -> promoteToAvatar(appeal)
        }

        notificationService.sendPromotionNotification(appeal.appealedBy, appeal)

        return true
    }

    @Secured("ROLE_ADMIN")
    fun promoteToPost(appeal: Appeal): Post {
        return postRepository.save(Post(appeal.appealedBy, appeal.media, null, PostType.MEDIA))
    }

    @Secured("ROLE_ADMIN")
    fun promoteToBattle(appeal: Appeal): Boolean {
        val result = battleService.queuePostForBattle(promoteToPost(appeal))
        if (result != null) {
            notificationService.sendBattleBeginNotification(appeal.appealedBy, result)
        }

        return true
    }

    @Secured("ROLE_ADMIN")
    fun promoteToAvatar(appeal: Appeal): Boolean {
        userDetailsServiceImpl.changeAvatar(appeal.media)
        return true
    }

    private fun getAppealReferenceByIdStringUnsafe(id: String): Appeal? {
        val idLong = id.toLongOrNull() ?: return null
        if (!appealRepository.existsById(idLong)) return null
        return appealRepository.getReferenceById(idLong)
    }

    fun getReportTicketReferenceByIdStringUnsafe(id: String): ReportTicket? {
        val idLong = id.toLongOrNull() ?: return null
        if (!reportTicketRepository.existsById(idLong)) return null
        return reportTicketRepository.getReferenceById(idLong)
    }

    @Secured("ROLE_ADMIN")
    @Transactional
    fun changeUsername(user: User, usernameChangeDto: UsernameChangeDto): Boolean {
        userRepository.setUsername(usernameChangeDto.username, user.id!!)
        userDetailsServiceImpl.expireUserSessions(user)
        return true
    }

    @Secured("ROLE_ADMIN")
    fun changePassword(user: User, passwordChangeDto: AdminPasswordChangeDto): Boolean {
        userDetailsServiceImpl.changePasswordForce(user, passwordChangeDto.newPassword)
        userDetailsServiceImpl.expireUserSessions(user)
        return true
    }

    @Secured("ROLE_ADMIN")
    fun deleteAvatar(user: User): Boolean {
        userDetailsServiceImpl.deleteAvatar(user)
        userDetailsServiceImpl.expireUserSessions(user)
        return true
    }

    @Secured("ROLE_ADMIN")
    fun deletePost(post: Post): Boolean {
        return postService.deletePost(post.id!!, post.poster)
    }

    @Secured("ROLE_ADMIN")
    fun getUserFollowerCount(user: User): CountDto {
        return CountDto(followRepository.countFollowsByFollowed(user))
    }

    @Secured("ROLE_ADMIN")
    fun getUserFollowedCount(user: User): CountDto {
        return CountDto(followRepository.countFollowsByFollower(user))
    }

    @Secured("ROLE_ADMIN")
    fun getUserFollowers(user: User, page: Int): List<UserDto> {
        return followRepository.getFollowsByFollowed(user, PageRequest.of(page, Constants.FOLLOWS_PER_PAGE)).map {
            return@map userDetailsServiceImpl.getAsDto(it.followed)
        }
    }

    @Secured("ROLE_ADMIN")
    fun getUserFollowed(user: User, page: Int): List<UserDto> {
        return followRepository.getFollowsByFollower(user, PageRequest.of(page, Constants.FOLLOWS_PER_PAGE)).map {
            return@map userDetailsServiceImpl.getAsDto(it.followed)
        }
    }

    @Secured("ROLE_ADMIN")
    fun getUserPosts(user: User, page: Int): List<PostDto> {
        return postRepository.getAllByPoster(user, PageRequest.of(page, Constants.POST_PAGE)).map {
            val post = it.post
            PostDto(post.type,
                    post.text,
                    if (post.media != null) MediaDto(post.media!!.mediaType, post.media!!.name.toString()) else null,
                    userDetailsServiceImpl.getAsDto(post.poster),
                    it.likes,
                    null,
                    post.creationDate,
                    post.id.toString())
        }
    }

    @Secured("ROLE_ADMIN")
    fun getPost(post: Post): PostDto {
        return PostDto(post.type,
                post.text,
                if (post.media != null) MediaDto(post.media!!.mediaType, post.media!!.name.toString()) else null,
                userDetailsServiceImpl.getAsDto(post.poster),
                postRepository.getLikesByPost(),
                null,
                post.creationDate,
                post.id.toString())
    }

    @Secured("ROLE_ADMIN")
    fun getBattleQueue(user: User, page: Int): List<PostDto> {
        return battleService.getPostsInQueue(user, page)
    }

    @Secured("ROLE_ADMIN")
    fun getBattles(user: User, page: Int): List<SelfBattleDto> {
        return battleService.getBattles(user, page)
    }

    @Secured("ROLE_ADMIN")
    fun getBattle(battle: Battle): AdminBattleDto {
        val intermediate = battleRepository.getBattleById(battle.id!!)

        val goldPost = battle.goldPost
        val redPost = battle.redPost

        return AdminBattleDto(
                if (goldPost == null) null else PostDto(goldPost.type,
                        goldPost.text,
                        if (goldPost.media != null) MediaDto(goldPost.media!!.mediaType, goldPost.media!!.name.toString()) else null,
                        userDetailsServiceImpl.getAsDto(goldPost.poster),
                        null,
                        null,
                        goldPost.creationDate,
                        goldPost.id.toString()),
                if (redPost == null) null else PostDto(redPost.type,
                        redPost.text,
                        if (redPost.media != null) MediaDto(redPost.media!!.mediaType, redPost.media!!.name.toString()) else null,
                        userDetailsServiceImpl.getAsDto(redPost.poster),
                        null,
                        null,
                        redPost.creationDate,
                        redPost.id.toString()),
                intermediate.votesForGold,
                intermediate.votesForRed,
                battle.finished,
                battle.finishDate!!,
                battle.id.toString())
    }

    @Secured("ROLE_ADMIN")
    fun getUserBattle(user: User, battle: Battle): SelfBattleDto? {
        return battleService.getSelfBattle(user, battle.id!!)
    }

    @Secured("ROLE_ADMIN")
    fun getVotes(user: User, page: Int): List<VotedBattleDto> {
        return voteService.getVoteHistory(user, page)
    }

    @Secured("ROLE_ADMIN")
    fun getLogins(user: User, page: Int): List<LoginInfoDto> {
        return loginInfoExtractorService.getByUser(user, PageRequest.of(page, Constants.LOGINS_PER_PAGE, Sort.by("date").descending()))
    }

    @Secured("ROLE_ADMIN")
    fun getNotifications(user: User, page: Int): List<NotificationDto> {
        return notificationService.getNotifications(user, page)
    }

    @Secured("ROLE_ADMIN")
    fun getUserAppeals(user: User, page: Int): List<AdminAppealDto> {
        return appealRepository.getAllByAppealedBy(user, PageRequest.of(page, 10, Sort.by("creationDate").descending())).map {
            appealToAdminAppealDto(it)
        }
    }

    @Secured("ROLE_ADMIN")
    fun getUserComments(user: User, page: Int): List<CommentDto> {
        return commentRepository.getAllByCommenter(user, PageRequest.of(page, Constants.COMMENTS_PAGE, Sort.by("creationDate").descending())).map {
            getAsCommentDto(it.comment, it.likes, it.responseCount.toInt())
        }
    }

    @Secured("ROLE_ADMIN")
    fun getCommentPost(post: Post, page: Int): List<CommentDto> {
        return commentRepository.getAllByParentPost(post, PageRequest.of(page, Constants.COMMENTS_PAGE)).map {
            getAsCommentDto(it.comment, it.likes, it.responseCount.toInt())
        }
    }

    @Secured("ROLE_ADMIN")
    fun getCommentBattle(battle: Battle, page: Int): List<CommentDto> {
        return commentRepository.getAllByParentBattle(battle, PageRequest.of(page, Constants.COMMENTS_PAGE)).map {
            getAsCommentDto(it.comment, it.likes, it.responseCount.toInt())
        }
    }

    @Secured("ROLE_ADMIN")
    fun getCommentComment(comment: Comment, page: Int): List<CommentDto> {
        return commentRepository.getAllByParentComment(comment, PageRequest.of(page, Constants.COMMENTS_PAGE)).map {
            getAsCommentDto(it.comment, it.likes, it.responseCount.toInt())
        }
    }

    @Secured("ROLE_ADMIN")
    fun getUserReports(user: User, page: Int): List<AdminReportDto> {
        return reportRepository.getAllByReporter(user, PageRequest.of(page, 10, Sort.by("creationDate").descending())).map {
            AdminReportDto(it.reportReason, it.other, userDetailsServiceImpl.getAsDto(it.reporter!!), it.ticketId.toString())
        }
    }

    @Secured("ROLE_ADMIN")
    fun getTicketById(reportTicket: ReportTicket): ReportTicketDto {
        return getAsReportTicketDto(reportTicketRepository.getIntermediaryById(reportTicket.id!!))
    }

    @Secured("ROLE_ADMIN")
    fun getLikedByPost(post: Post, page: Int): List<UserDto> {
        return likeRepository.getLikedByByPost(post, PageRequest.of(page, Constants.LIKED_BY_PAGE)).map {
            userDetailsServiceImpl.getAsDto(it)
        }
    }

    @Secured("ROLE_ADMIN")
    fun getLikedByComment(comment: Comment, page: Int): List<UserDto> {
        return commentLikeRepository.getLikedByByComment(comment, PageRequest.of(page, Constants.LIKED_BY_PAGE)).map {
            userDetailsServiceImpl.getAsDto(it)
        }
    }

    fun appealToAdminAppealDto(it: Appeal): AdminAppealDto {
        return AdminAppealDto(
                userDetailsServiceImpl.getAsDto(it.appealedBy),
                it.reason,
                it.objective,
                it.media.name.toString(),
                if (it.reason != AppealReason.SIMILAR_FOUND) null else {
                    battleRepository.getSimilarMedia(it.appealedBy, it.media,
                            if (it.media.mediaType == MediaType.VIDEO && it.media.duration != null) {
                                it.media.duration
                            } else 0f).map {post ->
                        PostDto(post.type,
                                null,
                                MediaDto(post.media!!.mediaType, post.media!!.name.toString()),
                                userDetailsServiceImpl.getAsDto(post.poster),
                                null,
                                null,
                                post.creationDate,
                                post.id.toString())
                    }
                },
                it.decided,
                if (it.decision != null && it.decision?.decidedBy != null) userDetailsServiceImpl.getAsDto(it.decision!!.decidedBy!!) else null,
                if (it.decision != null) it.decision!!.decision else null,
                if (it.decision != null) it.decision!!.explanation else null,
                it.creationDate,
                it.id.toString())
    }

    @Secured("ROLE_ADMIN")
    fun getUserCountWithin(days: Int): Long {
        return userRepository.findByAfterDate(Date.from(Instant.now().minus(days.toLong(), ChronoUnit.DAYS)))
    }

    @Secured("ROLE_ADMIN")
    fun getLoginsWithin(days: Int): Long {
        return successfulLoginRepository.findByAfterDate(Date.from(Instant.now().minus(days.toLong(), ChronoUnit.DAYS)))
    }

    @Secured("ROLE_ADMIN")
    fun getAverageVotePossibilitiesToActualVotes(days: Int): AverageVotePossibilitiesToActualVotesDto {
        return voteRepository.getAverageVotePossibilitiesToActualVotes(Date.from(Instant.now().minus(days.toLong(), ChronoUnit.DAYS)))
    }

    @Secured("ROLE_ADMIN")
    fun getPostsCount(days: Int): Long {
        return postRepository.findByAfterDate(Date.from(Instant.now().minus(days.toLong(), ChronoUnit.DAYS)))
    }

    @Secured("ROLE_ADMIN")
    fun getBattleCount(days: Int): Long {
        return battleRepository.findByAfterDate(Date.from(Instant.now().minus(days.toLong(), ChronoUnit.DAYS)))
    }

    @Secured("ROLE_ADMIN")
    fun getQueueCount(days: Int): Long {
        return battleQueueRepository.findByAfterDate(Date.from(Instant.now().minus(days.toLong(), ChronoUnit.DAYS)))
    }

    @Secured("ROLE_ADMIN")
    fun getUrl(url: String): AdminUrlDto? {
        val newUrl = urlRepository.getByInAppUrl(url) ?: return null
        return AdminUrlDto(userDetailsServiceImpl.getAsDto(newUrl.publisher), newUrl.inAppUrl, listOf())
    }

    @Secured("ROLE_ADMIN")
    fun deleteUrl(url: String): Boolean {
        val newUrl = urlRepository.getByInAppUrl(url) ?: return false
        urlRepository.delete(newUrl)
        return true
    }
}