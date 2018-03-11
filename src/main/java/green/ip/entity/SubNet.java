package green.ip.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;


@Document(collection="subnets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubNet {

    @Id
    private String id;
    private String subnet;
    private String mask;
    private String net;
    private String owner;
    private String vlan;
    private String vlanName;
    private String description;

    public String getIpFromBinaryIp() {
        int i1;
        int i2;
        int i3;
        int i4;
        i1 = Integer.parseInt(this.subnet.substring(0, 8), 2);
        i2 = Integer.parseInt(this.subnet.substring(8, 16), 2);
        i3 = Integer.parseInt(this.subnet.substring(16, 24), 2);
        i4 = Integer.parseInt(this.subnet.substring(24, 32), 2);
        return i1 + "." + i2 + "." + i3 + "." + i4;
    }

    public SubNet toSubNet(org.bson.Document document){
        if (document.get("_id") != null) {
            this.setSubnet((String) document.get("_id"));
        }
        if (document.get("mask") != null) {
            this.setMask((String) document.get("mask"));
        }
        if (document.get("owner") != null) {
            this.setOwner((String) document.get("owner"));
        }
        if (document.get("vlan") != null) {
            this.setVlan((String) document.get("vlan"));
        }
        if (document.get("vlanName") != null) {
            this.setVlanName((String) document.get("vlanName"));
        }
        if (document.get("description") != null) {
            this.setDescription((String) document.get("description"));
        }
        return this;
    }
}
