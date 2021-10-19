package fr.paladium.palabot.database.managers;

import dev.morphia.query.experimental.filters.Filters;
import fr.paladium.palabot.database.DatabaseMongo;
import fr.paladium.palabot.database.dao.Groups;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class GroupsManager {

    public DatabaseMongo connector;

    public GroupsManager(DatabaseMongo connector)
    {
        this.connector = connector;
    }

    public int initGuild(Guild guild)
    {
        List<Role> listRoles = guild.getRoles();
        AtomicInteger roleAdded = new AtomicInteger();
        listRoles.forEach((role) -> {
            Groups first = connector.getDatastore().find(Groups.class).filter(Filters.and(Filters.eq("guildID", guild.getId()), Filters.eq("groupID", role.getIdLong()))).first();
            if (first == null)
            {
                roleAdded.getAndIncrement();
                connector.getDatastore().save(Groups.builder().guildID(guild.getId()).groupID(role.getIdLong()).permissions(new ArrayList<>()).build());
            }
        }
        );
        return roleAdded.get();
    }
}
