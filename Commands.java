package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.HashMap;
import java.util.TreeMap;

/**
 * A class handling all the commands input by the user.
 *
 * @author shreyanshloharuka
 */
public class Commands implements Serializable {

    /**
     * Initialises a gitlet repository.
     */
    void init() throws IOException {
        try {
            if (Files.exists(Paths.get(".gitlet"))) {
                Utils.message("A Gitlet version-control "
                        + "system already exists in the current directory.");
                throw new GitletException();
            } else {
                Files.createDirectory(Paths.get(".gitlet"));
                Date epoch = new Date(0);
                String msg = "initial commit";
                String uID = Utils.sha1(epoch.toString(), msg);
                Commit first = new Commit(epoch, msg, null, null, uID, null);

                File commits = new File(".gitlet/commits");
                commits.mkdir();
                File commit = new File(commits, uID);
                Utils.writeObject(commit, first);

                Repository repository = new Repository();
                repository.addNode("master", first.uID());
                repository.setCurrentBranch("master");
                repository.setHead(uID);
                File pointers = new File(".gitlet/pointers");
                Utils.writeObject(pointers, repository);
            }
        } catch (GitletException e) {
            System.exit(0);
        }
    }

    /**
     * Stages a file FILENAME that needs to be committed later.
     */
    void add(String filename) throws IOException {
        File workingDir = new File(System.getProperty("user.dir"));
        File file = new File(workingDir, filename);
        File pointers = new File(".gitlet/pointers");
        Repository repo = Utils.readObject(pointers, Repository.class);
        if (file.exists()) {
            File stagingToAdd = new File(".gitlet/stagingToAdd");
            File[] filesStaged = stagingToAdd.listFiles();
            if (repo.stageFilesNames().containsKey(filename)) {
                String old = repo.stageFilesNames().get(filename);
                File oldFile = new File(stagingToAdd, old);
                int ind = Arrays.asList(filesStaged).indexOf(oldFile);
                File toRemove = Arrays.asList(filesStaged).get(ind);
                toRemove.delete();
                repo.stageFiles().remove(old);
                repo.stageFilesNames().remove(filename);
            }
            byte[] data = Utils.readContents(file);
            String id = Utils.sha1(data);
            if (!stagingToAdd.exists()) {
                stagingToAdd.mkdir();
            }
            File remove = new File(".gitlet/remove");
            if (remove.exists()) {
                if (repo.removedFiles().keySet().contains(filename)) {
                    File fileToRemove = new File(remove,
                            repo.removedFiles().get(filename));
                    fileToRemove.delete();
                    repo.removedFiles().remove(filename);
                    Utils.writeObject(pointers, repo);
                    return;
                }
            }
            File fileStage = new File(stagingToAdd, id);
            Utils.writeContents(fileStage, data);
            fileStage.createNewFile();
            Commit head = idToCommit(repo.head());
            if (head.fileNames() != null
                    && head.fileNames().containsKey(filename)
                    && head.fileNames().get(filename).equals(id)) {
                if (repo.stageFilesNames().containsKey(filename)) {
                    if (fileStage.exists()) {
                        fileStage.delete();
                    }
                    repo.stageFiles().remove(
                            repo.stageFilesNames().get(filename));
                    repo.stageFilesNames().remove(filename);
                }
                Utils.writeObject(pointers, repo);
                return;
            }
            repo.addFileName(filename, id);
            repo.addFile(id, filename);
            Utils.writeObject(pointers, repo);
        } else {
            Utils.message("File does not exist.");
            throw new GitletException();
        }
    }

