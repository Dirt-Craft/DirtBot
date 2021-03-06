package net.dirtcraft.dirtbot.modules;

import com.electronwill.nightconfig.core.ConfigSpec;
import com.electronwill.nightconfig.core.conversion.Path;
import net.dirtcraft.dirtbot.DirtBot;
import net.dirtcraft.dirtbot.commands.punishment.Mute;
import net.dirtcraft.dirtbot.commands.punishment.Unmute;
import net.dirtcraft.dirtbot.internal.configs.ConfigurationManager;
import net.dirtcraft.dirtbot.internal.configs.IConfigData;
import net.dirtcraft.dirtbot.internal.embeds.EmbedUtils;
import net.dirtcraft.dirtbot.internal.modules.Module;
import net.dirtcraft.dirtbot.internal.modules.ModuleClass;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.guild.GuildBanEvent;

import java.sql.*;
import java.time.Instant;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

@ModuleClass (requiresDatabase = true, experimental = true)
public class PunishmentModule extends Module<PunishmentModule.PunishmentConfigData, PunishmentModule.PunishmentEmbedUtils> {
	
	@Override
	public void initialize() {
		setEmbedUtils(new PunishmentModule.PunishmentEmbedUtils());
		
		initializeMutes();
		
		DirtBot.getCoreModule().registerCommands(
				new Mute(this),
				new Unmute(this)
		);
	}

	@Override
	public void initializeConfiguration() {
		ConfigSpec spec = new ConfigSpec();
		
        spec.define("database.url", "jdbc:mariadb://localhost:3306/support");
        spec.define("database.user", "");
        spec.define("database.password", "");
		
		spec.define("discord.channels.punishmentLogChannelID", "635917765160599585"); 
		
		spec.define("discord.roles.mutedRoleID", "589777192024670228"); 
		
        spec.define("discord.embeds.footer", "Created for DirtCraft");
        spec.define("discord.embeds.title", "<:redbulletpoint:539273059631104052> DirtCraft's DirtBOT <:redbulletpoint:539273059631104052>");
        spec.define("discord.embeds.color", 16711680);
		
		setConfig(new ConfigurationManager<>(PunishmentModule.PunishmentConfigData.class, spec, "Punishment"));
	}
	
	public static class PunishmentConfigData implements IConfigData {
        @Path("database.url")
        public String databaseUrl;
        @Path("database.user")
        public String databaseUser;
        @Path("database.password")
        public String databasePassword;
        
		@Path("discord.channels.punishmentLogChannelID")
		public String punishmentLogChannelID;
		
		@Path("discord.roles.mutedRoleID")
		public String mutedRoleID;
		
        @Path("discord.embeds.footer")
        public String embedFooter;
        @Path("discord.embeds.title")
        public String embedTitle;
        @Path("discord.embeds.color")
        public int embedColor;
	}
	
	public void initializeMutes() {
		//Retrieve a list of all mutes from the database
		Timer timer = new Timer();

		timer.schedule( new TimerTask() {
		    public void run() {
		    	try (
					Connection con = DriverManager.getConnection(getConfig().databaseUrl, getConfig().databaseUser, getConfig().databasePassword);
					
					PreparedStatement statement = con.prepareStatement("SELECT * FROM mutes")) {
					try(ResultSet results = statement.executeQuery()) {
						Guild guild = DirtBot.getJda().getGuildById(DirtBot.getConfig().serverID);
						if (guild == null) return;

						while(results.next()) {
							User user = DirtBot.getJda().retrieveUserById(results.getString("discordid")).complete();
							java.util.Date unmuteDate = results.getDate("unmutetime");
							java.util.Date currentDate = new java.util.Date();
							//Check if user is still in server
							if(guild.retrieveMember(user).complete() != null) {
								try (PreparedStatement statementDelete = con.prepareStatement("DELETE FROM mutes WHERE discordid = ?")) {
									statementDelete.setString(1, user.getId());
									statementDelete.executeUpdate();
								}
								continue;
							}
							
							//Check if user is still muted
							Member member = guild.retrieveMember(user).complete();
							if(!member.getRoles().contains(DirtBot.getJda().getRoleById(getConfig().mutedRoleID))) {
								try (PreparedStatement statementDelete = con.prepareStatement("DELETE FROM mutes WHERE discordid = ?")) {
									statementDelete.setString(1, user.getId());
									statementDelete.executeUpdate();
								}
								continue;
							}
							
							//Check if time is up on mute
							if(currentDate.compareTo(unmuteDate) > 0) {
								Role mutedRole = guild.getRoleById(getConfig().mutedRoleID);
								if (mutedRole == null) return;
								guild.removeRoleFromMember(member, mutedRole).queue();
								try (PreparedStatement statementDelete = con.prepareStatement("DELETE FROM mutes WHERE discordid = ?")) {
									statementDelete.setString(1, user.getId());
									statementDelete.executeUpdate();
								}
							}
						}
					}

				} catch (SQLException e) {
		    		e.printStackTrace();
					DirtBot.pokeDevs(e);
				}
		    }
		 }, 60, 60*1000);
	}
	
