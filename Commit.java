package gitlet;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Class for Gitlet, representing commits and their metadata.
 *
 * @author Shreyansh Loharuka
 */
public class Commit implements Serializable {
    /**
     * Commit UID using sha1.
     */
    private String _UID;

    /**
     * Timestamp for the commit.
     */
    private Date _timestamp;

    /**
     * The log message for the commit.
     */
    private String _logMessage;

    /**
     * Reference to Parent commit.
     */
    private List<String> _parent;

    /**
     * Files in the commit hashed from sha to contents.
     */
    private HashMap<String, byte[]> _files;

    /**
     * Files in the commit hashed from name to sha.
     */
    private HashMap<String, String> _fileNames;

    /**
     * Constructor initialising the commit with
     * the MSG,FILENAMES,UID,TIMESTAMP,LOG,PARENT commit, and the FILES.
     */
    public Commit(Date timestamp, String msg, List<String> parent,
                  HashMap<String, byte[]> files, String uID,
                  HashMap<String, String> fileNames) {
        _timestamp = timestamp;
        _logMessage = msg;
        _parent = parent;
        _files = files;
        _UID = uID;
        _fileNames = fileNames;
    }

    /**
     * Constructor initialising the commit as a copy of COM.
     */
    Commit(Commit com) {
        _timestamp = com._timestamp;
        _logMessage = com._logMessage;
        _parent = com._parent;
        _files = com._files;
    }

    /**
     * Returns the commits UID.
     */
    public String uID() {
        return _UID;
    }

    /**
     * Returns the tracked files in the commit.
     */
    public HashMap<String, byte[]> files() {
        return _files;
    }

    /**
     * Returns the Parent of commit.
     */
    public List<String> parent() {
        return _parent;
    }

    /**
     * Return the timestamp.
     */
    public Date timestamp() {
        return _timestamp;
    }

    /**
     * Return the log message.
     */
    public String logMessage() {
        return _logMessage;
    }

    /**
     * Returns the tracked files with names in the commit.
     */
    public HashMap<String, String> fileNames() {
        return _fileNames;
    }

}
