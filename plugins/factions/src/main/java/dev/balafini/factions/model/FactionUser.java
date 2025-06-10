package dev.balafini.factions.model;

import java.time.Instant;
import java.util.UUID;

public record FactionUser(
        UUID playerId,
        UUID factionId,
        FactionRole role,
        Instant joinedAt
) {

    public enum FactionRole {
        LEADER("Lider", "#", 1),
        OFFICER("Oficial", "*", 2),
        MEMBER("Membro", "+", 3),
        RECRUIT("Recruta", "-", 4);

        private final String name;
        private final String prefix;
        private final int level;

        FactionRole(String name, String prefix, int level) {
            this.name = name;
            this.prefix = prefix;
            this.level = level;
        }

        public String getName() {
            return name;
        }
        public String getPrefix() {
            return prefix;
        }
        public int getLevel() {
            return level;
        }

        public boolean canManage(FactionRole target) {
            return this.level < target.level;
        }
    }
}
