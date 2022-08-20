package net.dirtcraft.dirtbot.modules;

import com.electronwill.nightconfig.core.ConfigSpec;
import com.electronwill.nightconfig.core.conversion.Path;
import com.google.common.collect.Lists;
import com.vdurmont.emoji.EmojiParser;
import net.dirtcraft.dirtbot.DirtBot;
import net.dirtcraft.dirtbot.commands.tickets.*;
import net.dirtcraft.dirtbot.data.Ticket;
import net.dirtcraft.dirtbot.internal.configs.ConfigurationManager;
import net.dirtcraft.dirtbot.internal.configs.IConfigData;
import net.dirtcraft.dirtbot.internal.embeds.EmbedUtils;
import net.dirtcraft.dirtbot.internal.modules.Module;
import net.dirtcraft.dirtbot.internal.modules.ModuleClass;
import net.dirtcraft.dirtbot.utils.tickets.TicketUtils;
import net.dirtcraft.dirtbot.utils.tickets.TicketsDatabaseHelper;
import net.dirtcraft.dirtbot.utils.verification.VerificationDatabaseHelper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveEvent;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.model.ZipParameters;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@ModuleClass(requiresDatabase = true)
public class TicketModule extends Module<TicketModule.ConfigDataTickets, TicketModule.EmbedUtilsTickets> {

    private HashMap<String, String> ticketNotificationEmbeds;
    private TicketUtils ticketUtils;
    private TicketsDatabaseHelper databaseHelper;

