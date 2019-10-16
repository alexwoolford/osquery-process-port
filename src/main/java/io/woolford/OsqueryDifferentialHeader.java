package io.woolford;


import java.util.Map;

public class OsqueryDifferentialHeader {

    private String name;
    private String hostIdentifier;
    private long unixTime;
    private String action;
    private Map<String, String> columns;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHostIdentifier() {
        return hostIdentifier;
    }

    public void setHostIdentifier(String hostIdentifier) {
        this.hostIdentifier = hostIdentifier;
    }

    public long getUnixTime() {
        return unixTime;
    }

    public void setUnixTime(long unixTime) {
        this.unixTime = unixTime;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Map<String, String> getColumns() {
        return columns;
    }

    public void setColumns(Map<String, String> columns) {
        this.columns = columns;
    }

}
