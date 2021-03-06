package net.dirtcraft.dirtbot.commands.tickets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;

import com.vdurmont.emoji.EmojiParser;

import net.dirtcraft.dirtbot.internal.commands.CommandArgument;
import net.dirtcraft.dirtbot.internal.commands.CommandClass;
import net.dirtcraft.dirtbot.internal.commands.CommandTicket;
import net.dirtcraft.dirtbot.modules.TicketModule;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

@CommandClass(TicketModule.class)
public class CloseTicket extends CommandTicket {


    public CloseTicket(TicketModule module) {
        super(module);
    }

    @Override
    public boolean execute(MessageReceivedEvent event, List<String> args) {
        String reason;
        if (!args.isEmpty()) reason = EmojiParser.parseToAliases(String.join(" ", args), EmojiParser.FitzpatrickAction.REMOVE);
        else reason = "No Reason Specified.";

        EmbedBuilder responseEmbed = getModule().getEmbedUtils().getEmptyEmbed()
                .addField("__Close Ticket__", "Are you sure you want to close this ticket?\n" +
                        "Please confirm or cancel this command by selecting one of the options below.", false)
                .addField("__Reason__", "```" + StringUtils.capitalize(reason) + "```", false);

        MessageEmbed reviewEmbed = getModule().getEmbedUtils().getReviewEmbed();

        event.getTextChannel().sendMessage(responseEmbed.build()).queue(message ->
            event.getTextChannel().sendMessage(reviewEmbed).queue((msg) -> {
                msg.addReaction("\u2705").queue();
                msg.addReaction("\u274C").queue();
                getModule().getDatabaseHelper().addConfirmationMessage(getTicket(event).getId(), msg.getId(), reason);
            })
        );

        event.getTextChannel().getManager().setName("pending-review-" + getTicket(event).getId()).queueAfter(1, TimeUnit.SECONDS);
        event.getTextChannel().getManager().setTopic("This ticket has now been **completed** and pending a review").queueAfter(1, TimeUnit.SECONDS);
        return true;
    }

    @Override
    public boolean hasPermission(Member member) {
        return true;
    }

    @Override
    public List<String> aliases() {
        return new ArrayList<>(Collections.singletonList("close"));
    }

    @Override
    public List<CommandArgument> args() {
        return new ArrayList<>(Collections.singletonList(new CommandArgument("Reason", "Reason for closing the ticket", 1, 1024, true)));
    }
}
