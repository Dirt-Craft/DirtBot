package net.dirtcraft.dirtbot.modules;

import br.com.azalim.mcserverping.MCPing;
import br.com.azalim.mcserverping.MCPingResponse;
import com.electronwill.nightconfig.core.ConfigSpec;
import com.electronwill.nightconfig.core.conversion.Path;
import net.dirtcraft.dirtbot.DirtBot;
import net.dirtcraft.dirtbot.internal.configs.ConfigurationManager;
import net.dirtcraft.dirtbot.internal.configs.IConfigData;
import net.dirtcraft.dirtbot.internal.embeds.EmbedUtils;
import net.dirtcraft.dirtbot.internal.modules.Module;
import net.dirtcraft.dirtbot.internal.modules.ModuleClass;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberLeaveEvent;

import java.io.IOException;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.Timer;
import java.util.TimerTask;

@ModuleClass
public class MiscModule extends Module<MiscModule.ConfigDataMisc, MiscModule.EmbedUtilsMisc> {

    private final NumberFormat numberFormat = NumberFormat.getInstance();
    private Guild dirtcraftGuild;

    @Override
    public void initialize() {
        // Initialize Embed utils
        setEmbedUtils(new MiscModule.EmbedUtilsMisc());
        numberFormat.setGroupingUsed(true);
        dirtcraftGuild = DirtBot.getJda().getGuildById(DirtBot.getConfig().serverID);
    }

    @Override
    public void initializeConfiguration() {
        ConfigSpec spec = new ConfigSpec();

        spec.define("minecraft.server.ip", "dirtcraft.gg");

        spec.define("discord.channel.player-count-channel-id", "602886089971204100");
        spec.define("discord.channel.member-count-channel-id", "602887766430187541");

        spec.define("discord.embeds.footer", "DirtCraft's DirtBOT | 2020");
        spec.define("discord.embeds.title", "<:redbulletpoint:539273059631104052> DirtCraft's DirtBOT <:redbulletpoint:539273059631104052>");
        spec.define("discord.embeds.color", 16711680);

        setConfig(new ConfigurationManager<>(MiscModule.ConfigDataMisc.class, spec, "Miscellaneous"));
    }

    public static class ConfigDataMisc implements IConfigData {
        @Path("minecraft.server.ip")
        public String serverIP;

        @Path("discord.channel.player-count-channel-id")
        public String playerCountChannelID;
        @Path("discord.channel.member-count-channel-id")
        public String memberCountChannelID;

        @Path("discord.embeds.footer")
        public String embedFooter;
        @Path("discord.embeds.title")
        public String embedTitle;
        @Path("discord.embeds.color")
        public int embedColor;
    }

    public class EmbedUtilsMisc extends EmbedUtils {
        @Override
        public EmbedBuilder getEmptyEmbed() {
            return new EmbedBuilder()
                    .setTitle(getConfig().embedTitle)
                    .setColor(getConfig().embedColor)
                    .setFooter(getConfig().embedFooter, null)
                    .setTimestamp(Instant.now());
        }
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        this.setMemberCount();
    }

    @Override
    public void onGuildMemberLeave(GuildMemberLeaveEvent event) {
        this.setMemberCount();
    }

    private void setMemberCount() {
        VoiceChannel voiceChannel = DirtBot.getJda().getVoiceChannelById(getConfig().memberCountChannelID);
        int size = dirtcraftGuild.getMembers().size();
        if (Integer.parseInt(voiceChannel.getName().replace("Member Count: ", "").replace(",", "")) ==
                dirtcraftGuild.getMembers().size()) return;
        String sizeString = numberFormat.format(size);
        voiceChannel.getManager().setName("Member Count: " + sizeString).queue();
    }

    public void initPlayerCountScheduler() {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                VoiceChannel voiceChannel = DirtBot.getJda().getVoiceChannelById(getConfig().playerCountChannelID);
                int currentSize = Integer.parseInt(voiceChannel.getName().replace("Player Count: ", ""));
                int newSize = getPlayerCount();
                if (currentSize == newSize) return;
                if (newSize != -1) voiceChannel.getManager().setName("Player Count: " + getPlayerCount()).queue();
            }
        }, 0, 2000);
    }

    private int getPlayerCount() {
        try {
            MCPingResponse reply = MCPing.getPing(getConfig().serverIP);
            return reply.getPlayers().getOnline();
        } catch (IOException exception) {
            return -1;
        }
    }

}
