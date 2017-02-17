package Provisioning;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import TunnelConfiguration.EC2TunnelConf;
import TunnelConfiguration.GENITunnelConf;
import TunnelConfiguration.TunnelConf;
import toscaTransfer.Connection;
import toscaTransfer.Connection.node;
import toscaTransfer.Topology;
import toscaTransfer.toscaAddressAnalysis;
import toscaTransfer.toscaAddressAnalysis.name_address;
import toscaTransfer.toscaTotalAnalysis;

public class ProvisioningCore {

//    public static String ec2Jar = "EC2Provision.jar";
    public static String geniJar = "ExoGENIProvision.jar";

    public static String currentDir = "";

    private static Map<String, String> allconf = new HashMap<String, String>();
    private static Logger swLog;

    // converting to netmask
    private static final String[] netmaskConverter = {
        "128.0.0.0", "192.0.0.0", "224.0.0.0", "240.0.0.0", "248.0.0.0", "252.0.0.0", "254.0.0.0", "255.0.0.0",
        "255.128.0.0", "255.192.0.0", "255.224.0.0", "255.240.0.0", "255.248.0.0", "255.252.0.0", "255.254.0.0", "255.255.0.0",
        "255.255.128.0", "255.255.192.0", "255.255.224.0", "255.255.240.0", "255.255.248.0", "255.255.252.0", "255.255.254.0", "255.255.255.0",
        "255.255.255.128", "255.255.255.192", "255.255.255.224", "255.255.255.240", "255.255.255.248", "255.255.255.252", "255.255.255.254", "255.255.255.255"
    };

    /**
     * Convert netmask int to string (255.255.255.0 returned if nm > 32 or nm <
     * 1) @
     *
     *
     * param nm @return
     */
    public static String netmaskIntToString(int nm) {
        if ((nm > 32) || (nm < 1)) {
            return "255.255.255.0";
        } else {
            return netmaskConverter[nm - 1];
        }
    }

    /**
     * Convert netmask string to an integer (24-bit returned if no match)
     *
     * @param nm
     * @return
     */
    public static int netmaskStringToInt(String nm) {
        int i = 1;
        for (String s : netmaskConverter) {
            if (s.equals(nm)) {
                return i;
            }
            i++;
        }
        return 24;
    }

