package net.dirtcraft.dirtbot.commands.misc;

import net.dirtcraft.dirtbot.internal.commands.CommandArgument;
import net.dirtcraft.dirtbot.internal.commands.CommandClass;
import net.dirtcraft.dirtbot.internal.commands.ICommand;
import net.dirtcraft.dirtbot.modules.CommandsModule;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.Arrays;
import java.util.List;

@CommandClass()
public class Launcher implements ICommand {

    private final CommandsModule module;

    public Launcher(CommandsModule module) {
        this.module = module;
    }


    @Override
    public boolean execute(MessageReceivedEvent event, List<String> args) {
        MessageEmbed response = module.getEmbedUtils().getEmptyEmbed()
                .setTimestamp(null)
                .setFooter(null, null)
                .setTitle("<:redbulletpoint:539273059631104052> **DirtCraft's Dirt Launcher** <:redbulletpoint:539273059631104052>")
                .addField("__Windows Installer__",
                        "[**64-Bit**](https://dirtcraft.net/launcher/download/Dirt-Launcher_x64.msi)\n" +
                        "[**32-Bit**](https://dirtcraft.net/launcher/download/Dirt-Launcher_x86.msi)", false)
                .addField("__Universal Installer__", "[**All Operating Systems**](https://dirtcraft.net/launcher/Dirt-Launcher.jar)",
                        false).build();
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
        return Arrays.asList("launcher", "dirtlauncher");
    }

    @Override
    public List<CommandArgument> args() {
        return null;
    }
}
