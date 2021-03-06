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
    private String id;
    private String net;
    private String owner;
    private String description;

}
