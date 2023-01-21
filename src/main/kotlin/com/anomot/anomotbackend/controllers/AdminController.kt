package com.anomot.anomotbackend.controllers

import com.anomot.anomotbackend.dto.AdminReportDto
import com.anomot.anomotbackend.dto.DecisionDto
import com.anomot.anomotbackend.security.CustomUserDetails
import com.anomot.anomotbackend.services.AdminService
import com.anomot.anomotbackend.services.UserDetailsServiceImpl
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.annotation.Secured
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@RestController
class AdminController(
        private val adminService: AdminService,
        private val userDetailsServiceImpl: UserDetailsServiceImpl
) {
    @Secured("ROLE_ADMIN")
    @GetMapping("/admin/reports")
    fun getReports(@RequestParam("page") page: Int): ResponseEntity<List<AdminReportDto>> {
        return ResponseEntity(adminService.getReports(page), HttpStatus.OK)
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/admin/reports/undecided")
    fun getUndecidedReports(@RequestParam("page") page: Int): ResponseEntity<List<AdminReportDto>> {
        return ResponseEntity(adminService.getUndecidedReports(page), HttpStatus.OK)
    }

    @Secured("ROLE_ADMIN")
    @PostMapping("/admin/report/decide")
    fun decideReport(@RequestBody @Valid decisionDto: DecisionDto, authentication: Authentication): ResponseEntity<String> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)
        val result = adminService.decideReport(user, decisionDto.reportId, decisionDto.decision)
        return ResponseEntity(if (result) HttpStatus.OK else HttpStatus.BAD_REQUEST)
    }
}