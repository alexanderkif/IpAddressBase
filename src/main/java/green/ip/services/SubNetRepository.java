package green.ip.services;

import green.ip.entity.SubNet;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface SubNetRepository extends MongoRepository<SubNet, String> {
    List<SubNet> getByNet(String net);
    SubNet getBySubnet(String subnet);
}
