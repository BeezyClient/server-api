package dev.beezy.server.bukkit;

import dev.beezy.server.api.BeezyServerApi;
import dev.beezy.server.api.Feature;
import dev.beezy.server.api.Hello;
import dev.beezy.server.api.Notification;
import dev.beezy.server.api.Waypoint;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

/**
 * {@code /beezy}: help, check, notify, waypoint, reset. Arguments can be quoted ({@code "Bed Wars"}).
 */
final class BeezyCommand implements TabExecutor {

    private static final List<String> SUBCOMMANDS = Arrays.asList("check", "notify", "waypoint", "reset");
    private static final List<String> WAYPOINT_ACTIONS = Arrays.asList("add", "remove", "clear");

    private final BeezyPlugin plugin;

    BeezyCommand(BeezyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] rawArgs) {
        List<String> args = tokenize(rawArgs);
        String sub = args.isEmpty() ? "help" : args.get(0).toLowerCase(Locale.ROOT);
        if (!sub.equals("help") && !SUBCOMMANDS.contains(sub)) {
            sub = "help";
        }
        if (!sender.hasPermission("beezy.command." + sub)) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }
        try {
            switch (sub) {
                case "check":
                    check(sender, args);
                    break;
                case "notify":
                    notify(sender, args);
                    break;
                case "waypoint":
                    waypoint(sender, args, label);
                    break;
                case "reset":
                    reset(sender, args);
                    break;
                default:
                    help(sender, label);
            }
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + e.getMessage());
        }
        return true;
    }

    private void help(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.GOLD + "Beezy " + plugin.getDescription().getVersion() + ChatColor.GRAY + " - shows server features to Beezy Client players");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " check <player>" + ChatColor.GRAY + " - uses Beezy? version, features");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " notify <player|*> <title> <text...>" + ChatColor.GRAY + " - show a notification");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " waypoint <player|*> add <id> <x> <y> <z> [name...]");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " waypoint <player|*> remove <id>" + ChatColor.GRAY + " | " + ChatColor.YELLOW + "clear");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " reset <player|*>" + ChatColor.GRAY + " - clear everything sent");
        sender.sendMessage(ChatColor.GRAY + "Quote arguments with spaces: \"Bed Wars\". In waypoint coordinates ~ means your own.");
    }

    private void check(CommandSender sender, List<String> args) {
        need(args, 2, "check <player>");
        Player target = plugin.getServer().getPlayerExact(args.get(1));
        if (target == null) {
            throw new IllegalArgumentException("Player " + args.get(1) + " is not online.");
        }
        Hello hello = api().helloOf(target.getUniqueId());
        if (hello == null) {
            sender.sendMessage(ChatColor.YELLOW + target.getName() + ChatColor.GRAY + " is not using Beezy (no Hello received).");
            return;
        }
        List<String> on = new ArrayList<String>();
        for (Feature f : hello.features()) {
            on.add(f.name().toLowerCase(Locale.ROOT).replace('_', ' '));
        }
        sender.sendMessage(ChatColor.YELLOW + target.getName() + ChatColor.GREEN + " uses Beezy " + hello.beezyVersion()
                + ChatColor.GRAY + " on Minecraft " + hello.minecraftVersion() + ".");
        sender.sendMessage(ChatColor.GRAY + "Features on: " + (on.isEmpty() ? "none" : join(on, ", ")));
    }

    private void notify(CommandSender sender, List<String> args) {
        need(args, 4, "notify <player|*> <title> <text...>");
        Notification notification = Notification.of(args.get(2), join(args.subList(3, args.size()), " "));
        int sent = 0;
        List<UUID> targets = targets(args.get(1));
        for (UUID id : targets) {
            if (api().sendNotification(id, notification)) {
                sent++;
            }
        }
        report(sender, sent, targets.size());
    }

    private void waypoint(CommandSender sender, List<String> args, String label) {
        need(args, 3, "waypoint <player|*> <add|remove|clear> ...");
        List<UUID> targets = targets(args.get(1));
        String action = args.get(2).toLowerCase(Locale.ROOT);
        int sent = 0;
        if (action.equals("add")) {
            need(args, 7, "waypoint <player|*> add <id> <x> <y> <z> [name...]");
            Location here = sender instanceof Player ? ((Player) sender).getLocation() : null;
            Waypoint.Builder builder = Waypoint.builder(args.get(3))
                    .at(coord(args.get(4), here == null ? null : here.getBlockX()),
                            coord(args.get(5), here == null ? null : here.getBlockY()),
                            coord(args.get(6), here == null ? null : here.getBlockZ()))
                    // A player's waypoint belongs to the world they are in; the console's to any.
                    .dimension(here == null ? "" : Dimensions.of(here.getWorld()));
            if (args.size() > 7) {
                builder.name(join(args.subList(7, args.size()), " "));
            }
            Waypoint w = builder.build();
            for (UUID id : targets) {
                if (api().sendWaypoint(id, w)) {
                    sent++;
                }
            }
        } else if (action.equals("remove")) {
            need(args, 4, "waypoint <player|*> remove <id>");
            for (UUID id : targets) {
                if (api().removeWaypoint(id, args.get(3))) {
                    sent++;
                }
            }
        } else if (action.equals("clear")) {
            for (UUID id : targets) {
                if (api().clearWaypoints(id)) {
                    sent++;
                }
            }
        } else {
            throw new IllegalArgumentException("Usage: /" + label + " waypoint <player|*> <add|remove|clear> ...");
        }
        report(sender, sent, targets.size());
    }

    private void reset(CommandSender sender, List<String> args) {
        need(args, 2, "reset <player|*>");
        List<UUID> targets = targets(args.get(1));
        int sent = 0;
        for (UUID id : targets) {
            if (api().reset(id)) {
                sent++;
            }
        }
        report(sender, sent, targets.size());
    }

    // ---- helpers ---------------------------------------------------------------------------

    private BeezyServerApi api() {
        return plugin.api();
    }

    /** {@code *} is every Beezy player online; a name must be an online Beezy player. */
    private List<UUID> targets(String arg) {
        if (arg.equals("*")) {
            return new ArrayList<UUID>(api().beezyPlayers());
        }
        Player player = plugin.getServer().getPlayerExact(arg);
        if (player == null) {
            throw new IllegalArgumentException("Player " + arg + " is not online.");
        }
        if (!api().isBeezy(player.getUniqueId())) {
            throw new IllegalArgumentException(player.getName() + " is not using Beezy.");
        }
        return Collections.singletonList(player.getUniqueId());
    }

    private static void report(CommandSender sender, int sent, int targets) {
        if (targets == 0) {
            sender.sendMessage(ChatColor.YELLOW + "No Beezy players online.");
        } else if (sent == targets) {
            sender.sendMessage(ChatColor.GREEN + "Sent to " + sent + " Beezy player" + (sent == 1 ? "" : "s") + ".");
        } else {
            sender.sendMessage(ChatColor.YELLOW + "Sent to " + sent + " of " + targets
                    + " Beezy players; the others switched this feature off.");
        }
    }

    private static int coord(String arg, Integer own) {
        if (arg.startsWith("~")) {
            if (own == null) {
                throw new IllegalArgumentException("~ coordinates only work for players.");
            }
            return arg.length() == 1 ? own : own + parseInt(arg.substring(1));
        }
        return parseInt(arg);
    }

    private static int parseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Not a whole number: " + s);
        }
    }

    private static void need(List<String> args, int count, String usage) {
        if (args.size() < count) {
            throw new IllegalArgumentException("Usage: /beezy " + usage);
        }
    }

    private static String join(List<String> parts, String sep) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (sb.length() > 0) {
                sb.append(sep);
            }
            sb.append(p);
        }
        return sb.toString();
    }

    /** Re-splits the arguments, keeping "quoted text" together. */
    static List<String> tokenize(String[] raw) {
        List<String> out = new ArrayList<String>();
        StringBuilder current = null;
        for (String arg : raw) {
            if (current == null) {
                if (arg.startsWith("\"") && !(arg.length() > 1 && arg.endsWith("\""))) {
                    current = new StringBuilder(arg.substring(1));
                } else if (arg.length() > 1 && arg.startsWith("\"") && arg.endsWith("\"")) {
                    out.add(arg.substring(1, arg.length() - 1));
                } else {
                    out.add(arg);
                }
            } else if (arg.endsWith("\"")) {
                out.add(current.append(' ').append(arg, 0, arg.length() - 1).toString());
                current = null;
            } else {
                current.append(' ').append(arg);
            }
        }
        if (current != null) {
            out.add(current.toString()); // unterminated quote: take the rest as is
        }
        return out;
    }

    // ---- tab completion --------------------------------------------------------------------

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> options = new ArrayList<String>();
        if (args.length == 1) {
            for (String sub : SUBCOMMANDS) {
                if (sender.hasPermission("beezy.command." + sub)) {
                    options.add(sub);
                }
            }
        } else if (args.length == 2 && SUBCOMMANDS.contains(args[0].toLowerCase(Locale.ROOT))) {
            if (!args[0].equalsIgnoreCase("check")) {
                options.add("*");
            }
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                if (api().isBeezy(p.getUniqueId()) || args[0].equalsIgnoreCase("check")) {
                    options.add(p.getName());
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("waypoint")) {
            options.addAll(WAYPOINT_ACTIONS);
        } else {
            return Collections.emptyList();
        }
        return StringUtil.copyPartialMatches(args[args.length - 1], options, new ArrayList<String>());
    }
}
