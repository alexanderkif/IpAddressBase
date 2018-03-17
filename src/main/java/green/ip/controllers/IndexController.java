package green.ip.controllers;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import green.ip.entity.Account;
import green.ip.entity.Ip;
import green.ip.entity.Net;
import green.ip.entity.SubNet;
import green.ip.services.AccountRepository;
import green.ip.services.NetRepository;
import green.ip.services.SubNetRepository;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.websocket.server.PathParam;
import java.security.Principal;
import java.util.*;

import static com.mongodb.client.model.Filters.*;
import static org.codehaus.groovy.runtime.InvokerHelper.asList;


@Controller
public class IndexController {

    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private PasswordEncoder encoder;
    @Autowired
    private NetRepository netRepository;
    @Autowired
    private SubNetRepository subNetRepository;

    private String suffix;

    @Autowired
    MongoClient mongoClient;

    MongoCollection ips;
    private int docsOnPage = 50;

    @RequestMapping(value = "/")
    public String home(Model model) {
        model.addAttribute("nets", netRepository.findAll());
        model.addAttribute("title", "home");
        return "home";
    }

    @RequestMapping(value = "/subnets")
    public String net(Model model,
                      @PathParam("id") String id) {
        this.suffix = netRepository.getById(id).getId();
        ips = mongoClient.getDatabase("fish").getCollection("ips"+this.suffix);
//        ips = mongoDatabase.getCollection("ips"+this.suffix);
        model.addAttribute("suffix", suffix);
        model.addAttribute("subnets", subNetRepository.getByNet(suffix));
        model.addAttribute("title", "subnets");
        return "subnets";
    }

    @RequestMapping(value = "/index")
    public String index(Model model, @ModelAttribute("page") String page,
                        @ModelAttribute("ipNet") String ipNet,
                        @ModelAttribute("fVlan") String fVlan,
                        @ModelAttribute("fType") String fType,
                        @ModelAttribute("fDevId") String fDevId,
                        @ModelAttribute("fDescr") String fDescr,
                        @ModelAttribute("fnet") String fnet,
                        @PathParam("net") String net) {

        if (page.equals("")) page = "1";
        Integer pageNumber = Integer.parseInt(page);
        List<Ip> ipList = new ArrayList<>();
        Bson query = new BasicDBObject();
        if (!fnet.equals("")) net = fnet;
        if (!fVlan.equals("")) query = and(query, eq("vlan", fVlan));
        if (!fType.equals("")) query = and(query, regex("deviceType", fType, "i"));
        if (!fDevId.equals("")) query = and(query, regex("deviceId", fDevId, "i"));
        if (!fDescr.equals("")) query = and(query, regex("description", fDescr, "i"));
        if (net!=null && !net.equals("")) query = and(query, eq("net", net));
        ips.find(query)
                .sort(new Document("_id", 1))
                .skip(docsOnPage * (pageNumber - 1))
                .limit(docsOnPage)
                .into(new ArrayList())
                .stream().map(s -> toIp((Document) s)).forEach(s -> ipList.add((Ip) s));

        Long pages = Math.round(Math.ceil(1.0 * ips.count(query) / docsOnPage));
        if (pages == 0) pages = 1L;

        model.addAttribute("pages", pages);
        model.addAttribute("page", page);
        model.addAttribute("ipList", ipList);
        model.addAttribute("net", net);
        model.addAttribute("suffix", suffix);
        model.addAttribute("title", "index");
        return "index";
    }

    @RequestMapping(value = "/addnet", method = RequestMethod.GET)
    public String addnet(Model model) {
        model.addAttribute("title", "addnet");
        return "addnet";
    }

    @RequestMapping(value = "/addsubnet", method = RequestMethod.GET)
    public String addsubnet(Model model) {
        model.addAttribute("title", "addsubnet");
        return "addsubnet";
    }

    @RequestMapping(value = "/ip/", method = RequestMethod.GET)
    public String ip(Model model, @PathParam("ip") String ip) {
        Ip result = toIp((Document) ips.find(new Document("_id", ip)).first());
        SubNet net = subNetRepository.getBySubnet(result.getSubnet());
        model.addAttribute("mask", net.getMask());
        model.addAttribute("vlan", net.getVlan());
        model.addAttribute("ip", result);
        model.addAttribute("title", ip);
        return "card";
    }

