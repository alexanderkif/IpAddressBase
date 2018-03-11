package green.ip.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;


import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ip extends org.bson.Document{

    @Id
    private String ip;
    private String subnet;
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
