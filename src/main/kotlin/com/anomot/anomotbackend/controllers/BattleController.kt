package com.anomot.anomotbackend.controllers

import com.anomot.anomotbackend.dto.BattleDto
import com.anomot.anomotbackend.dto.BattlePostDto
import com.anomot.anomotbackend.dto.MediaDto
import com.anomot.anomotbackend.dto.TextPostDto
import com.anomot.anomotbackend.entities.Battle
import com.anomot.anomotbackend.entities.Post
import com.anomot.anomotbackend.security.CustomUserDetails
import com.anomot.anomotbackend.services.BattleService
import com.anomot.anomotbackend.services.MediaService
import com.anomot.anomotbackend.services.PostService
import com.anomot.anomotbackend.services.UserDetailsServiceImpl
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/account")
class BattleController @Autowired constructor(
        private val postService: PostService,
        private val mediaService: MediaService,
        private val userDetailsServiceImpl: UserDetailsServiceImpl,
        private val battleService: BattleService
) {

    private fun getSelfPost(post: Post, battle: Battle): Post? {
        return if (battle.goldPost == post) battle.goldPost
        else battle.redPost
    }

    private fun getEnemyPost(post: Post, battle: Battle): Post? {
        return if (battle.goldPost == post) battle.redPost
        else battle.goldPost
    }

    @PostMapping("/battle/text")
    fun uploadTextBattle(@RequestBody textPostDto: TextPostDto, authentication: Authentication): ResponseEntity<BattleDto> {
        val post = postService.addTextPost(textPostDto.text, (authentication.principal) as CustomUserDetails)

        val battle = battleService.queuePostForBattle(post) ?: return ResponseEntity(HttpStatus.CREATED)

        val selfPost = getSelfPost(post, battle)
        val enemyPost = getEnemyPost(post, battle)

        if (selfPost == null || enemyPost == null) return ResponseEntity(HttpStatus.BAD_REQUEST)

        return ResponseEntity(BattleDto(
                BattlePostDto(selfPost.type, selfPost.text, null),
                BattlePostDto(enemyPost.type, enemyPost.text, null),
                battle.finishDate!!), HttpStatus.CREATED)
    }

    @PostMapping("/battle/media")
    fun uploadMediaBattle(@RequestParam("file") file: MultipartFile, authentication: Authentication): ResponseEntity<BattleDto> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)
        val media = mediaService.uploadMedia(file, false, true, user)

        if (media?.media == null) {
            return ResponseEntity(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
        }

        if (!mediaService.inNsfwRequirements(media.avgNsfwScan!!)) {
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }

        val post = postService.addMediaPost(media.media, (authentication.principal) as CustomUserDetails)

        val battle = battleService.queuePostForBattle(post) ?: return ResponseEntity(HttpStatus.CREATED)

        val selfPost = getSelfPost(post, battle)
        val enemyPost = getEnemyPost(post, battle)

        if (selfPost == null || enemyPost == null) return ResponseEntity(HttpStatus.BAD_REQUEST)

        return ResponseEntity(BattleDto(
                BattlePostDto(selfPost.type, null, MediaDto(selfPost.media!!.mediaType, selfPost.media!!.name.toString())),
                BattlePostDto(enemyPost.type,null, MediaDto(enemyPost.media!!.mediaType, enemyPost.media!!.name.toString())),
                battle.finishDate!!), HttpStatus.CREATED)
    }
}