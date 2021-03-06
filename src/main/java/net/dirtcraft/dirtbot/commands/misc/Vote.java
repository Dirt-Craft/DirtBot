package net.dirtcraft.dirtbot.commands.misc;

import java.util.Arrays;
import java.util.List;

import net.dirtcraft.dirtbot.internal.commands.CommandArgument;
import net.dirtcraft.dirtbot.internal.commands.CommandClass;
import net.dirtcraft.dirtbot.internal.commands.ICommand;
import net.dirtcraft.dirtbot.modules.CommandsModule;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

@CommandClass()
public class Vote implements ICommand {

    private final CommandsModule module;

    public Vote(CommandsModule module) {
        this.module = module;
    }

    @Override
    public boolean execute(MessageReceivedEvent event, List<String> args) {
        MessageEmbed response = module.getEmbedUtils().getEmptyEmbed()
                .setTimestamp(null)
                .addField("__Voting Links__", "[**Click me to vote on Minecraft-MP**](https://minecraft-mp.com/server/206809/vote/)\n" +
                                "[**Click me to vote on Pixelmon Servers**](https://pixelmonservers.com/server/75qpnFWv/vote)\n" +
                                "[**Click me to vote on FTB Servers**](https://ftbservers.com/server/rDh9a32R/vote)",
                        false)
                .setFooter("Offline votes will be credited to your account upon login", null)
                .build();
        event.getTextChannel().sendMessage(response).queue();
        return true;
    }

    @Override
    public boolean hasPermission(Member member) {
        return true;
    }

    @Override
    public boolean validChannel(TextChannel channel) {
        return true;
    }

    @Override
    public List<String> aliases() {
        return Arrays.asList("vote");
    }

    @Override
    public List<CommandArgument> args() {
        return null;
    }
}
