/*
 * Copyright 2018 John Grosh (jagrosh).
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
package fr.paladium.palabot;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import com.jagrosh.jdautilities.command.CommandClient;
import com.jagrosh.jdautilities.command.CommandClientBuilder;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.examples.command.PingCommand;
import com.jagrosh.jdautilities.examples.command.RoleinfoCommand;
import com.jagrosh.jdautilities.examples.command.ServerinfoCommand;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import fr.paladium.palabot.automod.AutoMod;
import fr.paladium.palabot.automod.StrikeHandler;
import fr.paladium.palabot.commands.CommandExceptionListener;
import fr.paladium.palabot.commands.automod.*;
import fr.paladium.palabot.commands.general.AboutCmd;
import fr.paladium.palabot.commands.general.UserinfoCmd;
import fr.paladium.palabot.commands.moderation.*;
import fr.paladium.palabot.commands.owner.DebugCmd;
import fr.paladium.palabot.commands.owner.EvalCmd;
import fr.paladium.palabot.commands.owner.PremiumCmd;
import fr.paladium.palabot.commands.owner.ReloadCmd;
import fr.paladium.palabot.commands.settings.*;
import fr.paladium.palabot.commands.tools.*;
import fr.paladium.palabot.database.Database;
import fr.paladium.palabot.database.DatabaseMongo;
import fr.paladium.palabot.logging.BasicLogger;
import fr.paladium.palabot.logging.MessageCache;
import fr.paladium.palabot.logging.ModLogger;
import fr.paladium.palabot.logging.TextUploader;
import fr.paladium.palabot.utils.FormatUtil;
import fr.paladium.palabot.utils.MultiBotManager;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import java.util.Arrays;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Vortex
{
    private final EventWaiter waiter;
    private final ScheduledExecutorService threadpool;
    private final Database database;
    private final DatabaseMongo mdatabase;
    private final TextUploader uploader;
    private final MultiBotManager shards;
    private final ModLogger modlog;
    private final BasicLogger basiclog;
    private final MessageCache messages;
    private final WebhookClient logwebhook;
    private final AutoMod automod;
    private final StrikeHandler strikehandler;
    private final CommandExceptionListener listener;
    private final Locale in18 = new Locale("fr","FR");

    private final ResourceBundle traduction = ResourceBundle.getBundle("messages",in18);
    
    public Vortex() throws Exception
    {
        System.setProperty("config.file", System.getProperty("config.file", "application.conf"));
        Config config = ConfigFactory.load();
        waiter = new EventWaiter(Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "eventwaiter")), false);
        threadpool = Executors.newScheduledThreadPool(100, r -> new Thread(r, "vortex"));
        database = new Database(config.getString("database.host"), 
                                       config.getString("database.username"), 
                                       config.getString("database.password"));
        mdatabase = new DatabaseMongo(config.getString("mongo.host"), config.getInt("mongo.port"),
                config.getString("mongo.username"), config.getString("mongo.password"),config.getString("mongo.database"));
        uploader = new TextUploader(config.getStringList("upload-webhooks"));
        modlog = new ModLogger(this);
        basiclog = new BasicLogger(this, config);
        messages = new MessageCache();
        logwebhook = new WebhookClientBuilder(config.getString("webhook-url")).build();
        automod = new AutoMod(this, config);
        strikehandler = new StrikeHandler(this);
        listener = new CommandExceptionListener();
        CommandClient client = new CommandClientBuilder()
                        .setPrefix(Constants.PREFIX)
                        .setActivity(Activity.playing("Paladium - PVP Faction"))
                        .setOwnerId(Constants.OWNER_ID)
                        .setEmojis(Constants.SUCCESS, Constants.WARNING, Constants.ERROR)
                        .setLinkedCacheSize(0)
                        .setGuildSettingsManager(database.settings)
                        .setListener(listener)
                        .setScheduleExecutor(threadpool)
                        .setShutdownAutomatically(false)
                        .addCommands(
                            // General
                            new AboutCmd(),
                            new PingCommand(),
                            new RoleinfoCommand(),
                            new ServerinfoCommand(),
                            new UserinfoCmd(),

                            // Moderation
                            new KickCmd(this),
                            new BanCmd(this),
                            new SoftbanCmd(this),
                            new SilentbanCmd(this),
                            new UnbanCmd(this),
                            new CleanCmd(this),
                            new VoicemoveCmd(this),
                            new VoicekickCmd(this),
                            new MuteCmd(this),
                            new UnmuteCmd(this),
                            new RaidCmd(this),
                            new StrikeCmd(this),
                            new PardonCmd(this),
                            new CheckCmd(this),
                            new ReasonCmd(this),
                            new SlowmodeCmd(this),

                            // Settings
                            new SetupCmd(this),
                            new PunishmentCmd(this),
                            new MessagelogCmd(this),
                            new ModlogCmd(this),
                            new ServerlogCmd(this),
                            new VoicelogCmd(this),
                            new AvatarlogCmd(this),
                            new TimezoneCmd(this),
                            new ModroleCmd(this),
                            new PrefixCmd(this),
                            new SettingsCmd(this),

                            // Automoderation
                            new AntiinviteCmd(this),
                            new AnticopypastaCmd(this),
                            new AntieveryoneCmd(this),
                            new AntirefCmd(this),
                            new MaxlinesCmd(this),
                            new MaxmentionsCmd(this),
                            new AntiduplicateCmd(this),
                            new AutodehoistCmd(this),
                            new FilterCmd(this),
                            new ResolvelinksCmd(this),
                            new AutoraidmodeCmd(this),
                            new IgnoreCmd(this),
                            new UnignoreCmd(this),
                            
                            // Tools
                            new AnnounceCmd(),
                            new AuditCmd(),
                            new DehoistCmd(),
                            new ExportCmd(this),
                            new InvitepruneCmd(this),

                            // Owner
                            new EvalCmd(this),
                            new DebugCmd(this),
                            new PremiumCmd(this),
                            new ReloadCmd(this)
                            //new TransferCmd(this)
                        )
                        .setHelpConsumer(event -> event.replyInDm(FormatUtil.formatHelp(event, this), m ->
                        {
                            if(event.isFromType(ChannelType.TEXT))
                                try
                                {
                                    event.getMessage().addReaction(Constants.HELP_REACTION).queue(s->{}, f->{});
                                } catch(PermissionException ignore) {}
                        }, t -> event.replyWarning("Help cannot be sent because you are blocking Direct Messages.")))
                        .setDiscordBotsKey(config.getString("listing.discord-bots"))
                        //.setCarbonitexKey(config.getString("listing.carbon"))
                        .build();
        MessageAction.setDefaultMentions(Arrays.asList(Message.MentionType.EMOTE, Message.MentionType.CHANNEL));
        shards = new MultiBotManager.MultiBotManagerBuilder()
                .addBot(config.getString("bot-token"), Constants.INTENTS)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .enableCache(CacheFlag.MEMBER_OVERRIDES, CacheFlag.VOICE_STATE)
                .disableCache(CacheFlag.EMOTE, CacheFlag.ACTIVITY, CacheFlag.CLIENT_STATUS)
                .addEventListeners(new Listener(this), client, waiter)
                .setStatus(OnlineStatus.DO_NOT_DISTURB)
                .setActivity(Activity.playing("loading..."))
                .build();
        
        modlog.start();

        threadpool.scheduleWithFixedDelay(() -> mdatabase.tempbans.checkUnbans(shards), 0, 2, TimeUnit.MINUTES);
        threadpool.scheduleWithFixedDelay(() -> mdatabase.tempmutes.checkUnmutes(shards, database.settings), 0, 45, TimeUnit.SECONDS);
        threadpool.scheduleWithFixedDelay(() -> database.tempslowmodes.checkSlowmode(shards), 0, 45, TimeUnit.SECONDS);
    }
    
    
    // Getters
    public EventWaiter getEventWaiter()
    {
        return waiter;
    }
    
    public Database getDatabase()
    {
        return database;
    }

    public DatabaseMongo getDatabaseMongo()
    {
        return mdatabase;
    }
    
    public ScheduledExecutorService getThreadpool()
    {
        return threadpool;
    }
    
    public TextUploader getTextUploader()
    {
        return uploader;
    }
    
    public MultiBotManager getShardManager()
    {
        return shards;
    }
    
    public ModLogger getModLogger()
    {
        return modlog;
    }
    
    public BasicLogger getBasicLogger()
    {
        return basiclog;
    }
    
    public MessageCache getMessageCache()
    {
        return messages;
    }
    
    public WebhookClient getLogWebhook()
    {
        return logwebhook;
    }
    
    public AutoMod getAutoMod()
    {
        return automod;
    }
    
    public StrikeHandler getStrikeHandler()
    {
        return strikehandler;
    }

    public ResourceBundle getTraduction()
    {
        return traduction;
    }
    
    public CommandExceptionListener getListener()
    {
        return listener;
    }
    
    
    // Global methods
    public void cleanPremium()
    {
        database.premium.cleanPremiumList().forEach((gid) ->
        {
            database.automod.setResolveUrls(gid, false);
            database.settings.setAvatarLogChannel(gid, null);
            database.settings.setVoiceLogChannel(gid, null);
            database.filters.deleteAllFilters(gid);
        });
    }
    
    
    /**
     * @param args the command line arguments
     * @throws java.lang.Exception
     */
    public static void main(String[] args) throws Exception
    {
        new Vortex();
    }
}
