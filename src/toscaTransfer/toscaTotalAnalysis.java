package toscaTransfer;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.ho.yaml.Yaml;
import org.ho.yaml.YamlStream;
import org.json.JSONArray;
import org.json.JSONObject;

import Provisioning.Logger;

public class toscaTotalAnalysis {
	public ArrayList<Topology> topologies;
	public ArrayList<Connection> connections;
	
	private static Logger swLog;
	
	public toscaTotalAnalysis(Logger log){
		this.swLog = log;
	}

	public void generateTopology(String toscaFilePath){
		try {
            File file = new File(toscaFilePath);
            YamlStream stream = Yaml.loadStream(file);
            boolean find_conn = false;
            for (Iterator iter = stream.iterator(); iter.hasNext();) {
                HashMap hashMap = (HashMap) iter.next();
                for (Iterator iter2 = hashMap.entrySet().iterator(); iter2.hasNext();) {
                    Map.Entry entry = (Map.Entry) iter2.next();
                    Object key = entry.getKey();
                    Object value = entry.getValue();
                    String keyS = key.toString();
                    String valueS = value.toString();
                    String jsonValue = transfer2json(valueS);
                    if(keyS.equals("topologies")){
                    	topologies = json2topology(jsonValue);
                    }
                    if(keyS.equals("connections")){
                    	connections = json2connection(jsonValue);
                    	find_conn = true;
                    }
                }
            }if(!find_conn)
            	connections = new ArrayList<Connection>();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
	}
	
	
	private String transfer2json(String x){
		String y = x.replace("=", ":");
		char [] org = new char[y.length()];
		org = y.toCharArray();
		char [] target = new char[2*y.length()];
		int target_i = 0;
		for(int i = 0 ; i<y.length(); i++){
			target[target_i++] = org[i];
			if(i+1 < y.length() && org[i] == ':' && org[i+1] != '[' && org[i+1] != '{')
				target[target_i++] = '\'';
			
			if(i+1 < y.length() && org[i+1] == ','){
				if(org[i] == '}' || org[i] == ']')
					;
				else{
					int j = i+2;
					boolean find_semicolon = false;
					while(j < y.length() && org[j] != ','){
						if(org[j] == ':'){
							find_semicolon = true;
							break;
						}
						j++;
					}
					if(find_semicolon)
						target[target_i++] = '\'';
				}
			}
			
			if(i+1<y.length() && org[i] != '}' && org[i] != ']' && (org[i+1] == '}' || org[i+1] == ']'))
				target[target_i++] = '\'';
		}
		target[target_i] = 0;
		return new String(target);
	}
	
	private ArrayList<Topology> json2topology(String jsonString){
		ArrayList<Topology> topologySet = new ArrayList<Topology>();
		JSONArray jsonTopologies = new JSONArray(jsonString);
		for(int i = 0 ; i<jsonTopologies.length() ; i++){
			JSONObject jsonLink = jsonTopologies.getJSONObject(i);
			Topology tmp = new Topology();
			tmp.name = jsonLink.getString("topology");
			tmp.cloudProvider = jsonLink.getString("cloudProvider");
			topologySet.add(tmp);
		}
		return topologySet;
	}
	
	private ArrayList<Connection> json2connection(String jsonString){
		ArrayList<Connection> connectionSet = new ArrayList<Connection>();
		JSONArray jsonConnections = new JSONArray(jsonString);
		for(int i = 0 ; i<jsonConnections.length() ; i++){
			JSONObject jsonConnection = jsonConnections.getJSONObject(i);
			Connection tmp = new Connection();
			tmp.name = jsonConnection.getString("name");
			tmp.bandwidth = jsonConnection.getInt("bandwidth");
			tmp.latency = jsonConnection.getDouble("latency");
			JSONObject jsonSource = jsonConnection.getJSONObject("source");
			tmp.source.name = jsonSource.getString("component_name");
			tmp.source.port_name = tmp.name+"."+jsonSource.getString("port_name");
			tmp.source.netmask = jsonSource.getString("netmask");
			tmp.source.pri_address = jsonSource.getString("address");
			tmp.source.pub_address = "TBD";
			tmp.source.provider = "Unknown";
			JSONObject jsonTarget = jsonConnection.getJSONObject("target");
			tmp.target.name = jsonTarget.getString("component_name");
			tmp.target.port_name = tmp.name+"."+jsonTarget.getString("port_name");
			tmp.target.netmask = jsonTarget.getString("netmask");
			tmp.target.pri_address = jsonTarget.getString("address");
			tmp.target.pub_address = "TBD";
			tmp.target.provider = "Unknown";
			connectionSet.add(tmp);
		}
		return connectionSet;
	}
	
	
	
}