    /**
     * Commits a file with the message.
     * Takes in MSG,MERGEBRANCH.
     */
    void commit(String msg, String mergeBranch) {
        File stage = new File(".gitlet/stagingToAdd");
        File commits = new File(".gitlet/commits");
        File pointers = new File(".gitlet/pointers");
        File remove = new File(".gitlet/remove");
        File[] filesToUntrack = remove.listFiles();
        File[] filesToCommit = stage.listFiles();
        Repository repo = Utils.readObject(pointers, Repository.class);
        Commit parent = idToCommit(repo.head());
        if ((!stage.exists() || filesToCommit.length == 0)
                && (!remove.exists() || filesToUntrack.length == 0)) {
            Utils.message("No changes added to the commit.");
            throw new GitletException();
        } else {
            List<String> parents = new ArrayList<>();
            parents.add(repo.head());
            if (!mergeBranch.equals("")) {
                parents.add(repo.branches().get(mergeBranch));
            }
            HashMap<String, byte[]> trackfiles = new HashMap<>();
            HashMap<String, String> filesNames = new HashMap<>();
            if (parent.files() != null && parent.fileNames() != null) {
                trackfiles = new HashMap<>(parent.files());
                filesNames = new HashMap<>(parent.fileNames());
            }
            for (File f : filesToCommit) {
                byte[] contents = Utils.readContents(f);
                if (!(trackfiles.containsKey(f.getName())
                        && trackfiles.get(f) == contents)) {
                    trackfiles.put(f.getName(), contents);
                    filesNames.put(repo.stageFiles().get(f.getName()),
                            f.getName());
                }
            }
            if (repo.removedFiles() != null) {
                for (String removal : repo.removedFiles().keySet()) {
                    if (filesNames.containsKey(removal)) {
                        trackfiles.remove(filesNames.get(removal));
                        filesNames.remove(removal);
                    }
                }
            }
            Date curr = new Date();
            String uID = Utils.sha1(curr.toString(),
                    msg, parents.toString(),
                    trackfiles.toString(), filesNames.toString());
            Commit commit = new Commit(curr,
                    msg, parents, trackfiles, uID, filesNames);
            File newCommit = new File(commits, uID);
            Utils.writeObject(newCommit, commit);
            repo.addNode(repo.currentBranch(), uID);
            repo.setHead(uID);
            repo.stageFiles().clear();
            repo.stageFilesNames().clear();
            clearStageToAdd();
            repo.removedFiles().clear();
            clearRemove();
            Utils.writeObject(pointers, repo);
        }
    }

    /**
     * Clears the remove staging area.
     */
    public void clearRemove() {
        File remove = new File(".gitlet/remove");
        File[] toRemove = remove.listFiles();
        if (toRemove != null && toRemove.length != 0) {
            for (File file : toRemove) {
                file.delete();
            }
        }
    }

    /**
     * Clears the add staging area.
     */
    public void clearStageToAdd() {
        File stage = new File(".gitlet/stagingToAdd");
        File[] stageFiles = stage.listFiles();
        for (File file : stageFiles) {
            file.delete();
        }
    }

    /**
     * Unstage the file if it is currently staged.
     * If the file is tracked in the current commit,
     * mark it to indicate that it is not to be included in the next commit,
     * and remove the file from the working directory
     * if the user has not already done so
     * (do not remove it unless it is tracked in the current commit).
     * takes in FILENAME.
     */
    public void rm(String filename) throws IOException {
        try {
            File remove = new File(".gitlet/remove");
            remove.mkdir();
            File stage = new File(".gitlet/stagingToAdd");
            File[] filesInStagingArea = stage.listFiles();
            List<File> filesToCommit = null;
            if (filesInStagingArea != null) {
                filesToCommit = Arrays.asList(stage.listFiles());
            }
            File pointers = new File(".gitlet/pointers");
            Repository repo = Utils.readObject(pointers, Repository.class);
            Commit parent = idToCommit(repo.head());

            if ((!repo.stageFiles().values().contains(filename)
                    && !(parent.fileNames() != null
                    && parent.fileNames().containsKey(filename)))) {
                Utils.message("No reason to remove the file.");
                throw new GitletException();
            }
            String sha = "";
            if (repo.stageFilesNames().keySet().contains(filename)) {
                for (File file : filesToCommit) {
                    if (file.getName().equals(
                            repo.stageFilesNames().get(filename))) {
                        file.delete();
                        break;
                    }
                }
                repo.stageFiles().remove(repo.stageFilesNames().get(filename));
                repo.stageFilesNames().get(filename);
            }
            if (parent.fileNames() != null
                    && parent.fileNames().containsKey(filename)) {
                for (String tracking : parent.fileNames().keySet()) {
                    if (tracking != null && tracking.equals(filename)) {
                        sha = parent.fileNames().get(tracking);
                        File file = new File(remove, sha);
                        file.createNewFile();
                        Utils.restrictedDelete(filename);
                    }
                }
                repo.addRemovedFile(filename, sha);
            }
            Utils.writeObject(pointers, repo);
        } catch (GitletException e) {
            System.exit(0);
        }
    }

