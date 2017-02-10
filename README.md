# ovsc

This ONOS app is used for populating OVS flow rules programmatically (instead of manually) in order to implement VLAN-based SFC for E-CORD L2VPN Use Case

Following is an example cfg file:

    {
    "apps" : {
            "org.onosproject.ovsc" : {
               "ovsc-cfg" : {
                 "bridgeId" : "of:0000000000000001",
                 "forMACSecVNF" : "false",
                 "dataPlanePort" : 26,
                 "vmPortLeft" : 30,
                 "vmPortRight" : 31,
                 "leftVlan" : 100,
                 "rightVlan" : 200,
                 "defaultPriority" : 60000
                 }
             }
          }
    }
