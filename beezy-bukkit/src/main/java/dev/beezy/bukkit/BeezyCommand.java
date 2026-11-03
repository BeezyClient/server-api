package dev.beezy.bukkit;

import dev.beezy.api.BeezyAudience;
import dev.beezy.api.BeezyClientApi;
import dev.beezy.api.BeezyUser;
import dev.beezy.api.DeliveryReport;
import dev.beezy.api.Feature;
import dev.beezy.api.SendResult;
import dev.beezy.api.display.Beam;
import dev.beezy.api.display.Notification;
import dev.beezy.api.display.Waypoint;
import dev.beezy.api.exception.BeezyException;
import dev.beezy.api.exception.FeatureUnavailableException;
import dev.beezy.api.exception.RateLimitedException;
import dev.beezy.api.exception.TransferTimeoutException;
import dev.beezy.api.transfer.TransferResult;
import dev.beezy.api.value.BlockPosition;
import dev.beezy.api.value.Color;
import dev.beezy.api.value.WorldKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletionException;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.StringUtil;

/**
 * {@code /beezy}: help, check, notify, waypoint, beam, transfer, reset, all built on the public
 * API. Arguments can be quoted ({@code "Bed Wars"}); {@code *} is every Beezy player online.
 */
final class BeezyCommand implements TabExecutor {

    private static final List<String> SUBCOMMANDS = Arrays.asList("check", "notify", "waypoint", "beam", "transfer", "reset");
    private static final List<String> MARKER_ACTIONS = Arrays.asList("add", "remove", "clear");

    private final Plugin plugin;
    private final BeezyClientApi api;

