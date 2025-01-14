/*
 * Copyright 2016 John Grosh (jagrosh).
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
package fr.paladium.palabot.commands.moderation;

import java.util.LinkedList;
import com.jagrosh.jdautilities.command.CommandEvent;
import fr.paladium.palabot.Vortex;
import fr.paladium.palabot.commands.ModCommand;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import fr.paladium.palabot.utils.ArgsUtil;
import fr.paladium.palabot.utils.FormatUtil;
import fr.paladium.palabot.utils.LogUtil;
import java.util.List;
import net.dv8tion.jda.api.entities.Role;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class KickCmd extends ModCommand
{
    public KickCmd(Vortex vortex)
    {
        super(vortex, Permission.KICK_MEMBERS);
        this.name = "kick";
        this.arguments = "<@users> [reason]";
        this.help = "kicks users";
        this.botPermissions = new Permission[]{Permission.KICK_MEMBERS};
    }

    @Override
    protected void execute(CommandEvent event)
    {
        ArgsUtil.ResolvedArgs args = ArgsUtil.resolve(event.getArgs(), event.getGuild());
        if(args.isEmpty())
        {
            event.replyError("Please include at least one user to kick (@mention or ID)!");
            return;
        }
        String reason = LogUtil.auditReasonFormat(event.getMember(), args.reason);
        Role modrole = vortex.getDatabase().settings.getSettings(event.getGuild()).getModeratorRole(event.getGuild());
        StringBuilder builder = new StringBuilder();
        List<Member> toKick = new LinkedList<>();
        
        args.members.forEach(m -> 
        {
            if(!event.getMember().canInteract(m))
                builder.append("\n").append(event.getClient().getError()).append(" You do not have permission to kick ").append(FormatUtil.formatUser(m.getUser()));
            else if(!event.getSelfMember().canInteract(m))
                builder.append("\n").append(event.getClient().getError()).append(" I am unable to kick ").append(FormatUtil.formatUser(m.getUser()));
            else if(modrole!=null && m.getRoles().contains(modrole))
                builder.append("\n").append(event.getClient().getError()).append(" I won't kick ").append(FormatUtil.formatUser(m.getUser())).append(" because they have the Moderator Role");
            else
                toKick.add(m);
        });
        
        args.unresolved.forEach(un -> builder.append("\n").append(event.getClient().getWarning()).append(" Could not resolve `").append(un).append("` to a member"));
        
        args.users.forEach(u -> builder.append("\n").append(event.getClient().getWarning()).append("The user ").append(FormatUtil.formatUser(u)).append(" is not in this server."));
        
        args.ids.forEach(id -> builder.append("\n").append(event.getClient().getWarning()).append("The user <@").append(id).append("> is not in this server."));
        
        if(toKick.isEmpty())
        {
            event.reply(builder.toString());
            return;
        }
        
        if(toKick.size() > 5)
            event.reactSuccess();
        
        for(int i=0; i<toKick.size(); i++)
        {
            Member m = toKick.get(i);
            boolean last = i+1 == toKick.size();
            event.getGuild().kick(m, reason).queue(success -> 
            {
                builder.append("\n").append(event.getClient().getSuccess()).append(" Successfully kicked ").append(FormatUtil.formatUser(m.getUser()));
                if(last)
                    event.reply(builder.toString());
            }, failure -> 
            {
                builder.append("\n").append(event.getClient().getError()).append(" Failed to kick ").append(FormatUtil.formatUser(m.getUser()));
                if(last)
                    event.reply(builder.toString());
            });
        }
    }
}
