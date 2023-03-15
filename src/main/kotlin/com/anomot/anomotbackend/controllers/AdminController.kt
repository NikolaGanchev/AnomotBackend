package com.anomot.anomotbackend.controllers

import com.anomot.anomotbackend.dto.*
import com.anomot.anomotbackend.security.CustomUserDetails
import com.anomot.anomotbackend.services.*
import com.anomot.anomotbackend.utils.Constants
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.annotation.Secured
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

@RestController
class AdminController(
        private val adminService: AdminService,
        private val userDetailsServiceImpl: UserDetailsServiceImpl,
        private val postService: PostService,
        private val battleService: BattleService,
        private val loginInfoExtractorService: LoginInfoExtractorService,
        private val commentService: CommentService
) {
    @Secured("ROLE_ADMIN")
    @GetMapping("/admin/tickets")
    fun getReports(@RequestParam("page") page: Int, authentication: Authentication): ResponseEntity<List<ReportTicketDto>> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)
        return ResponseEntity(adminService.getReports(user.id!!, page), HttpStatus.OK)
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/admin/ticket/undecided")
    fun getUndecidedReports(@RequestParam("page") page: Int, authentication: Authentication): ResponseEntity<List<ReportTicketDto>> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)
        return ResponseEntity(adminService.getUndecidedReports(user.id!!, page), HttpStatus.OK)
    }

    @Secured("ROLE_ADMIN")
    @PostMapping("/admin/ticket/decide")
    fun decideReport(@RequestBody @Valid decisionDto: DecisionDto, authentication: Authentication): ResponseEntity<String> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)
        val result = adminService.decideReport(user, decisionDto.reportTicketId, decisionDto.decision)
        return ResponseEntity(if (result) HttpStatus.OK else HttpStatus.BAD_REQUEST)
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/admin/ticket/decisions")
    fun getDecisions(@RequestParam("id") id: String, @RequestParam("page") page: Int): ResponseEntity<List<TicketDecisionDto>> {
        val result = adminService.getDecisions(id, page) ?: return ResponseEntity(HttpStatus.BAD_REQUEST)

        return ResponseEntity(result, HttpStatus.OK)
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/admin/ticket/reports")
    fun getReports(@RequestParam("id") id: String, @RequestParam("page") page: Int): ResponseEntity<List<AdminReportDto>> {
        val result = adminService.getReports(id, page) ?: return ResponseEntity(HttpStatus.BAD_REQUEST)

        return ResponseEntity(result, HttpStatus.OK)
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/admin/appeals")
    fun getAppeals(@RequestParam("page") page: Int): ResponseEntity<List<AdminAppealDto>> {
        return ResponseEntity(adminService.getAppeals(page), HttpStatus.OK)
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/admin/appeals/undecided")
    fun getUndecidedAppeals(@RequestParam("page") page: Int): ResponseEntity<List<AdminAppealDto>> {
        return ResponseEntity(adminService.getUndecidedAppeals(page), HttpStatus.OK)
    }

    @Secured("ROLE_ADMIN")
    @PostMapping("/admin/appeal/decide")
    fun decideAppeal(@RequestBody @Valid appealDecisionDto: AppealDecisionDto, authentication: Authentication): ResponseEntity<String> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)
        val result = adminService.decideAppeal(user, appealDecisionDto.id, appealDecisionDto.decision, appealDecisionDto.explanation)
        return ResponseEntity(if (result) HttpStatus.OK else HttpStatus.BAD_REQUEST)
    }

    // The media has to not be inside a post or battle
    @Secured("ROLE_ADMIN")
    @DeleteMapping("/admin/media/{id}")
    fun deleteMedia(@PathVariable(value="id") @Min(36) @Max(36) id: String, authentication: Authentication): ResponseEntity<String> {
        val result = adminService.deleteMedia(id)
        return ResponseEntity(if (result) HttpStatus.OK else HttpStatus.BAD_REQUEST)
    }

    @Secured("ROLE_ADMIN")
    @PostMapping("/admin/user/ban")
    fun banUser(@RequestBody @Valid userBanDto: UserBanDto, authentication: Authentication): ResponseEntity<String> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)
        val otherUser = userDetailsServiceImpl.getUserReferenceFromIdUnsafe(userBanDto.userId) ?: return ResponseEntity(HttpStatus.BAD_REQUEST)
        val result = adminService.banUser(user, otherUser, userBanDto.reason, userBanDto.until)
        return ResponseEntity(if (result) HttpStatus.OK else HttpStatus.BAD_REQUEST)
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/admin/user/bans")
    fun getBans(@RequestParam("id") id: String, @RequestParam("page") page: Int): ResponseEntity<List<BanDto>> {
        val user = userDetailsServiceImpl.getUserReferenceFromIdUnsafe(id) ?: return ResponseEntity(HttpStatus.BAD_REQUEST)
        return ResponseEntity(adminService.getBans(user, page), HttpStatus.OK)
    }

    @Secured("ROLE_ADMIN")
    @DeleteMapping("/admin/user")
    fun deleteUser(@RequestParam("id") id: String, @RequestBody @Valid deleteDto: AccountDeleteDto, authentication: Authentication): ResponseEntity<String> {
        val user = userDetailsServiceImpl.getUserReferenceFromIdUnsafe(id) ?: return ResponseEntity(HttpStatus.BAD_REQUEST)
        try {
            adminService.deleteUser(user, authentication, deleteDto.password)
        } catch (e: BadCredentialsException) {
            return ResponseEntity(HttpStatus.UNAUTHORIZED)
        }
        return ResponseEntity(HttpStatus.OK)
    }

    @Secured("ROLE_ADMIN")
    @PostMapping("/admin/appeal/promote")
    fun promote(@RequestParam("id") id: String, authentication: Authentication): ResponseEntity<String> {
        val result = adminService.promote(id)

        return ResponseEntity(if (result) HttpStatus.OK else HttpStatus.BAD_REQUEST)
    }

    @Secured("ROLE_ADMIN")
    @PutMapping("/admin/user/username")
    fun changeUsername(@RequestParam("id") id: String, @RequestBody @Valid usernameChangeDto: UsernameChangeDto, authentication: Authentication): ResponseEntity<String> {
        val user = userDetailsServiceImpl.getUserReferenceFromIdUnsafe(id) ?: return ResponseEntity(HttpStatus.BAD_REQUEST)
        val result = adminService.changeUsername(user, usernameChangeDto)

        return ResponseEntity(if (result) HttpStatus.OK else HttpStatus.BAD_REQUEST)
    }

    @Secured("ROLE_ADMIN")
    @PutMapping("/admin/user/password")
    fun changePassword(@RequestParam("id") id: String, @RequestBody @Valid passwordChangeDto: AdminPasswordChangeDto, authentication: Authentication): ResponseEntity<String> {
        val user = userDetailsServiceImpl.getUserReferenceFromIdUnsafe(id) ?: return ResponseEntity(HttpStatus.BAD_REQUEST)
        val result = adminService.changePassword(user, passwordChangeDto)

        return ResponseEntity(if (result) HttpStatus.OK else HttpStatus.BAD_REQUEST)
    }

    @Secured("ROLE_ADMIN")
    @PutMapping("/admin/user/email")
    fun changeEmail(@RequestParam("id") id: String, @RequestBody @Valid adminEmailChangeDto: AdminEmailChangeDto, authentication: Authentication): ResponseEntity<String> {
        val user = userDetailsServiceImpl.getUserReferenceFromIdUnsafe(id) ?: return ResponseEntity(HttpStatus.BAD_REQUEST)
        val result = adminService.changeEmail(user, adminEmailChangeDto.newEmail)

        return ResponseEntity(if (result) HttpStatus.OK else HttpStatus.BAD_REQUEST)
    }

    @Secured("ROLE_ADMIN")
    @PutMapping("/admin/user/mfa")
    fun disableMfa(@RequestParam("id") id: String, authentication: Authentication): ResponseEntity<String> {
        val user = userDetailsServiceImpl.getUserReferenceFromIdUnsafe(id) ?: return ResponseEntity(HttpStatus.BAD_REQUEST)
        val result = adminService.disableMfa(user)

        return ResponseEntity(if (result) HttpStatus.OK else HttpStatus.BAD_REQUEST)
    }

    @Secured("ROLE_ADMIN")
    @DeleteMapping("/admin/user/avatar")
    fun deleteAvatar(@RequestParam("id") id: String, authentication: Authentication): ResponseEntity<String> {
        val user = userDetailsServiceImpl.getUserReferenceFromIdUnsafe(id) ?: return ResponseEntity(HttpStatus.BAD_REQUEST)
        val result = adminService.deleteAvatar(user)

        return ResponseEntity(if (result) HttpStatus.OK else HttpStatus.BAD_REQUEST)
    }

    @Secured("ROLE_ADMIN")
    @DeleteMapping("/admin/post")
    fun deletePost(@RequestParam("id") id: String, authentication: Authentication): ResponseEntity<String> {
        val post = postService.getPostReferenceFromIdUnsafe(id) ?: return ResponseEntity(HttpStatus.BAD_REQUEST)
        val result = adminService.deletePost(post)

        return ResponseEntity(if (result) HttpStatus.OK else HttpStatus.BAD_REQUEST)
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/admin/user")
    fun getUser(@RequestParam("id") id: String, authentication: Authentication): ResponseEntity<UserDto> {
        val user = userDetailsServiceImpl.getUserReferenceFromIdUnsafe(id) ?: return ResponseEntity(HttpStatus.BAD_REQUEST)

        return ResponseEntity(userDetailsServiceImpl.getAsDto(user), HttpStatus.OK)
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/admin/user/followers/count")
    fun getUserFollowerCount(@RequestParam("id") id: String, authentication: Authentication): ResponseEntity<CountDto> {
        val user = userDetailsServiceImpl.getUserReferenceFromIdUnsafe(id) ?: return ResponseEntity(HttpStatus.BAD_REQUEST)

        return ResponseEntity(adminService.getUserFollowerCount(user), HttpStatus.OK)
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/admin/user/followed/count")
    fun getUserFollowedCount(@RequestParam("id") id: String, authentication: Authentication): ResponseEntity<CountDto> {
        val user = userDetailsServiceImpl.getUserReferenceFromIdUnsafe(id) ?: return ResponseEntity(HttpStatus.BAD_REQUEST)

        return ResponseEntity(adminService.getUserFollowedCount(user), HttpStatus.OK)
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/admin/user/followers")
    fun getUserFollowers(@RequestParam("id") id: String, @RequestParam("page") page: Int, authentication: Authentication): ResponseEntity<List<UserDto>> {
        val user = userDetailsServiceImpl.getUserReferenceFromIdUnsafe(id) ?: return ResponseEntity(HttpStatus.BAD_REQUEST)

        return ResponseEntity(adminService.getUserFollowers(user, page), HttpStatus.OK)
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/admin/user/followed")
    fun getUserFollowed(@RequestParam("id") id: String, @RequestParam("page") page: Int, authentication: Authentication): ResponseEntity<List<UserDto>> {
        val user = userDetailsServiceImpl.getUserReferenceFromIdUnsafe(id) ?: return ResponseEntity(HttpStatus.BAD_REQUEST)

        return ResponseEntity(adminService.getUserFollowed(user, page), HttpStatus.OK)
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/admin/posts")
    fun getUserPosts(@RequestParam("id") id: String, @RequestParam("page") page: Int, authentication: Authentication): ResponseEntity<List<PostDto>> {
        val user = userDetailsServiceImpl.getUserReferenceFromIdUnsafe(id) ?: return ResponseEntity(HttpStatus.BAD_REQUEST)
        val admin = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)

        return ResponseEntity(adminService.getUserPosts(admin, user, page), HttpStatus.OK)
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/admin/post")
    fun getPost(@RequestParam("id") id: String, authentication: Authentication): ResponseEntity<PostDto> {
        val post = postService.getPostReferenceFromIdUnsafe(id) ?: return ResponseEntity(HttpStatus.BAD_REQUEST)

        return ResponseEntity(adminService.getPost(post), HttpStatus.OK)
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/admin/battle/queue")
    fun getBattleQueue(@RequestParam("id") id: String, @RequestParam("page") page: Int, authentication: Authentication): ResponseEntity<List<PostDto>> {
        val user = userDetailsServiceImpl.getUserReferenceFromIdUnsafe(id) ?: return ResponseEntity(HttpStatus.BAD_REQUEST)

        return ResponseEntity(adminService.getBattleQueue(user, page), HttpStatus.OK)
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/admin/battles")
    fun getBattles(@RequestParam("id") id: String, @RequestParam("page") page: Int, authentication: Authentication): ResponseEntity<List<SelfBattleDto>> {
        val user = userDetailsServiceImpl.getUserReferenceFromIdUnsafe(id) ?: return ResponseEntity(HttpStatus.BAD_REQUEST)

        return ResponseEntity(adminService.getBattles(user, page), HttpStatus.OK)
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/admin/battle")
    fun getBattle(@RequestParam("id") id: String, authentication: Authentication): ResponseEntity<AdminBattleDto> {
        val battle = battleService.getBattleReferenceFromIdUnsafe(id) ?: return ResponseEntity(HttpStatus.BAD_REQUEST)

        return ResponseEntity(adminService.getBattle(battle), HttpStatus.OK)
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/admin/user/battle")
    fun getUserBattle(@RequestParam("userId") userId: String, @RequestParam("battleId") battleId: String, authentication: Authentication): ResponseEntity<SelfBattleDto> {
        val user = userDetailsServiceImpl.getUserReferenceFromIdUnsafe(userId) ?: return ResponseEntity(HttpStatus.BAD_REQUEST)
        val battle = battleService.getBattleReferenceFromIdUnsafe(battleId) ?: return ResponseEntity(HttpStatus.BAD_REQUEST)

        return ResponseEntity(adminService.getUserBattle(user, battle), HttpStatus.OK)
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/admin/user/votes")
    fun getVotes(@RequestParam("id") id: String, @RequestParam("page") page: Int, authentication: Authentication): ResponseEntity<List<VotedBattleDto>> {
        val user = userDetailsServiceImpl.getUserReferenceFromIdUnsafe(id) ?: return ResponseEntity(HttpStatus.BAD_REQUEST)

        return ResponseEntity(adminService.getVotes(user, page), HttpStatus.OK)
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/admin/user/logins")
    fun getLogins(@RequestParam("id") id: String, @RequestParam("page") page: Int, authentication: Authentication): ResponseEntity<List<LoginInfoDto>> {
        val user = userDetailsServiceImpl.getUserReferenceFromIdUnsafe(id) ?: return ResponseEntity(HttpStatus.BAD_REQUEST)

        return ResponseEntity(adminService.getLogins(user, page), HttpStatus.OK)
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/admin/user/login")
    fun getLogin(@RequestParam("id") id: String, authentication: Authentication): ResponseEntity<LoginInfoDto> {
        val login = loginInfoExtractorService.getLoginReferenceFromIdUnsafe(id) ?: return ResponseEntity(HttpStatus.BAD_REQUEST)

        return ResponseEntity(LoginInfoDto.from(login), HttpStatus.OK)
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/admin/users/notifications")
    fun getNotifications(@RequestParam("id") id: String, @RequestParam("page") page: Int, authentication: Authentication): ResponseEntity<List<NotificationDto>> {
        val user = userDetailsServiceImpl.getUserReferenceFromIdUnsafe(id) ?: return ResponseEntity(HttpStatus.BAD_REQUEST)

        return ResponseEntity(adminService.getNotifications(user, page), HttpStatus.OK)
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/admin/users/appeals")
    fun getUserAppeals(@RequestParam("id") id: String, @RequestParam("page") page: Int, authentication: Authentication): ResponseEntity<List<AdminAppealDto>> {
        val user = userDetailsServiceImpl.getUserReferenceFromIdUnsafe(id) ?: return ResponseEntity(HttpStatus.BAD_REQUEST)

        return ResponseEntity(adminService.getUserAppeals(user, page), HttpStatus.OK)
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/admin/users/comments")
    fun getUserComments(@RequestParam("id") id: String, @RequestParam("page") page: Int, authentication: Authentication): ResponseEntity<List<CommentDto>> {
        val user = userDetailsServiceImpl.getUserReferenceFromIdUnsafe(id) ?: return ResponseEntity(HttpStatus.BAD_REQUEST)
        val admin = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)

        return ResponseEntity(adminService.getUserComments(admin, user, page), HttpStatus.OK)
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/admin/post/comment")
    fun getCommentPost(@RequestParam("id") postId: String, @RequestParam("page") page: Int, authentication: Authentication): ResponseEntity<List<CommentDto>> {
        val post = postService.getPostReferenceFromIdUnsafe(postId) ?: return ResponseEntity(HttpStatus.BAD_REQUEST)
        val admin = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)

        return ResponseEntity(adminService.getCommentPost(admin, post, page), HttpStatus.OK)
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/admin/battle/comment")
    fun getCommentBattle(@RequestParam("id") battleId: String, @RequestParam("page") page: Int, authentication: Authentication): ResponseEntity<List<CommentDto>> {
        val battle = battleService.getBattleReferenceFromIdUnsafe(battleId) ?: return ResponseEntity(HttpStatus.BAD_REQUEST)
        val admin = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)

        return ResponseEntity(adminService.getCommentBattle(admin, battle, page), HttpStatus.OK)
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/admin/comment/comment")
    fun getCommentComment(@RequestParam("id") commentId: String, @RequestParam("page") page: Int, authentication: Authentication): ResponseEntity<List<CommentDto>> {
        val comment = commentService.getCommentReferenceFromIdUnsafe(commentId) ?: return ResponseEntity(HttpStatus.BAD_REQUEST)
        val admin = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)

        return ResponseEntity(adminService.getCommentComment(admin, comment, page), HttpStatus.OK)
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/admin/comment/history")
    fun getCommentEdits(@RequestParam("id") commentId: String, @RequestParam("page") page: Int, authentication: Authentication): ResponseEntity<List<CommentEditDto>> {
        val comment = commentService.getCommentReferenceFromIdUnsafe(commentId) ?: return ResponseEntity(HttpStatus.BAD_REQUEST)

        return ResponseEntity(adminService.getCommentEdits(comment, page), HttpStatus.OK)
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/admin/user/reports")
    fun getUserReports(@RequestParam("id") id: String, @RequestParam("page") page: Int, authentication: Authentication): ResponseEntity<List<AdminReportDto>> {
        val user = userDetailsServiceImpl.getUserReferenceFromIdUnsafe(id) ?: return ResponseEntity(HttpStatus.BAD_REQUEST)

        return ResponseEntity(adminService.getUserReports(user, page), HttpStatus.OK)
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/admin/report/ticket")
    fun getTicketById(@RequestParam("id") id: String, authentication: Authentication): ResponseEntity<ReportTicketDto> {
        val reportTicket = adminService.getReportTicketReferenceByIdStringUnsafe(id) ?: return ResponseEntity(HttpStatus.BAD_REQUEST)
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)

        return ResponseEntity(adminService.getTicketById(user.id!!, reportTicket), HttpStatus.OK)
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/admin/post/likes")
    fun getPostLikes(@RequestParam("page") page: Int, @RequestParam("id") postId: String, authentication: Authentication): ResponseEntity<List<UserDto>> {
        val post = postService.getPostReferenceFromIdUnsafe(postId) ?: return ResponseEntity(HttpStatus.BAD_REQUEST)

        return ResponseEntity(adminService.getLikedByPost(post, page), HttpStatus.OK)
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/admin/comment/likes")
    fun getCommentLikes(@RequestParam("page") page: Int, @RequestParam("id") commentId: String, authentication: Authentication): ResponseEntity<List<UserDto>> {
        val comment = commentService.getCommentReferenceFromIdUnsafe(commentId) ?: return ResponseEntity(HttpStatus.BAD_REQUEST)

        return ResponseEntity(adminService.getLikedByComment(comment, page), HttpStatus.OK)
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/admin/statistics/users/count")
    fun getUserCountWithin(@RequestParam("days") days: Int): ResponseEntity<CountDto> {
        return ResponseEntity(CountDto(adminService.getUserCountWithin(days)), HttpStatus.OK)
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/admin/statistics/logins/count")
    fun getLoginsWithin(@RequestParam("days") days: Int): ResponseEntity<CountDto> {
        return ResponseEntity(CountDto(adminService.getLoginsWithin(days)), HttpStatus.OK)
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/admin/statistics/vote/possibilities")
    fun getAverageVotePossibilitiesToActualVotes(@RequestParam("days") days: Int): ResponseEntity<AverageVotePossibilitiesToActualVotesDto> {
        return ResponseEntity(adminService.getAverageVotePossibilitiesToActualVotes(days), HttpStatus.OK)
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/admin/statistics/post/count")
    fun getPostsCount(@RequestParam("days") days: Int): ResponseEntity<CountDto> {
        return ResponseEntity(CountDto(adminService.getPostsCount(days)), HttpStatus.OK)
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/admin/statistics/battle/count")
    fun getBattleCount(@RequestParam("days") days: Int): ResponseEntity<CountDto> {
        return ResponseEntity(CountDto(adminService.getBattleCount(days)), HttpStatus.OK)
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/admin/statistics/queue/count")
    fun getQueue(@RequestParam("days") days: Int): ResponseEntity<CountDto> {
        return ResponseEntity(CountDto(adminService.getQueueCount(days)), HttpStatus.OK)
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/admin/url/{url}")
    fun getUrl(@PathVariable(value="url") @Min(Constants.MIN_URL_LENGTH.toLong()) @Max(Constants.URL_LENGTH.toLong()) url: String): ResponseEntity<AdminUrlDto> {
        val urlDto = adminService.getUrl(url)
        return if (urlDto == null) {
            ResponseEntity(HttpStatus.NOT_FOUND)
        } else {
            ResponseEntity(urlDto, HttpStatus.OK)
        }
    }

    @Secured("ROLE_ADMIN")
    @DeleteMapping("/admin/url/{url}")
    fun deleteUrl(@PathVariable(value="url") @Min(Constants.MIN_URL_LENGTH.toLong()) @Max(Constants.URL_LENGTH.toLong()) url: String): ResponseEntity<String> {
        val result = adminService.deleteUrl(url)

        return if (result) ResponseEntity(HttpStatus.OK) else ResponseEntity(HttpStatus.NOT_FOUND)
    }

    @Secured("ROLE_ADMIN")
    @DeleteMapping("/admin/comment")
    fun deleteComment(@RequestParam("id") commentId: String): ResponseEntity<String> {
        val comment = commentService.getCommentReferenceFromIdUnsafe(commentId) ?: return ResponseEntity(HttpStatus.BAD_REQUEST)
        val result = adminService.deleteComment(comment)

        return if (result) ResponseEntity(HttpStatus.OK) else ResponseEntity(HttpStatus.BAD_REQUEST)
    }
}