    @RequestMapping(value = "/ip/", method = RequestMethod.POST)
    public String ipsave(Model model, Principal principal,
                         @PathParam("ip") String ip,
                         @ModelAttribute("vlan") String vlan,
                         @ModelAttribute("deviceType") String deviceType,
                         @ModelAttribute("deviceId") String deviceId,
                         @ModelAttribute("description") String description) {
        Ip result = toIp((Document) ips.find(new Document("_id", ip)).first());
        String reason = "changed:";
        Boolean letsChange = false;
        if (!result.getDeviceType().equals(deviceType)) {
            reason += " TYPE " + result.getDeviceType() + " to " + deviceType;
            letsChange = true;
        }
        if (!result.getDeviceId().equals(deviceId)) {
            reason += " ID " + result.getDeviceId() + " to " + deviceId;
            letsChange = true;
        }
        if (!result.getDescription().equals(description)) {
            reason += " DESCR " + result.getDescription() + " to " + description;
            letsChange = true;
        }
        if (letsChange) {
            result.setDeviceType(deviceType);
            result.setDeviceId(deviceId);
            result.setDescription(description);
            result.getHistory().add(new Document("data", Calendar.getInstance().getTime())
                    .append("reason", reason)
                    .append("user", principal.getName())
            );
            ips.replaceOne(eq("_id", ip), toBson(result));
        }
        model.addAttribute("ip", result);
        model.addAttribute("title", ip);
        return "card";
    }

    @RequestMapping(value = "/createnet", method = RequestMethod.POST)
    public String createnet(Model model, @ModelAttribute("netName") String netName,
                               @ModelAttribute("owner") String owner,
                               @ModelAttribute("description") String description) {
        if (netName.equals("")){
            model.addAttribute("msg", "Net name can not be empty");
            return "addnet";
        }
        Net net = Net.builder()
                .net(netName)
                .owner(owner)
                .description(description)
                .build();
        netRepository.save(net);
        return "redirect:/";
    }

    @RequestMapping(value = "/createsubnet", method = RequestMethod.POST)
    public String createsubnet(Model model, Principal principal,
                         @ModelAttribute("ipNet") String ipNet,
                         @ModelAttribute("mask") String mask,
                         @ModelAttribute("vlan") String vlan,
                         @ModelAttribute("owner") String owner,
                         @ModelAttribute("vlanName") String vlanName,
                         @ModelAttribute("description") String description) {

        String ipBinaryFull = getBinaryIpFromIp(ipNet);

        if (ipNet.equals("") || ipBinaryFull.equals("wrong ip")) {
            model.addAttribute("ipNet", ipNet);
            model.addAttribute("ma", mask);
            model.addAttribute("vlan", vlan);
            model.addAttribute("vlanName", vlanName);
            model.addAttribute("owner", owner);
            model.addAttribute("description", description);
            model.addAttribute("msg", "Введите правильно адрес сети");
            return "add";
        }
        if (mask.equals("") || Integer.parseInt(mask) > 31) {
            model.addAttribute("ipNet", ipNet);
            model.addAttribute("ma", mask);
            model.addAttribute("vlan", vlan);
            model.addAttribute("vlanName", vlanName);
            model.addAttribute("owner", owner);
            model.addAttribute("description", description);
            model.addAttribute("msg", "Введите правильно маску сети");
            return "add";
        }

        String ipWithoutMask = ipBinaryFull.substring(Integer.parseInt(mask));
        Integer countIp = Integer.parseInt(ipWithoutMask, 2);

        if (countIp != 0) {
            model.addAttribute("ipNet", ipNet);
            model.addAttribute("ma", mask);
            model.addAttribute("vlan", vlan);
            model.addAttribute("vlanName", vlanName);
            model.addAttribute("owner", owner);
            model.addAttribute("description", description);
            model.addAttribute("msg", "Введите правильный адрес сети");
            return "add";
        }
        String ipWithoutMaskLast = "";
        for (int i = 0; i < ipWithoutMask.length(); i++) ipWithoutMaskLast += "1";

        List<Ip> ipList = new ArrayList<>();
        String strFormat = "%" + ipWithoutMask.length() + "s";
        String iBinary = "", iBinaryFull = "";
        int ipWML = Integer.parseInt(ipWithoutMaskLast, 2);

        iBinary = String.format(strFormat, Integer.toBinaryString(0)).replace(' ', '0');
        iBinaryFull = ipBinaryFull.substring(0, Integer.parseInt(mask)) + iBinary;
        SubNet subNet = SubNet.builder()
                .subnet(iBinaryFull)
                .mask(mask)
                .net(suffix)
                .vlan(vlan)
                .vlanName(vlanName)
                .owner(owner)
                .description(description)
                .build();
        subNetRepository.save(subNet);

        for (int i = 1; i < ipWML; i++) {
            iBinary = String.format(strFormat, Integer.toBinaryString(i)).replace(' ', '0');
            iBinaryFull = ipBinaryFull.substring(0, Integer.parseInt(mask)) + iBinary;
            Ip ip = Ip.builder()
                    .ip(iBinaryFull)
                    .subnet(subNet.getSubnet())
                    .deviceId("")
                    .deviceType("")
                    .description("")
                    .history(asList(new Document("data", Calendar.getInstance().getTime())
                            .append("reason", "create")
                            .append("user", principal.getName())
                    ))
                    .build();
            if (ips.find(new Document("_id", ip.getIp())).first() == null) {
                ips.insertOne(toBson(ip));
            } else {
                ipList.add(ip);
            }
        }
        model.addAttribute("title", "create");
        model.addAttribute("ipList", ipList);
        return "create";
    }

