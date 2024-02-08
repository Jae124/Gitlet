package gitlet;

import java.io.File;
import static gitlet.Utils.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

/** Represents a gitlet repository.
 *  does at a high level.
 *
 *  @author Jae Won Kim
 */
public class Repository {
    /**
     *
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    /** Staging Area directory */
    public static final File STAGING = join(GITLET_DIR, "staging");
    /** Blob hash directory */
    public static final File BLOB = join(GITLET_DIR, "blob");
    /** Commit director */
    public static final File COMMIT = join(GITLET_DIR, "commit");
    /** Remove directory */
    public static final File REMOVE = join(GITLET_DIR, "remove");
    /** Branches directory */
    public static final File BRANCH = join(GITLET_DIR, "branch");
    /** Untracked files directory */
    public static final File UNTRACTED = join(GITLET_DIR, "untracked");

    /** The HEAD */
    public static final File HEAD = join(GITLET_DIR, "head");

    /** The blob hash */
    private static HashMap<String, byte[]> blobHash;
    /** Hash of branches */
    private static HashMap<String, String> branchHash;
    /** Hash of untracked */
    private static HashMap<String, String> untrackHash;

    public static void initCommand() {
        if (GITLET_DIR.exists()) {
            throw Utils.error("A Gitlet version-control system "
                    + "already exists in the current directory.");
        } else {
            GITLET_DIR.mkdir();
            COMMIT.mkdir();
            STAGING.mkdir();
            REMOVE.mkdir();
            Commit initialCommit = new Commit("initial commit", null, null);
            String id = sha1(Utils.serialize(initialCommit));
            File initialCommitFile = join(COMMIT, id);
            Utils.writeObject(initialCommitFile, initialCommit);
            Utils.writeObject(HEAD, initialCommit);

            blobHash = new HashMap<>();
            Utils.writeObject(BLOB, blobHash);

            branchHash = new HashMap<>();
            branchHash.put("master", id);
            branchHash.put("currHead", "master");
            Utils.writeObject(BRANCH, branchHash);

            untrackHash = new HashMap<>();
            for (String files: CWD.list()) {
                untrackHash.put(files, "hi" + files);
            }
            Utils.writeObject(UNTRACTED, untrackHash);
        }
    }

    public static void generalCommit(String message, String parent1, String parent2) {
        if (STAGING.list().length == 0 && REMOVE.list().length == 0) {
            throw Utils.error("No changes added to the commit.");
        }

        branchHash = (HashMap<String, String>) Utils.readObject(BRANCH, HashMap.class);

        Commit newCommit = new Commit(message, parent1, parent2);
        HashMap<String, String> trackFiles = newCommit.getTrackFiles();

        Commit parentCommit = Utils.readObject(HEAD, Commit.class);
        HashMap<String, String> parentFiles = parentCommit.getTrackFiles();

        if (parentCommit.getTrackFiles() != null) {
            for (String rm : REMOVE.list()) {
                parentFiles.remove(rm);
            }
            for (Map.Entry<String, String> entry : parentFiles.entrySet()) {
                trackFiles.put(entry.getKey(), entry.getValue());
            }
        }

        blobHash = (HashMap<String, byte[]>) Utils.readObject(Repository.BLOB, HashMap.class);

        for (String fileName: STAGING.list()) {
            byte[] blob = readContents(join(STAGING, fileName));
            String stringBlob = fileName + readContentsAsString(join(STAGING, fileName));
            String blobID = sha1(stringBlob);

            blobHash.put(blobID, blob);
            trackFiles.put(fileName, blobID);
            join(STAGING, fileName).delete();
        }

        for (String fileName: REMOVE.list()) {
            join(REMOVE, fileName).delete();
        }

        Utils.writeObject(BLOB, blobHash);
        String id = sha1(Utils.serialize(newCommit));
        File commitFile = join(COMMIT, id);
        Utils.writeObject(commitFile, newCommit);
        Utils.writeObject(HEAD, newCommit);
        branchHash.put(branchHash.get("currHead"), id);
        Utils.writeObject(BRANCH, branchHash);
    }

