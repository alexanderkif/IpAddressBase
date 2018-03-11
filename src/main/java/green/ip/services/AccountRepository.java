package green.ip.services;


import green.ip.entity.Account;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AccountRepository extends MongoRepository<Account, String> {

    Account findByEmail(String email);
//    Stream<Account> findAllByPlaceContaining(String place);

}
