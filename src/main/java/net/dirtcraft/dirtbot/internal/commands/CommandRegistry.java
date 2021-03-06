package net.dirtcraft.dirtbot.internal.commands;

import net.dirtcraft.dirtbot.DirtBot;
import net.dirtcraft.dirtbot.internal.modules.Module;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class CommandRegistry {

    private List<ICommand> commands;

    public CommandRegistry() {
        commands = new ArrayList<>();
    }

    public void registerCommand(ICommand command) {
        commands.add(command);
    }
    
    public List<ICommand> getCommands() {
    	return commands;
    }

    // Call when a message that is potentially a command is sent
    public boolean executeCommand(MessageReceivedEvent event) {
        // If the message does not contain the set prefix, ignore it
        if(!event.getMessage().getContentDisplay().startsWith(DirtBot.getConfig().botPrefix)) return false;
        new Thread(() -> {
            List<String> args = new ArrayList<>(Arrays.asList(event.getMessage().getContentDisplay().substring(DirtBot.getConfig().botPrefix.length()).split(" ")));
            String commandString = args.get(0).toLowerCase();
            args.remove(0);
            // See if a command with a matching alias is registered
            for (ICommand command : commands) {
                if (command.aliases().contains(commandString)) {
                    // You better start typing!
                    event.getTextChannel().sendTyping().queue();
                    // If a command IS found, make sure that all of the requirements are met.
                    if (!command.hasPermission(event.getMember())) {
                        EmbedBuilder responseEmbed = DirtBot.getModuleRegistry().getModule(command.getClass().getAnnotation(CommandClass.class).value()).getEmbedUtils().getEmptyEmbed()
                                .addField("__Error | No Permission!__", "You do not have permission to execute this command!", false);
                        event.getTextChannel().sendMessage(responseEmbed.build()).queue((message) -> {
                            message.delete().queueAfter(10, TimeUnit.SECONDS);
                        });
                        event.getMessage().delete().queue();
                    } else if (!command.validChannel(event.getTextChannel())) {
                        EmbedBuilder responseEmbed = DirtBot.getModuleRegistry().getModule(command.getClass().getAnnotation(CommandClass.class).value()).getEmbedUtils().getEmptyEmbed()
                                .addField("__Error | Invalid Channel!__", "This is not a valid channel for this command!", false);
                        event.getTextChannel().sendMessage(responseEmbed.build()).queue((message) -> message.delete().queueAfter(10, TimeUnit.SECONDS));
                        event.getMessage().delete().queue();
                    } // Check if command is sent in Gamechat category, do not allow command to run if it is
                    else if (event.getJDA().getCategoryById("516473998478016512").getTextChannels().contains(event.getTextChannel())) event.getMessage().delete().queue();
                    else if (!validArgs(command, args)) {
                        Module module = DirtBot.getModuleRegistry().getModule(command.getClass().getAnnotation(CommandClass.class).value());
                        module.getEmbedUtils().sendResponse(module.getEmbedUtils().invalidArgsEmbed(command.args(), command.aliases().get(0)).build(), event.getTextChannel());
                        event.getMessage().delete().queue();
                    } else {
                        // Attempt to execute the command
                        if (!command.execute(event, args)) {
                            // TODO: ICommand Failed to Execute Error Embed
                        }
                        /* The command executed successfully, delete the message the player sent */
                        else if (!event.getChannel().getId().equals(DirtBot.getConfig().botspamChannelID))
                            event.getMessage().delete().queue();
                    }
                }
            }
        }).start();
        return false;
    }

    public boolean validArgs(ICommand command, List<String> args) {
        // If the command does not require arguments return true
        if (command.args() == null || command.args().isEmpty()) return true;

        List<CommandArgument> requiredArgs = command.args().stream().filter(arg -> !arg.isOptional()).collect(Collectors.toList());

        // If the command wasn't passed enough arguments return false
        if(args.size() < requiredArgs.size()) return false;

        // Cannot use foreach because we need i
        for(int i = 0; i < requiredArgs.size(); i++) {

            // If any of the required arguments and arguments found do not mach up
            if(!command.args().get(i).validArgument(args.get(i))) return false;
        }
        // At this point we can assume valid arguments have been found, return true
        return true;
    }
}
