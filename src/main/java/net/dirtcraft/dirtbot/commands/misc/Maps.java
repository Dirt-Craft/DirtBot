package net.dirtcraft.dirtbot.commands.misc;

import net.dirtcraft.dirtbot.DirtBot;
import net.dirtcraft.dirtbot.internal.commands.CommandArgument;
import net.dirtcraft.dirtbot.internal.commands.CommandClass;
import net.dirtcraft.dirtbot.internal.commands.ICommand;
import net.dirtcraft.dirtbot.modules.CommandsModule;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.Arrays;
import java.util.List;

@CommandClass(CommandsModule.class)
public class Maps implements ICommand {

    private final CommandsModule module;

    public Maps(CommandsModule module) {
        this.module = module;
    }

    @Override
    public boolean execute(MessageReceivedEvent event, List<String> args) {
        String packs = "";
        String dates = "";
        for (int i = 0; i < module.getConfig().oldMaps.size(); i++) {
            List<String> oldMap = module.getConfig().oldMaps.get(i);
            packs += "[**" + oldMap.get(0) + "**](" + oldMap.get(1) + ")";
            dates += "`" + oldMap.get(2) + "`";
            if (!(i + 1 == DirtBot.getConfig().servers.size())) {
                packs += "\n";
                dates += "\n";
            }
        }
        MessageEmbed response = module.getEmbedUtils().getEmptyEmbed()
                .addField("__Modpacks__", packs, true)
                .addField("__Dates__", dates, true)
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
        return Arrays.asList("maps", "worlds", "oldmaps", "oldworlds", "map", "world", "save", "saves", "oldsaves");
    }

    @Override
    public List<CommandArgument> args() {
        return null;
    }
}