    @Override
    public void initialize() {
        // Initialize Database Helper
        databaseHelper = new TicketsDatabaseHelper(this);

        // Initialize Archives Folder
        File archivesFolder = new File("archives" + File.separator + "tickets");
        if(!archivesFolder.exists()) archivesFolder.mkdirs();

        // Initialize Notification Embeds List
        ticketNotificationEmbeds = new HashMap<>();

        // Initialize Embed Utils
        setEmbedUtils(new EmbedUtilsTickets());

        // Initialize Ticket Utils
        ticketUtils = new TicketUtils(this);

        // Print Channel Header
        printChannelHeader();

        // Print Notification Embeds
        printTicketNotificationEmbeds();

        // Register Commands
        DirtBot.getCoreModule().registerCommands(
                new AddMember(this),
                new CloseTicket(this),
                new CloseTimer(this),
                new GetInfo(this),
                new PopulateTicket(this),
                new RemoveMember(this),
                new SetLevel(this, Ticket.Level.NORMAL),
                new SetLevel(this, Ticket.Level.ADMIN),
                new SetLevel(this, Ticket.Level.OWNER),
                new SetServer(this),
                new SetTicketName(this),
                new SetUsername(this),
                new SilentClose(this),
                new LauncherTickets(this)
        );

        // Establish Tasks
        Timer autoClose = new Timer();
        autoClose.schedule(new TimerTask() {
        	@Override
        	public void run() {
        		HashMap<Integer, String> timedCloses = getDatabaseHelper().getAllTimedCloses();
        		for(int ticketID : timedCloses.keySet()) {
        			 LocalDateTime ticketCloseTime = LocalDateTime.parse(timedCloses.get(ticketID));
                     LocalDateTime now = LocalDateTime.now();
                     if (ChronoUnit.SECONDS.between(now, ticketCloseTime) <= 60) {
                         getDatabaseHelper().getTicket(ticketID).ifPresent(ticket -> {
                             getEmbedUtils().sendLog("Timer Closed", "The user failed to respond to this ticket within 24 hours of the timer being started, so the ticket was automatically closed.", ticket, DirtBot.getJda().getGuildById(DirtBot.getConfig().serverID).getMemberById(DirtBot.getJda().getSelfUser().getId()));
                             getTicketUtils().closeTicket(ticket, "No responses were received for 24 hours after starting a timer, and thus the ticket has been closed automatically. Please submit a new ticket if you need further assistance.", true);
                         });
                     }
        		}
        	}
        }, 60, 60*1000);
        
        //DEPRECATED BY KARATE
        /*if(getConfig().autocloseInterval > 0) {
            TimerTask autoClose = new TimerTask() {
                @Override
                public void run() {
                    HashMap<Integer, String> timedCloses = getDatabaseHelper().getAllTimedCloses();
                    for(int ticketID : timedCloses.keySet()) {
                        LocalDateTime ticketCloseTime = LocalDateTime.parse(timedCloses.get(ticketID));
                        LocalDateTime now = LocalDateTime.now();
                        if (ChronoUnit.SECONDS.between(now, ticketCloseTime) <= 60) {
                            getDatabaseHelper().getTicket(ticketID).ifPresent(ticket -> {
                                getEmbedUtils().sendLog("Timer Closed", "The user failed to respond to this ticket within 24 hours of the timer being started, so the ticket was automatically closed.", ticket, DirtBot.getJda().getGuildById(DirtBot.getConfig().serverID).getMemberById(DirtBot.getJda().getSelfUser().getId()));
                                getTicketUtils().closeTicket(ticket, "No responses were received for 24 hours after starting a timer, and thus the ticket has been closed automatically. Please submit a new ticket if you need further assistance.", true);
                            });
                        }
                    }
                }
            };
            Timer autoCloseTimer = new Timer();
            autoCloseTimer.scheduleAtFixedRate(autoClose, 0, getConfig().autocloseInterval * 1000);
        } else {
            EmbedBuilder autoCloseNoInitEmbed = getEmbedUtils().getEmptyEmbed()
                    .addField("__Initialization Event | Timer Close__",
                            "Timer Close has been **disabled** due to autoclose interval being set to -1.\n To enable Timer Close, configure autoclose interval to an integer greater than 0 and restart the bot.", false);
            getEmbedUtils().sendLog(autoCloseNoInitEmbed.build());
        }*/
        if (getConfig().gamesyncInterval > 0) {
            /*
            TimerTask gameSync = new TimerTask() {
                @Override
                public void run() {
                    for(Ticket ticket : getDatabaseHelper().getGameTickets()) {
                        final String discordID = ticket.getDiscordID(true);
                        if (Strings.isNullOrEmpty(discordID)) continue;
                        getTicketUtils().createTicket(ticket, discordID);
                    }
                }
            };
            Timer gameSyncTimer = new Timer();
            gameSyncTimer.scheduleAtFixedRate(gameSync, 0, getConfig().gamesyncInterval * 1000);

             */
        } else {
            EmbedBuilder gameSyncNoInitEmbed = getEmbedUtils().getEmptyEmbed()
                    .addField("__Initialization Event | Game Sync__", "Game Sync has been **disabled** due to gamesync interval being set to -1.\n To enable Game Sync, configure gamesync interval to an integer greater than 0 and restart the bot.", false);
            getEmbedUtils().sendLog(gameSyncNoInitEmbed.build());
        }
    }

    @Override
    public void initializeConfiguration() {
        ConfigSpec spec = new ConfigSpec();

        spec.define("database.url", "jdbc:mariadb://localhost:3306/support");
        spec.define("database.user", "");
        spec.define("database.password", "");

        spec.define("intervals.gamesync", -1);
        spec.define("intervals.autoclose", -1);

        spec.define("discord.embeds.footer", "Created for DirtCraft");
        spec.define("discord.embeds.title", ":redbulletpoint: DirtCraft Support :redbulletpoint:");
        spec.define("discord.embeds.color", 16711680);

        spec.define("discord.channels.supportChannelID", "");
        spec.define("discord.channels.notificationChannelID", "");
        spec.define("discord.channels.loggingChannelID", "");
        spec.define("discord.channels.adminSupportGenericChannelID", "");

        spec.define("discord.categories.supportCategoryID", "");
        spec.define("discord.categories.launcherSupportCategoryID", "");
        spec.define("discord.categories.ownerSupportCategoryID", "");

        setConfig(new ConfigurationManager<>(ConfigDataTickets.class, spec, "Tickets"));
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if(event.getAuthor().isBot()) return;

        // Ticket Creation Message?
        if(event.getChannel().getId().equals(getConfig().supportChannelID)) {
            ticketCreationMessageReceived(event);
        // Ticket Internal Message?
        } else if(getTicketUtils().isTicketChannel(event.getTextChannel())) {
            ticketInternalMessageReceived(event);
        }
    }

