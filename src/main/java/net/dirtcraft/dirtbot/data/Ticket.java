package net.dirtcraft.dirtbot.data;

public class Ticket {

    private int id;
    private boolean open;
    private String message;
    private String username;
    private String server;
    private String channel;
    private Level level;
    private String discordid;

    public Ticket(int id, boolean open, String message, String username, String server, String channel, Level level, String discordid) {
        this.id = id;
        this.open = open;
        this.message = message;
        this.username = username;
        this.server = server;
        this.channel = channel;
        this.level = level;
        this.discordid = discordid;
    }

    public Ticket(String message, String discordID, String username) {
        this(0, true, message, username, null, null, Level.NORMAL, discordID);
    }

    public int getId() { return id; }
    public boolean getOpen() { return open; }
    public String getMessage() { return message; }
    public String getUsername(boolean nullable) {
        return nullable ? username : (username != null ? username : "");
    }
    public String getServer(boolean nullable) {
        return nullable ? server : (server != null ? server : "");
    }
    public String getChannel() { return channel; }
    public Level getLevel() { return level; }

    public String getDiscordID(boolean nullable) {
        return nullable ? discordid : (discordid != null ? discordid : "");
    }

    public void setId(int id) {this.id = id;}
    public void setOpen(boolean open) { this.open = open; }

    public boolean setUsername(String username) {
        if(username == null || username.toLowerCase().equals("null")) {
            this.username = null;
            return true;
        }
        if(username.length() <= 16 && username.length() >= 3) {
            this.username = username;
            return true;
        } else return false;
    }

    public boolean setServer(String server) {
        if(server == null || server.toLowerCase().equals("null")) {
            this.server = null;
            return true;
        } else if(server.length() <= 8 ) {
            this.server = server;
            return true;
        } else return false;
    }

    public boolean setChannel(String channel) {
        if(channel == null || channel.toLowerCase().equals("null")) {
            this.channel = null;
            return true;
        } else if(channel.length() >= 18) {
            this.channel = channel;
            return true;
        } else return false;
    }

    public void setLevel(Level level) { this.level = level; }
    public void setDiscordID(String DiscordID) { this.discordid = DiscordID; }

    public enum Level {
        NORMAL,
        ADMIN,
        URGENT,
        OWNER
    }

}
