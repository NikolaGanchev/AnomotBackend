package com.anomot.anomotbackend.controllers

import com.anomot.anomotbackend.dto.*
import com.anomot.anomotbackend.entities.Battle
import com.anomot.anomotbackend.entities.Post
import com.anomot.anomotbackend.security.CustomUserDetails
import com.anomot.anomotbackend.security.EmailVerified
import com.anomot.anomotbackend.services.*
import com.anomot.anomotbackend.utils.AppealObjective
import com.anomot.anomotbackend.utils.AppealReason
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import jakarta.validation.Valid

@RestController
@RequestMapping
class BattleController @Autowired constructor(
        private val postService: PostService,
        private val userDetailsServiceImpl: UserDetailsServiceImpl,
        private val battleService: BattleService,
        private val userModerationService: UserModerationService
) {

    private fun getSelfPost(post: Post, battle: Battle): Post? {
        return if (battle.goldPost == post) battle.goldPost
        else battle.redPost
    }


    private fun getEnemyPost(post: Post, battle: Battle): Post? {
        return if (battle.goldPost == post) battle.redPost
        else battle.goldPost
    }

    // returns either a battle dto if battle is successfully found, list of posts too similar if available or null
    @PostMapping("/account/battle/text")
    @EmailVerified
    fun uploadTextBattle(@RequestBody @Valid textPostDto: TextPostDto, authentication: Authentication): ResponseEntity<Any> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)
        val postCreateStatus = postService.createTextPost(textPostDto.text, user, true)

        if (postCreateStatus == PostCreateStatus.SIMILAR_FOUND || postCreateStatus.post == null) {
            return ResponseEntity(postCreateStatus.similar?.map {
                return@map PostDto(it.type,
                        it.text,
                        null,
                        userDetailsServiceImpl.getAsDto(it.poster),
                        null,
                        null,
                        it.creationDate,
                        it.id.toString())
            }, HttpStatus.CONFLICT)
        }

        val battle = battleService.queuePostForBattle(postCreateStatus.post!!) ?: return ResponseEntity(HttpStatus.CREATED)

        val selfPost = getSelfPost(postCreateStatus.post!!, battle)
        val enemyPost = getEnemyPost(postCreateStatus.post!!, battle)

        if (selfPost == null || enemyPost == null)  {
            // TODO
            // Handle post rejection because of deleted user if ever happens
            return ResponseEntity(HttpStatus.CONFLICT)
        }

        return ResponseEntity(SelfBattleDto(
                PostDto(selfPost.type,
                        selfPost.text,
                        null,
                        userDetailsServiceImpl.getAsDto(selfPost.poster),
                        0,
                        false,
                        selfPost.creationDate,
                        selfPost.id.toString()),
                PostDto(enemyPost.type,
                        enemyPost.text,
                        null,
                        userDetailsServiceImpl.getAsDto(enemyPost.poster),
                        0,
                        false,
                        enemyPost.creationDate,
                        enemyPost.id.toString()),
                0,
                0,
                false,
                battle.finishDate!!, battle.id.toString()), HttpStatus.CREATED)
    }

    // returns either a battle dto if battle is successfully found, list of posts too similar if available or null
    @PostMapping("/account/battle/media")
    @EmailVerified
    fun uploadMediaBattle(@RequestParam("file") file: MultipartFile, authentication: Authentication): ResponseEntity<Any> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)

        val response = postService.createMediaPost(file, user, true)

        if (response == PostCreateStatus.MEDIA_UNSUPPORTED) return ResponseEntity(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
        else if (response == PostCreateStatus.NSFW_FOUND) return ResponseEntity(
                NsfwFoundDto(
                        userModerationService.generateAppealJwt(user,
                                response.media!!.media!!.name.toString(),
                                AppealReason.NSFW_FOUND,
                                AppealObjective.BATTLE),
                        MediaDto(response.media!!.media!!.mediaType,
                                response.media!!.media!!.name.toString())),
                HttpStatus.NOT_ACCEPTABLE)
        else if (response == PostCreateStatus.SIMILAR_FOUND) return ResponseEntity(
                SimilarMediaFoundDto(
                        userModerationService.generateAppealJwt(user,
                            response.media!!.media!!.name.toString(),
                            AppealReason.SIMILAR_FOUND,
                            AppealObjective.BATTLE),
                        MediaDto(response.media!!.media!!.mediaType, response.media!!.media!!.name.toString()),
                        response.similar?.map {
                            return@map PostDto(it.type,
                                    null,
                                    MediaDto(it.media!!.mediaType, it.media!!.name.toString()),
                                    userDetailsServiceImpl.getAsDto(it.poster),
                                    null,
                                    null,
                                    it.creationDate,
                                    it.id.toString())
        }), HttpStatus.CONFLICT)

        val post = response.post ?: return ResponseEntity(HttpStatus.BAD_REQUEST)

        val battle = battleService.queuePostForBattle(post) ?: return ResponseEntity(HttpStatus.CREATED)

        val selfPost = getSelfPost(post, battle)
        val enemyPost = getEnemyPost(post, battle)

        if (selfPost == null || enemyPost == null) {
            // TODO
            // Handle post rejection because of deleted user if ever happens
            // That shouldn't be possible as deleting a user also deletes their posts
            return ResponseEntity(HttpStatus.CONFLICT)
        }

        return ResponseEntity(SelfBattleDto(
                PostDto(selfPost.type,
                        null,
                        MediaDto(selfPost.media!!.mediaType, selfPost.media!!.name.toString()),
                        userDetailsServiceImpl.getAsDto(selfPost.poster),
                        0,
                        false,
                        selfPost.creationDate,
                        selfPost.id.toString()),
                PostDto(enemyPost.type,
                        null,
                        MediaDto(enemyPost.media!!.mediaType, enemyPost.media!!.name.toString()),
                        userDetailsServiceImpl.getAsDto(enemyPost.poster),
                        0,
                        false,
                        enemyPost.creationDate,
                        enemyPost.id.toString()),
                0,
                0,
                false,
                battle.finishDate!!, battle.id.toString()), HttpStatus.CREATED)
    }

    @GetMapping("/battle")
    @EmailVerified
    fun getBattle(@RequestParam("page") page: Int, authentication: Authentication): ResponseEntity<BattleDto?> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)
        return ResponseEntity(battleService.getBattle(user, page), HttpStatus.OK)
    }

    @GetMapping("/account/battles")
    fun getSelfBattles(@RequestParam("page") page: Int, authentication: Authentication): ResponseEntity<List<SelfBattleDto>> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)
        return ResponseEntity(battleService.getBattles(user, page), HttpStatus.OK)
    }

    @GetMapping("/account/battle")
    fun getBattleById(@RequestParam("id") battleId: String, authentication: Authentication): ResponseEntity<Any> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)
        val result = battleService.getBattleById(user, battleId)
        return if (result != null) {
            ResponseEntity(result, HttpStatus.OK)
        } else ResponseEntity(HttpStatus.BAD_REQUEST)
    }

    @GetMapping("/account/battles/queue")
    fun getSelfBattlesQueue(@RequestParam("page") page: Int, authentication: Authentication): ResponseEntity<List<PostDto>> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)
        return ResponseEntity(battleService.getPostsInQueue(user, page), HttpStatus.OK)
    }

    @PostMapping("/battle/report")
    @EmailVerified
    fun reportBattle(@RequestBody @Valid battleReportDto: BattleReportDto, authentication: Authentication): ResponseEntity<String> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)

        val result = battleService.report(battleReportDto, user)

        return ResponseEntity(if (result) HttpStatus.CREATED else HttpStatus.BAD_REQUEST)
    }

    @GetMapping("/battle/report")
    fun getBattleReport(@RequestParam("postId") postId: String,
                        @RequestParam("battleId") battleId: String,
                        authentication: Authentication): ResponseEntity<ReportDto> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)

        val result = battleService.getReport(user, postId, battleId)

        return if (result != null) {
            ResponseEntity(result, HttpStatus.OK)
        } else {
            ResponseEntity(HttpStatus.BAD_REQUEST)
        }
    }
}