package io.woolford;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ProcessPortRecordSubset {

        String name;
        String pid;
        String username;

        @JsonProperty("name")
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @JsonProperty("pid")
        public String getPid() {
            return pid;
        }

        public void setPid(String pid) {
            this.pid = pid;
        }

        @JsonProperty("username")
        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

}
