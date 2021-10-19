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
package fr.paladium.palabot.commands.moderation;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.utils.FinderUtil;
import fr.paladium.palabot.Vortex;
import fr.paladium.palabot.commands.ModCommand;
import fr.paladium.palabot.utils.FormatUtil;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild.Ban;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class CheckCmd extends ModCommand
{
    private final static String LINESTART = "\u25AB"; // ▫
    private final static String GENERAL_EMOJI = "\uD83D\uDCDA • ";
    private final static String SANCTION_EMOJI = "\uD83D\uDEE1 •️ ";
    private final static String STATUS_EMOJI = "\uD83D\uDCE1 •️ ";

    public CheckCmd(Vortex vortex)
    {
        super(vortex, Permission.BAN_MEMBERS);
        this.name = "lookup";
        this.arguments = "<user>";
        this.help = "lookup a user";
        this.botPermissions = new Permission[]{Permission.BAN_MEMBERS};
    }
    
    @Override
    protected void execute(CommandEvent event)
    {
        if(event.getArgs().isEmpty() || event.getArgs().equalsIgnoreCase("help"))
        {
            event.replySuccess("This command is used to see a user's strikes and mute/ban status for the current server. Please include a user or user ID to check.");
            return;
        }
        event.getChannel().sendTyping().queue();
        List<Member> members = FinderUtil.findMembers(event.getArgs(), event.getGuild());
        if(!members.isEmpty())
        {
            check(event, members.get(0).getUser());
            return;
        }
        List<User> users = FinderUtil.findUsers(event.getArgs(), event.getJDA());
        if(!users.isEmpty())
        {
            check(event, users.get(0));
            return;
        }
        try
        {
            Long uid = Long.parseLong(event.getArgs());
            User u = vortex.getShardManager().getUserById(uid);
            if(u!=null)
                check(event, u);
            else
                event.getJDA().retrieveUserById(uid).queue(
                        user -> check(event, user), 
                        v -> event.replyError("`"+uid+"` is not a valid user ID"));
        }
        catch(Exception ex)
        {
            event.replyError(FormatUtil.filterEveryone("Could not find a user `"+event.getArgs()+"`"));
        }
    }
    
    private void check(CommandEvent event, User user)
    {
        if(event.getGuild().isMember(user))
            check(event, user, null);
        else
            event.getGuild().retrieveBan(user).queue(ban -> check(event, user, ban), t -> check(event, user, null));
    }
    
    private void check(CommandEvent event, User user, Ban ban)
    {

        EmbedBuilder eb = new EmbedBuilder();
        Member member = event.getGuild().getMember(user);
        eb.setAuthor(FormatUtil.formatFullUser(user),null, user.getAvatarUrl());
        //eb.setTitle(vortex.getTraduction().getString("moderation.check.information.title")+" "++":\n");
        eb.addField(GENERAL_EMOJI + vortex.getTraduction().getString("moderation.check.information.general"), generateGeneral(event, user, ban), false);
        if (member != null)
            eb.addField(STATUS_EMOJI + vortex.getTraduction().getString("moderation.check.information.status"), generateStatus(event,user,ban), false);
        eb.addField(SANCTION_EMOJI + vortex.getTraduction().getString("moderation.check.information.sanction"), generateSanction(event,user,ban), false);

        eb.setFooter("ID : " + user.getIdLong());
        eb.setThumbnail(user.getEffectiveAvatarUrl());


        event.getMessage().delete().queue();
        event.getChannel().sendMessage(eb.build()).queue();

    }

    private String generateGeneral(CommandEvent event, User user, Ban ban)
    {
        Member member = event.getGuild().getMember(user);
        String name;
        if(member != null && member.getNickname()!=null)
            name = member.getNickname();
        else
            name = user.getName();
        StringBuilder str = new StringBuilder(LINESTART +vortex.getTraduction().getString("moderation.check.information.general.nickname") + " : `" + name +"`\n");
        str.append(LINESTART +vortex.getTraduction().getString("moderation.check.information.general.id") + " : `" + user.getId() +"`\n");

        String roles="";
        if (member != null) {
            roles = member.getRoles().stream().map((rol) -> "`, `" + rol.getName()).reduce(roles, String::concat);
            if (roles.isEmpty())
                roles = "None";
            else
                roles = roles.substring(3) + "`";
        }
        DateTimeFormatter pattern = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withZone(ZoneId.of("Europe/Paris"));
        str.append(LINESTART +vortex.getTraduction().getString("moderation.check.information.general.roles") + " : " + roles + "\n");
        str.append(LINESTART + vortex.getTraduction().getString("moderation.check.information.general.created") + " : `" + user.getTimeCreated().format(pattern) + "`\n");
        if (member != null)
            str.append(LINESTART + vortex.getTraduction().getString("moderation.check.information.general.joined") + " : `" + member.getTimeJoined().format(pattern) + "`\n");
        Role mRole = vortex.getDatabase().settings.getSettings(event.getGuild()).getMutedRole(event.getGuild());
        String muted = (event.getGuild().isMember(user)
                ? (event.getGuild().getMember(user).getRoles().contains(mRole) ? vortex.getTraduction().getString("yes") : vortex.getTraduction().getString("no"))
                : vortex.getTraduction().getString("notintheserver"));
        str.append(LINESTART +vortex.getTraduction().getString("moderation.check.information.general.muted") + " : `"+muted+"`\n" );
        String banned = (ban==null ? vortex.getTraduction().getString("no")
                : vortex.getTraduction().getString("yes")+" (" + ban.getReason() + ")");
        str.append(LINESTART + vortex.getTraduction().getString("moderation.check.information.general.banned") + " : `"+banned+"`\n" );

        return str.toString();
    }

    private String generateStatus(CommandEvent event, User user, Ban ban)
    {
        Member member = event.getGuild().getMember(user);
        assert member != null;
        StringBuilder str = new StringBuilder(LINESTART + vortex.getTraduction().getString("moderation.check.information.status.actual") + " : " + statusToEmote(member.getOnlineStatus()) + "\n");
            str.append(LINESTART + vortex.getTraduction().getString("moderation.check.information.status.activity") + " : " + formatGame(member.getActivities()) + "\n");
            return str.toString();
    }

    private String generateSanction(CommandEvent event, User user, Ban ban)
    {
        int strikes = vortex.getDatabase().strikes.getStrikes(event.getGuild(), user.getIdLong());
        long mutes = vortex.getDatabaseMongo().tempmutes.countMute(event.getGuild(), user.getIdLong());
        long bans = vortex.getDatabaseMongo().tempbans.countBan(event.getGuild(), user.getIdLong());
        StringBuilder str = new StringBuilder( LINESTART + vortex.getTraduction().getString("moderation.check.information.sanction.strikes") + " : `" + strikes +"`\n");
        str.append(LINESTART + vortex.getTraduction().getString("moderation.check.information.sanction.mutes") + " : `" + mutes +"`\n");
        str.append(LINESTART + vortex.getTraduction().getString("moderation.check.information.sanction.bans") + " : `" + bans +"`\n");
        return str.toString();
    }

    private static String statusToEmote(OnlineStatus status)
    {
        System.out.println(status);
        switch(status) {
            case ONLINE: return "<:online:835271418081050694>";
            case IDLE: return "<:away:835271417914064956>";
            case DO_NOT_DISTURB: return "<:dnd:835271417922715708>";
            case INVISIBLE: return "<:invisible:835271418010009600>";
            case OFFLINE: return "<:offline:835271417921929256>";
            default: return "";
        }
    }

    private static String formatGame(List<Activity> games)
    {
        System.out.println(games);
        StringBuilder full = new StringBuilder();
        for (Activity game : games) {
            String str;
            switch (game.getType()) {
                case STREAMING:
                    return "Streaming [*" + game.getName() + "*](" + game.getUrl() + ")";
                case LISTENING:
                    str = "Listening to";
                    break;
                case WATCHING:
                    str = "Watching";
                    break;
                case DEFAULT:
                default:
                    str = "Playing";
                    break;
            }
            full.append("`").append(str).append(" ").append(game.getName()).append("`");
        }
        return full.toString();
    }
}
