package net.dirtcraft.dirtbot.internal.embeds;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

import net.dirtcraft.dirtbot.DirtBot;
import net.dirtcraft.dirtbot.internal.commands.CommandArgument;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public abstract class EmbedUtils {

    public abstract EmbedBuilder getEmptyEmbed();

    public boolean sendError(MessageReceivedEvent event, String error) {
        event.getChannel().sendMessage(getErrorEmbed(error).build()).queue(message -> message.delete().queueAfter(10, TimeUnit.SECONDS));
        return false;
    }

    public EmbedBuilder getErrorEmbed(String error) {
        return getEmptyEmbed()
                .setColor(10223635)
                .addField("__Error!__", error, false)
                .setTimestamp(Instant.now());
    }

    public EmbedBuilder invalidArgsEmbed(List<CommandArgument> args, String alias) {
        String commandSchema = "**" + DirtBot.getConfig().botPrefix + alias + "**";
        String argsInfo = "";
        for(CommandArgument arg : args) {
            commandSchema += " " + arg.getName().toLowerCase();
            argsInfo += "**" + arg.getName() + "**: " + arg.getDescription() + "\n";
        }
        return getEmptyEmbed()
                .addField("__Error | Invalid Arguments!__", "*" + commandSchema + "*" + "\n" + argsInfo, false);
    }

    public void sendResponse(MessageEmbed embed, TextChannel channel) {
        if (channel.getId().equalsIgnoreCase("538768628351238174")) channel.sendMessage(embed).queue();
        else channel.sendMessage(embed).queue((message) -> message.delete().queueAfter(10, TimeUnit.SECONDS));
    }

    public MessageEmbed getReviewEmbed() {
        return getEmptyEmbed()
                .addField("__Review__",
                        "Please consider leaving a review on your experiences on DirtCraft and the support you have received.\n" +
                                "We appreciate the review and hope you enjoy your time on DirtCraft!\n" +
                                "[Click me to leave a review on **Minecraft-MP**](https://minecraft-mp.com/server/206809/discussions/)" + "\n" +
                                "[Click me to leave a review on **Pixelmon Servers**](https://pixelmonservers.com/server/75qpnFWv/dirtcraft-pixelmon-reforged)\n" +
                                "[Click me to leave a review on **FTB Servers**](https://ftbservers.com/server/rDh9a32R/dirtcraft-modded-network)", false).build();
    }
}
