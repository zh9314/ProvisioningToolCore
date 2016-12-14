package toscaTransfer;

public class Connection {
	
	public class node {
		public String name;
		public String port_name;
		public String provider;
		public String netmask;
		public String pri_address;
		public String pub_address;
		public String domain = "";
		public String OStype = "";
	}
	
	public String name;
	public node source = new node();
	public node target = new node();
	public int bandwidth;
	public double latency;

}
