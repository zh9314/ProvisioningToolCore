package toscaTransfer;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class toscaAddressAnalysis {

    public class name_address {

        public String name;
        public String pub_address;
        public String domain;
        public String OStype;

        ////following are mainly for deploying cluster master.
        public String role = "";     ////specify the role of the vm in container cluster.
    }

    public ArrayList<name_address> na;
    public String userAccountName;
    public String sshKeyPath;

    public void generateNameAddress(String toscaFilePath, String topologyName) {
        try {
            File file = new File(toscaFilePath);
//            YamlStream stream = Yaml.loadStream(file);
            org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml();
            Map<String, Object> hashMap = (Map<String, Object>) yaml.load(new FileInputStream(file));

//            for (Iterator iter = stream.iterator(); iter.hasNext();) {
//                HashMap hashMap = (HashMap) iter.next();
            for (Iterator iter2 = hashMap.entrySet().iterator(); iter2.hasNext();) {
                Map.Entry entry = (Map.Entry) iter2.next();
                Object key = entry.getKey();
                Object value = entry.getValue();
                String keyS = key.toString();
                String valueS = value.toString();
                String jsonValue = transfer2json(valueS);
                if (keyS.equals("components")) {
                    na = json2NameAddress(jsonValue, topologyName);
                }
                if (keyS.equals("publicKeyPath")) {
                    sshKeyPath = valueS;
                }
                if (keyS.equals("userName")) {
                    userAccountName = valueS;
                }
            }
//            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String transfer2json(String x) {
        String y = x.replace("=", ":");
        char[] org = new char[y.length()];
        org = y.toCharArray();
        char[] target = new char[2 * y.length()];
        int target_i = 0;
        for (int i = 0; i < y.length(); i++) {
            target[target_i++] = org[i];
            if (i + 1 < y.length() && org[i] == ':' && org[i + 1] != '[' && org[i + 1] != '{') {
                target[target_i++] = '\'';
            }

            if (i + 1 < y.length() && org[i + 1] == ',') {
                if (org[i] == '}' || org[i] == ']')
					; else {
                    int j = i + 2;
                    boolean find_semicolon = false;
                    while (j < y.length() && org[j] != ',') {
                        if (org[j] == ':') {
                            find_semicolon = true;
                            break;
                        }
                        j++;
                    }
                    if (find_semicolon) {
                        target[target_i++] = '\'';
                    }
                }
            }

            if (i + 1 < y.length() && org[i] != '}' && org[i] != ']' && (org[i + 1] == '}' || org[i + 1] == ']')) {
                target[target_i++] = '\'';
            }
        }
        target[target_i] = 0;
        return new String(target);
    }

    private ArrayList<name_address> json2NameAddress(String jsonString, String topologyName) throws JSONException {
        ArrayList<name_address> nameAddress = new ArrayList<>();
        JSONArray jsonTopologies = new JSONArray(jsonString);
        for (int i = 0; i < jsonTopologies.length(); i++) {
            JSONObject jsonTopology = jsonTopologies.getJSONObject(i);
            name_address tmp = new name_address();
            tmp.name = topologyName + "." + jsonTopology.getString("name");
            tmp.pub_address = jsonTopology.getString("public_address");
            tmp.domain = jsonTopology.getString("domain");
            tmp.OStype = jsonTopology.getString("OStype");
            if (jsonTopology.has("role")) {
                tmp.role = jsonTopology.getString("role");
            }
            nameAddress.add(tmp);
        }
        return nameAddress;
    }

}