    /**
     * Starting at the current head commit,
     * display information about each
     * commit backwards along the commit tree until the initial commit,
     * following the first parent commit links,
     * ignoring any second parents found in merge commits.
     */
    public void log() {
        File pointers = new File(".gitlet/pointers");
        Repository repo = Utils.readObject(pointers, Repository.class);
        Commit first = idToCommit(repo.head());
        while (first != null) {
            displayCommitLog(first.uID(), first);
            if (first.parent() != null) {
                first = idToCommit(first.parent().get(0));
            } else {
                first = null;
            }
        }
    }

    /**
     * Displays log for all commits ever.
     */
    public void globalLog() {
        File commits = new File(".gitlet/commits");
        File[] files = commits.listFiles();
        for (File file : files) {
            Commit commit = idToCommit(file.getName());
            displayCommitLog(file.getName(), commit);
        }
    }

    /**
     * Disply the commit.
     * Takes in ID,COM.
     */
    void displayCommitLog(String id, Commit com) {
        String pattern = "EEE MMM dd HH:mm:ss yyyy Z";
        SimpleDateFormat format = new SimpleDateFormat(pattern);
        String date = format.format(com.timestamp());
        if (com.parent() != null && com.parent().size() > 1) {
            System.out.println("===");
            System.out.println("commit " + id);
            String short1 = com.parent().get(0).substring(0, 7);
            String short2 = com.parent().get(1).substring(0, 7);
            System.out.println("Merge: " + short1 + " " + short2);
            System.out.println("Date: " + date);
            System.out.println(com.logMessage());
            System.out.println();
        } else {
            System.out.println("===");
            System.out.println("commit " + id);
            System.out.println("Date: " + date);
            System.out.println(com.logMessage());
            System.out.println();
        }
    }

    /**
     * Finds all commits with the message MSG.
     */
    public void find(String msg) {
        File commits = new File(".gitlet/commits");
        File[] files = commits.listFiles();
        int count = 0;
        for (File file : files) {
            Commit commit = idToCommit(file.getName());
            if (commit.logMessage().equals(msg)) {
                count++;
                Utils.message(commit.uID());
            }
        }
        if (count == 0) {
            Utils.message("Found no commit with that message.");
            throw new GitletException();
        }
    }

    /**
     * Displays what branches currently exist, and marks the current branch.
     */
    public void status() {
        File pointers = new File(".gitlet/pointers");
        Repository repo = Utils.readObject(pointers, Repository.class);
        System.out.println("=== Branches ===");
        TreeMap<String, String> sortedBranches = new TreeMap<>(repo.branches());
        for (String branch : sortedBranches.keySet()) {
            if (branch.equals(repo.currentBranch())) {
                System.out.println("*" + branch);
            } else {
                System.out.println(branch);
            }
        }
        System.out.println();
        System.out.println("=== Staged Files ===");
        TreeMap<String, String> sortedStage = new TreeMap<>(repo.stageFiles());
        for (String staged : sortedStage.keySet()) {
            System.out.println(repo.stageFiles().get(staged));
        }
        System.out.println();
        System.out.println("=== Removed Files ===");
        TreeMap<String, String> sortedRemoved =
                new TreeMap<>(repo.removedFiles());
        for (String removed : sortedRemoved.keySet()) {
            System.out.println(removed);
        }
        System.out.println();
        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println();
        System.out.println("=== Untracked Files ===");

        System.out.println();
    }

    /**
     * Checkout first 2 uses using ARGS.
     */
    void checkout(String[] args) {
        try {
            String id, filename;
            if (args.length == 2 && args[0].equals("--")) {
                id = getHead();
                filename = args[1];
            } else if (args.length == 3 && args[1].equals("--")) {
                id = args[0];
                filename = args[2];
            } else {
                Utils.message("Incorrect operands.");
                throw new GitletException();
            }
            Commit commit = idToCommit(convertShortenedID(id));
            if (commit.fileNames() != null
                    && commit.fileNames().containsKey(filename)) {
                File f = new File(filename);
                String fileSha = commit.fileNames().get(filename);
                byte[] contents = commit.files().get(fileSha);
                String string = new String(contents);
                Utils.writeContents(f, string);
            } else {
                Utils.message("File does not exist in that commit.");
                throw new GitletException();
            }
        } catch (GitletException e) {
            System.exit(0);
        }
    }

