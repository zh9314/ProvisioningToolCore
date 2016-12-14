package TunnelConfiguration;

import java.io.FileWriter;
import java.io.IOException;

import Provisioning.ProvisioningCore;
import toscaTransfer.Connection.node;

public class GENITunnelConf implements TunnelConf {
	
	private String sshOption = "-o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null";
	
	public String generateConfFile(String st, String remotePubAddress, String remotePrivateAddress, 
			String localPrivateAddress, String subnet, String netmask, String linkName){
		java.util.Calendar cal = java.util.Calendar.getInstance();
		long currentMili = cal.getTimeInMillis();
		String confFileName = st+"_geni_conf_"+currentMili+".sh";
		String confFilePath = ProvisioningCore.currentDir+confFileName;
		try {
			FileWriter fw = new FileWriter(confFilePath);
			fw.write("lp=`ifconfig eth0|grep 'inet addr'|awk -F'[ :]' '{print $13}'`\n");
			fw.write("ip tunnel add "+linkName+" mode ipip remote "+remotePubAddress+" local $lp\n");
			fw.write("ifconfig "+linkName+" "+localPrivateAddress+" netmask "+netmask+"\n");
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return confFileName;
	}
	
	////Here certDir is not actually the directory of the certificates. It is the file path.
	public void runConf(String confName, node info, String st, String certDir){
		try {
			String prefixOS = "root";
			
			java.util.Calendar cal = java.util.Calendar.getInstance();
			long currentMili = cal.getTimeInMillis();
			String runFile = "run_"+st+"_"+currentMili+".sh";
			String runFilePath = ProvisioningCore.currentDir+runFile;
			String confPath = ProvisioningCore.currentDir+confName;
			FileWriter fw = new FileWriter(runFilePath);
			fw.write("scp -i "+certDir+" "+sshOption+" "+confPath+" "+prefixOS+"@"+info.pub_address+":~/\n");
		    fw.write("ssh -i "+certDir+" "+sshOption+" "+prefixOS+"@"+info.pub_address+" \"./"+confName+" 0</dev/null 1>/dev/null 2>/dev/null\"\n");
		    fw.write("sleep 2s\n");
		    fw.write("ssh -i "+certDir+" "+sshOption+" "+prefixOS+"@"+info.pub_address+" \"rm ./"+confName+"\"\n");
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