	@Override
	public void onGuildBan(GuildBanEvent event) {
		event.getGuild().retrieveBan(event.getUser()).queue(ban -> {
			String reason = ban.getReason();
			String punished = ban.getUser().getId();
			
			event.getGuild().retrieveAuditLogs().queue(logs -> {
				for(AuditLogEntry log : logs) {
					if(log.getType() == ActionType.BAN) {
						if(log.getTargetId().equals(punished)) {
							String punisher = log.getUser().getId();
							getEmbedUtils().sendPunishLog(punisher, punished, PunishmentLogType.BAN, null, reason);
						}
					}
				}
			});
		});
	}
	
	public class PunishmentEmbedUtils extends EmbedUtils {
        @Override
        public EmbedBuilder getEmptyEmbed() {
            return new EmbedBuilder()
                    .setTitle(getConfig().embedTitle)
                    .setColor(getConfig().embedColor)
                    .setFooter(getConfig().embedFooter, null)
                    .setTimestamp(Instant.now());
        }
        
        public void sendError(String errorMessage, TextChannel channel) {
        	EmbedBuilder error = getEmptyEmbed().addField("__Error__", errorMessage, false);
        	channel.sendMessage(error.build()).queue((message) -> message.delete().queueAfter(10, TimeUnit.SECONDS));
        }
        
        public void sendPunishLog(String punisherID, String punishedID, PunishmentLogType logType, String length, String reason) {
        	TextChannel punishmentLogChannel = DirtBot.getJda().getTextChannelById(getConfig().punishmentLogChannelID);

        	switch(logType) {
				case BAN:
					EmbedBuilder banLog = getEmptyEmbed().addField("__Punishment Event__",
							"**Punishment Type:** Ban\n" +
									"**Punisher:** <@" + punisherID + ">\n" +
									"**Punished Player:** <@" + punishedID + ">\n" +
									"**Reason:** " + reason, false);
					punishmentLogChannel.sendMessage(banLog.build()).queue();
					break;
				case KICK:
					EmbedBuilder kickLog = getEmptyEmbed().addField("__Punishment Event__",
							"**Punishment Type:** Kick\n" +
									"**Punisher:** <@" + punisherID + ">\n" +
									"**Punished Player:** <@" + punishedID + ">\n" +
									"**Reason:** " + reason, false);
					punishmentLogChannel.sendMessage(kickLog.build()).queue();
					break;
				case MUTE:
					EmbedBuilder muteLog = getEmptyEmbed().addField("__Punishment Event__",
							"**Punishment Type:** Mute\n" +
									"**Punisher:** <@" + punisherID + ">\n" +
									"**Punished Player:** <@" + punishedID + ">\n" +
									"**Duration:** " + length + "\n" +
									"**Reason:** " + reason, false);
					punishmentLogChannel.sendMessage(muteLog.build()).queue();
					break;
				case UNMUTE:
					EmbedBuilder unmuteLog = getEmptyEmbed().addField("__Punishment Event__",
							"**Punishment Type:** Unmute\n" +
									"**Punisher:** <@" + punisherID + ">\n" +
									"**Punished Player:** <@" + punishedID + ">", false);
					punishmentLogChannel.sendMessage(unmuteLog.build()).queue();
					break;
				case CLEARCHATALL:
					EmbedBuilder clearChatAllLog = getEmptyEmbed().addField("__Punishment Event__",
							"**Punishment Type:** Clear All Chat\n" +
									"**Punisher:** <@" + punisherID + ">\n" +
									"**Channel:** <#" + punishedID + ">\n" +
									"**Messages Cleared:** " + length + "\n" +
									"**Reason:** " + reason, false);
					punishmentLogChannel.sendMessage(clearChatAllLog.build()).queue();
					break;
				case CLEARCHATUSER:
					EmbedBuilder clearChatUserLog = getEmptyEmbed().addField("__Punishment Event__",
							"**Punishment Type:** Clear User Chat\n" +
									"**Punisher:** <@" + punisherID + ">\n" +
									"**Punished Player:** <@" + punishedID + ">\n" +
									"**Messages Cleared:** " + length + "\n" +
									"**Reason:** " + reason, false);
					punishmentLogChannel.sendMessage(clearChatUserLog.build()).queue();
					break;
			}
        }
		
	}
	
	public enum PunishmentLogType {
		BAN,
		KICK,
		MUTE,
		UNMUTE,
		CLEARCHATALL,
		CLEARCHATUSER
	}
	

}
