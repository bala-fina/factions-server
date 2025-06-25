package dev.balafini.factions.faction.util;

import dev.balafini.factions.faction.exception.FactionAlreadyExistsException;
import dev.balafini.factions.faction.exception.InvalidFactionParametersException;
import dev.balafini.factions.faction.exception.PlayerAlreadyInFactionException;
import dev.balafini.factions.faction.repository.FactionMemberRepository;
import dev.balafini.factions.faction.repository.FactionRepository;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class FactionValidator {

    private final FactionRepository factionRepository;
    private final FactionMemberRepository factionMemberRepository;

    public static final String FACTION_NAME_PATTERN = "^[a-zA-Z0-9]{6,16}$";
    public static final String FACTION_TAG_PATTERN = "^[a-zA-Z0-9]{3,4}$";

    public FactionValidator(FactionRepository factionRepository, FactionMemberRepository factionMemberRepository) {
        this.factionRepository = factionRepository;
        this.factionMemberRepository = factionMemberRepository;
    }

    public static void validateFactionParameters(String name, String tag) {
        if (!name.matches(FACTION_NAME_PATTERN)) {
            throw new InvalidFactionParametersException("O nome da facção deve ter entre 6 e 16 caracteres alfanuméricos.");
        }
        if (!tag.matches(FACTION_TAG_PATTERN)) {
            throw new InvalidFactionParametersException("A tag da facção deve ter entre 3 e 4 caracteres alfanuméricos.");
        }
    }

    public CompletionStage<Void> validateFactionCreation(String name, String tag, UUID leaderId) {
        CompletionStage<Boolean> nameCheck = factionRepository.existsByName(name);
        CompletionStage<Boolean> tagCheck = factionRepository.existsByTag(tag);
        CompletionStage<Boolean> playerCheck = factionMemberRepository.findByPlayerId(leaderId).thenApply(Optional::isPresent);

        return CompletableFuture.allOf(nameCheck.toCompletableFuture(), tagCheck.toCompletableFuture(), playerCheck.toCompletableFuture())
                .thenRun(() -> {
                    if (nameCheck.toCompletableFuture().join()) {
                        throw new FactionAlreadyExistsException("O nome da facção já está em uso.");
                    }
                    if (tagCheck.toCompletableFuture().join()) {
                        throw new FactionAlreadyExistsException("A tag da facção já está em uso.");
                    }
                    if (playerCheck.toCompletableFuture().join()) {
                        throw new PlayerAlreadyInFactionException("O jogador já está em uma facção.");
                    }
                });
    }
}