    /**
     * Takes all files in the commit at the head of the given branch,
     * and puts them in the working directory,overwriting the versions of
     * the files that are already there if they exist.
     * Also, at the end of this command, the given branch will now
     * be considered the current branch (HEAD).
     * Any files that are tracked in the current branch but are not
     * present in the checked-out branch are deleted.
     * The staging area is cleared,
     * unless the checked-out branch is the current branch.
     * Takes in BRANCHNAME.
     */
    void checkoutBranch(String branchName) {
        try {
            File pointers = new File(".gitlet/pointers");
            Repository repo = Utils.readObject(pointers, Repository.class);
            if (!repo.branches().containsKey(branchName)) {
                Utils.message("No such branch exists.");
                throw new GitletException();
            }
            if (repo.currentBranch().equals(branchName)) {
                Utils.message("No need to checkout the current branch.");
                throw new GitletException();
            }
            List<String> nameFilesInWorkingDir =
                    Utils.plainFilenamesIn(System.getProperty("user.dir"));
            Commit currBranch = idToCommit(repo.head());
            String id = repo.branches().get(branchName);
            Commit commit = idToCommit(id);
            String[] otherFiles = new String[]{".DS_Store",
                ".gitignore", "Makefile", "gitlet-design.txt", "proj3.iml"};
            for (String file : nameFilesInWorkingDir) {
                if (!Arrays.asList(otherFiles).contains(file)
                        && (currBranch.fileNames() == null
                        || !currBranch.fileNames().keySet().contains(file))
                        && commit.fileNames() != null
                        && commit.fileNames().keySet().contains(file)) {
                    Utils.message("There is an untracked file in the way;"
                            + " delete it or add it first.");
                    throw new GitletException();
                }
            }
            if (currBranch.fileNames() != null) {
                for (String name : currBranch.fileNames().keySet()) {
                    if ((commit.fileNames() == null
                            || !commit.fileNames().containsKey(name))
                            && name != null) {
                        Utils.restrictedDelete(name);
                    }
                }
            }
            if (commit.fileNames() != null) {
                for (String name : commit.fileNames().keySet()) {
                    File workingDir = new File(System.getProperty("user.dir"));
                    File f = new File(workingDir, name);
                    String fileSha = commit.fileNames().get(name);
                    byte[] contents = commit.files().get(fileSha);
                    String string = new String(contents);
                    Utils.writeContents(f, string);
                }
            }
            repo.setHead(id);
            repo.setCurrentBranch(branchName);
            repo.stageFiles().clear();
            Utils.writeObject(pointers, repo);
        } catch (GitletException e) {
            System.exit(0);
        }
    }

    /**
     * Creates a new branch with the given name,
     * and points it at the current head node.
     * Takes in BRANCHNAME.
     */
    public void branch(String branchName) {
        try {
            File pointers = new File(".gitlet/pointers");
            Repository repo = Utils.readObject(pointers, Repository.class);
            if (repo.branches().containsKey(branchName)) {
                Utils.message("A branch with that name already exists.");
                throw new GitletException();
            } else {
                repo.addNode(branchName, repo.head());
                Utils.writeObject(pointers, repo);
            }
        } catch (GitletException e) {
            System.exit(0);
        }
    }

    /**
     * Deletes the branch with the given name BRANCHNAME.
     */
    public void rmbranch(String branchName) {
        try {
            File pointers = new File(".gitlet/pointers");
            Repository repo = Utils.readObject(pointers, Repository.class);
            if (!repo.branches().containsKey(branchName)) {
                Utils.message("A branch with that name does not exist.");
                throw new GitletException();
            } else if (repo.currentBranch().equals(branchName)) {
                Utils.message("Cannot remove the current branch.");
                throw new GitletException();
            } else {
                repo.branches().remove(branchName);
                Utils.writeObject(pointers, repo);
            }
        } catch (GitletException e) {
            System.exit(0);
        }
    }

