package org.onosproject.ovsc;

import com.fasterxml.jackson.databind.JsonNode;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.config.Config;
import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Define an OVSCConfig class that stores the information read from the user-input configuration file.
 * Created by Chunhai Feng and modified by Cheng (Ian) Liu on 8/15/16.
 * All rights reserved.
 */

public class OvscConfig extends Config<ApplicationId> {

    protected final Logger log = getLogger(getClass());

    private static final String BRIDGEID = "bridgeId";
    private static final String FORMACSEC = "forMACSecVNF";
    private static final String DATAPLANEPORT = "dataPlanePort";
    private static final String VMPORTLEFT = "vmPortLeft";
    private static final String VMPORTRIGHT = "vmPortRight";
    private static final String LEFTVLAN = "leftVlan";
    private static final String RIGHTVLAN = "rightVlan";
    private static final String DEFAULTPRIORITY = "defaultPriority";

    /**
     * Returns bridgeId or null
     */
    public String bridgeId() {
        JsonNode jsonNode = object.get(BRIDGEID);
        if (jsonNode == null) {
            return null;
        }

        try {
            return jsonNode.asText();
        } catch (IllegalArgumentException e) {
            log.error("Wrong bridgeID format {}", jsonNode.asText());
            return null;
        }
    }

    /**
     * Returns whether this is for MACSec VNF
     */

    // TODO: Test this part

    public boolean forMACSec() {
        JsonNode jsonNode = object.get(FORMACSEC);
        if (jsonNode == null) {
            return false;
        }

        try {
            return jsonNode.asBoolean();
        } catch (IllegalArgumentException e) {
            log.error("Wrong forMACSec format {}", jsonNode.asText());
            return false;
        }
    }

    /**
     * Returns dataPlanePort or null
     */
    public long dataPlanePort() {
        JsonNode jsonNode = object.get(DATAPLANEPORT);
        if (jsonNode == null) {
            return 0;
        }

        try {
            return jsonNode.asLong();
        } catch (IllegalArgumentException e) {
            log.error("Wrong port format {}", jsonNode.asLong());
            return 0;
        }
    }

    /**
     * Returns vmPortLeft or null
     */
    public long vmPortLeft() {
        JsonNode jsonNode = object.get(VMPORTLEFT);
        if (jsonNode == null) {
            return 0;
        }

        try {
            return jsonNode.asLong();
        } catch (IllegalArgumentException e) {
            log.error("Wrong port format {}", jsonNode.asLong());
            return 0;
        }
    }

    /**
     * Returns vmPortRight or null
     */
    public long vmPortRight() {
        JsonNode jsonNode = object.get(VMPORTRIGHT);
        if (jsonNode == null) {
            return 0;
        }

        try {
            return jsonNode.asLong();
        } catch (IllegalArgumentException e) {
            log.error("Wrong port format {}", jsonNode.asLong());
            return 0;
        }
    }

    /**
     * Returns leftVlan or null
     */
    public short leftVlan() {
        JsonNode jsonNode = object.get(LEFTVLAN);
        if (jsonNode == null) {
            return 0;
        }

        try {
            return (short) jsonNode.asInt();
        } catch (IllegalArgumentException e) {
            log.error("Wrong vlan format {}", jsonNode.asInt());
            return 0;
        }
    }

    /**
     * Returns rightVlan or null
     */
    public short rightVlan() {
        JsonNode jsonNode = object.get(RIGHTVLAN);
        if (jsonNode == null) {
            return 0;
        }

        try {
            return (short) jsonNode.asInt();
        } catch (IllegalArgumentException e) {
            log.error("Wrong vlan format {}", jsonNode.asInt());
            return 0;
        }
    }

    /**
     * Returns Priority or null
     */
    public int defaultPriority() {
        JsonNode jsonNode = object.get(DEFAULTPRIORITY);
        if (jsonNode == null) {
            return 0;
        }

        try {
            return jsonNode.asInt();
        } catch (IllegalArgumentException e) {
            log.error("Wrong priority format {}", jsonNode.asInt());
            return 0;
        }
    }
}

