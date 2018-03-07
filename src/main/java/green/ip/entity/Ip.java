package green.ip.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;


import java.util.List;

@Document(collection="ips")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ip {

    @Id
    private String ip;
    private String net;
    private String deviceType;
    private String deviceId;
    private String dns;
    private String gate;
    private String mac;
    private String cable;
    private String description;
    private List<org.bson.Document> history;

    public String getIpFromBinaryIp() {
        int i1;
        int i2;
        int i3;
        int i4;
        i1 = Integer.parseInt(this.ip.substring(0, 8), 2);
        i2 = Integer.parseInt(this.ip.substring(8, 16), 2);
        i3 = Integer.parseInt(this.ip.substring(16, 24), 2);
        i4 = Integer.parseInt(this.ip.substring(24, 32), 2);
        return i1 + "." + i2 + "." + i3 + "." + i4;
    }
}
