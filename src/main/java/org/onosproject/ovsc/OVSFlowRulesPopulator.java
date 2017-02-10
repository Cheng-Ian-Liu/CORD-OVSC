package org.onosproject.ovsc;

/*
 * Copyright 2016-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.EthType;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.basics.SubjectFactories;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;

/**
 * Populate some flow rules and send to OVS programmatically instead of pushing manually
 * Created by Chunhai Feng and modified by Cheng (Ian) Liu on 01/25/2017
 * All rights reserved.
 */

@Component(immediate = true)
public class OVSFlowRulesPopulator {

    private final Logger log = LoggerFactory.getLogger(getClass());

    // initialize the following services that we will need later

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowObjectiveService flowObjectiveService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry configRegistry;

    //@Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    //protected NetworkConfigService configService;

    // create a configuration file listener, so it triggers when User pushes configuration file for OVSC
    private final NetworkConfigListener configListener = new InternalConfigListener();

    private final ConfigFactory configFactory =
            new ConfigFactory(SubjectFactories.APP_SUBJECT_FACTORY, OvscConfig.class, "ovsc-cfg") {
                @Override
                public OvscConfig createConfig() {
                    return new OvscConfig();
                }
            };

    private final ExecutorService eventExecutor =
            newSingleThreadExecutor(groupedThreads("onos/ovsc", "event-handler", log));

    // initialize some parameters we will read from configuration file
    private static final String SR_APP_ID = "org.onosproject.ovsc";
    private String bridgeId;
    private boolean forMACSecVNF;
    private int defaultPriority;
    private long dataInterface;
    private long vmInterfaceLeft;
    private long vmInterfaceRight;
    private short leftVlan;
    private short rightVlan;

    // Convert MACSec EthType 0x88e5 to Short Format
    // TODO: TEST
    private short MACSecEthType = new EthType(0x88e5).toShort();

    protected DeviceId bridgeID;
    protected ApplicationId appId;


    @Activate
    protected void activate() {

        log.info("OVSC App Started");

        appId = coreService.registerApplication(SR_APP_ID);
        configRegistry.registerConfigFactory(configFactory);
        configRegistry.addListener(configListener);

    }

    @Deactivate
    protected void deactivate() {

        flowRuleService.removeFlowRulesById(appId);
        //configService.removeListener(configListener);
        configRegistry.unregisterConfigFactory(configFactory);
        configRegistry.removeListener(configListener);
        eventExecutor.shutdown();

        log.info("OVSC App Stopped");
    }

    // find the right OVS and call installDefaultRule() to push flow rules
    private void populateFlowRules() {

        bridgeID = DeviceId.deviceId(bridgeId);

        int flag = 0;

        // first check if there is a OVS switch with the target bridgeID
        for (Device device : deviceService.getDevices()) {
            if (device.id().equals(bridgeID)) {

                log.info("Populate flow rule for: {} ", bridgeId);
                flag = 1;

                // different treatment depending on if it is for MACSec VNF
                if (forMACSecVNF == false){

                    log.info("Flow rules are NOT for connecting a MACSec VNF");
                    // push four flow rules for normal VNFs
                    // one direction, two rules
                    installDefaultRule(PortNumber.portNumber(dataInterface), PortNumber.portNumber(vmInterfaceLeft),
                            leftVlan, (short) 0);
                    installDefaultRule(PortNumber.portNumber(vmInterfaceRight), PortNumber.portNumber(dataInterface),
                            leftVlan, rightVlan);
                    // the other direction, two rules
                    installDefaultRule(PortNumber.portNumber(dataInterface), PortNumber.portNumber(vmInterfaceRight),
                            rightVlan, (short) 0);
                    installDefaultRule(PortNumber.portNumber(vmInterfaceLeft), PortNumber.portNumber(dataInterface),
                            rightVlan, leftVlan);

                    log.info("Completed flow rule pushing for: {}", bridgeId);
                }

                // if it is for MACSec VNF
                else {
                    // TODO: TEST
                    // push four flow rules for MACSec VNF
                    // one direction, two rules
                    log.info("Flow rules are for connecting a MACSec VNF");
                    installMACSecRule(PortNumber.portNumber(dataInterface), leftVlan, (short) 0, PortNumber.portNumber(vmInterfaceLeft),
                            false, false, (short) 0);
                    installMACSecRule(PortNumber.portNumber(vmInterfaceRight), (short) 0, MACSecEthType, PortNumber.portNumber(dataInterface),
                            false, true, rightVlan);
                    // the other direction, two rules
                    installMACSecRule(PortNumber.portNumber(dataInterface), rightVlan, (short) 0, PortNumber.portNumber(vmInterfaceRight),
                            true, false, (short) 0);
                    installMACSecRule(PortNumber.portNumber(vmInterfaceLeft), leftVlan, (short) 0, PortNumber.portNumber(dataInterface),
                            false, false, (short) 0);

                    log.info("Completed flow rule pushing for: {}", bridgeId);
                }

            }
        }

        if (flag == 0) {
            log.error("Failed to populate flow rules to {}. No such device exists", bridgeId);
        }
    }