    /**
     * Reset Command.
     * Takes in COMMID.
     */
    public void reset(String commId) {
        File pointers = new File(".gitlet/pointers");
        Repository repo = Utils.readObject(pointers, Repository.class);
        String id = convertShortenedID(commId);
        File commits = new File(".gitlet/commits");
        if (!Utils.plainFilenamesIn(commits).contains(id)) {
            Utils.message(" No commit with that id exists.");
            throw new GitletException();
        }
        File workingDir = new File(System.getProperty("user.dir"));
        List<String> nameFilesInWorkingDir =
                Utils.plainFilenamesIn(System.getProperty("user.dir"));
        File[] filesInWorkingDir = workingDir.listFiles();
        Commit currBranch = idToCommit(repo.head());
        Commit commit = idToCommit(id);
        String[] otherFiles = new String[]{".DS_Store",
            ".gitignore", "Makefile", "gitlet-design.txt", "proj3.iml"};

        for (String file : nameFilesInWorkingDir) {
            if (!Arrays.asList(otherFiles).contains(file)
                    && !currBranch.fileNames().keySet().contains(file)
                    && commit.fileNames().keySet().contains(file)) {
                Utils.message("There is an untracked file in the way;"
                        + " delete it or add it first.");
                throw new GitletException();
            }
        }

        for (String name : commit.fileNames().keySet()) {
            String[] args = new String[]{id, "--", name};
            checkout(args);
        }

        for (String name : currBranch.fileNames().keySet()) {
            if (!commit.fileNames().containsKey(name)) {
                Utils.restrictedDelete(name);
            }
        }
        repo.setHead(id);
        repo.addNode(repo.currentBranch(), id);
        repo.stageFilesNames().clear();
        repo.stageFiles().clear();
        Utils.writeObject(pointers, repo);
    }

    /**
     * Merge the current branch with BRANCHNAME.
     */
    public void merge(String branchName) throws IOException {
        File pointers = new File(".gitlet/pointers");
        Repository repo = Utils.readObject(pointers, Repository.class);
        mergeException(repo, branchName);
        String splitPoint = splitPoint(
                repo.branches().get(repo.currentBranch()),
                repo.branches().get(branchName));
        mergeHelper(branchName, repo, splitPoint);
        Commit current = idToCommit(repo.branches().get(repo.currentBranch()));
        Commit given = idToCommit(repo.branches().get(branchName));
        Commit split = idToCommit(splitPoint);
        boolean conflict = false;
        if (split.fileNames() != null) {
            for (String fileName : split.fileNames().keySet()) {
                if (!given.fileNames().containsKey(fileName)
                        && modified(fileName, current, split)) {
                    conflict = mergeConflict(fileName, current, given, repo);
                }
                if (!current.fileNames().containsKey(fileName)
                        && modified(fileName, given, split)) {
                    conflict = mergeConflict(fileName, current, given, repo);
                }
                if (modified(fileName, given, split)
                        && modified(fileName, current, split)) {
                    conflict = mergeConflict(fileName, current, given, repo);
                }
                if (modified(fileName, given, split)
                        && !modified(fileName, current, split)) {
                    checkout(new String[]{repo.branches().get(branchName),
                        "--", fileName});
                    repo.stageFiles().put(fileName,
                            given.fileNames().get(fileName));
                    File stage = new File(".gitlet/stagingToAdd");
                    File thisFile = new File(stage,
                            given.fileNames().get(fileName));
                    thisFile.createNewFile();
                }
                if (!modified(fileName, current, split)
                        && !given.fileNames().containsKey(fileName)) {
                    repo.addRemovedFile(fileName,
                            given.fileNames().get(fileName));
                    Utils.restrictedDelete(fileName);
                }
            }
        }
        conflict = mergeHelper2(given, current,
                split, branchName, repo, conflict);
        Utils.writeObject(pointers, repo);
        mergeCommit(branchName, repo.currentBranch(), conflict);
    }

    /**
     * Helps Merge reduce the lines.
     * Takes in BRANCHNAME,REPO,SPLITPOINT.
     */
    public void mergeHelper(String branchName,
                            Repository repo, String splitPoint) {
        if (repo.branches().get(branchName).equals(splitPoint)) {
            Utils.message("Given branch is an ancestor of the current branch.");
            throw new GitletException();
        }
        if (repo.branches().get(repo.currentBranch()).equals(splitPoint)) {
            repo.addNode(repo.currentBranch(),
                    repo.branches().get(branchName));
            Utils.message("Current branch fast-forwarded.");
            checkoutBranch(branchName);
            throw new GitletException();
        }
    }