    BeezyCommand(Plugin plugin, BeezyClientApi api) {
        this.plugin = plugin;
        this.api = api;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] rawArgs) {
        List<String> args = tokenize(rawArgs);
        String sub = args.isEmpty() ? "help" : args.get(0).toLowerCase(Locale.ROOT);
        if (!SUBCOMMANDS.contains(sub)) {
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
                    marker(sender, args, label, true);
                    break;
                case "beam":
                    marker(sender, args, label, false);
                    break;
                case "transfer":
                    transfer(sender, args);
                    break;
                case "reset":
                    need(args, 2, "reset <player|*>");
                    report(sender, targets(args.get(1)).reset());
                    break;
                default:
                    help(sender, label);
            }
        } catch (IllegalArgumentException | BeezyException e) {
            sender.sendMessage(ChatColor.RED + e.getMessage());
        }
        return true;
    }

    private void help(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.GOLD + "Beezy " + plugin.getDescription().getVersion() + ChatColor.GRAY + " - server features for Beezy Client players");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " check <player>" + ChatColor.GRAY + " - uses Beezy? version, features");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " notify <player|*> <title> <text...>" + ChatColor.GRAY + " - show a notification");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " waypoint <player|*> add <id> <x> <y> <z> [name...]");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " waypoint <player|*> remove <id>" + ChatColor.GRAY + " | " + ChatColor.YELLOW + "clear");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " beam <player|*> add <id> <x> <y> <z> [#rrggbb|color]" + ChatColor.GRAY + " | "
                + ChatColor.YELLOW + "remove <id>" + ChatColor.GRAY + " | " + ChatColor.YELLOW + "clear");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " transfer <player|*> <host[:port]>" + ChatColor.GRAY + " - send to another server");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " reset <player|*>" + ChatColor.GRAY + " - clear everything sent");
        sender.sendMessage(ChatColor.GRAY + "Quote arguments with spaces: \"Bed Wars\". In coordinates ~ means your own.");
    }

    private void check(CommandSender sender, List<String> args) {
        need(args, 2, "check <player>");
        Player target = online(args.get(1));
        BeezyUser user = api.user(target.getUniqueId()).orElse(null);
        if (user == null) {
            sender.sendMessage(ChatColor.YELLOW + target.getName() + ChatColor.GRAY + " is not using Beezy (no Hello received).");
            return;
        }
        List<String> on = new ArrayList<String>();
        for (Feature f : user.features()) {
            on.add(f.name().toLowerCase(Locale.ROOT).replace('_', ' '));
        }
        sender.sendMessage(ChatColor.YELLOW + target.getName() + ChatColor.GREEN + " uses Beezy " + user.client().beezyVersion()
                + ChatColor.GRAY + " on Minecraft " + user.client().minecraftVersion() + ".");
        sender.sendMessage(ChatColor.GRAY + "Features on: " + (on.isEmpty() ? "none" : join(on, ", ")));
    }

    private void notify(CommandSender sender, List<String> args) {
        need(args, 4, "notify <player|*> <title> <text...>");
        Notification n = Notification.of(args.get(2), join(args.subList(3, args.size()), " "));
        report(sender, targets(args.get(1)).notify(n));
    }

    private void marker(CommandSender sender, List<String> args, String label, boolean waypoint) {
        String kind = waypoint ? "waypoint" : "beam";
        need(args, 3, kind + " <player|*> <add|remove|clear> ...");
        BeezyAudience targets = targets(args.get(1));
        String action = args.get(2).toLowerCase(Locale.ROOT);
        if (action.equals("add")) {
            need(args, 7, waypoint ? "waypoint <player|*> add <id> <x> <y> <z> [name...]" : "beam <player|*> add <id> <x> <y> <z> [#rrggbb|color]");
            Location here = sender instanceof Player ? ((Player) sender).getLocation() : null;
            BlockPosition at = coords(args, 4, here);
            // A player's marker belongs to the world they are in; the console's to any.
            WorldKey world = here == null ? null : BeezyBukkit.world(here.getWorld());
            if (waypoint) {
                Waypoint.Builder b = Waypoint.builder(args.get(3)).at(at).world(world);
                if (args.size() > 7) {
                    b.name(join(args.subList(7, args.size()), " "));
                }
                report(sender, targets.showWaypoint(b.build()).report());
            } else {
                Beam beam = Beam.builder(args.get(3)).at(at).world(world)
                        .color(args.size() > 7 ? Color.parse(args.get(7)) : null).build();
                report(sender, targets.showBeam(beam).report());
            }
        } else if (action.equals("remove")) {
            need(args, 4, kind + " <player|*> remove <id>");
            final String id = args.get(3);
            report(sender, targets.sendEach(u -> waypoint ? u.waypoints().remove(id) : u.beams().remove(id)));
        } else if (action.equals("clear")) {
            report(sender, targets.sendEach(u -> waypoint ? u.waypoints().clear() : u.beams().clear()));
        } else {
            throw new IllegalArgumentException("Usage: /" + label + " " + kind + " <player|*> <add|remove|clear> ...");
        }
    }

    private void transfer(final CommandSender sender, List<String> args) {
        need(args, 3, "transfer <player|*> <host[:port]>");
        BeezyAudience targets = targets(args.get(1));
        String address = args.get(2);
        int count = 0;
        for (final BeezyUser user : targets) {
            count++;
            final String who = ChatColor.YELLOW + user.name().orElse(user.uniqueId().toString()) + ChatColor.GRAY;
            user.transfer(address).whenComplete((result, error) -> {
                if (sender instanceof Player && !((Player) sender).isOnline()) {
                    return;
                }
                sender.sendMessage(error != null ? who + ": " + failure(error) : who + describe(result));
            });
        }
        if (count == 0) {
            sender.sendMessage(ChatColor.YELLOW + "No Beezy players online.");
        }
    }

    private static String describe(TransferResult result) {
        switch (result.status()) {
            case CONNECTING:
                return " is connecting to " + result.address() + ".";
            case REFUSED:
                return " has transfers switched off.";
            default:
                return ChatColor.RED + ": invalid address, or already transferred in the last minute.";
        }
    }

    private static String failure(Throwable error) {
        Throwable e = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
        if (e instanceof FeatureUnavailableException) {
            return "transfers are switched off.";
        }
        if (e instanceof RateLimitedException) {
            return ChatColor.RED + "at most one transfer a minute.";
        }
        if (e instanceof TransferTimeoutException) {
            return ChatColor.RED + "no answer from the client.";
        }
        return ChatColor.RED + String.valueOf(e.getMessage());
    }

    // ---- helpers -----------------------------------------------------------------------------------

    /** {@code *} is every Beezy player online; a name must be an online Beezy player. */
    private BeezyAudience targets(String arg) {
        if (arg.equals("*")) {
            return api.all();
        }
        Player player = online(arg);
        BeezyUser user = api.user(player.getUniqueId()).orElse(null);
        if (user == null) {
            throw new IllegalArgumentException(player.getName() + " is not using Beezy.");
        }
        return BeezyAudience.of(user);
    }

    private Player online(String name) {
        Player player = plugin.getServer().getPlayerExact(name);
        if (player == null) {
            throw new IllegalArgumentException("Player " + name + " is not online.");
        }
        return player;
    }

    private static void report(CommandSender sender, DeliveryReport report) {
        int sent = report.sent();
        int total = report.total();
        if (total == 0) {
            sender.sendMessage(ChatColor.YELLOW + "No Beezy players online.");
        } else if (sent == total) {
            sender.sendMessage(ChatColor.GREEN + "Sent to " + sent + " Beezy player" + (sent == 1 ? "" : "s") + ".");
        } else {
            List<String> why = new ArrayList<String>();
            for (SendResult r : report.counts().keySet()) {
                if (!r.isSent()) {
                    why.add(r.name().toLowerCase(Locale.ROOT).replace('_', ' ') + ": " + report.count(r));
                }
            }
            sender.sendMessage(ChatColor.YELLOW + "Sent to " + sent + " of " + total + " Beezy players (" + join(why, ", ") + ").");
        }
    }

    /** Three block coordinates from {@code args[from..from+2]}; {@code ~} means the sender's own. */
    private static BlockPosition coords(List<String> args, int from, Location here) {
        return BlockPosition.of(
                coord(args.get(from), here == null ? null : here.getBlockX()),
                coord(args.get(from + 1), here == null ? null : here.getBlockY()),
                coord(args.get(from + 2), here == null ? null : here.getBlockZ()));
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
            boolean check = args[0].equalsIgnoreCase("check");
            if (!check) {
                options.add("*");
            }
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                if (check || api.isBeezy(p.getUniqueId())) {
                    options.add(p.getName());
                }
            }
        } else if (args.length == 3 && (args[0].equalsIgnoreCase("waypoint") || args[0].equalsIgnoreCase("beam"))) {
            options.addAll(MARKER_ACTIONS);
        } else {
            return Collections.emptyList();
        }
        return StringUtil.copyPartialMatches(args[args.length - 1], options, new ArrayList<String>());
    }
}