    public static void provisionTopology(Topology tmp, String topologyDir) {
        System.out.println("Provisioning topology of " + tmp.name + " from " + tmp.cloudProvider);
        swLog.log("INFO", "ProvisioningCore.provisionTopology", "Provisioning topology of " + tmp.name + " from " + tmp.cloudProvider);
        String topologyPath = topologyDir + tmp.name + ".yml";
        String cp = tmp.cloudProvider.toLowerCase();
        if (cp.equals("ec2")) {

//			try {
            String confPath = allconf.get("ec2");
            String[] args = new String[]{confPath, topologyPath};
            Provisioning.ARP.main(args);
//				Process ps = Runtime.getRuntime().exec("java -jar "+currentDir+ec2Jar+" "+confPath+" "+topologyPath);
//				ps.waitFor();
//			} catch (IOException | InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}

        } else if (cp.equals("exogeni") || cp.equals("geni")) {
            try {
                String confPath = allconf.get("exogeni");
                Process ps = Runtime.getRuntime().exec("java -jar " + currentDir + geniJar + " createSlice " + confPath + " tosca " + topologyPath);
                ps.waitFor();
            } catch (IOException | InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            System.out.println("Unknown cloud provider!");
        }
    }

    public static String getTopologyDir(String totalTPath) {
        int index = totalTPath.lastIndexOf('/');
        String topologyDir = "";
        if (index != -1) {
            topologyDir = totalTPath.substring(0, index + 1);
        }
        return topologyDir;
    }

    public static void getProvisionResults(String topologyDir, ArrayList<Connection> connections, ArrayList<Topology> topologies) {
        boolean getAll = false;
        ArrayList<name_address> totalNA = new ArrayList<name_address>();
        String topologyList = "";
        for (int i = 0; i < topologies.size(); i++) {
            topologyList += topologies.get(i).name + " ";
        }

        swLog.log("INFO", "ProvisioningCore.getProvisionResults", "The topology list is: " + topologyList);
        System.out.println("The topology list is: " + topologyList);
        ArrayList<String> deploy_input = new ArrayList<String>();
        while (!getAll) {
            getAll = true;
            for (int i = 0; i < topologies.size(); i++) {
                Topology tmpt = topologies.get(i);
                if (!tmpt.provisioned) {
                    String provisionedPath = topologyDir + tmpt.name + "_provisioned.yml";
                    System.out.println("To test whether the file exists! " + provisionedPath);
                    File f = new File(provisionedPath);
                    if (f.exists()) {
                        System.out.println("Topology " + tmpt.name + " is provisioned!");
                        swLog.log("INFO", "ProvisioningCore.getProvisionResults", "Topology " + tmpt.name + " is provisioned!");
                        toscaAddressAnalysis taa = new toscaAddressAnalysis();
                        taa.generateNameAddress(provisionedPath, tmpt.name);
                        for (int j = 0; j < taa.na.size(); j++) {
                            totalNA.add(taa.na.get(j));
                            System.out.println("Get address " + taa.na.get(j).pub_address + " of " + taa.na.get(j).name);
                            swLog.log("INFO", "ProvisioningCore.getProvisionResults", "Get address " + taa.na.get(j).pub_address + " of " + taa.na.get(j).name);
                            String roleOfNode = taa.na.get(j).role.toLowerCase();
                            if (roleOfNode.equals("master") || roleOfNode.equals("slave")) {
                                if (tmpt.cloudProvider.toLowerCase().contains("ec2")) {
                                    String locationName = EC2TunnelConf.endpointMap.get(taa.na.get(j).domain);
                                    String certDir = certDirFromConf("ec2", allconf.get("ec2"));
                                    String certPath = certDir + locationName + ".pem";
                                    deploy_input.add(taa.na.get(j).pub_address + " ubuntu " + certPath + " " + roleOfNode);
                                    swLog.log("INFO", "ProvisioningCore.getProvisionResults", taa.na.get(j).pub_address + " is the " + roleOfNode + " node in container cluster");

                                } else if (tmpt.cloudProvider.toLowerCase().contains("geni")) {
                                    String certPath = certDirFromConf("exogeni", allconf.get("exogeni"));
                                    deploy_input.add(taa.na.get(j).pub_address + " root " + certPath + " " + roleOfNode);
                                    swLog.log("INFO", "ProvisioningCore.getProvisionResults", taa.na.get(j).pub_address + " is the " + roleOfNode + " node in container cluster");
                                } else {
                                    swLog.log("WARN", "ProvisioningCore.getProvisionResults", "Unsported cloud provider " + tmpt.cloudProvider + " to install dockers");
                                }

                            } else if (roleOfNode.equals("null")) {
                                swLog.log("INFO", "ProvisioningCore.getProvisionResults", taa.na.get(j).pub_address + " is not in container cluster");
                            } else {
                                swLog.log("WARN", "ProvisioningCore.getProvisionResults", "The node of " + taa.na.get(j).pub_address + " is not configured correctly with the field of \"role\"");
                            }

                        }
                        tmpt.provisioned = true;
                    } else {
                        getAll = false;
                    }
                }
            }

            if (getAll) {
                System.out.println("getAll is true");
            } else {
                System.out.println("getAll is false");
            }

            if (!getAll) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

        }
        System.out.println("Complete provisioning!");

        ////generate the input file for the deploy agent
        try {
            FileWriter inputFw = new FileWriter(topologyDir + "file_kubernetes", false);
            for (int input_i = 0; input_i < deploy_input.size(); input_i++) {
                inputFw.write(deploy_input.get(input_i) + "\n");
            }
            inputFw.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        ////configure the connections among the sub-topologies
        for (int i = 0; i < connections.size(); i++) {
            Connection tmpc = connections.get(i);
            boolean findSPubAdd = false, findTPubAdd = false;
            String[] Snames = tmpc.source.name.split("\\.");
            String StName = Snames[0];
            String[] Tnames = tmpc.target.name.split("\\.");
            String TtName = Tnames[0];
            for (int j = 0; j < topologies.size(); j++) {
                if (topologies.get(j).name.equals(StName)) {
                    tmpc.source.provider = topologies.get(j).cloudProvider;
                }
                if (topologies.get(j).name.equals(TtName)) {
                    tmpc.target.provider = topologies.get(j).cloudProvider;
                }
            }

            if (tmpc.source.provider.equals("Unknown") || tmpc.target.provider.equals("Unknown")) {
                System.out.println("Sth wrong with the provider of the connections");
                swLog.log("ERROR", "ProvisioningCore.getProvisionResults",
                        "Sth wrong with the provider of the connections");
                System.exit(-1);
            }
            for (int j = 0; j < totalNA.size(); j++) {
                if (totalNA.get(j).name.equals(tmpc.source.name)) {
                    tmpc.source.pub_address = totalNA.get(j).pub_address;
                    tmpc.source.domain = totalNA.get(j).domain;
                    tmpc.source.OStype = totalNA.get(j).OStype;
                    findSPubAdd = true;
                }
                if (totalNA.get(j).name.equals(tmpc.target.name)) {
                    tmpc.target.pub_address = totalNA.get(j).pub_address;
                    tmpc.target.domain = totalNA.get(j).domain;
                    tmpc.target.OStype = totalNA.get(j).OStype;
                    findTPubAdd = true;
                }
                if (findSPubAdd && findTPubAdd) {
                    break;
                }
            }
            if (!(findSPubAdd && findTPubAdd)) {
                System.out.println("Sth wrong with the public address of the connections");
                swLog.log("ERROR", "ProvisioningCore.getProvisionResults", "Sth wrong with the public address of the connections");
                System.exit(-1);
            }
        }

    }

    private static String certDirFromConf(String provider, String confPath) {
        String keyDir = "";
        File conf = new File(confPath);
        try {
            BufferedReader in = new BufferedReader(new FileReader(conf));
            String line = null;
            String KeyDir = "";
            while ((line = in.readLine()) != null) {
                String[] cmd = line.split("=");
                if (provider.equals("ec2") && cmd[0].trim().toLowerCase().equals("keydir")) {
                    KeyDir = cmd[1];
                    break;
                }
                if (provider.equals("exogeni") && cmd[0].trim().toLowerCase().equals("sshprikeypath")) {
                    KeyDir = cmd[1];
                    break;
                }
            }
            keyDir = KeyDir;
            if (provider.equals("ec2")) {
                if (KeyDir.lastIndexOf('/') != KeyDir.length() - 1) {
                    keyDir += "/";
                }
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return keyDir;
    }

    private static String getSubnet(String netmask, String privateAddress) {
        String subnet = "";
        int netmaskNum = netmaskStringToInt(netmask);
        String[] subPriAddress = privateAddress.split("\\.");
        String combineAddress = "";
        for (int i = 0; i < subPriAddress.length; i++) {
            int subAddNum = Integer.valueOf(subPriAddress[i]);
            String bString = Integer.toBinaryString(subAddNum);
            int len = 8 - bString.length();
            for (int j = 0; j < len; j++) {
                bString = "0" + bString;
            }
            combineAddress += bString;
        }
        String binarySubnet = combineAddress.substring(0, netmaskNum);
        for (int i = 0; i < (32 - netmaskNum); i++) {
            binarySubnet += "0";
        }

        for (int i = 0; i < 4; i++) {
            String nums = binarySubnet.substring(i * 8, i * 8 + 8);
            int num = Integer.parseInt(nums, 2);
            if (i == 0) {
                subnet = num + "";
            } else {
                subnet += "." + num;
            }
        }

        return subnet;
    }

    public static void conf4Connections(ArrayList<Connection> connections) {
        System.out.println("Configuration for inter-domain connections!");
        for (int i = 0; i < connections.size(); i++) {
            Connection tmpc = connections.get(i);
            String netmask = tmpc.source.netmask;
            if (!netmask.contains(".")) {
                netmask = netmaskIntToString(Integer.valueOf(netmask));
            }
            String subnet = getSubnet(netmask, tmpc.source.pri_address);

            TunnelConf SrcTC = null, TargetTC = null;
            String TargetCertDir = "", SrcCertDir = "";
            if (tmpc.source.provider.toLowerCase().equals("ec2")) {
                SrcTC = new EC2TunnelConf();
                SrcCertDir = certDirFromConf("ec2", allconf.get("ec2"));
            } else if (tmpc.source.provider.toLowerCase().contains("geni")) {
                SrcTC = new GENITunnelConf();
                SrcCertDir = certDirFromConf("exogeni", allconf.get("exogeni"));
            } else {
                System.out.println("Unknown Provider " + tmpc.source.provider);
                swLog.log("ERROR", "ProvisioningCore.conf4Connections", "Unknown Provider " + tmpc.source.provider);
                System.exit(-1);
            }

            swLog.log("DEBUG", "ProvisioningCore.conf4Connections", "Get source node certificates directory or path! :: " + SrcCertDir);
            String srcConfFile = SrcTC.generateConfFile("source", tmpc.target.pub_address, tmpc.target.pri_address,
                    tmpc.source.pri_address, subnet, netmask, tmpc.source.port_name);
            SrcTC.runConf(srcConfFile, tmpc.source, "source", SrcCertDir);

            if (tmpc.target.provider.toLowerCase().equals("ec2")) {
                TargetTC = new EC2TunnelConf();
                TargetCertDir = certDirFromConf("ec2", allconf.get("ec2"));
            } else if (tmpc.source.provider.toLowerCase().contains("geni")) {
                TargetTC = new GENITunnelConf();
                TargetCertDir = certDirFromConf("exogeni", allconf.get("exogeni"));
            } else {
                System.out.println("Unknown Provider for " + tmpc.target.provider);
                swLog.log("ERROR", "ProvisioningCore.conf4Connections", "Unknown Provider for " + tmpc.target.provider);
                System.exit(-1);
            }

            swLog.log("DEBUG", "ProvisioningCore.conf4Connections", "Get target node certificates directory or path! :: " + TargetCertDir);
            String targetConfFile = TargetTC.generateConfFile("target", tmpc.source.pub_address, tmpc.source.pri_address,
                    tmpc.target.pri_address, subnet, netmask, tmpc.target.port_name);
            TargetTC.runConf(targetConfFile, tmpc.target, "target", TargetCertDir);

        }

    }

    private static boolean analysisArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("&")) {
                continue;
            }
            String[] arg = args[i].split("=");
            String argkey = arg[0].toLowerCase();
            if (!argkey.equals("ec2") && !argkey.equals("exogeni") && !argkey.equals("topology")
                    && !argkey.equals("logdir")) {
                return false;
            }
            allconf.put(argkey, arg[1]);
        }
        if (!allconf.containsKey("topology") || !allconf.containsKey("logdir")) {
            return false;
        }
        return true;

    }

    ///make the dir path always end up with character '/'
    private static String rephaseTheDir(String inputDir) {
        String outputDir = inputDir;
        if (inputDir.lastIndexOf('/') != inputDir.length() - 1) {
            outputDir += "/";
        }
        return outputDir;
    }

    ////absolute path of current directory
    private static String getCurrentDir() {
        String curDir = new ProvisioningCore().getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
        int index = curDir.lastIndexOf('/');
        return curDir.substring(0, index + 1);
    }

    /////java -jar xx.jar ec2=/xx/xx/ec2.conf exogeni=/xx/xx/exogeni.conf topology=/xx/xx.yml logDir=/xx/xx.yml
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        currentDir = getCurrentDir();
        System.out.println(currentDir);
        if (!analysisArgs(args)) {
            System.out.println("Error: argument is wrong!");
            return;
        }
        swLog = new Logger(rephaseTheDir(allconf.get("logdir")) + "total.log");
        String mainTopologyPath = allconf.get("topology");
        toscaTotalAnalysis tta = new toscaTotalAnalysis(swLog);
        tta.generateTopology(mainTopologyPath);
        String topologyDir = getTopologyDir(mainTopologyPath);
        for (int i = 0; i < tta.topologies.size(); i++) {
            provisionTopology(tta.topologies.get(i), topologyDir);
        }
        getProvisionResults(topologyDir, tta.connections, tta.topologies);
        conf4Connections(tta.connections);

    }

}