    /**
     * Returns conflict and Helps Merge reduce the lines.
     * Takes in GIVEN,CURRENT,SPLIT,BRANCHNAME,
     * REPO,CONFLICT.
     */
    public boolean mergeHelper2(Commit given, Commit current,
                                Commit split, String branchName,
                                Repository repo,
                                boolean conflict) throws IOException {
        for (String fileName : given.fileNames().keySet()) {
            if ((split.fileNames() == null
                    || !split.fileNames().containsKey(fileName))
                    && fileName != null) {
                if (different(fileName, current, given)) {
                    conflict = mergeConflict(fileName, current, given, repo);
                    continue;
                }
                checkout(new String[]{
                        repo.branches().get(branchName), "--", fileName});
                repo.stageFiles().put(
                        fileName, given.fileNames().get(fileName));
                File stage = new File(".gitlet/stagingToAdd");
                File thisFile = new File(stage,
                        given.fileNames().get(fileName));
                thisFile.createNewFile();
            }
        }
        return conflict;
    }

    /**
     * Handles the exceptional cases of merge.
     * Takes in REPO,BRANCHNAME.
     */
    public void mergeException(Repository repo, String branchName) {
        if (!repo.branches().containsKey(branchName)) {
            Utils.message("A branch with that name does not exist.");
            throw new GitletException();
        }
        if (repo.stageFiles().size() != 0) {
            Utils.message("You have uncommitted changes.");
            throw new GitletException();
        }
        if (branchName.equals(repo.currentBranch())) {
            Utils.message("Cannot merge a branch with itself.");
            throw new GitletException();
        }
        List<String> nameFilesInWorkingDir = Utils.plainFilenamesIn(
                System.getProperty("user.dir"));
        Commit currBranch = idToCommit(repo.head());
        String id = repo.branches().get(branchName);
        Commit commit = idToCommit(id);
        String[] otherFiles = new String[]{".DS_Store",
            ".gitignore", "Makefile", "gitlet-design.txt", "proj3.iml"};
        for (String file : nameFilesInWorkingDir) {
            if (!Arrays.asList(otherFiles).contains(file)
                    && !currBranch.fileNames().keySet().contains(file)
                    && commit.fileNames().keySet().contains(file)) {
                Utils.message("There is an untracked file in the way;"
                        + " delete it or add it first.");
                throw new GitletException();
            }
        }
    }

    /**
     * Handle Merge Conflict and Return the merged file.
     * Takes in FILENAME,CURRENT,GIVEN,REPO.
     */
    public boolean mergeConflict(String fileName, Commit current,
                                 Commit given,
                                 Repository repo) throws IOException {
        String contentsOfCurrent = "", contentsOfGiven = "";
        if (!current.fileNames().containsKey(fileName)
                && !given.fileNames().containsKey(fileName)) {
            return false;
        }
        if (!current.fileNames().containsKey(fileName)) {
            contentsOfGiven = new String(
                    given.files().get(given.fileNames().get(fileName)));
        }
        if (!given.fileNames().containsKey(fileName)) {
            contentsOfCurrent = new String(
                    current.files().get(current.fileNames().get(fileName)));
        }
        if (different(fileName, current, given)) {
            contentsOfGiven = new String(
                    given.files().get(given.fileNames().get(fileName)));
            contentsOfCurrent = new String(
                    current.files().get(current.fileNames().get(fileName)));

        }
        String contents = "<<<<<<< HEAD\n";
        contents += contentsOfCurrent;
        contents += "=======\n";
        contents += contentsOfGiven + ">>>>>>>\n";

        File workingDir = new File(System.getProperty("user.dir"));
        File[] workingDirFiles = workingDir.listFiles();
        for (File file : workingDirFiles) {
            if (file.getName().equals(fileName)) {
                Utils.writeContents(file, contents);
                String shaOfFile = Utils.sha1(Utils.readContents(file));
                repo.stageFilesNames().put(fileName, shaOfFile);
                repo.stageFiles().put(shaOfFile, fileName);
                File stage = new File(".gitlet/stagingToAdd");
                stage.mkdir();
                File thisFile = new File(stage, shaOfFile);
                thisFile.createNewFile();
                break;
            }
        }
        return true;
    }

