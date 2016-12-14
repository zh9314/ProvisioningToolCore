package TunnelConfiguration;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import Provisioning.ProvisioningCore;
import toscaTransfer.Connection.node;

public class EC2TunnelConf implements TunnelConf {
	
<<<<<<< HEAD
	public static final Map<String, String> endpointMap;
=======
	private static final Map<String, String> endpointMap;
>>>>>>> f1878db384bff1a13b888b42eca1d0a4a6e67b25
	static {
		Map<String, String> em = new HashMap<String, String>();
		em.put("ec2.us-east-1.amazonaws.com", "Virginia");
		em.put("ec2.us-west-1.amazonaws.com", "California");
		em.put("ec2.us-west-2.amazonaws.com", "Oregon");
		em.put("ec2.ap-south-1.amazonaws.com", "Mumbai");
		em.put("ec2.ap-southeast-1.amazonaws.com", "Singapore");
		em.put("ec2.ap-northeast-2.amazonaws.com", "Seoul");
		em.put("ec2.ap-southeast-2.amazonaws.com", "Sydney");
		em.put("ec2.ap-northeast-1.amazonaws.com", "Tokyo");
		em.put("ec2.eu-central-1.amazonaws.com", "Frankfurt");
		em.put("ec2.eu-west-1.amazonaws.com", "Ireland");
		em.put("ec2.sa-east-1.amazonaws.com", "Paulo");

		endpointMap = Collections.unmodifiableMap(em);
	}
	
	private String sshOption = "-o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null";
	
	
	public String generateConfFile(String st, String remotePubAddress, String remotePrivateAddress, 
			String localPrivateAddress, String subnet, String netmask, String linkName){
		java.util.Calendar cal = java.util.Calendar.getInstance();
		long currentMili = cal.getTimeInMillis();
		String confFileName = st+"_ec2_conf_"+currentMili+".sh";
		String confFilePath = ProvisioningCore.currentDir+confFileName;
		System.out.println("Generate EC2 conf path "+confFilePath);
		try {
			FileWriter fw = new FileWriter(confFilePath);
			fw.write("lp=`ifconfig eth0|grep 'inet addr'|awk -F'[ :]' '{print $13}'`\n");
			fw.write("ip tunnel add "+linkName+" mode ipip remote "+remotePubAddress+" local $lp\n");
			fw.write("ifconfig "+linkName+" "+localPrivateAddress+" netmask "+netmask+"\n");
			fw.write("route del -net "+subnet+" netmask "+netmask+" dev "+linkName+"\n");
			fw.write("route add -host "+remotePrivateAddress+" dev "+linkName+"\n");
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return confFileName;
	}
	
	public void runConf(String confName, node info, String st, String certDir){
		try {
			String location = endpointMap.get(info.domain);
			String prefixOS = "";
			if(info.OStype.toLowerCase().contains("ubuntu"))
				prefixOS = "ubuntu";
			else{
				System.out.println("Unknown OS for "+info.OStype);
				System.exit(-1);
			}
			
			java.util.Calendar cal = java.util.Calendar.getInstance();
			long currentMili = cal.getTimeInMillis();
			String runFile = "run_ec2_"+st+"_"+currentMili+".sh";
			String runFilePath = ProvisioningCore.currentDir+runFile;
			String confPath = ProvisioningCore.currentDir+confName;
			FileWriter fw = new FileWriter(runFilePath);
			fw.write("scp -i "+certDir+location+".pem "+sshOption+" "+confPath+" "+prefixOS+"@"+info.pub_address+":~/\n");
		    fw.write("ssh -i "+certDir+location+".pem "+sshOption+" "+prefixOS+"@"+info.pub_address+" \"sudo ./"+confName+" 0</dev/null 1>/dev/null 2>/dev/null\"\n");
		    fw.write("ssh -i "+certDir+location+".pem "+sshOption+" "+prefixOS+"@"+info.pub_address+" \"sudo rm ./"+confName+"\"\n");
		    fw.close();
			
	        Process ps = Runtime.getRuntime().exec("chmod +x "+confPath);  
			ps.waitFor();
			ps = Runtime.getRuntime().exec("chmod +x "+runFilePath);  
			ps.waitFor();
	        ps = Runtime.getRuntime().exec("sh "+runFilePath);  
			ps.waitFor();
			ps = Runtime.getRuntime().exec("rm "+runFilePath+" "+confPath);  
			ps.waitFor();
			System.out.println("Configuration for node "+info.name+" is done!");
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