    @Override
    public void onGuildMessageReactionAdd(GuildMessageReactionAddEvent event) {
        if(event.getUser().isBot()) return;

        event.getChannel().retrieveMessageById(event.getMessageId()).queue((message) -> {
            if(!message.getAuthor().isBot()) return;
            // Notification Subscription?
            if(event.getChannel().getId().equals(getConfig().notificationChannelID)) {
                notificationSubscribed(event, message);
            // Confirmation Embed?
            } else if(getDatabaseHelper().isConfirmationMessage(message.getId())) {
                confirmationReceived(event, message);
            }
        });
    }

    @Override
    public void onGuildMessageReactionRemove(GuildMessageReactionRemoveEvent event) {
        if (event.getUser() == null) return;
        if (event.getUser().isBot()) return;

        event.getChannel().retrieveMessageById(event.getMessageId()).queue((message) -> {
            if(!message.getAuthor().isBot()) return;

            // Notification Unsubscription?
            if(event.getChannel().getId().equals(getConfig().notificationChannelID)) {
                notificationUnsubscribed(event, message);
            }
        });
    }

    public static class ConfigDataTickets implements IConfigData {
        @Path("database.url")
        public String databaseUrl;
        @Path("database.user")
        public String databaseUser;
        @Path("database.password")
        public String databasePassword;

        @Path("intervals.gamesync")
        public int gamesyncInterval;
        @Path("intervals.autoclose")
        public int autocloseInterval;

        @Path("discord.embeds.footer")
        public String embedFooter;
        @Path("discord.embeds.title")
        public String embedTitle;
        @Path("discord.embeds.color")
        public int embedColor;

        @Path("discord.channels.supportChannelID")
        public String supportChannelID;
        @Path("discord.channels.notificationChannelID")
        public String notificationChannelID;
        @Path("discord.channels.loggingChannelID")
        public String loggingChannelID;
        @Path("discord.channels.adminSupportGenericChannelID")
        public String adminSupportGenericChannelID;

        @Path("discord.categories.supportCategoryID")
        public String supportCategoryID;
        @Path("discord.categories.launcherSupportCategoryID")
        public String launcherSupportCategoryID;
        @Path("discord.categories.ownerSupportCategoryID")
        public String ownerSupportCategoryID;
    }

    public class EmbedUtilsTickets extends EmbedUtils {

        @Override
        public EmbedBuilder getEmptyEmbed() {
            return new EmbedBuilder()
                    .setTitle(getConfig().embedTitle)
                    .setColor(getConfig().embedColor)
                    .setFooter(getConfig().embedFooter, null)
                    .setTimestamp(Instant.now());
        }

        public EmbedBuilder getExternalEmbed() {
            return getEmptyEmbed();
        }

        public void sendLog(String eventName, String eventInfo, Ticket ticket, Member member) {
            sendLog(getEmptyEmbed()
                    .addField("__Ticket Event | " + eventName + "__", eventInfo, false)
                    .addField("__Ticket Information__",
                            "**Ticket ID:** " + ticket.getId() + "\n" +
                                    "**Ticket Channel:** <#" + ticket.getChannel() + ">\n" +
                                    "**Action Completed By:** <@" + member.getUser().getId() + ">", false).build());
        }

        public void sendLog(MessageEmbed embed) {
            DirtBot.getJda().getTextChannelById(getConfig().loggingChannelID).sendMessage(embed).queue();
        }

