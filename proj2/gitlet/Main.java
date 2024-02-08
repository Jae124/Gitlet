package gitlet;

import java.io.IOException;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author TODO
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) throws IOException {

        try {
            if (args.length == 0) {
                throw Utils.error("Please enter a command.");
            }
            String firstArg = args[0];

            if (firstArg.equals("init")) {
                Repository.initCommand();
                System.exit(0);
            }

            if (!Repository.GITLET_DIR.exists()) {
                throw Utils.error("Not in an initialized Gitlet directory.");
            }

            switch (firstArg) {
                case "init":
                    Repository.initCommand();
                    break;
                case "add":
                    if (args.length < 2) {
                        throw Utils.error("Filename needed.");
                    }
                    Repository.add(args[1]);
                    break;
                case "commit":
                    if (args.length != 2 || args[1].equals("")) {
                        throw Utils.error("Please enter a commit message.");
                    }

                    Commit parentCommit = Utils.readObject(Repository.HEAD, Commit.class);
                    Repository.generalCommit(args[1], Utils.sha1(Utils.serialize(parentCommit)), null);
                    break;
                case "rm":
                    if (args.length < 2) {
                        throw Utils.error("Filename needed.");
                    }
                    Repository.remove(args[1]);
                    break;
                case "log":
                    Repository.log(false);
                    break;
                case "global-log":
                    Repository.log(true);
                    break;
                case "find":
                    if (args.length != 2) {
                        throw Utils.error("Please enter a message.");
                    }
                    Repository.find(args[1]);
                    break;
                case "status":
                    Repository.status();
                    break;
                case "checkout":
                    Repository.checkout(args);
                    break;
                case "branch":
                    Repository.branch(args[1]);
                    break;
                case "rm-branch":
                    Repository.rmBranch(args[1]);
                    break;
                case "reset":
                    Repository.reset(args[1]);
                    break;
                case "merge":
                    Repository.merge(args[1]);
                    break;
                default:
                    throw Utils.error("No command with that name exists.");
            }
        } catch (GitletException e) {
            System.out.println(e.getMessage());
            System.exit(0);
        }
    }
}