    private String getBinaryIpFromIp(String ip) {
        List<Integer> start = new ArrayList();
        Arrays.stream(ip.split("\\.")).mapToInt(Integer::parseInt).forEach(start::add);
        if (start.size() != 4 || start.get(0) > 255 || start.get(1) > 255 || start.get(2) > 255 || start.get(3) > 255) return "wrong ip";
        String binaryIp = "";
        for (int i = 0; i < 4; i++) {
            binaryIp += String.format("%8s", Integer.toBinaryString(start.get(i))).replace(' ', '0');
        }
        return binaryIp;
    }

    public Ip toIp(Document document) {
        Ip ip = new Ip();
        if (document.get("_id") != null) {
            ip.setIp((String) document.get("_id"));
        }
        if (document.get("net") != null) {
            ip.setSubnet((String) document.get("net"));
        }
        if (document.get("gate") != null) {
            ip.setGate((String) document.get("gate"));
        }
        if (document.get("deviceType") != null) {
            ip.setDeviceType((String) document.get("deviceType"));
        }
        if (document.get("deviceId") != null) {
            ip.setDeviceId((String) document.get("deviceId"));
        }
        if (document.get("dns") != null) {
            ip.setDns((String) document.get("dns"));
        }
        if (document.get("mac") != null) {
            ip.setMac((String) document.get("mac"));
        }
        if (document.get("cable") != null) {
            ip.setCable((String) document.get("cable"));
        }
        if (document.get("description") != null) {
            ip.setDescription((String) document.get("description"));
        }
        if (document.get("history") != null) {
            ip.setHistory((List<org.bson.Document>
                    ) document.get("history"));
        }
        return ip;
    }

    public Document toBson(Ip ip) {
        Document doc = new Document("_id", ip.getIp());
        if (ip.getSubnet() != null) {
            doc.append("net", ip.getSubnet());
        }
        if (ip.getGate() != null) {
            doc.append("gate", ip.getGate());
        }
        if (ip.getDeviceType() != null) {
            doc.append("deviceType",ip.getDeviceType());
        }
        if (ip.getDeviceId() != null) {
            doc.append("deviceId",ip.getDeviceId());
        }
        if (ip.getDns() != null) {
            doc.append("dns",ip.getDns());
        }
        if (ip.getMac() != null) {
            doc.append("mac",ip.getMac());
        }
        if (ip.getCable() != null) {
            doc.append("cable",ip.getCable());
        }
        if (ip.getDescription() != null) {
            doc.append("description",ip.getDescription());
        }
        if (ip.getHistory() != null) {
            doc.append("history", ip.getHistory());
        }
        return doc;
    }


    @RequestMapping("/login")
    public String login(Model model) {
        model.addAttribute("links", "login");
        model.addAttribute("titl", "Login");
        return "login";
    }

    @RequestMapping("/register")
    public String register(Model model, Principal principal) {
        String phone = "";
        try{
            Account account = accountRepository.findByEmail(principal.getName());
            phone = account.getPhone();
        }catch(Exception e){
            System.out.println("No user in repository "+e);
        }
        model.addAttribute("links", "register");
        model.addAttribute("titl", "Register");
        model.addAttribute("phone", phone);
        return "register";
    }

    @RequestMapping("/adduser")
    public String adduser(Model model, @ModelAttribute("username") String email,
                          @ModelAttribute("password") String password,
                          @ModelAttribute("phone") String phone,
                          @ModelAttribute("existing") String existing) {
        String li, titl;
        if ((accountRepository.findByEmail(email) == null || existing.equals("true"))
                && !Objects.equals(email, "") && !Objects.equals(password, "")) {
            accountRepository.save(Account.builder()
                    .email(email)
                    .phone(phone)
                    .password(encoder.encode(password))
                    .enabled(true)
                    .build()
            );
            li = "login";
            titl = "Added";
        } else {
            li = "login";
            titl = "Not added";
        }
        model.addAttribute("links", li);
        model.addAttribute("titl", titl);
        return "login";
    }
}
