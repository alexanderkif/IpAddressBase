package green.ip.services;

import green.ip.entity.Net;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface NetRepository extends MongoRepository<Net, String> {
    public Net getByNet(String net);
}
