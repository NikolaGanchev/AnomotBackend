package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.dto.BattleIntermediate
import com.anomot.anomotbackend.entities.Battle
import com.anomot.anomotbackend.entities.User
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface BattleRepository: JpaRepository<Battle, Long> {

    @Query("update battle set finished = true where (finish_date < now() and finished = false) " + // select if battle finished normally
            // end early if one of the posts is deleted
            "or (finished = false and (not exists(select * from post where post.id = battle.red_post_id or post.id = battle.gold_post_id)))" +
            "returning *",
            nativeQuery = true)
    @Modifying
    fun getUnprocessedFinishedBattlesAndUpdate(): List<Battle>

    @Query("select new com.anomot.anomotbackend.dto.BattleIntermediate(b, " +
            "(select count (v1) from Vote v1 where v1.battle = b and v1.post.poster = ?1)," +
            "(select count (v2) from Vote v2 where v2.battle = b and v2.post.poster <> ?1)) " +
            "from Battle b where b.goldPost.poster = ?1 or b.redPost.poster = ?1")
    fun getAllBattlesByUser(user: User, pageable: Pageable): List<BattleIntermediate>
}