    public static void add(String fileName) {
        File fileDirectory = join(CWD, fileName);
        if (!fileDirectory.exists()) {
            throw Utils.error("File does not exist.");
        }

        Commit currCommit = Utils.readObject(HEAD, Commit.class);
        File stagingFile = join(STAGING, fileName);
        File removeFile = join(REMOVE, fileName);
        String haha = sha1(fileName + Utils.readContentsAsString(fileDirectory));

        if (currCommit.getTrackFiles().containsKey(fileName)
                && currCommit.getTrackFiles().get(fileName).equals(haha)) {
            if (stagingFile.exists()) {
                stagingFile.delete();
            }
            if (removeFile.exists()) {
                removeFile.delete();
            }
        } else {

            try {
                Files.copy(fileDirectory.toPath(), stagingFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void log(boolean isGlobal) {
        if (isGlobal) {
            for (String commitID: COMMIT.list()) {
                Commit currCommit = Utils.readObject(Utils.join(COMMIT, commitID), Commit.class);
                Date cD = currCommit.getTimestamp();
                Formatter fmt = new Formatter();
                fmt.format("%ta %tb %te %tT %tY %tz", cD, cD, cD, cD, cD, cD, cD);
                System.out.println("===");
                System.out.println(String.format("commit %s", commitID));
                System.out.print("Date: ");
                System.out.println(fmt);
                System.out.println(currCommit.getMessage());
                System.out.println();
            }
        } else {
            Commit headCommit = readObject(HEAD, Commit.class);

            while (headCommit != null) {
                String commitID = String.format("commit %s", sha1(serialize(headCommit)));
                String message = headCommit.getMessage();
                Date cD = headCommit.getTimestamp();
                Formatter fmt = new Formatter();
                fmt.format("%ta %tb %te %tT %tY %tz", cD, cD, cD, cD, cD, cD, cD);
                if (headCommit.getParent2() != null) {
                    String[] tokens = message.split(" ");
                    System.out.println("===");
                    System.out.println(commitID);
                    System.out.print("Merge: ");
                    System.out.print(headCommit.getParent2().substring(0, 7) + " ");
                    System.out.println(headCommit.getParent1().substring(0, 7));
                    System.out.print("Date: ");
                    System.out.println(fmt);
                    System.out.println("Merged " + tokens[1] + " into " + tokens[3] + ".");
                    System.out.println();
                } else {
                    System.out.println("===");
                    System.out.println(commitID);
                    System.out.print("Date: ");
                    System.out.println(fmt);
                    System.out.println(headCommit.getMessage());
                    System.out.println();
                }
                if (headCommit.getParent1() == null) {
                    break;
                } else {
                    headCommit = readObject(join(COMMIT, headCommit.getParent1()), Commit.class);
                }
            }
        }
    }

    public static void remove(String fileName) {
        boolean inStaging = false;
        boolean inCommit = false;

        for (String files : STAGING.list()) {
            if (fileName.equals(files)) {
                Utils.join(STAGING, fileName).delete();
                inStaging = true;
            }
        }

        Commit currCommit = Utils.readObject(HEAD, Commit.class);

        for (String key : currCommit.getTrackFiles().keySet()) {
            if (fileName.equals(key)) {
                Utils.join(CWD, fileName).delete();
                Utils.join(REMOVE, fileName).mkdir();
                inCommit = true;
            }
        }

        if (!(inCommit || inStaging)) {
            throw Utils.error("No reason to remove the file.");
        }

    }

    public static void checkout(String[] args) {
        int len = args.length;
        blobHash = (HashMap<String, byte[]>) Utils.readObject(BLOB, HashMap.class);
        branchHash = (HashMap<String, String>) Utils.readObject(BRANCH, HashMap.class);
        untrackHash = (HashMap<String, String>) Utils.readObject(UNTRACTED, HashMap.class);
        Commit currCommit = Utils.readObject(HEAD, Commit.class);
        HashMap<String, String> currTrack = currCommit.getTrackFiles();

        switch (len) {
            case 2:
                String branchName = args[1];
                if (!branchHash.containsKey(branchName)) {
                    throw Utils.error("No such branch exists.");
                }
                if (branchName.equals(branchHash.get("currHead"))) {
                    throw Utils.error("No need to checkout the current branch");
                }

                Commit tarComm = readObject(join(COMMIT, branchHash.get(branchName)), Commit.class);
                HashMap<String, String> tarTrack = tarComm.getTrackFiles();

                testUntracked(currTrack, tarTrack);

                Utils.writeObject(HEAD, tarComm);
                branchHash.put("currHead", branchName);
                branchHash.put(branchHash.get("currHead"), sha1(serialize(tarComm)));
                Utils.writeObject(BRANCH, branchHash);

                for (String files : CWD.list()) {
                    if (!untrackHash.containsKey(files)) {
                        Utils.join(CWD, files).delete();
                    }
                }
                for (String files : tarTrack.keySet()) {
                    String fileID = tarTrack.get(files);
                    Utils.writeContents(Utils.join(CWD, files), blobHash.get(fileID));
                }

                for (String files : STAGING.list()) {
                    Utils.join(STAGING, files).delete();
                }

                for (String files : REMOVE.list()) {
                    Utils.join(REMOVE, files).delete();
                }
                break;
            case 3:
                if (!args[1].equals("--")) {
                    throw Utils.error("Incorrect operands.");
                }

                if (currTrack.containsKey(args[2])) {
                    writeContents(join(CWD, args[2]), blobHash.get(currTrack.get(args[2])));
                } else {
                    throw Utils.error("File does not exist in that commit.");
                }
                break;
            case 4:
                String id = args[1];
                String file = args[3];

                if (!args[2].equals("--")) {
                    throw Utils.error("Incorrect operands.");
                }

                id = fullId(id);

                Commit target = Utils.readObject(Utils.join(COMMIT, id), Commit.class);
                HashMap<String, String> files = target.getTrackFiles();

                if (files.containsKey(file)) {
                    Utils.writeContents(Utils.join(CWD, file), blobHash.get(files.get(file)));
                } else {
                    throw Utils.error("File does not exist in that commit.");
                }
                break;
            default:
                throw Utils.error("No such command exists.");
        }
    }

    public static void find(String message) {
        int num = 0;

        for (String commitID: COMMIT.list()) {
            Commit currCommit = Utils.readObject(Utils.join(COMMIT, commitID), Commit.class);
            if (currCommit.getMessage().equals(message)) {
                System.out.println(commitID);
                num++;
            }
        }

        if (num == 0) {
            throw Utils.error("Found no commit with that message.");
        }
    }

    public static void status() {
        branchHash = (HashMap<String, String>) Utils.readObject(BRANCH, HashMap.class);
        String[] keys = branchHash.keySet().toArray(new String[0]);
        Arrays.sort(keys);

        System.out.println("=== Branches ===");
        for (int i = 0; i < keys.length; i++) {
            if (!keys[i].equals("currHead")) {
                if (keys[i].equals(branchHash.get("currHead"))) {
                    System.out.print("*");
                }
                System.out.println(keys[i]);
            }
        }
        System.out.println();

        System.out.println("=== Staged Files ===");
        String[] files = STAGING.list();
        Arrays.sort(files);

        for (String fileName : files) {
            System.out.println(fileName);
        }
        System.out.println();

        System.out.println("=== Removed Files ===");
        String[] file = REMOVE.list();
        Arrays.sort(file);

        for (String fileName : file) {
            System.out.println(fileName);
        }
        System.out.println();

        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println();

        System.out.println("=== Untracked Files ===");
        System.out.println();
    }

    public static void branch(String name) {
        branchHash = (HashMap<String, String>) Utils.readObject(BRANCH, HashMap.class);

        if (branchHash.containsKey(name)) {
            throw Utils.error("A branch with that name already exists.");
        }

        String id = sha1(Utils.readContents(HEAD));
        branchHash.put(name, id);
        Utils.writeObject(BRANCH, branchHash);
    }

    public static void rmBranch(String name) {
        branchHash = (HashMap<String, String>) Utils.readObject(BRANCH, HashMap.class);

        if (!branchHash.containsKey(name)) {
            throw Utils.error("A branch with that name does not exist.");
        }

        if (name.equals(branchHash.get("currHead"))) {
            throw Utils.error("Cannot remove the current branch.");
        }

        branchHash.remove(name);
        Utils.writeObject(BRANCH, branchHash);
    }

    public static void reset(String id) {
        id = fullId(id);

        branchHash = (HashMap<String, String>) Utils.readObject(BRANCH, HashMap.class);
        branchHash.put("tempBranch", id);
        Utils.writeObject(BRANCH, branchHash);
        String head = branchHash.get("currHead");

        String[] args = {"checkout", "tempBranch"};
        checkout(args);
        branchHash.put(head, id);
        branchHash.put("currHead", head);
        branchHash.remove("tempBranch");

        Utils.writeObject(BRANCH, branchHash);
    }

    public static void merge(String branchName) {
        if (STAGING.list().length != 0 || REMOVE.list().length != 0) {
            throw error("You have uncommitted changes.");
        }
        blobHash = (HashMap<String, byte[]>) readObject(BLOB, HashMap.class);
        branchHash = (HashMap<String, String>) Utils.readObject(BRANCH, HashMap.class);

        if (!branchHash.containsKey(branchName)) {
            throw error("A branch with that name does not exist.");
        } else if (branchName.equals(branchHash.get("currHead"))) {
            throw error("Cannot merge a branch with itself.");
        }
        Commit head = readObject(HEAD, Commit.class);
        Commit branch = readObject(join(COMMIT, branchHash.get(branchName)), Commit.class);
        Commit split = findSplit2(head, branch);
        HashMap<String, String> splitTrack = split.getTrackFiles();
        HashMap<String, String> headTrack = head.getTrackFiles();
        HashMap<String, String> branchTrack = branch.getTrackFiles();
        String headId = sha1(serialize(head));
        String branchId = sha1(serialize(branch));
        if (sha1(serialize(split)).equals(branchId)) {
            System.out.println("Given branch is an ancestor of the current branch.");
            System.exit(0);
        } else if (sha1(serialize(split)).equals(headId)) {
            String[] args = {"checkout", branchName};
            checkout(args);
            System.out.println("Current branch fast-forwarded.");
            System.exit(0);
        }
        testUntracked(headTrack, branchTrack);
        Commit newCommit = new Commit("Merged " + branchName + " into "
                + branchHash.get("currHead"), headId, branchId);
        HashMap<String, String> trackFiles = newCommit.getTrackFiles();
        HashMap<String, String> listOfFiles = mergeHelper(branchTrack, headTrack, splitTrack);

        for (String fileName : listOfFiles.keySet()) {
            int mergeCase = findCase(splitTrack, headTrack, branchTrack, fileName);
            String branchFile = branchTrack.get(fileName);
            String headFile = headTrack.get(fileName);
            join(CWD, fileName).delete();
            switch (mergeCase) {
                case 1:
                    trackFiles.put(fileName, branchFile);
                    writeContents(join(CWD, fileName), blobHash.get(branchFile));
                    break;
                case 2:
                    if (headFile != null) {
                        trackFiles.put(fileName, headFile);
                        writeContents(join(CWD, fileName), blobHash.get(headFile));
                    }
                    break;
                case 3:
                    System.out.println("Encountered a merge conflict.");
                    String newContents = "<<<<<<< HEAD\n";
                    writeContents(join(CWD, fileName), blobHash.get(headFile));
                    newContents += readContentsAsString(join(CWD, fileName)) + "=======\n";
                    if (blobHash.containsKey(branchFile)) {
                        writeContents(join(CWD, fileName), blobHash.get(branchFile));
                        newContents += readContentsAsString(join(CWD, fileName)) + ">>>>>>>\n";
                    } else {
                        newContents += ">>>>>>>\n";
                    }
                    trackFiles.put(fileName, sha1(fileName + newContents));
                    writeContents(join(CWD, fileName), newContents);
                    blobHash.put(sha1(fileName + newContents), readContents(join(CWD, fileName)));
                    break;
                default:
                    break;
            }
        }
        Utils.writeObject(BLOB, blobHash);
        String id = sha1(Utils.serialize(newCommit));
        File commitFile = join(COMMIT, id);
        Utils.writeObject(commitFile, newCommit);
        Utils.writeObject(HEAD, newCommit);
        branchHash.put(branchHash.get("currHead"), id);
        Utils.writeObject(BRANCH, branchHash);
    }

    private static String fullId(String id) {
        if (!join(COMMIT, id).exists()) {
            if (id.length() < 40) {
                for (String iDs : COMMIT.list()) {
                    if (iDs.substring(0, id.length()).equals(id)) {
                        id = iDs;
                        break;
                    }
                }
            } else {
                throw Utils.error("No commit with that id exists.");
            }
        }

        return id;
    }

    private static void testUntracked(HashMap<String, String> currTrack,
                                      HashMap<String, String> tarTrack) {
        untrackHash = (HashMap<String, String>) Utils.readObject(UNTRACTED, HashMap.class);

        for (String files : CWD.list()) {
            if (!untrackHash.containsKey(files)) {
                if (!currTrack.containsKey(files)) {
                    String tarID = tarTrack.get(files);
                    String cwdID = sha1(files + sha1(readContentsAsString(join(CWD, files))));
                    if (!cwdID.equals(tarID) && tarID != null) {
                        throw Utils.error("There is an untracked file in the way; "
                                + "delete it, or add and commit it first.");
                    }
                }
            }
        }
    }

    private static Commit findSplit(Commit head, Commit branch) {
        HashMap<String, String> headParents = new HashMap<>();
        HashMap<String, String> branchParents = new HashMap<>();
        String headId = sha1(serialize(head));
        String branchId = sha1(serialize(branch));
        headParents.put(headId, headId);
        branchParents.put(branchId, branchId);
        Commit split;

        while (true) {
            if (head.getParent1() != null) {
                head = readObject(join(COMMIT, head.getParent1()), Commit.class);
                headId = sha1(serialize(head));
                headParents.put(headId, headId);
            }

            if (branch.getParent1() != null) {
                branch = readObject(join(COMMIT, branch.getParent1()), Commit.class);
                branchId = sha1(serialize(branch));
                branchParents.put(branchId, branchId);
            }

            if (headParents.containsKey(branchId)) {
                split = readObject(join(COMMIT, branchId), Commit.class);
                break;
            }
            if (branchParents.containsKey(headId)) {
                split = readObject(join(COMMIT, headId), Commit.class);
                break;
            }
        }

        return split;
    }

    private static Commit findSplit2(Commit head, Commit branch) {
        HashMap<String, String> headParents = new HashMap<>();
        HashMap<String, String> branchParents = new HashMap<>();
        ArrayList<String> headRecent = new ArrayList<>();
        ArrayList<String> branchRecent = new ArrayList<>();

        String headId = sha1(serialize(head));
        String branchId = sha1(serialize(branch));

        headParents.put(headId, headId);
        branchParents.put(branchId, branchId);
        headRecent.add(headId);
        branchRecent.add(branchId);

        Commit split;

        while (true) {
            int i = headRecent.size();
            for (String vals : headRecent.toArray(new String[0])) {
                if (branchParents.containsKey(vals)) {
                    split = readObject(join(COMMIT, vals), Commit.class);
                    return split;
                }

                head = readObject(join(COMMIT, vals), Commit.class);
                if (head.getParent1() != null) {
                    headId = head.getParent1();
                    headParents.put(headId, headId);
                    headRecent.add(headId);
                }
                if (head.getParent2() != null) {
                    headId = head.getParent2();
                    headParents.put(headId, headId);
                    headRecent.add(headId);
                }
            }
            for (int j = 0; j < i; j++) {
                headRecent.remove(j);
            }

            i = branchRecent.size();
            for (String vals : branchRecent.toArray(new String[0])) {
                if (headParents.containsKey(vals)) {
                    split = readObject(join(COMMIT, vals), Commit.class);
                    return split;
                }

                branch = readObject(join(COMMIT, vals), Commit.class);
                if (branch.getParent1() != null) {
                    branchId = branch.getParent1();
                    branchParents.put(branchId, branchId);
                    branchRecent.add(branchId);
                }
                if (branch.getParent2() != null) {
                    branchId = branch.getParent2();
                    branchParents.put(branchId, branchId);
                    branchRecent.add(branchId);
                }
            }
            for (int j = 0; j < i; j++) {
                branchRecent.remove(j);
            }
        }

    }

    private static int findCase(HashMap<String, String> splitTrack, HashMap<String,
            String> headTrack, HashMap<String, String> branchTrack, String fileName) {

        boolean splitExists = splitTrack.containsKey(fileName);
        boolean headExists = headTrack.containsKey(fileName);
        boolean branchExists = branchTrack.containsKey(fileName);
        boolean headChanged = true;
        boolean branchChanged = true;

        String splitId;
        String headId;
        String branchId;

        if (splitExists) {
            splitId = splitTrack.get(fileName);
            headId = headTrack.get(fileName);
            branchId = branchTrack.get(fileName);

            headChanged = !splitId.equals(headId);
            branchChanged = !splitId.equals(branchId);

            if (!headChanged && !branchExists) {
                //System.out.println(6);
                return -1; //6
            }
            if (!branchChanged && !headExists) {
                //System.out.println(7);
                return -1; //7
            }

            if (headChanged && !branchChanged) {
                //System.out.println(2);
                return 2;
            }
            if (branchChanged && !headChanged) {
                //System.out.println(1);
                return 1;
            }

            if (headChanged && branchChanged) {
                if ((headId == null && branchId == null) || headId.equals(branchId)) {
                    //System.out.println("3a");
                    return 2; //3a
                }
                //System.out.println("3b");
                return 3; //3b
            }

        } else {
            if (headExists && !branchExists) {
                //System.out.println(4);
                return 2; //4
            }
            if (branchExists && !headExists) {
                //System.out.println(5);
                return 1; //5
            }
        }

        System.out.println("oops");
        return 0;
    }

    private static HashMap<String, String>
        mergeHelper(HashMap<String, String> branchTrack,
                HashMap<String, String> headTrack, HashMap<String, String> splitTrack) {

        HashMap<String, String> listOfFiles = new HashMap<>();

        for (String branchKey : branchTrack.keySet()) {
            for (String headKey : headTrack.keySet()) {
                listOfFiles.put(branchKey, branchKey);
                listOfFiles.put(headKey, headKey);
                if (!splitTrack.isEmpty()) {
                    for (String splitKey : splitTrack.keySet()) {
                        listOfFiles.put(splitKey, splitKey);
                    }
                }
            }
        }

        return listOfFiles;
    }
}
