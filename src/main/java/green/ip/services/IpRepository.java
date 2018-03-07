package green.ip.services;

import org.springframework.data.mongodb.repository.MongoRepository;
import green.ip.entity.Ip;

public interface IpRepository extends MongoRepository<Ip, String> {
    public Ip getByIp(String ip);
}