        public MessageEmbed getTicketHeader(Ticket ticket) {
            String membersFormatted = "";
            boolean hasMember = false;
            List<Member> members = getTicketUtils().getTicketMembers(ticket);
            for(Member member : members) {
                hasMember = true;
                membersFormatted += "<@" + member.getUser().getId() + ">, ";
            }
            if(hasMember) membersFormatted = membersFormatted.substring(0, membersFormatted.length() - 2);
            return getEmptyEmbed()
                .addField("__Ticket Message__", ticket.getMessage(), false)
                .addField("__Ticket Information__",
                    "**Ticket ID:** " + ticket.getId() + "\n" +
                    "**" + (members.size() != 1 ? "Members" : "Member") + ":** " + membersFormatted + "\n" +
                    "**Username:** " + ticket.getUsername(false) + "\n" +
                    "**Server:** " + ticket.getServer(false).toUpperCase() + "\n" +
                    "**Level:** " + ticket.getLevel().toString().toUpperCase(), false).build();
        }

        public void postTicketAdminMessage(Ticket ticket) {
            TextChannel adminListChannel = DirtBot.getJda().getTextChannelById(getConfig().adminSupportGenericChannelID);
            if (ticket.getServer(true) != null) {
                for(List<String> serverInfo : DirtBot.getConfig().servers) {
                    if(serverInfo.get(1).equals(ticket.getServer(false))) {
                        adminListChannel = DirtBot.getJda().getTextChannelById(serverInfo.get(3));
                    }
                }
            }
            adminListChannel.sendMessage(getEmptyEmbed()
                .addField("__Ticket Information__",
                        "**Ticket ID:** " + ticket.getId() + "\n" +
                        "**Ticket Channel:** <#" + ticket.getChannel() + ">", false).build()).queue();
        }

        public void updateTicketHeaderMessage(Ticket ticket) {
            TextChannel ticketChannel = DirtBot.getJda().getTextChannelById(ticket.getChannel());
            ticketChannel.retrievePinnedMessages().queue((messages) -> {
                for (Message message : messages) {
                    if (message.getEmbeds().size() == 0) DirtBot.pokeDevs(new IllegalArgumentException("There is no embed! @ " + ticketChannel.getId()));
                    final MessageEmbed embed = message.getEmbeds().get(0);

                    if (embed.getFields().size() <= 1) DirtBot.pokeDevs(new IllegalArgumentException("There is not enough fields! @ " + ticketChannel.getId()));
                    final MessageEmbed.Field field = embed.getFields().get(1);

                    if (field.getName().contains("__Ticket Information__")) {
                        message.editMessage(getTicketHeader(ticket)).queue();
                    }
                }
            });
        }

        public void deleteTicketAdminMessage(Ticket ticket, String oldServer) {
            TextChannel adminListChannel = DirtBot.getJda().getTextChannelById(getConfig().adminSupportGenericChannelID);
            if(oldServer != null) {
                for(List<String> serverInfo : DirtBot.getConfig().servers) {
                    if(serverInfo.get(1).equals(oldServer)) {
                        adminListChannel = DirtBot.getJda().getTextChannelById(serverInfo.get(3));
                    }
                }
            }
            adminListChannel.getIterableHistory().queue((messages) -> {
                if(messages.size() < 1) return;
                for(Message message : messages) {
                    if(message.getEmbeds().get(0).getFields().get(0).getValue().contains("**Ticket ID:** " + ticket.getId())) message.delete().queue();
                }
            });
        }
    }

    public TicketUtils getTicketUtils() { return ticketUtils; }

    public TicketsDatabaseHelper getDatabaseHelper() { return databaseHelper; }

    public HashMap<String, String> getTicketNotificationEmbeds() { return ticketNotificationEmbeds; }

    public CompletableFuture<Void> archiveTicket(Ticket ticket) {
        return CompletableFuture.runAsync(() -> archiveTicket(DirtBot.getJda().getTextChannelById(ticket.getChannel()), ticket.getId()));
    }

