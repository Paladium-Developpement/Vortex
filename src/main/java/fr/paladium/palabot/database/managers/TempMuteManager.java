/*
 * Copyright 2018 John Grosh (john.a.grosh@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.paladium.palabot.database.managers;

import com.mongodb.CursorType;
import dev.morphia.query.FindOptions;
import dev.morphia.query.experimental.updates.UpdateOperators;
import fr.paladium.palabot.database.DatabaseMongo;
import fr.paladium.palabot.database.dao.Mute;
import fr.paladium.palabot.utils.MultiBotManager;

import java.time.Instant;
import java.time.temporal.ChronoUnit;


import com.mongodb.reactivestreams.client.MongoCollection;
import dev.morphia.query.experimental.filters.Filters;
import dev.morphia.query.internal.MorphiaCursor;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import org.json.JSONObject;


public class TempMuteManager
{

    public DatabaseMongo connector;
    
    public TempMuteManager(DatabaseMongo connector)
    {
        this.connector = connector;

    }

    
    public JSONObject getAllMutesJson(Guild guild)
    {
        MorphiaCursor<Mute> cursor = connector.getDatastore().find(Mute.class).filter(Filters.eq("guildID", guild.getId())).iterator();

        JSONObject json = new JSONObject();
        cursor.toList().forEach(p -> json.put(String.valueOf(p.getUserID()), p.getEnd()));
        return json;
    }

    public long countMute(Guild guild, long userId)
    {
        try {
            return this.connector.getDatastore().createQuery(Mute.class).filter(Filters.and(Filters.eq("guildID", guild.getId()), Filters.eq("userID", userId))).count();
        }catch (Throwable e)
        {
            e.printStackTrace();
        }
        return 0;
    }


    public boolean isMuted(Member member)
    {
        long count = connector.getDatastore().createQuery(Mute.class).filter(Filters.and(Filters.eq("guildID", member.getGuild().getId()), Filters.eq("userID", member.getUser().getId()),Filters.gt("end", Instant.now().getEpochSecond()))).count();
        return count > 0;
    }
    
    public void setMute(Guild guild, long userId, Instant finish, String reason)
    {
        Mute mute = Mute.builder().guildID(guild.getId()).userID(userId).end(finish.getEpochSecond()).reason(reason).proof(null).build();
        this.connector.getDatastore().save(mute);
    }

    public void setMute(Guild guild, long userId, Instant finish)
    {
        Mute mute = Mute.builder().guildID(guild.getId()).userID(userId).end(finish.getEpochSecond()).reason(null).proof(null).build();
        this.connector.getDatastore().save(mute);
    }

    public void setMute(Guild guild, long userId, Instant finish, String reason, String proof)
    {
        Mute mute = Mute.builder().guildID(guild.getId()).userID(userId).end(finish.getEpochSecond()).reason(reason).proof(proof).build();
        this.connector.getDatastore().save(mute);
    }
    
    
    public void removeMute(Guild guild, long userId)
    {

    }
    
    public int timeUntilUnmute(Guild guild, long userId)
    {
        try {
            MorphiaCursor<Mute> mute = this.connector.getDatastore().find(Mute.class).filter(Filters.and(Filters.eq("guildID", guild.getId()), Filters.eq("userID", userId),Filters.gt("end", Instant.now().getEpochSecond()))).iterator();
        while(mute.hasNext())
        {
            Mute next = mute.next();

            return (int)(Instant.now().until(Instant.ofEpochMilli(next.getEnd()), ChronoUnit.MINUTES));
        }
        }
        catch (Throwable e)
        {
            e.printStackTrace();
        }
        return 0;
    }
    
    public void checkUnmutes(MultiBotManager shards, GuildSettingsDataManager data)
    {
        for (Mute next : this.connector.getDatastore().find(Mute.class).filter(Filters.and(Filters.lt("end", Instant.now().getEpochSecond()), Filters.eq("ended", false)))) {
            Guild g = shards.getGuildById(Long.parseLong(next.getGuildID()));
            if (g == null || g.getMemberCache().isEmpty() || !g.getSelfMember().hasPermission(Permission.MANAGE_ROLES))
                continue;
            Role mRole = data.getSettings(g).getMutedRole(g);
            if (mRole == null || !g.getSelfMember().canInteract(mRole)) {
                this.connector.getDatastore().find(Mute.class).filter(Filters.eq("_id", next.getId())).update(UpdateOperators.set("ended", true)).execute();
                continue;
            }
            Member m = g.getMemberById(next.getUserID());
            if (m != null && m.getRoles().contains(mRole))
                g.removeRoleFromMember(m, mRole).reason("Temporary Mute Completed").queue();
            this.connector.getDatastore().find(Mute.class).filter(Filters.eq("_id", next.getId())).update(UpdateOperators.set("ended", true)).execute();
        }
    }
}
