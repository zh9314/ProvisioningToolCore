package TunnelConfiguration;

import toscaTransfer.Connection.node;

public interface TunnelConf {
	public String generateConfFile(String st, String remotePubAddress, String remotePrivateAddress, 
			String localPrivateAddress, String subnet, String netmask, String linkName);
	
	public void runConf(String confPath, node info, String st, String certDir);  ///st means this is source or target

}
