package gitlet;

import java.io.Serializable;
import java.util.HashMap;

/**
 * A class representing a tree in gitlet,
 * mapping names to references to blobs and other trees.
 *
 * @author Shreyansh Loharuka
 */
public class Repository implements Serializable {

    /**
     * Hashmap of branches with their branch names mapped to their sha's.
     */
    private HashMap<String, String> _branches;

    /**
     * Current Head commit's SHA.
     */
    private String _head;

    /**
     * Hashmap of staged files with their sha mapped to their name.
     */
    private HashMap<String, String> _stageFiles;

    /**
     * Hashmap of staged files with their names mapped to their sha.
     */
    private HashMap<String, String> _stageFilesNames;

    /**
     * Hashmap of removed files with their name mapped to their sha.
     */
    private HashMap<String, String> _removedFiles;

    /**
     * Current Branch's name.
     */
    private String _currentBranch;

    /**
     * Initialise a Repository.
     */
    public Repository() {
        _branches = new HashMap<String, String>();
        _stageFiles = new HashMap<>();
        _removedFiles = new HashMap<>();
        _stageFilesNames = new HashMap<>();
    }

    /**
     * Return the current head.
     */
    public String head() {
        return _head;
    }

    /**
     * Set the current head to HEAD.
     */
    public void setHead(String head) {
        _head = head;
    }

    /**
     * Return the current branch.
     */
    public String currentBranch() {
        return _currentBranch;
    }

    /**
     * Set the current branch to BRANCH.
     */
    public void setCurrentBranch(String branch) {
        _currentBranch = branch;
    }


    /**
     * Return the commit tree.
     */
    public HashMap<String, String> branches() {
        return _branches;
    }

    /**
     * Add NODE to tree with name BRANCH and id SHA.
     */
    public void addNode(String branch, String sha) {
        _branches.put(branch, sha);
    }

    /**
     * Return the files that are staged.
     */
    public HashMap<String, String> stageFiles() {
        return _stageFiles;
    }

    /**
     * Add pair to filenames with id SHA and name NAME.
     */
    public void addFile(String sha, String name) {
        _stageFiles.put(sha, name);
    }

    /**
     * Return the filenames that are staged.
     */
    public HashMap<String, String> stageFilesNames() {
        return _stageFilesNames;
    }

    /**
     * Add pair to filenames with id SHA and name NAME.
     */
    public void addFileName(String name, String sha) {
        _stageFilesNames.put(name, sha);
    }

    /**
     * Return the filenames that have been removed.
     */
    public HashMap<String, String> removedFiles() {
        return _removedFiles;
    }

    /**
     * Add NODE to tree with name NAME and id SHA.
     */
    public void addRemovedFile(String name, String sha) {
        _removedFiles.put(name, sha);
    }


}
