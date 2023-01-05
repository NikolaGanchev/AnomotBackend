package com.anomot.anomotbackend.services

import com.anomot.anomotbackend.entities.User
import org.springframework.stereotype.Service
import kotlin.math.pow
import kotlin.math.roundToInt

data class UserDifference(
        val goldUserProbability: Double,
        val redUserProbability: Double,
)

@Service
class EloService {
    companion object {
        private fun k(elo: Int): Int {
            return when(elo) {
                in Int.MIN_VALUE .. 2100 -> 32
                in 2100 .. 2400 -> 24
                else -> 16
            }
        }

        const val exponentDenominator = 400
        const val exponentBase = 10
        const val winScore = 1.0
        const val loseScore = 0.0
        const val drawScore = 0.5
    }

    private fun getExpectedProbability(ratingDifference: Int): Double {
        val exponent = ratingDifference.toDouble() / exponentDenominator.toDouble()

        return 1 / (1 + exponentBase.toDouble().pow(exponent))
    }

    fun getUserProbability(goldUser: User, redUser: User): UserDifference {
        val goldUserDifference = redUser.elo - goldUser.elo
        val redUserDifference = goldUser.elo - redUser.elo

        val goldUserProbability = getExpectedProbability(goldUserDifference)
        val redUserProbability = getExpectedProbability(redUserDifference)

        return UserDifference(goldUserProbability, redUserProbability)
    }

    fun getNextRating(elo: Int, actualPoints: Double, expectedPoints: Double): Int {
        val change = (k(elo) * (actualPoints - expectedPoints)).roundToInt()

        return elo + change
    }
}