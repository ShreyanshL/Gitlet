package gitlet;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * Driver class for Gitlet, the tiny stupid version-control system.
 *
 * @author Shreyansh Loharuka
 */
public class Main {

    /**
     * Array of possible valid commands.
     */
    private static String[] commands1 = new String[]{"init", "add"};
    /**
     * Array of possible valid commands.
     */
    private static String[] commands2 = new String[]{"commit", "rm", "log",
        "global-log", "find"};
    /**
     * Array of possible valid commands.
     */
    private static String[] commands3 = new String[]{"status", "checkout",
        "branch",
        "rm-branch", "reset", "merge"};

    /**
     * Usage: java gitlet.Main ARGS, where ARGS contains
     * <COMMAND> <OPERAND> ....
     */
    public static void main(String... args) {
        try {
            int pos = 0;
            if (args.length == 0) {
                Utils.message("Please enter a command.");
                throw new GitletException();
            } else if (args[0].equals("java")
                    && args[1].equals("gitlet.Main")) {
                if (args.length == 2) {
                    Utils.message("Please enter a command.");
                    throw new GitletException();
                }
                pos = 2;
            }
            if (Arrays.asList(commands1).contains(args[pos])
                    || Arrays.asList(commands2).contains(args[pos])
                    || Arrays.asList(commands3).contains(args[pos])) {
                String action = args[pos];
                Commands cmd = new Commands();
                if (!action.equals("init") && !initialised()) {
                    Utils.message("Not in an initialized Gitlet directory.");
                    throw new GitletException();
                }
                if (Arrays.asList(commands2).contains(args[pos])) {
                    secondSetOfCommands(action, cmd, args, pos);
                    return;
                }
                if (Arrays.asList(commands3).contains(args[pos])) {
                    thirdSetOfCommands(action, cmd, args, pos);
                    return;
                }
                switch (action) {
                case "init":
                    if (args.length != pos + 1) {
                        Utils.message("Incorrect operands.");
                        throw new GitletException();
                    }
                    cmd.init();
                    break;
                case "add":
                    if (args.length != pos + 2) {
                        Utils.message("Incorrect operands.");
                        throw new GitletException();
                    }
                    cmd.add(args[pos + 1]);
                    break;

                default:
                    secondSetOfCommands(action, cmd, args, pos);
                }
            } else {
                Utils.message("No command with that name exists.");
                throw new GitletException();
            }
        } catch (GitletException | IOException excep) {
            System.exit(0);
        }
    }

    /**
     * Returns if .gitlet initialised.
     */
    public static boolean initialised() {
        File workingDir = new File(System.getProperty("user.dir"));
        File initialised = new File(workingDir, ".gitlet");
        if (initialised.exists()) {
            return true;
        }
        return false;
    }

    /**
     * Takes in an ACTION,CMD,ARGS,POS and performs it.
     */
    public static void secondSetOfCommands(String action, Commands cmd,
                                           String[] args,
                                           int pos) throws IOException {
        switch (action) {
        case "commit":
            if (args.length != pos + 2) {
                Utils.message("Incorrect operands.");
                throw new GitletException();
            }
            if (args[pos + 1].trim().equals("")) {
                Utils.message("Please enter a commit message.");
                throw new GitletException();
            }
            cmd.commit(args[pos + 1], "");
            break;
        case "rm":
            if (args.length != pos + 2) {
                Utils.message("Incorrect operands.");
                throw new GitletException();
            }
            cmd.rm(args[pos + 1]);
            break;
        case "log":
            if (args.length != pos + 1) {
                Utils.message("Incorrect operands.");
                throw new GitletException();
            }
            cmd.log();
            break;
        case "global-log":
            if (args.length != pos + 1) {
                Utils.message("Incorrect operands.");
                throw new GitletException();
            }
            cmd.globalLog();
            break;
        case "find":
            if (args.length != pos + 2) {
                Utils.message("Incorrect operands.");
                throw new GitletException();
            }
            cmd.find(args[pos + 1]);
            break;

        default:
            Utils.message("No command with that name exists.");
            throw new GitletException();
        }
    }

    /**
     * Takes in an ACTION,CMD,ARGS,POS and performs it.
     */
    public static void thirdSetOfCommands(String action, Commands cmd,
                                          String[] args,
                                          int pos) throws IOException {
        switch (action) {
        case "status":
            if (args.length != pos + 1) {
                Utils.message("Incorrect operands.");
                throw new GitletException();
            }
            cmd.status();
            break;
        case "checkout":
            if (args.length - 1 - pos == 3 || args.length - 1 - pos == 2) {
                cmd.checkout(Arrays.copyOfRange(args, pos + 1, args.length));
            } else if (args.length - 1 - pos == 1) {
                cmd.checkoutBranch(args[pos + 1]);
            } else {
                Utils.message("Incorrect operands.");
                throw new GitletException();
            }
            break;
        case "branch":
            if (args.length - 1 - pos != 1) {
                Utils.message("Incorrect operands.");
                throw new GitletException();
            }
            cmd.branch(args[pos + 1]);
            break;
        case "rm-branch":
            if (args.length != pos + 2) {
                Utils.message("Incorrect operands.");
                throw new GitletException();
            }
            cmd.rmbranch(args[pos + 1]);
            break;
        case "reset":
            if (args.length != pos + 2) {
                Utils.message("Incorrect operands.");
                throw new GitletException();
            }
            cmd.reset(args[pos + 1]);
            break;
        case "merge":
            if (args.length != pos + 2) {
                Utils.message("Incorrect operands.");
                throw new GitletException();
            }
            cmd.merge(args[pos + 1]);
            break;
        default:
            Utils.message("No command with that name exists.");
            throw new GitletException();
        }
    }
}
