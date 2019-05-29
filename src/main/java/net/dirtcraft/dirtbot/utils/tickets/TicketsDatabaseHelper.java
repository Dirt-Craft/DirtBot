package net.dirtcraft.dirtbot.utils.tickets;

import net.dirtcraft.dirtbot.data.Ticket;
import net.dirtcraft.dirtbot.modules.TicketModule;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TicketsDatabaseHelper {

    private TicketModule module;

    public TicketsDatabaseHelper(TicketModule module) {
        this.module = module;
    }

    public Connection getDatabaseConnection() throws SQLException {
        return DriverManager.getConnection(module.getConfig().databaseUrl, module.getConfig().databaseUser, module.getConfig().databasePassword);
    }

    /**
     * Inserts the given ticket into the database and returns the ticket populated with auto-fill fields.
     *
     * @param ticket A ticket populated with the desired data to be inserted into the database
     * @return The given ticket with the ID populated
     */
    public Ticket createTicket(Ticket ticket) {
        try {
            // Establish Database Connection
            Connection con = getDatabaseConnection();

            // Prepare Query
            PreparedStatement statement = con.prepareStatement("INSERT INTO tickets (open, message) VALUES (?, ?)");
            statement.setBoolean(1, ticket.getOpen());
            statement.setString(2, ticket.getMessage());

            // Create Ticket
            statement.execute();
            statement.close();

            // Get latest ticket
            PreparedStatement statement2 = con.prepareStatement("SELECT LAST_INSERT_ID()");
            ResultSet results = statement2.executeQuery();

            if (results.next()) {
                ticket.setId(results.getInt(1));
                results.close();
                statement2.close();
                con.close();
                return ticket;
            } else {
                // TODO Ticket Not Found After Insertion
            }
        } catch (SQLException e) {
            // TODO Exception Handling
        }
        return null;
    }

    /**
     * Gets a ticket from the database based on row id.
     *
     * @param id Row id of the ticket in the database
     * @return The fetched ticket
     */
    public Ticket getTicket(int id) {
        try {
            // Establish Database Connection
            Connection con = getDatabaseConnection();

            // Prepare Query
            PreparedStatement statement = con.prepareStatement("SELECT * FROM tickets WHERE id=?");
            statement.setInt(1, id);

            // Execute Query
            ResultSet results = statement.executeQuery();
            if(results.next()) {
                return new Ticket(results.getInt("id"), true, results.getString("message"), results.getString("username"), results.getString("server"), results.getString("channel"), Ticket.Level.valueOf(results.getString("level").toUpperCase()));
            } else {
                // TODO Something Went Wrong (No Ticket Found)
            }

            // Clean Up
            results.close();
            statement.close();
            con.close();
        } catch (SQLException e) {
            // TODO Exception Handling
        }
        return null;
    }

    /**
     * Gets a ticket from the database based on discord channel id.
     *
     * @param channel The discord channel id
     * @return The fetched ticket
     */
    public Ticket getTicket(String channel) {
        try {
            // Establish Database Connection
            Connection con = getDatabaseConnection();

            // Prepare Query
            PreparedStatement statement = con.prepareStatement("SELECT * FROM tickets WHERE channel=?");
            statement.setString(1, channel);

            // Execute Query
            ResultSet results = statement.executeQuery();
            if(results.next()) {
                return new Ticket(results.getInt("id"), true, results.getString("message"), results.getString("username"), results.getString("server"), results.getString("channel"), Ticket.Level.valueOf(results.getString("level").toUpperCase()));
            } else {
                // TODO Something Went Wrong (No Ticket Found)
            }

            // Clean Up
            results.close();
            statement.close();
            con.close();
        } catch (SQLException e) {
            // TODO Exception Handling
        }
        return null;
    }

    /**
     * Gets a list of tickets that have not yet been assigned channels from the database.
     *
     * @return A list of unassigned tickets
     */
    public List<Ticket> getGameTickets() {
        try {
            // Establish Database Connection
            Connection con = getDatabaseConnection();

            // Prepare Query
            PreparedStatement statement = con.prepareStatement("SELECT * FROM tickets WHERE open=1 AND channel IS NULL");

            // Fetch Results
            ResultSet results = statement.executeQuery();

            List<Ticket> tickets = new ArrayList<>();
            while (results.next()) {
                tickets.add(new Ticket(results.getInt("id"), true, results.getString("message"), results.getString("username"), results.getString("server"), results.getString("channel"), Ticket.Level.valueOf(results.getString("level"))));
            }

            // Clean Up
            results.close();
            statement.close();
            con.close();

            return tickets;
        } catch (SQLException e) {
            // TODO Exception Handling
        }
        return null;
    }

    /**
     * Updates a ticket in the database.
     *
     * @param ticket The ticket with updated data
     */
    public void modifyTicket(Ticket ticket) {
        try {
            // Establish Database Connection
            Connection con = getDatabaseConnection();

            // Prepare Query
            PreparedStatement statement = con.prepareStatement("UPDATE tickets SET open=?, username=?, server=?, channel=?, level=? WHERE id=?");
            statement.setBoolean(1, ticket.getOpen());
            statement.setString(2, ticket.getUsername(true));
            statement.setString(3, ticket.getServer(true));
            statement.setString(4, ticket.getChannel());
            statement.setString(5, ticket.getLevel().toString().toLowerCase());
            statement.setInt(6, ticket.getId());

            // Push the Changes & Clean Up
            if (!statement.execute()) {
                // TODO Something went wrong
            }
            statement.close();
            con.close();
        } catch (SQLException e) {
            // TODO Exception Handling
        }
    }

    public void addConfirmationMessage(int ticketId, String confirmationMessageId, String reason) {
        try {
            // Establish Database Connection
            Connection con = getDatabaseConnection();

            // Prepare Query
            PreparedStatement statement = con.prepareStatement("INSERT INTO closes (ticketid, confirmationid, reason) VALUES (?, ?, ?)");
            statement.setInt(1, ticketId);
            statement.setString(2, confirmationMessageId);
            statement.setString(3, reason);

            // Execute Query & Clean Up
            if (!statement.execute()) {
                // TODO Something went wrong
            }
            statement.close();
            con.close();
        } catch (SQLException e) {
            // TODO Exception Handling
        }
    }

    public void addConfirmationMessage(int ticketId, String confirmationMessageId, String reason, String closeTime) {
        try {
            // Establish Database Connection
            Connection con = getDatabaseConnection();

            // Prepare Query
            PreparedStatement statement = con.prepareStatement("INSERT INTO closes (ticketid, confirmationid, reason, closetime) VALUES (?, ?, ?, ?)");
            statement.setInt(1, ticketId);
            statement.setString(2, confirmationMessageId);
            statement.setString(3, reason);
            statement.setString(4, closeTime);

            // Execute Query & Clean Up
            if (!statement.execute()) {
                // TODO Something went wrong
            }
            statement.close();
            con.close();
        } catch (SQLException e) {
            // TODO Exception Handling
        }
    }

    public HashMap<Integer, String> getAllTimedCloses() {
        HashMap<Integer, String> timedCloses = new HashMap<>();
        try {
            //Establish Database Connection
            Connection con = getDatabaseConnection();

            // Prepare Query
            PreparedStatement statement = con.prepareStatement("SELECT * FROM closes WHERE closetime IS NOT NULL");

            // Fetch Results
            ResultSet results = statement.executeQuery();

            while (results.next()) {
                timedCloses.put(results.getInt("ticketid"), results.getString("closetime"));
            }

            // Clean Up
            results.close();
            statement.close();
            con.close();
        } catch (SQLException e) {
            // TODO Exception Handling
        }
        return timedCloses;
    }

    public void removeConfirmationMessage(String confirmationMessageId) {
        try {
            // Establish Database Connection
            Connection con = getDatabaseConnection();

            // Prepare Query
            PreparedStatement statement = con.prepareStatement("DELETE FROM closes WHERE confirmationid=?");
            statement.setString(1, confirmationMessageId);

            // Execute Query & Clean Up
            if (!statement.execute()) {
                // TODO Something went wrong
            }
            statement.close();
            con.close();
        } catch (SQLException e) {
            // TODO Exception Handling
        }
    }

    public void removeAllConfirmationMessages(int ticketId) {
        try {
            // Establish Database Connection
            Connection con = getDatabaseConnection();

            // Prepare Query
            PreparedStatement statement = con.prepareStatement("DELETE FROM closes WHERE ticketid=?");
            statement.setInt(1, ticketId);

            // Execute Query & Clean Up
            if (!statement.execute()) {
                // TODO Something went wrong
            }
            statement.close();
            con.close();
        } catch (SQLException e) {
            // TODO Exception Handling
        }
    }

    public void removeAllConfirmationMessages(String confirmationMessageId) {
        try {
            // Establish Database Connection
            Connection con = getDatabaseConnection();

            // Prepare Query
            PreparedStatement statement = con.prepareStatement("SELECT * FROM closes WHERE confirmationid=? LIMIT 1");
            statement.setString(1, confirmationMessageId);

            // Execute Query
            ResultSet results = statement.executeQuery();
            if (results.next()) {
                removeAllConfirmationMessages(results.getInt("ticketid"));
            }

            // Clean Up
            results.close();
            statement.close();
            con.close();
        } catch (SQLException e) {
            // TODO Exception Handling
        }
    }

    public boolean isConfirmationMessage(String confirmationMessageId) {
        try {
            // Establish Database Connection
            Connection con = getDatabaseConnection();

            // Prepare Query
            PreparedStatement statement = con.prepareStatement("SELECT * FROM closes WHERE confirmationid=?");
            statement.setString(1, confirmationMessageId);

            // Execute Query
            ResultSet results = statement.executeQuery();
            if (results.next()) return true;

            // Clean Up
            results.close();
            statement.close();
            con.close();
        } catch (SQLException e) {
            // TODO Exception Handling
        }
        return false;
    }

    public String getClosureReason(String confirmationMessageId) {
        try {
            // Establish Database Connection
            Connection con = getDatabaseConnection();

            // Prepare Query
            PreparedStatement statement = con.prepareStatement("SELECT * FROM closes WHERE confirmationid=?");
            statement.setString(1, confirmationMessageId);

            // Execute Query
            ResultSet results = statement.executeQuery();
            String reason = "No reason found.";
            if (results.next()) reason = results.getString("reason");

            // Clean Up
            results.close();
            statement.close();
            con.close();

            return reason;
        } catch (SQLException e) {
            // TODO Exception Handling
        }
        return "No reason found.";
    }
}
