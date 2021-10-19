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

import com.jagrosh.easysql.DataManager;
import com.jagrosh.easysql.DatabaseConnector;
import com.jagrosh.easysql.SQLColumn;
import com.jagrosh.easysql.columns.InstantColumn;
import com.jagrosh.easysql.columns.LongColumn;
import dev.morphia.query.experimental.filters.Filters;
import dev.morphia.query.experimental.updates.UpdateOperators;
import dev.morphia.query.internal.MorphiaCursor;
import fr.paladium.palabot.database.DatabaseMongo;
import fr.paladium.palabot.database.dao.Ban;
import fr.paladium.palabot.database.dao.Mute;
import fr.paladium.palabot.utils.MultiBotManager;
import fr.paladium.palabot.utils.Pair;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import org.json.JSONObject;

public class TempBanManager
{

    public DatabaseMongo connector;

    public TempBanManager(DatabaseMongo connector)
    {
        this.connector = connector;
    }

    
    public JSONObject getAllBansJson(Guild guild)
    {
        MorphiaCursor<Ban> cursor = connector.getDatastore().find(Ban.class).filter(Filters.eq("guildID", guild.getId())).iterator();

        JSONObject json = new JSONObject();
        cursor.toList().forEach(p -> json.put(String.valueOf(p.getUserID()), p.getEnd()));
        return json;
    }

    public long countBan(Guild guild, long userId)
    {
        try {
            return this.connector.getDatastore().createQuery(Ban.class).filter(Filters.and(Filters.eq("guildID", guild.getId()), Filters.eq("userID", userId))).count();
        }catch (Throwable e)
        {
            e.printStackTrace();
        }
        return 0;
    }
    
    public void setBan(Guild guild, long userId, Instant finish, String reason, boolean perm)
    {
        Ban cursor = connector.getDatastore().find(Ban.class).filter(Filters.and(Filters.eq("guildID", guild.getId()),Filters.eq("ended", false))).first();
        if (cursor != null) {
            this.connector.getDatastore().find(Ban.class).filter(Filters.eq("_id", cursor.getId())).update(UpdateOperators.set("end", finish.getEpochSecond())).execute();

        }else {
            Ban ban = Ban.builder().guildID(guild.getId()).userID(userId).end(finish.getEpochSecond()).reason(reason).proof(null).ended(perm).build();
            this.connector.getDatastore().save(ban);
        }

    }
    
    public void clearBan(Guild guild, long userId)
    {
        for (Ban next : this.connector.getDatastore().find(Ban.class).filter(Filters.and(Filters.eq("guildID", guild.getId()), Filters.eq("userID", userId),Filters.lt("end", Instant.now().getEpochSecond()), Filters.eq("ended", false)))) {
            this.connector.getDatastore().find(Ban.class).filter(Filters.eq("_id", next.getId())).update(UpdateOperators.set("ended", true)).execute();
        }

    }
    
    public int timeUntilUnban(Guild guild, long userId)
    {
        try {
            MorphiaCursor<Ban> mute = this.connector.getDatastore().find(Ban.class).filter(Filters.and(Filters.eq("guildID", guild.getId()), Filters.eq("userID", userId),Filters.gt("end", Instant.now().getEpochSecond()))).iterator();
            while(mute.hasNext())
            {
                Ban next = mute.next();

                return (int)(Instant.now().until(Instant.ofEpochMilli(next.getEnd()), ChronoUnit.MINUTES));
            }
        }
        catch (Throwable e)
        {
            e.printStackTrace();
        }
        return 0;
    }
    
    public void checkUnbans(MultiBotManager shards)
    {
        for (Ban next : this.connector.getDatastore().find(Ban.class).filter(Filters.and(Filters.lt("end", Instant.now().getEpochSecond()), Filters.eq("ended", false)))) {
            Guild g = shards.getGuildById(Long.parseLong(next.getGuildID()));
            if (g == null || g.getMemberCache().isEmpty() || !g.getSelfMember().hasPermission(Permission.BAN_MEMBERS))
                continue;
            g.unban(Long.toString(next.getUserID())).reason("Temporary Ban Completed").queue(s -> {
            }, f -> {
            });
            this.connector.getDatastore().find(Ban.class).filter(Filters.eq("_id", next.getId())).update(UpdateOperators.set("ended", true)).execute();
        }
    }
}