    private void archiveTicket(TextChannel channel, int ticketID) {
            List<String> lines = new ArrayList<>();
            if (channel == null) return;

            new Thread(() ->
                channel.getIterableHistory().queue((messageHistory) -> {
                    try {
                        for(Message message : Lists.reverse(messageHistory)) {
                            if (message == null) continue;
                            if (message.getMember() == null) continue;
                            String line = "";
                            line += message.getMember().getEffectiveName();
                            line += " : ";
                            line += message.getTimeCreated().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT));
                            line += "> ";
                            line += message.getContentDisplay();
                            for(MessageEmbed embed : message.getEmbeds()) {
                                for(MessageEmbed.Field field : embed.getFields()) {
                                    line += " [" + field.getName() + "] ";
                                    line += field.getValue();
                                }
                            }
                            line += "\n";
                            lines.add(line);
                        }
                        java.nio.file.Path file = Paths.get("archives" + File.separator + "tickets" + File.separator + ticketID + ".txt");
                        Files.write(file, lines, Charset.forName("UTF-8"));
                        getArchive(ticketID).addFile(file.toFile(), new ZipParameters());
                        file.toFile().delete();
                    } catch (Exception e) {
                        e.printStackTrace();
                        DirtBot.pokeDevs(e);
                    }
                })).start();
    }

    private ZipFile getArchive(int ticketID) {
        int diff = ticketID % 100;
        try {
            return new ZipFile("archives" + File.separator + "tickets" + File.separator + "Tickets-" + (ticketID - diff) + "-" + (ticketID - diff + 99) + ".zip");
        } catch (Exception e) {
            e.printStackTrace();
            DirtBot.pokeDevs(e);
        }
        return null;
    }

    private void printChannelHeader() {
        EmbedBuilder instructions = getEmbedUtils().getEmptyEmbed()
                .setTimestamp(null)
                .addField("__Ticket System Instructions__",
                        "**1.** Briefly describe the issue you're having **inside of this channel**. Make sure to include your username and server." + "\n" +
                                "**2.** Proceed to your linked ticket. Our staff team will assist you shortly.\n" +
                                "**3.** Wait patiently! Our staff team is very busy working to provide service to __all__ players.", false);
        TextChannel supportChannel = DirtBot.getJda().getTextChannelById(getConfig().supportChannelID);
        boolean messageFound = false;
        for(Message message : supportChannel.retrievePinnedMessages().complete()) {
            if(message.getEmbeds().size() > 0 && message.getAuthor().isBot() && message.getEmbeds().get(0).getFields().get(0).getName().contains(instructions.getFields().get(0).getName())) {
                message.editMessage(instructions.build()).complete();
                messageFound = true;
            }
        }
        if(!messageFound) {
            supportChannel.sendMessage(instructions.build()).queue((message) -> {
                message.pin().queue();
            });
        }
    }

    private void printTicketNotificationEmbeds() {
        TextChannel notificationSubChannel = DirtBot.getJda().getTextChannelById(getConfig().notificationChannelID);
        for(List<String> serverInfo : DirtBot.getConfig().servers) {
            boolean found = false;
            for(Message message : notificationSubChannel.getIterableHistory().complete()) {
                if(message.getAuthor().isBot() && message.getEmbeds().size() > 0 && message.getEmbeds().get(0).getFields().size() > 0) {
                    if(message.getEmbeds().get(0).getFields().get(0).getName().contains(serverInfo.get(0))) {
                        ticketNotificationEmbeds.put(serverInfo.get(1).toLowerCase(), message.getId());
                        found = true;
                    }
                }
            }
            if(!found) {
                EmbedBuilder notificationEmbed = getEmbedUtils().getEmptyEmbed()
                        .addField("__**Receive " + serverInfo.get(0) + " Notifications**__", "Please react with \uD83D\uDCEC to subscribe to to ticket notifications for **" + serverInfo.get(0) + "**! To unsubscribe, simply click the emote again.\n\n<@&" + DirtBot.getConfig().adminRoleID + "> users will also be pinged when a ticket is escalated to the admin level.", false);
                notificationSubChannel.sendMessage(notificationEmbed.build()).queue((message) -> {
                    message.addReaction("\uD83D\uDCEC").queue();
                    ticketNotificationEmbeds.put(serverInfo.get(1).toLowerCase(), message.getId());
                });
            }
        }
        
        boolean launcher = false;
        //Launcher notification
        for(Message message : notificationSubChannel.getIterableHistory().complete()) {
        	if(message.getAuthor().isBot() && message.getEmbeds().size() > 0 && message.getEmbeds().get(0).getFields().size() > 0) {
        		if(message.getEmbeds().get(0).getFields().get(0).getName().contains("Launcher")) {
                    ticketNotificationEmbeds.put("launcher", message.getId());
                    launcher = true;
        		}
        	}
        }
        if(!launcher) {
            EmbedBuilder notificationEmbed = getEmbedUtils().getEmptyEmbed()
                    .addField("__Receive Launcher Notifications__", "Please react with \uD83D\uDCEC to subscribe to to ticket notifications for **Launcher**! To unsubscribe, simply click the emote again.", false);
            notificationSubChannel.sendMessage(notificationEmbed.build()).queue((message) -> {
                message.addReaction("\uD83D\uDCEC").queue();
                ticketNotificationEmbeds.put("launcher", message.getId());
            });
        }
    }

    private void ticketCreationMessageReceived(MessageReceivedEvent event) {
        // If a staff member is using ignore, don't go any further
        if (event.getMessage().getContentRaw().startsWith(DirtBot.getConfig().botPrefix + "ignore") && event.getMember().getRoles().contains(DirtBot.getJda().getRoleById(DirtBot.getConfig().staffRoleID))) return;

        if (databaseHelper.hasOpenTicket(event.getAuthor().getId()) && !getVerificationDB().isVerified(event.getAuthor().getId())) {
            String verificationChannelID = getVerification().getConfig().verificationChannelID;
            Optional<String> optionalChannelId = databaseHelper.getLastTicketChannelID(event.getAuthor().getId());
            EmbedBuilder response = getEmbedUtils().getErrorEmbed(
                    (optionalChannelId.map(s -> "You already have ticket <#" + s + "> open!")
                            .orElse("You already have a ticket open!")) +
                            "Verify in <#" + verificationChannelID + "> to create more!");

            event.getChannel().sendMessage(response.build()).queue((message) -> message.delete().queueAfter(10, TimeUnit.SECONDS));

            event.getMessage().delete().queue();
            return;
        }

        // Ensure length is within boundaries
        if (event.getMessage().getContentRaw().length() > 1024) {
            EmbedBuilder response = getEmbedUtils().getErrorEmbed(
                    "Ticket length exceeds the maximum length of 1024 characters, and this ticket is **" + event.getMessage().getContentRaw().length() + "**!" +
                            "\nPlease use a short message and expand upon your problem once the ticket has been created.");

            event.getChannel().sendMessage(response.build()).queue((message) -> message.delete().queueAfter(10, TimeUnit.SECONDS));
            event.getMessage().delete().queue();
            return;
        }

        String username = databaseHelper.getUsernameFromDiscordId(event.getAuthor().getId()).orElse(null);

        String reason = EmojiParser.parseToAliases(event.getMessage().getContentRaw().replaceAll("[^a-zA-Z0-9.]", " "), EmojiParser.FitzpatrickAction.REMOVE);
        TextChannel ticketChannel = ticketUtils.createTicket(
                !reason.isEmpty() && reason.length() > 1 ? reason : "N/A",
                event.getMember(), username);
        EmbedBuilder response = getEmbedUtils().getEmptyEmbed()
                .addField("__**Ticket Created**__", "Hello <@" + event.getAuthor().getId() + ">,\nI have created the channel <#" + ticketChannel.getId() + ">. Our staff team will assist you shortly. Thank you for your patience!", false);
        event.getChannel().sendMessage(response.build()).queue((message) -> message.delete().queueAfter(10, TimeUnit.SECONDS));
        event.getMessage().delete().queue();

        String message = "Awaiting ";
        for (PermissionOverride po : event.getTextChannel().getMemberPermissionOverrides()) {
            if (po.getMember() == null) continue;
            if (!po.getMember().getUser().isBot()) {
                message += "<@" + po.getMember().getUser().getId() + ">, ";
            }
        }
        message = message.substring(0, message.length() - 2);
        message += "'s Response...";

        ticketChannel.getManager().setTopic(message).queue();
    }

    private void ticketInternalMessageReceived(MessageReceivedEvent event) {
        if (!getTicketUtils().isTicketChannel(event.getTextChannel())) return;
        // Awaiting Member
        if(event.getMember().getRoles().contains(DirtBot.getJda().getRoleById(DirtBot.getConfig().staffRoleID))) {
            String message = "Awaiting ";
            for(PermissionOverride po : event.getTextChannel().getMemberPermissionOverrides()) {
                if (po.getMember() == null) continue;
                if(!po.getMember().getUser().isBot()) {
                    message += "<@" + po.getMember().getUser().getId() + ">, ";
                }
            }
            message = message.substring(0, message.length() - 2);
            message += "'s Response...";

            event.getTextChannel().getManager().setTopic(message).queueAfter(10, TimeUnit.SECONDS);
        // Awaiting Staff
        } else {
            event.getTextChannel().getManager().setTopic("**Awaiting <@&" + DirtBot.getConfig().staffRoleID + ">'s Response...**").queueAfter(10, TimeUnit.SECONDS);
        }
    }

    private void confirmationReceived(GuildMessageReactionAddEvent event, Message message) {
        switch(event.getReactionEmote().getName()) {
            case "\u2705": //Confirmed
                getDatabaseHelper().getTicket(message.getChannel().getId()).ifPresent(ticket -> {
                    String reason = getDatabaseHelper().getClosureReason(message.getId());
                    getEmbedUtils().sendLog("Closed", "**Reason:** " + reason, ticket, event.getMember());

                    ticketUtils.closeTicket(ticket, reason, true);
                });
                break;
            case "\u274c": // Cancelled
                getDatabaseHelper().removeConfirmationMessage(message.getId());
                message.delete().queue();
                event.getChannel().getHistoryBefore(message.getId(), 1).queue((obj) -> obj.getRetrievedHistory().get(0).delete().queue());
                break;
        }
    }

    private void notificationSubscribed(GuildMessageReactionAddEvent event, Message message) {
        if(message.getEmbeds().size() > 0 && message.getEmbeds().get(0).getFields().size() > 0) {
            String server = message.getEmbeds().get(0).getFields().get(0).getName().substring(12, message.getEmbeds().get(0).getFields().get(0).getName().length() - 18);
            EmbedBuilder subscriptionNotification = getEmbedUtils().getExternalEmbed().addField("__Subscribed__", "You have successfully subscribed to notifications for **" + server + "** tickets!", false);
            event.getUser().openPrivateChannel().queue((privateChannel) -> privateChannel.sendMessage(subscriptionNotification.build()).queue());
        }
    }

    private void notificationUnsubscribed(GuildMessageReactionRemoveEvent event, Message message) {
        if(message.getEmbeds().size() > 0 && message.getEmbeds().get(0).getFields().size() > 0) {
            String server = message.getEmbeds().get(0).getFields().get(0).getName().substring(12, message.getEmbeds().get(0).getFields().get(0).getName().length() - 18);
            EmbedBuilder unsubscriptionNotification = getEmbedUtils().getExternalEmbed().addField("__Unubscribed__", "You have successfully unsubscribed to notifications for **" + server + "** tickets!", false);
            event.getUser().openPrivateChannel().queue((privateChannel) -> privateChannel.sendMessage(unsubscriptionNotification.build()).queue());
        }
    }

    private VerificationDatabaseHelper getVerificationDB() {
        return DirtBot.getModuleRegistry().getModule(VerificationModule.class).getVerificationDatabase();
    }

    private VerificationModule getVerification() {
        return DirtBot.getModuleRegistry().getModule(VerificationModule.class);
    }
}
