package green.ip.controllers;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import green.ip.entity.Ip;
import green.ip.entity.Net;
import green.ip.services.NetRepository;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import green.ip.services.IpRepository;

import javax.websocket.server.PathParam;
import java.util.*;

import static com.mongodb.client.model.Filters.*;
import static org.codehaus.groovy.runtime.InvokerHelper.asList;


@Controller
public class IndexController {

    @Autowired
    private IpRepository ipRepository;
    @Autowired
    private NetRepository netRepository;

    private MongoClient mongoClient = new MongoClient("localhost", 27017);
    MongoDatabase mongoDatabase = mongoClient.getDatabase("ipbase");
    MongoCollection ips = mongoDatabase.getCollection("ips");
//    MongoCollection nets = mongoDatabase.getCollection("nets");
    private int docsOnPage = 50;

    @RequestMapping(value = "/")
    public String home(Model model) {
        model.addAttribute("nets", netRepository.findAll());
        model.addAttribute("title", "home");
        return "home";
    }

    @RequestMapping(value = "/index")
    public String index(Model model, @ModelAttribute("page") String page,
                        @ModelAttribute("ipNet") String ipNet,
                        @ModelAttribute("fVlan") String fVlan,
                        @ModelAttribute("fType") String fType,
                        @ModelAttribute("fDevId") String fDevId,
                        @ModelAttribute("fDescr") String fDescr,
                        @ModelAttribute("fnet") String fnet,
                        @PathParam("net") String net,
                        @PathParam("mask") String mask) {

        if (page.equals("")) page = "1";
        Integer pageNumber = Integer.parseInt(page);
        List<Ip> ipList = new ArrayList<>();
        Bson query = new BasicDBObject();
        if (!fnet.equals("")) net = fnet;
//        if (!ipNet.equals("")) {
//            String ipNetBinary = Arrays.stream(ipNet.split("\\."))
//                    .mapToInt(Integer::parseInt)
//                    .mapToObj(Integer::toBinaryString)
//                    .reduce("", String::concat);
//            query = regex("_id", ipNetBinary);
//        }
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
        model.addAttribute("title", "index");
        return "index";
    }

    @RequestMapping(value = "/add", method = RequestMethod.GET)
    public String add(Model model) {
        model.addAttribute("title", "add");
        return "add";
    }

    @RequestMapping(value = "/ip/", method = RequestMethod.GET)
    public String ip(Model model, @PathParam("ip") String ip) {
        Ip result = ipRepository.getByIp(ip);
        Net net = netRepository.getByNet(result.getNet());
        model.addAttribute("mask", net.getMask());
        model.addAttribute("vlan", net.getVlan());
        model.addAttribute("ip", result);
        model.addAttribute("title", ip);
        return "card";
    }

    @RequestMapping(value = "/ip/", method = RequestMethod.POST)
    public String ipsave(Model model, @PathParam("ip") String ip,
                         @ModelAttribute("vlan") String vlan,
                         @ModelAttribute("deviceType") String deviceType,
                         @ModelAttribute("deviceId") String deviceId,
                         @ModelAttribute("description") String description) {
        Ip result = ipRepository.getByIp(ip);
        String reason = "changed:";
        Boolean letsChange = false;
//        if (!result.getVlan().equals(vlan)) {
//            reason += " VLAN " + result.getVlan() + " to " + vlan;
//            letsChange = true;
//        }
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
//            result.setVlan(vlan);
            result.setDeviceType(deviceType);
            result.setDeviceId(deviceId);
            result.setDescription(description);
            result.getHistory().add(new Document("data", Calendar.getInstance().getTime())
                    .append("reason", reason)
                    .append("user", "User1")
            );
            ipRepository.save(result);
        }
        model.addAttribute("ip", result);
        model.addAttribute("title", ip);
        return "card";
    }

    @RequestMapping(value = "/create", method = RequestMethod.POST)
    public String create(Model model, @ModelAttribute("ipNet") String ipNet,
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
        Net net = Net.builder()
                .net(iBinaryFull)
                .mask(mask)
                .vlan(vlan)
                .vlanName(vlanName)
                .owner(owner)
                .description(description)
                .build();
        netRepository.save(net);

        for (int i = 1; i < ipWML; i++) {
            iBinary = String.format(strFormat, Integer.toBinaryString(i)).replace(' ', '0');
            iBinaryFull = ipBinaryFull.substring(0, Integer.parseInt(mask)) + iBinary;
            Ip ip = Ip.builder()
                    .ip(iBinaryFull)
                    .net(net.getNet())
                    .deviceId("")
                    .deviceType("")
                    .description("")
                    .history(asList(new Document("data", Calendar.getInstance().getTime())
                            .append("reason", "create")
                            .append("user", "User1")
                    ))
                    .build();
            if (ipRepository.getByIp(ip.getIp()) == null) {
                ipRepository.save(ip);
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

    public Ip toIp(org.bson.Document document) {
        Ip ip = new Ip();
        if (document.get("_id") != null) {
            ip.setIp((String) document.get("_id"));
        }
//        if (document.get("vlan") != null) {
//            ip.setVlan((String) document.get("vlan"));
//        }
//        if (document.get("vlanName") != null) {
//            ip.setVlanName((String) document.get("vlanName"));
//        }
//        if (document.get("mask") != null) {
//            ip.setMask((String) document.get("mask"));
//        }
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
}
