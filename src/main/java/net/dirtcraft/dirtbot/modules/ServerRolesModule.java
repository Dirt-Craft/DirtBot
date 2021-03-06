package net.dirtcraft.dirtbot.modules;

import com.electronwill.nightconfig.core.ConfigSpec;
import com.electronwill.nightconfig.core.conversion.Path;
import net.dirtcraft.dirtbot.DirtBot;
import net.dirtcraft.dirtbot.commands.roles.CreateModpackRole;
import net.dirtcraft.dirtbot.commands.roles.EditModpackRole;
import net.dirtcraft.dirtbot.internal.configs.ConfigurationManager;
import net.dirtcraft.dirtbot.internal.configs.IConfigData;
import net.dirtcraft.dirtbot.internal.embeds.EmbedUtils;
import net.dirtcraft.dirtbot.internal.modules.Module;
import net.dirtcraft.dirtbot.internal.modules.ModuleClass;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.time.Instant;

@ModuleClass
public class ServerRolesModule extends Module<ServerRolesModule.ConfigDataServerRoles, ServerRolesModule.EmbedUtilsServerRoles> {

    @Override
    public void initialize() {
        // Initialize Embed Utils
        setEmbedUtils(new EmbedUtilsServerRoles());

        DirtBot.getCoreModule().registerCommands(
                new CreateModpackRole(this),
                new EditModpackRole(this)
        );
    }

    @Override
    public void initializeConfiguration() {
        ConfigSpec spec = new ConfigSpec();

        spec.define("discord.embeds.footer", "Created for DirtCraft");
        spec.define("discord.embeds.title", ":redbulletpoint: DirtCraft's DirtBOT :redbulletpoint:");
        spec.define("discord.embeds.color", 16711680);

        spec.define("server.roles.token", "");

        setConfig(new ConfigurationManager<>(ConfigDataServerRoles.class, spec, "ServerRoles"));
    }

    public static class ConfigDataServerRoles implements IConfigData {
        @Path("discord.embeds.footer")
        public String embedFooter;
        @Path("discord.embeds.title")
        public String embedTitle;
        @Path("discord.embeds.color")
        public int embedColor;

        @Path("server.roles.token")
        public String serverRolesToken;
    }

    public class EmbedUtilsServerRoles extends EmbedUtils {
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
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        if (event.getMessageId().equals("538574783319638026")) giveRole(event);
    }

    @Override
    public void onMessageReactionRemove(MessageReactionRemoveEvent event) {
        if (event.getMessageId().equals("538574783319638026")) removeRole(event);
    }

    private void giveRole(MessageReactionAddEvent event) {
        Member member = event.retrieveMember().complete();
        if (member.getUser().isBot()) return;

        event.getGuild().getRolesByName(event.getReactionEmote().getName().toLowerCase(), true).forEach(role ->
                event.getGuild().addRoleToMember(member, role).queue());

        EmbedBuilder embed = getEmbedUtils().getEmptyEmbed()
                .setColor(Color.GREEN)
                .setTitle("<:redbulletpoint:539273059631104052>**DirtCraft Role Assignments**<:redbulletpoint:539273059631104052>")
                .setTimestamp(Instant.now());
        if (!event.getReactionEmote().getName().equalsIgnoreCase("pixelmon")) {
            embed.setDescription("You are now subscribed to updates regarding the ModPack **" + String.join(" ", StringUtils.splitByCharacterTypeCamelCase(event.getReactionEmote().getName())) + "**");
        } else {
            embed.setDescription("You are now subscribed to updates regarding **Pixelmon Reforged**");
        }

        member.getUser().openPrivateChannel().queue(dm -> dm.sendMessage(embed.build()).queue());
    }

    private void removeRole(MessageReactionRemoveEvent event) {
        Member member = event.retrieveMember().complete();
        if (member.getUser().isBot()) return;

        event.getGuild().getRolesByName(event.getReactionEmote().getName().toLowerCase(), true).forEach(role ->
                event.getGuild().removeRoleFromMember(member, role).queue());

        EmbedBuilder embed = getEmbedUtils().getEmptyEmbed()
                .setColor(Color.RED)
                .setTitle("<:redbulletpoint:539273059631104052>**DirtCraft Role Assignments**<:redbulletpoint:539273059631104052>")
                .setTimestamp(Instant.now());
        if (!event.getReactionEmote().getName().toLowerCase().equalsIgnoreCase("pixelmon")) {
                embed.setDescription("You are no longer subscribed to updates regarding the ModPack **" + String.join(" ", StringUtils.splitByCharacterTypeCamelCase(event.getReactionEmote().getName())) + "**");
        } else {
            embed.setDescription("You are no longer subscribed to updates regarding **Pixelmon Reforged**");
        }

        member.getUser().openPrivateChannel().queue(dm -> dm.sendMessage(embed.build()).queue());
    }
}
