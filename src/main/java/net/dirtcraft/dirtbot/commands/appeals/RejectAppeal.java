package net.dirtcraft.dirtbot.commands.appeals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.dirtcraft.dirtbot.internal.commands.CommandAppealStaff;
import net.dirtcraft.dirtbot.internal.commands.CommandArgument;
import net.dirtcraft.dirtbot.internal.commands.CommandClass;
import net.dirtcraft.dirtbot.modules.AppealModule;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

@CommandClass(AppealModule.class)
public class RejectAppeal extends CommandAppealStaff {

    public RejectAppeal(AppealModule module) {
        super(module);
    }

    @Override
    public boolean execute(MessageReceivedEvent event, List<String> args) {
        String message = String.join(" ", args);
        MessageEmbed responseEmbed = getModule().getEmbedUtils().getExternalEmbed()
                .addField("__Appeal Rejected__", "Your appeal has been rejected by <@" + event.getMember().getUser().getId() + "> with the following message:\n + '''" + message + "'''", false)
                .build();
        event.getTextChannel().getIterableHistory().queue((iterableHistory) -> {
            String appealerName = "";
            for(Member member : getModule().getAppealUtils().getAppealMembers(event.getTextChannel())) {
                member.getUser().openPrivateChannel().queue((privateChannel -> privateChannel.sendMessage(responseEmbed).queue()));
                appealerName = member.getEffectiveName();
            }
            getModule().archiveAppeal(iterableHistory, appealerName);
            getModule().getEmbedUtils().sendLog("Rejected", "An appeal has been rejected with the following message:\n\n```" + message + "```", event.getTextChannel(), event.getMember());
            event.getTextChannel().delete().queue();
        });
        return true;
    }

    @Override
    public List<String> aliases() {
        return new ArrayList<>(Arrays.asList("reject", "deny"));
    }

    @Override
    public List<CommandArgument> args() {
        return new ArrayList<>(Arrays.asList(new CommandArgument("Message", "The message to be sent to the appealer.", 1, 1024)));
    }
}
