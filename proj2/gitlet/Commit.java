package gitlet;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;

/** Represents a gitlet commit object.
 *  does at a high level.
 *
 *  @author Jae Won Kim
 */
public class Commit implements Serializable {
    /**
     *
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    /** Two possible parents of each Commit */
    private String parent1;
    private String parent2;

    /** The message of this Commit. */
    private String message;

    /** timestamp*/
    private Date timestamp;

    /** Files i track and their respective blobs*/
    private HashMap<String, String> trackFiles;

    /** for commit*/
    public Commit(String message, String parent1, String parent2) {
        this.message = message;
        this.parent1 = parent1;
        this.parent2 = parent2;
        this.trackFiles = new HashMap<>();

        if (parent1 == null) {
            this.timestamp = new Date(0); // is the epoch date.
        } else {
            this.timestamp = new Date();
        }
    }

    public String getMessage() {
        return this.message;
    }

    public Date getTimestamp() {
        return this.timestamp;
    }

    public String getParent1() {
        return this.parent1;
    }

    public String getParent2() {
        return this.parent2;
    }

    public HashMap<String, String> getTrackFiles() {
        return this.trackFiles;
    }

}