    /**
     * Merge commit special.
     * Takes in GIVEN,CURRENT,CONFLICT.
     */
    public void mergeCommit(String given, String current, boolean conflict) {
        if (conflict) {
            Utils.message("Encountered a merge conflict.");
        }
        String msg = "Merged " + given + " into " + current + ".";
        commit(msg, given);
    }

    /**
     * RETURNS the sha of the split point commit.
     * Takes in CURRBRANCH,MERGEBRANCH.
     */
    public String splitPoint(String currBranch, String mergeBranch) {
        Commit current = idToCommit(currBranch);
        Commit given = idToCommit(mergeBranch);

        ArrayList<String> parentOfCurrent = new ArrayList<>();
        ArrayList<String> parentOfGiven = new ArrayList<>();
        ArrayList<String> commonAncestors = new ArrayList<>();
        String parentCurr, parentFrom;
        parentOfCurrent.add(currBranch);
        parentOfGiven.add(mergeBranch);

        parentOfCurrent = findParents(current, parentOfCurrent);
        parentOfGiven = findParents(given, parentOfGiven);
        for (String p : parentOfCurrent) {
            if (parentOfGiven.contains(p)) {
                commonAncestors.add(p);
            }
        }
        for (String c : commonAncestors) {
            int count = 0;
            for (String a : commonAncestors) {
                if (!a.equals(c)) {
                    if (findParents(idToCommit(a),
                            new ArrayList<String>()).contains(c)) {
                        count++;
                    }
                }
            }
            if (count == 0) {
                return c;
            }
        }
        return "";

    }

    /**
     * Returns a list of all parents of a commit.
     * Takes in BRANCH,PARENTS.
     */
    public ArrayList<String> findParents(Commit branch,
                                         ArrayList<String> parents) {
        if (branch.parent() == null) {
            return parents;
        } else {
            boolean second = false;
            parents.add(branch.parent().get(0));
            if (branch.parent().size() > 1) {
                second = true;
                parents.add(branch.parent().get(1));
            }
            Commit firstParent = idToCommit(branch.parent().get(0));
            if (second) {
                Commit secondParent = idToCommit(branch.parent().get(1));
                parents.addAll(findParents(secondParent, parents));
            }
            parents.addAll(findParents(firstParent, parents));
            return parents;
        }
    }

    /**
     * Returns true if file FILE has been modified in COMMIT since SPLITPOINT.
     */
    public boolean modified(String file, Commit commit, Commit splitPoint) {
        if (commit.fileNames().containsKey(file)) {
            String string = splitPoint.fileNames().get(file);
            if (!commit.fileNames().get(file).equals(string)) {
                return true;
            }
            return false;
        }
        return false;
    }

    /**
     * Returns true if the file differs in the both the commits.
     * Takes in FILE,BRANCH,COMMIT.
     */
    public boolean different(String file, Commit branch, Commit commit) {
        if (!commit.fileNames().containsKey(file)
                || !branch.fileNames().containsKey(file)) {
            return false;
        }
        if (commit.fileNames().containsKey(file)
                && branch.fileNames().containsKey(file)) {
            String string = branch.fileNames().get(file);
            return !commit.fileNames().get(file).equals(string);
        }
        return false;
    }

    /**
     * Convert and Returns a SHA1 UID to its corresponding commit.
     */
    public Commit idToCommit(String uid) {
        File file = new File(".gitlet/commits/" + uid);
        if (file.exists()) {
            return Utils.readObject(file, Commit.class);
        } else {
            Utils.message("No commit with that id exists.");
            throw new GitletException();
        }
    }

    /**
     * Returns the commit which is the head.
     */
    public String getHead() {
        File pointers = new File(".gitlet/pointers");
        Repository repo = Utils.readObject(pointers, Repository.class);
        return repo.head();
    }

    /**
     * Returns the actual id from ID.
     * Converts a shorter id to the actual one.
     */
    private String convertShortenedID(String id) {
        if (id.length() == Utils.UID_LENGTH) {
            return id;
        }
        File commits = new File(".gitlet/commits");
        File[] prevCommits = commits.listFiles();

        for (File file : prevCommits) {
            if (file.getName().contains(id)) {
                return file.getName();
            }
        }
        Utils.message("No commit with that id exists.");
        throw new GitletException();
    }
}
