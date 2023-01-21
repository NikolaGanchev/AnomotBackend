package com.anomot.anomotbackend.utils

enum class ReportReason {
    NSFW_CONTENT,
    ADVERTISING,
    VIOLENCE,
    HARASSMENT,
    HATE_SPEECH,
    TERRORISM,
    SPAM,
    INAPPROPRIATE_USERNAME,
    UNDERAGE,
    ABUSE_OF_SERVICE,
    IDENTITY_REVEAL;

    companion object {
        fun from(postReportReason: PostReportReason): ReportReason {
            return ReportReason.valueOf(postReportReason.toString())
        }

        fun from(userReportReason: UserReportReason): ReportReason {
            return ReportReason.valueOf(userReportReason.toString())
        }

        fun from(battlePostReportReason: BattlePostReportReason): ReportReason {
            return ReportReason.valueOf(battlePostReportReason.toString())
        }

        fun from(commentReportReason: CommentReportReason): ReportReason {
            return ReportReason.valueOf(commentReportReason.toString())
        }
    }
}

enum class PostReportReason {
    NSFW_CONTENT,
    ADVERTISING,
    VIOLENCE,
    HARASSMENT,
    HATE_SPEECH,
    TERRORISM,
    SPAM;
}

enum class UserReportReason {
    ADVERTISING,
    INAPPROPRIATE_USERNAME,
    TERRORISM,
    UNDERAGE,
    ABUSE_OF_SERVICE;
}

enum class BattlePostReportReason {
    NSFW_CONTENT,
    ADVERTISING,
    VIOLENCE,
    HARASSMENT,
    HATE_SPEECH,
    TERRORISM,
    SPAM,
    IDENTITY_REVEAL;
}

enum class CommentReportReason {
    NSFW_CONTENT,
    ADVERTISING,
    VIOLENCE,
    HARASSMENT,
    HATE_SPEECH,
    TERRORISM,
    SPAM;
}
