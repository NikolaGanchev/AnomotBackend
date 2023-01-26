package com.anomot.anomotbackend.controllers

import com.anomot.anomotbackend.dto.*
import com.anomot.anomotbackend.security.CustomUserDetails
import com.anomot.anomotbackend.services.AdminService
import com.anomot.anomotbackend.services.UserDetailsServiceImpl
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.annotation.Secured
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import javax.validation.Valid
import javax.validation.constraints.Max
import javax.validation.constraints.Min

@RestController
class AdminController(
        private val adminService: AdminService,
        private val userDetailsServiceImpl: UserDetailsServiceImpl
) {
    @Secured("ROLE_ADMIN")
    @GetMapping("/admin/tickets")
    fun getReports(@RequestParam("page") page: Int): ResponseEntity<List<ReportTicketDto>> {
        return ResponseEntity(adminService.getReports(page), HttpStatus.OK)
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/admin/ticket/undecided")
    fun getUndecidedReports(@RequestParam("page") page: Int): ResponseEntity<List<ReportTicketDto>> {
        return ResponseEntity(adminService.getUndecidedReports(page), HttpStatus.OK)
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

    // The media needs to not be inside a post or battle
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
    fun deleteUser(@RequestParam("id") id: String): ResponseEntity<String> {
        val user = userDetailsServiceImpl.getUserReferenceFromIdUnsafe(id) ?: return ResponseEntity(HttpStatus.BAD_REQUEST)
        adminService.deleteUser(user)
        return ResponseEntity(HttpStatus.OK)
    }
}