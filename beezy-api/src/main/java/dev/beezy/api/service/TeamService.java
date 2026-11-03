package dev.beezy.api.service;

import dev.beezy.api.SendResult;
import dev.beezy.api.display.TeamMember;
import dev.beezy.stability.NonExtendable;
import dev.beezy.stability.Stable;
import java.util.Collection;
import java.util.List;

/** The user's team mates on the minimap: each set replaces the whole team (at most 32). */
@Stable
@NonExtendable
public interface TeamService {

    /**
     * @param members 0-32 mates
     * @return the outcome
     */
    SendResult set(Collection<TeamMember> members);

    /** @return the outcome */
    SendResult clear();

    /** @return the team last sent, empty when cleared */
    List<TeamMember> current();
}