    // real action to install the flow rules to OVS for normal VNF
    private void installDefaultRule(PortNumber inPortNumber, PortNumber outPortNumber, short inVlan, short outVlan) {

        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder()
                    .matchInPort(inPortNumber)
                    .matchVlanId(VlanId.vlanId(inVlan));

        TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment.builder();

        // if need to swap vlan tag
        if (outVlan != (short) 0) {
                treatmentBuilder.setVlanId(VlanId.vlanId(outVlan));
        }

        TrafficTreatment treatment = treatmentBuilder.setOutput(outPortNumber)
                    .build();

        ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                .withSelector(selectorBuilder.build())
                .withTreatment(treatment)
                .withPriority(defaultPriority)
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .fromApp(appId)
                .makePermanent()
                .add();

        // use flowObjectiveService to push flow rule
        flowObjectiveService.forward(DeviceId.deviceId(bridgeId),
                forwardingObjective);
    }


    // real action to install the flow rules to OVS for MACSec VNF
    private void installMACSecRule(PortNumber inPortNumber, short inVlan, short inEthType, PortNumber outPortNumber, boolean outVLANPop, boolean outVLANPush, short outVlan) {
        //TODO: Test

        // build selector
        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();

        selectorBuilder.matchInPort(inPortNumber);

        if (inVlan != (short) 0) {
            selectorBuilder.matchVlanId(VlanId.vlanId(inVlan));
        }

        if (inEthType != (short) 0) {
            selectorBuilder.matchEthType(inEthType);
        }

        TrafficSelector selector = selectorBuilder.build();


        // build treatment
        TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment.builder();

        if (outVLANPop == true ) {
            treatmentBuilder.popVlan();
        }

        if (outVLANPush == true && outVlan != (short) 0) {
            treatmentBuilder.pushVlan();
            treatmentBuilder.setVlanId(VlanId.vlanId(outVlan));
        }

        treatmentBuilder.setOutput(outPortNumber);

        TrafficTreatment treatment = treatmentBuilder.build();

        // generate forwarding objective and push

        ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                .withSelector(selectorBuilder.build())
                .withTreatment(treatment)
                .withPriority(defaultPriority)
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .fromApp(appId)
                .makePermanent()
                .add();

        // use flowObjectiveService to push flow rule
        flowObjectiveService.forward(DeviceId.deviceId(bridgeId),
                forwardingObjective);

    }



    /**
     * Reads cfg information from config file.
     */
    private void readConfiguration() {

        OvscConfig config = configRegistry.getConfig(appId, OvscConfig.class);
        if (config == null) {
            log.debug("No valid configuration found");
            return;
        }

        log.info("Load OVSC configurations");

        bridgeId = config.bridgeId();
        forMACSecVNF = config.forMACSec();
        defaultPriority = config.defaultPriority();
        dataInterface = config.dataPlanePort();
        vmInterfaceLeft = config.vmPortLeft();
        vmInterfaceRight = config.vmPortRight();
        leftVlan = config.leftVlan();
        rightVlan = config.rightVlan();

        // call this function to populate rules
        populateFlowRules();

    }

    // register a Configuration file listener
    private class InternalConfigListener implements NetworkConfigListener {

        @Override
        public void event(NetworkConfigEvent event) {
            if (!event.configClass().equals(OvscConfig.class)) {
                log.info("App domain in the configure file is wrong");
                return;
            }

            switch (event.type()) {
                case CONFIG_ADDED:
                case CONFIG_UPDATED:
                    readConfiguration();
                    break;
                default:
                    break;
            }
        }
    }
}
