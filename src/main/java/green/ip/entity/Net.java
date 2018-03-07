package green.ip.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;


@Document(collection="nets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Net {

    @Id
    private String net;
    private String mask;
    private String owner;
    private String vlan;
    private String vlanName;
    private String description;

    public String getIpFromBinaryIp() {
        int i1;
        int i2;
        int i3;
        int i4;
        i1 = Integer.parseInt(this.net.substring(0, 8), 2);
        i2 = Integer.parseInt(this.net.substring(8, 16), 2);
        i3 = Integer.parseInt(this.net.substring(16, 24), 2);
        i4 = Integer.parseInt(this.net.substring(24, 32), 2);
        return i1 + "." + i2 + "." + i3 + "." + i4;
    }
}
