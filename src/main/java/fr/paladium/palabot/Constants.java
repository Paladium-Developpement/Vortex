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

import java.time.OffsetDateTime;
import java.util.EnumSet;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.requests.GatewayIntent;

import static net.dv8tion.jda.api.requests.GatewayIntent.GUILD_PRESENCES;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Constants
{
    public final static OffsetDateTime STARTUP = OffsetDateTime.now();
    public final static String PREFIX          = ">>";
    public final static String SUCCESS         = ":white_check_mark:";
    public final static String WARNING         = ":warning:";
    public final static String ERROR           = ":no_entry:";
    public final static String LOADING         = "<a:typing:825514032696918087>";
    public final static String HELP_REACTION   = SUCCESS.replaceAll("<a?:(.+):(\\d+)>", "$1:$2");
    public final static String ERROR_REACTION  = ERROR.replaceAll("<a?:(.+):(\\d+)>", "$1:$2");
    public final static String VORTEX_EMOJI    = "<:Vortex:386971287282515970>";
    public final static int DEFAULT_CACHE_SIZE = 8000;
    public final static Permission[] PERMISSIONS = {Permission.ADMINISTRATOR, Permission.BAN_MEMBERS, Permission.KICK_MEMBERS, Permission.MANAGE_ROLES,
                                        Permission.MANAGE_SERVER, Permission.MESSAGE_ADD_REACTION, Permission.MESSAGE_ATTACH_FILES, Permission.MESSAGE_READ,
                                        Permission.MESSAGE_WRITE,Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_HISTORY, Permission.MESSAGE_EXT_EMOJI,
                                        Permission.MESSAGE_MANAGE, Permission.VOICE_CONNECT, Permission.VOICE_MOVE_OTHERS, Permission.VOICE_DEAF_OTHERS, 
                                        Permission.VOICE_MUTE_OTHERS, Permission.NICKNAME_CHANGE, Permission.NICKNAME_MANAGE, Permission.VIEW_AUDIT_LOGS};
    public final static EnumSet<GatewayIntent> INTENTS = EnumSet.allOf(GatewayIntent.class);
    public final static String OWNER_ID      = "128909510587711488";
}
