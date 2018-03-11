package green.ip.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "Accounts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account {

//    private String id;
    @Id
    private String email;
    private String password;
    private String phone;
    private boolean enabled;

}
